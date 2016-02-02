source ./baseconfig.properties

aws s3 cp dist/$APPNAME.zip s3://$S3BUCKET/$APPNAME.zip
