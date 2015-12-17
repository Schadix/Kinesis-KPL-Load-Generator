#!/usr/bin/env bash
mvn compile exec:java -Dexec.mainClass=com.amazonaws.services.blog.kinesis.loadgenerator.ClickEventsToKinesisTestDriver
