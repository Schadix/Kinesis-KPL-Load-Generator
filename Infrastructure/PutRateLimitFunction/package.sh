source ./baseconfig.properties

rm -f dist/$APPNAME.zip
zip -r dist/$APPNAME.zip $APPNAME.js

