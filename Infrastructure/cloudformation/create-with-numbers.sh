#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/

. ./config

aws cloudformation create-stack --stack-name spark-number --capabilities CAPABILITY_IAM \
  --template-url https://s3.amazonaws.com/$S3BUCKETNAME/$TEMPLATENAME --region $REGION \
  --parameters \
  ParameterKey=RateLimit,ParameterValue=10 \
  ParameterKey=S3FileNameGZ,ParameterValue=sdx-kinesis-load/numbers.gz \
  ParameterKey=StreamName,ParameterValue=spark-number
