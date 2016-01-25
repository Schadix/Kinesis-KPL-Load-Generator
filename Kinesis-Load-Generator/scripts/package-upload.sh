#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/../

mvn package
if [[ $? -ne 0 ]] ; then exit 1 ; fi
aws s3 cp target/kinesis-load-generator-1.0-SNAPSHOT.jar s3://sdx-kinesis-load/
aws s3 cp scripts/kinesis-load-generator s3://sdx-kinesis-load/
aws s3 cp scripts/logrotate.d/kinesis-load-generator s3://sdx-kinesis-load/logrotate.d/kinesis-load-generator
