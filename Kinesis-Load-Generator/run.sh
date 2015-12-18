#!/usr/bin/env bash

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <filename.gz>"
    exit
fi

mvn compile exec:java -Dexec.mainClass=com.amazonaws.services.blog.kinesis.loadgenerator.ClickEventsToKinesisTestDriver $1
