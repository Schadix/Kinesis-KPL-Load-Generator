#!/usr/bin/env bash

if [ "$#" -ne 4 ]; then
    echo "Usage: $0 <filename.gz> <region> <streamname> <rate-limit>"
    exit
fi

FILENAME=$1
REGION=$2
STREAMNAME=$3
RATELIMIT=$4

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/../

rm -rf test/logs
rm test/*

echo "jsvc -jvm server -home $JAVA_HOME \
-cwd `pwd`/test/ \
-cp `pwd`/target/kinesis-load-generator-1.0-SNAPSHOT.jar \
-user root -outfile out.log -errfile error.log \
-pidfile `pwd`/test/mydaemon.pid \
-debug \
com.amazonaws.services.blog.kinesis.loadgenerator.DaemonProcess \
$FILENAME $REGION $STREAMNAME
"

jsvc -jvm server -home $JAVA_HOME \
-cwd `pwd`/test/ \
-cp `pwd`/target/kinesis-load-generator-1.0-SNAPSHOT.jar \
-outfile out.log -errfile error.log \
-pidfile `pwd`/test/mydaemon.pid \
-debug \
com.amazonaws.services.blog.kinesis.loadgenerator.DaemonProcess \
$FILENAME $REGION $STREAMNAME $RATELIMIT
