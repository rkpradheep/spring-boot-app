#!/bin/bash
echo "############## Deployment started ##############"


ZIP_FILE_PATH=$1

export APP_SERVER_HOME=$ZIP_FILE_PATH/container

rm -rf $APP_SERVER_HOME

mkdir -p "$APP_SERVER_HOME" || { echo "Failed to create container directory"; exit 1; }

cd ${APP_SERVER_HOME}

ZIP_FILE="${ZIP_FILE_PATH}/SpringBootApp.zip"

if [ ! -f "$ZIP_FILE" ]; then
  echo "Error: $ZIP_FILE not found!"
  exit 1
fi

unzip -oq "$ZIP_FILE" -d "${APP_SERVER_HOME}" || { echo "Failed to unzip $ZIP_FILE"; exit 1; }

. ./set_variables.sh

bash run.sh

appHealth="false"
attempt=0
maxAttempt=3
pollingInterval=10
timeout=$((maxAttempt * pollingInterval))


echo "Starting health check polling for Spring Boot application..."
echo "Max attempts: $maxAttempt, Polling interval: ${pollingInterval}s, Timeout: ${timeout}s"


while [ "$appHealth" != "true" ] && [ $attempt -lt $maxAttempt ]; do
    attempt=$((attempt + 1))
    echo "######## Health check attempt $attempt/$maxAttempt ######## "

    curl_output=$(curl -s -X GET http://localhost:80/_app/health 2>&1)
    curl_exit_code=$?

    if [ $curl_exit_code -eq 0 ]; then
        appHealth="$curl_output"
        if [ "$appHealth" = "true" ]; then
            echo "Health check successful! Application is ready."
            break
        else
            echo "Health check failed. Response: $appHealth"
        fi
    else
        echo "Health check failed - curl command error"
        appHealth="false"
    fi

    if [ $attempt -lt $maxAttempt ]; then
        echo "Waiting ${pollingInterval} seconds before next attempt..."
        sleep $pollingInterval
    fi
done



if [ $attempt -eq $maxAttempt ] && [ "$appHealth" != "true" ]; then
    echo "ERROR: Application failed to start within ${timeout} seconds (${maxAttempt} attempts)"
    echo "Please check the application logs for startup errors"
    exit 1
fi


echo "############## Deployment completed ##############"