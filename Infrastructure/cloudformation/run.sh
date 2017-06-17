#!/usr/bin/env bash
#if [ "$#" -ne 3 ]; then
#    echo "Usage: $0 <template-name> <stack-name> <region>"
#    exit
#fi
#
#TEMPLATENAME=$1
#STACKNAME=$2
#REGION=$3

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd $DIR

TARGET_FOLDER="../../kinesis-load-generator/target/"

cd $DIR/../../kinesis-load-generator/
VERSION=`mvn  org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v INFO`

cd $DIR

echo "VERSION=$VERSION"

sed "s/<<VERSION>>/$VERSION/g" kinesis-loader.template > $TARGET_FOLDER/kinesis-loader.template

. ./config

#aws cloudformation delete-stack --stack-name $STACKNAME --region $REGION
aws s3 cp $TARGET_FOLDER/kinesis-loader.template s3://$S3BUCKETNAME/$VERSION/
aws cloudformation validate-template --template-url https://s3.amazonaws.com/$S3BUCKETNAME/$VERSION/$TEMPLATENAME
if [[ $? -ne 0 ]] ; then exit 1 ; fi
aws cloudformation create-stack --stack-name $STACKNAME --capabilities CAPABILITY_IAM \
  --template-url https://s3.amazonaws.com/$S3BUCKETNAME/$VERSION/$TEMPLATENAME --region $REGION \
  --parameters \
  ParameterKey=RateLimit,ParameterValue=$RATELIMIT \



