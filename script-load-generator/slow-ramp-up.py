import subprocess
import boto3
import time

stream_name='spark-example'
region='us-east-1'
table_name="kinesis-load-config"
rate_limit_key_name='rateLimit'

dynamodb = boto3.resource('dynamodb')

load_script=[
    {"rate":100, "shards":1, "duration":60},
    {"rate":1000, "shards":2, "duration":60},
    {"rate":2000, "shards":3, "duration":60},
    {"rate":10000, "shards":11, "duration":120},
    {"rate":20000, "shards":22, "duration":120},
    {"rate":1000, "shards":2, "duration":60}
]


def scale_kinesis_shards(size, stream_name, region):
    args=['java', '-cp', 'KinesisScalingUtils.jar-complete.jar', '-Dstream-name='+stream_name,
          '-Dscaling-action=resize', '-Dcount='+str(size), '-Dregion='+region, 'ScalingClient']
    return_code = subprocess.call(args)

def update_rate_limit(rate_limit, table_name):
    table = dynamodb.Table(table_name)
    table.put_item(
        Item={
            'id': 'rateLimit',
            'value': str(rate_limit)
        }
    )

# main
for s in load_script:
    # we set the rate limit before and after because it could either increase or decrease and we don't want to
    # over-saturate the shards
    update_rate_limit(s['rate'], table_name)
    scale_kinesis_shards(s['shards'], stream_name, region)
    update_rate_limit(s['rate'], table_name)
    time.sleep(s['duration'])

