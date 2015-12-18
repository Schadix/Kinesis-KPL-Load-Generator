#!/usr/bin/env bash
#if [ "$#" -ne 3 ]; then
#    echo "Usage: $0 <template-name> <stack-name> <region>"
#    exit
#fi
#
#TEMPLATENAME=$1
#STACKNAME=$2
#REGION=$3

. ./config.sh

#aws cloudformation delete-stack --stack-name $STACKNAME --region $REGION
aws s3 sync . s3://$S3BUCKETNAME/
aws cloudformation validate-template --template-url https://s3.amazonaws.com/$S3BUCKETNAME/$TEMPLATENAME
if [[ $? -ne 0 ]] ; then exit 1 ; fi
aws cloudformation create-stack --stack-name $STACKNAME --capabilities CAPABILITY_IAM \
  --template-url https://s3.amazonaws.com/$S3BUCKETNAME/$TEMPLATENAME --region $REGION

