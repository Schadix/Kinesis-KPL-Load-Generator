#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR/

. ./config

aws cloudformation create-stack --stack-name spark-numbers --capabilities CAPABILITY_IAM \
  --template-url https://s3.amazonaws.com/$S3BUCKETNAME/$TEMPLATENAME --region $REGION \
  --parameters \
  ParameterKey=RateLimit,ParameterValue=2000 \
  ParameterKey=S3FileNameGZ,ParameterValue=countup \
  ParameterKey=StreamName,ParameterValue=spark-numbers \
  ParameterKey=SshCidr,ParameterValue=72.21.198.67/32

