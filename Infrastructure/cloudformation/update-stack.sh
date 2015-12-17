#!/usr/bin/env bash
aws cloudformation update-stack --stack-name kinesis-load \
  --use-previous-template \
  --capabilities CAPABILITY_IAM \
  --parameters \
  ParameterKey=AutoscalingMin,ParameterValue=1 \
  ParameterKey=AutoscalingMax,ParameterValue=1
