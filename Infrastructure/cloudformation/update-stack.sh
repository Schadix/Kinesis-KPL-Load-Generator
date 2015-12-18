#!/usr/bin/env bash

. ./config

aws s3 sync . s3://$S3BUCKETNAME/
aws cloudformation validate-template --template-url https://s3.amazonaws.com/$S3BUCKETNAME/$TEMPLATENAME
if [[ $? -ne 0 ]] ; then exit 1 ; fi
aws cloudformation update-stack --stack-name $STACKNAME \
  --capabilities CAPABILITY_IAM \
  --template-url https://s3.amazonaws.com/$S3BUCKETNAME/$TEMPLATENAME \
  --region $REGION
