package com.amazonaws.services.blog.kinesis.loadgenerator;

import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * Created by schadem on 12/17/15.
 */
public class DaemonProcess implements Daemon {

  private static Random RANDOM = new Random();

  private Thread sendThread;
  private boolean stopped = false;

  private List<String> httpLog = new ArrayList<>();
  private AtomicInteger position = new AtomicInteger(0);

  private final BlockingQueue<ClickEvent> events = new ArrayBlockingQueue<>(65536);
  private final ExecutorService exec = Executors.newCachedThreadPool();
  final LocalDateTime testStart = LocalDateTime.now();

  KinesisProducerConfiguration config = new KinesisProducerConfiguration();

  AbstractClickEventsToKinesis worker;

  private void readGzipFile(String filename) throws IOException {
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
  }

  synchronized private ClickEvent generateClickEvent() {
    byte[] id = new byte[13];
    RANDOM.nextBytes(id);
    if (position.get() > httpLog.size() - 1) {
      System.out.println(
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
    if (args.length != 3) {
      System.out.println("Usage: command <filename.gz> <AWS Region> <Streamname>");
      System.exit(1);
    } ;

    System.out.println(String.format("file: %s, region: %s, stream: %s", args[0], args[1], args[2]));
    readGzipFile(args[0]);
    config.setRegion(args[1]);
    config.setAggregationEnabled(false);
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
            }
            Thread.sleep(1000);
          }

          exec.submit(() -> {
            while (!stopped) {
              try {
                events.put(generateClickEvent());
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            }

          });
        } catch (Exception e) {
          e.printStackTrace();
          System.exit(1);
        }
      }
    };

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

          System.out.println(String.format(
              "%d seconds, %d records total, %.2f RPS (avg last 10s), position: %s, httpLog.size(): %s, event.size()" +
                  ": %s",
              seconds, records,
              rps, position.get(), httpLog.size(), events.size()));
          Thread.sleep(1000);
        }
        System.out.println("Finished.");
      } catch (Exception e) {
        e.printStackTrace();
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
