#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

jsvc -jvm server -home $JAVA_HOME \
-cwd `pwd`/../test/ \
-cp `pwd`/../target/kinesis-load-generator-1.0-SNAPSHOT.jar \
-user root -outfile out.log -errfile error.log \
-pidfile `pwd`/../test/mydaemon.pid \
-debug \
-stop \
com.amazonaws.services.blog.kinesis.loadgenerator.DaemonProcess