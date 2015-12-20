package com.amazonaws.services.blog.kinesis.loadgenerator;

import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public class KPLClickEventsToKinesis extends AbstractClickEventsToKinesis {
  private final KinesisProducer kinesis;
  protected static String streamName = "not defined";
  protected static String region = "us-east-1";


  public KPLClickEventsToKinesis(BlockingQueue<ClickEvent> inputQueue) {
    super(inputQueue);
    KinesisProducerConfiguration kinesisProducerConfiguration = new KinesisProducerConfiguration();
    kinesisProducerConfiguration.setAggregationEnabled(false);
    kinesis = new KinesisProducer(kinesisProducerConfiguration
        .setRegion(region)
        .setRecordMaxBufferedTime(5000));
  }

  @Override
  protected void runOnce() throws Exception {
    ClickEvent event = inputQueue.take();
    String partitionKey = event.getSessionId();
    ByteBuffer data = ByteBuffer.wrap(
        event.getPayload().getBytes("UTF-8"));
    while (kinesis.getOutstandingRecordsCount() > 5e4) {
      Thread.sleep(1);
    }
    kinesis.addUserRecord(streamName, partitionKey, data);
    recordsPut.getAndIncrement();
  }

  @Override
  public long recordsPut() {
    return super.recordsPut() - kinesis.getOutstandingRecordsCount();
  }

  @Override
  public void stop() {
    super.stop();
    kinesis.flushSync();
    kinesis.destroy();
  }
}
