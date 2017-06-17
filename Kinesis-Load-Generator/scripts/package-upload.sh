#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/../

mvn package

VERSION=`mvn  org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v INFO`

sed "s/<<VERSION>>/$VERSION/g" scripts/kinesis-load-generator > target/kinesis-load-generator

if [[ $? -ne 0 ]] ; then exit 1 ; fi
aws s3 cp target/kinesis-load-generator-$VERSION.jar s3://sdx-kinesis-load/$VERSION/
aws s3 cp target/kinesis-load-generator s3://sdx-kinesis-load/$VERSION/
aws s3 cp scripts/logrotate.d/kinesis-load-generator s3://sdx-kinesis-load/$VERSION/logrotate.d/kinesis-load-generator
