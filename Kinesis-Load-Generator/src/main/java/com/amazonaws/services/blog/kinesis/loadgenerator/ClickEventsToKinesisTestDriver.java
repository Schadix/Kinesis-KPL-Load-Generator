package com.amazonaws.services.blog.kinesis.loadgenerator;

import org.apache.commons.lang.StringUtils;

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
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

public class ClickEventsToKinesisTestDriver {
  private static Random RANDOM = new Random();

  private static final Duration TEST_DURATION = Duration.ofSeconds(180);
  private static List<String> httpLog = new ArrayList<>();
  private static AtomicInteger position = new AtomicInteger(0);

  private static ClickEvent generateClickEvent() {
    byte[] id = new byte[13];
    RANDOM.nextBytes(id);
    if (position.get()>httpLog.size()){
      position.set(0);
    }
    String data = httpLog.get(position.getAndIncrement());
    return new ClickEvent(DatatypeConverter.printBase64Binary(id), data);
  }

  private static void readGzipFile(String filename) throws IOException {
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

  public static void main(String[] args) throws Exception {
    if (args.length<1){
      System.out.println("Usage: ClickEventsToKinesisTestDriver <filename as gz>");
      System.exit(1);
    }

    final BlockingQueue<ClickEvent> events = new ArrayBlockingQueue<ClickEvent>(65536);
    final ExecutorService exec = Executors.newCachedThreadPool();

    readGzipFile(args[0]);

    System.out.println(String.format("length of httpLog: %s", httpLog.size()));
    // Change this line to use a different implementation
    final AbstractClickEventsToKinesis worker = new AdvancedKPLClickEventsToKinesis(events);
    exec.submit(worker);

    // Warm up the KinesisProducer instance
    if (worker instanceof KPLClickEventsToKinesis) {
      for (int i = 0; i < 200; i++) {
        events.offer(generateClickEvent());
      }
      Thread.sleep(1000);
    }

    final LocalDateTime testStart = LocalDateTime.now();
    final LocalDateTime testEnd = testStart.plus(TEST_DURATION);

    exec.submit(() -> {
      try {
        while (LocalDateTime.now().isBefore(testEnd)) {
          events.put(generateClickEvent());
        }

        worker.stop();
        // This will unblock worker if it's blocked on the queue
        events.offer(generateClickEvent());
        exec.shutdown();
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    });

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
              "%d seconds, %d records total, %.2f RPS (avg last 10s)",
              seconds,
              records,
              rps));
          Thread.sleep(1000);
        }
        System.out.println("Finished.");
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }).start();
  }
}
