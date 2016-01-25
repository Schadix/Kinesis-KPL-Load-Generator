package com.amazonaws.services.blog.kinesis.loadgenerator;


import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MyDaemon implements Daemon {
  private static Logger log = LogManager.getLogger();

  private Thread myThread;
  private boolean stopped = false;
  private boolean lastOneWasATick = false;

  @Override
  public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {
        /*
         * Construct objects and initialize variables here.
         * You can access the command line arguments that would normally be passed to your main()
         * method as follows:
         */
    String[] args = daemonContext.getArguments();

    myThread = new Thread() {
      private long lastTick = 0;

      @Override
      public synchronized void start() {
        MyDaemon.this.stopped = false;
        super.start();
      }

      @Override
      public void run() {
        while (!stopped) {
          long now = System.currentTimeMillis();
          if (now - lastTick >= 1000) {
            log.info(!lastOneWasATick ? "tick" : "tock");
            lastOneWasATick = !lastOneWasATick;
            lastTick = now;
          }
        }
      }
    };
  }

  @Override
  public void start() throws Exception {
    myThread.start();
  }

  @Override
  public void stop() throws Exception {
    stopped = true;
    try {
      myThread.join(1000);
    } catch (InterruptedException e) {
      System.err.println(e.getMessage());
      throw e;
    }
  }

  @Override
  public void destroy() {
    myThread = null;
  }
}