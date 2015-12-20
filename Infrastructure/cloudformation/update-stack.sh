#!/usr/bin/env bash

. ./sync.sh

. ./config

echo aws cloudformation update-stack --stack-name $STACKNAME \
  --capabilities CAPABILITY_IAM \
  --template-url https://s3.amazonaws.com/$S3BUCKETNAME/$TEMPLATENAME \
  --region $REGION

aws cloudformation update-stack --stack-name $STACKNAME \
  --capabilities CAPABILITY_IAM \
  --template-url https://s3.amazonaws.com/$S3BUCKETNAME/$TEMPLATENAME \
  --region $REGION
