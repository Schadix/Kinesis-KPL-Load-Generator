package com.amazonaws.services.blog.kinesis.loadgenerator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractClickEventsToKinesis implements Runnable {
    protected final static String STREAM_NAME = "spark-example";
    protected final static String REGION = "us-east-1";

    protected final BlockingQueue<ClickEvent> inputQueue;
    protected volatile boolean shutdown = false;
    protected final AtomicLong recordsPut = new AtomicLong(0);

    protected AbstractClickEventsToKinesis(
            BlockingQueue<ClickEvent> inputQueue) {
        this.inputQueue = inputQueue;
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                runOnce();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public long recordsPut() {
        return recordsPut.get();
    }

    public void stop() {
        shutdown = true;
    }

    protected abstract void runOnce() throws Exception;
}
