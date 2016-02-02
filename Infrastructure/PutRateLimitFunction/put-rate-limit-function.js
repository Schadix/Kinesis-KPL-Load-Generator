'use strict';

var AWS = require('aws-sdk');
var util = require('util');

var dynamodb = new AWS.DynamoDB({
	apiVersion: '2012-08-10'
});

AWS.config.update({
	region: "us-east-1"
});

var docClient = new AWS.DynamoDB.DocumentClient();

exports.handler = function(event, context) {

	console.log(util.inspect(event, {depth:null}));

  // For Delete requests, immediately send a SUCCESS response.
  if (event.RequestType == "Delete") {
      sendResponse(event, context, "SUCCESS");
      return;
  }
  var responseStatus = "FAILED";
  var responseData = {};
  var rateLimit = event.ResourceProperties.RateLimit;
  var table = event.ResourceProperties.Table;
  console.log("rateLimit: "+rateLimit+", table: "+table);

  if (rateLimit) {
    var params = {};
    params.TableName = table;
    params.Item = {
      id: "rateLimit",
      value: rateLimit
    };

    docClient.put(params, function(err, data) {
      if (err) {
        console.log(err + "\nparams:" + util.inspect(params, {
          depth: null
        }));
        // TODO: set response to FAILED
      } else {
        console.log(rateLimit);
      };
      sendResponse(event, context, "SUCCESS");
    });
  } else {
    console.log("undefined\n" + rateLimit);
    // TODO: set response to FAILED
    sendResponse(event, context, "SUCCESS");
  }
}

// Send response to the pre-signed S3 URL
function sendResponse(event, context, responseStatus, responseData) {

    var responseBody = JSON.stringify({
        Status: responseStatus,
        Reason: "See the details in CloudWatch Log Stream: " + context.logStreamName,
        PhysicalResourceId: context.logStreamName,
        StackId: event.StackId,
        RequestId: event.RequestId,
        LogicalResourceId: event.LogicalResourceId,
        Data: responseData
    });

    console.log("RESPONSE BODY:\n", responseBody);

    var https = require("https");
    var url = require("url");

    var parsedUrl = url.parse(event.ResponseURL);
    var options = {
        hostname: parsedUrl.hostname,
        port: 443,
        path: parsedUrl.path,
        method: "PUT",
        headers: {
            "content-type": "",
            "content-length": responseBody.length
        }
    };

    console.log("SENDING RESPONSE...\n");

    var request = https.request(options, function(response) {
        console.log("STATUS: " + response.statusCode);
        console.log("HEADERS: " + JSON.stringify(response.headers));
        // Tell AWS Lambda that the function execution is done
        context.done(null, "success");
    });

    request.on("error", function(error) {
        console.log("sendResponse Error:" + error);
        // TODO: change to error
        context.done(null, "failed");
    });

    // write data to request body
    request.write(responseBody);
    request.end();
}