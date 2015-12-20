package com.amazonaws.services.blog.kinesis.loadgenerator;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractClickEventsToKinesis implements Runnable {
    protected final BlockingQueue<ClickEvent> inputQueue;
    protected volatile boolean shutdown = false;
    protected final AtomicLong recordsPut = new AtomicLong(0);

    protected String streamName = "undefined";

    public AbstractClickEventsToKinesis setStreamName(String streamName) {
        this.streamName = streamName;
        return this;
    }


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
