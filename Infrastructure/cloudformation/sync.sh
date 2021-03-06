#!/usr/bin/env bash

. ./config

aws s3 cp $TEMPLATENAME s3://$S3BUCKETNAME/
aws cloudformation validate-template --template-url https://s3.amazonaws.com/$S3BUCKETNAME/$TEMPLATENAME
