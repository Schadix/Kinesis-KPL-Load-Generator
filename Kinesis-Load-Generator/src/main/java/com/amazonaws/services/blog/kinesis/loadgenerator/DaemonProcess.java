package com.amazonaws.services.blog.kinesis.loadgenerator;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * Created by schadem on 12/17/15.
 */
public class DaemonProcess implements Daemon {

  private static Logger log = LogManager.getLogger();

  private static Random RANDOM = new Random();

  private Thread sendThread;
  private Thread configUpdateThread;
  private boolean stopped = false;

  private List<String> httpLog = new ArrayList<>();
  private AtomicInteger position = new AtomicInteger(0);
  private static String ConfigDDBTable = "configDDBTable";
  private static String configRateLimitDDBKey = "rateLimit";

  private final BlockingQueue<ClickEvent> events = new ArrayBlockingQueue<>(65536);
  private final ExecutorService exec = Executors.newCachedThreadPool();
  final LocalDateTime testStart = LocalDateTime.now();

  KinesisProducerConfiguration config = new KinesisProducerConfiguration();
  static ConcurrentHashMap<String, String> loadConfig =
      new ConcurrentHashMap<>(8, 0.9f, 1);

  AbstractClickEventsToKinesis worker;

  private boolean readGzipFile(String filename) {
    try {
      BufferedReader is = new BufferedReader(
          new InputStreamReader(
              new GZIPInputStream(
                  new FileInputStream(filename))));
      String line;
      // Now read lines of text: the BufferedReader puts them in lines,
      // the InputStreamReader does Unicode conversion, and the
      // GZipInputStream "gunzip"s the data from the FileInputStream.
      while ((line = is.readLine()) != null)
        httpLog.add(line);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return false;
    }
    log.info(String.format("Reading input file %s with %s lines succeeded.", filename, httpLog.size()));
    return true;
  }

  synchronized private ClickEvent generateClickEvent() {
    byte[] id = new byte[13];
    RANDOM.nextBytes(id);
    if (position.get() > httpLog.size() - 1) {
      log.info(
          String.format("in position.set(0 - pos: %s, httpLog.size: %s)", position.get(), httpLog.size()));
      position.set(0);
    }
    String data = httpLog.get(position.getAndIncrement());
    return new ClickEvent(DatatypeConverter.printBase64Binary(id), data);
  }

  @Override
  public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
        /*
         * Construct objects and initialize variables here.
         * You can access the command line arguments that would normally be passed to your main()
         * method as follows:
         */
    String[] args = daemonContext.getArguments();
    if (args.length != 5) {
      log.info("Usage: command <filename.gz> <AWS Region> <Streamname> <Rate-Limit> <Configuration-DDB-Table>");
      System.exit(1);
    }

    log.info(String.format("file: %s, region: %s, stream: %s, rate: %s, config-ddb: %s", args[0], args[1], args[2],
        args[3], args[4]));
    if (!readGzipFile(args[0])) {
      log.error("Exit with error. Reading input file failed.");
      System.exit(1);
    }

    config.setRegion(args[1]);
    config.setAggregationEnabled(false);
//    TODO: RateLimit didn't work, using it to pause the thread now
//    config.setRateLimit(Long.parseLong(args[3]));
    loadConfig.put(ConfigDDBTable, args[4]);
    loadConfig.put(configRateLimitDDBKey, args[3]);
    worker = new AdvancedKPLClickEventsToKinesis(events, config);
    worker.setStreamName(args[2]);

    position.set(0);

    sendThread = new Thread() {

      @Override
      public synchronized void start() {
        DaemonProcess.this.stopped = false;
        super.start();
      }

      @Override
      public void run() {

        try {
          exec.submit(worker);

          if (worker instanceof AdvancedKPLClickEventsToKinesis) {
            for (int i = 0; i < 200; i++) {
              events.offer(generateClickEvent());
              long rateLimit = Long.valueOf(loadConfig.get(configRateLimitDDBKey));
              Thread.sleep(1000 / rateLimit, (int) (1000 % rateLimit));
            }
            Thread.sleep(1000);
          }

          exec.submit(() -> {
            while (!stopped) {
              try {
                events.put(generateClickEvent());
                long rateLimit = Long.valueOf(loadConfig.get(configRateLimitDDBKey));
                Thread.sleep(1000 / rateLimit, (int) (1000 % rateLimit));
              } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
              }
            }

          });
        } catch (Exception e) {
          log.error(e.getMessage(), e);
          System.exit(1);
        }
      }
    };

    new Thread(() -> {
      try {
        while (!exec.isTerminated()) {
          DynamoDB dynamoDB = new DynamoDB(new AmazonDynamoDBClient());
          Table table = dynamoDB.getTable(loadConfig.get(ConfigDDBTable));
          Item rateLimitDDB = table.getItem("id", configRateLimitDDBKey);
          String oldValue = loadConfig.get(configRateLimitDDBKey);
          String newValue = rateLimitDDB.getString("value");
          loadConfig.put(configRateLimitDDBKey, newValue);
          if (!oldValue.equals(newValue)) {
            log.info(String.format("RateLimit changed from %s to %s", oldValue, newValue));
          }
          Thread.sleep(10000);
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        System.exit(1);
      }
    }).start();

    // This reports the average records per second over a 10 second sliding window
    new Thread(() -> {
      Map<Long, Long> history = new TreeMap<>();
      try {
        while (!exec.isTerminated()) {
          long seconds = Duration.between(testStart, LocalDateTime.now()).getSeconds();
          long records = worker.recordsPut();
          history.put(seconds, records);

          long windowStart = seconds - 10;
          long recordsAtWinStart =
              history.containsKey(windowStart) ? history.get(windowStart) : 0;
          double rps = (double) (records - recordsAtWinStart) / 10;

          log.info(String.format(
              "%d seconds, %d records total, %.2f RPS (avg last 10s), position: %s, httpLog.size(): %s, event.size()" +
                  ": %s",
              seconds, records,
              rps, position.get(), httpLog.size(), events.size()));
          Thread.sleep(1000);
        }
        log.info("Finished.");
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        System.exit(1);
      }
    }).start();

  }

  @Override
  public void start() throws Exception {
    sendThread.start();
  }

  @Override
  public void stop() throws Exception {
    stopped = true;
    worker.stop();
    // This will unblock worker if it's blocked on the queue
    events.offer(generateClickEvent());
    exec.shutdown();
  }

  @Override
  public void destroy() {
    sendThread = null;
  }
}
