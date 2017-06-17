#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/../

mvn package

VERSION=`mvn  org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v INFO`

sed "s/<<VERSION>>/$VERSION/g" scripts/kinesis-load-generator > target/kinesis-load-generator