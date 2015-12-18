#!/usr/bin/env bash

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <min and max for AutoScalingGroup>"
    exit
fi

MINMAX=$1

aws cloudformation update-stack --stack-name kinesis-load \
  --use-previous-template \
  --capabilities CAPABILITY_IAM \
  --parameters \
  ParameterKey=AutoscalingMin,ParameterValue=$MINMAX \
  ParameterKey=AutoscalingMax,ParameterValue=$MINMAX
