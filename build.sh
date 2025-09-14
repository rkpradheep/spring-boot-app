#!/bin/bash

set -e
trap '[ $? -eq 0 ] || echo "${RED}######### OPERATION FAILED #########${NC}"' EXIT


if [ -z "$(git log origin/$(git rev-parse --abbrev-ref HEAD)..HEAD)" ]; then
    if git diff --quiet; then
      git pull origin master --rebase
    fi
else
  echo "${RED}############## There are some unpushed commits. Please push and try again ##############${NC}\n"
  exit 1
fi

export APP_SERVER_HOME=$(pwd)

. ./set_variables.sh

setupMysql() {

MACHINE_NAME="podman-machine-default"
podman=/opt/podman/bin/podman

  if ! $podman machine inspect "$MACHINE_NAME" | grep -q '"State": "running"'; then
      echo "Starting Podman machine '$MACHINE_NAME'..."
      $podman machine start "$MACHINE_NAME"
  else
      echo "Podman machine '$MACHINE_NAME' is already running."
  fi

    CONTAINER_NAME="mysql-tomcat-container"

    if $podman ps --filter "name=^${CONTAINER_NAME}$" --filter "status=running" --format "{{.Names}}" | grep -wq "$CONTAINER_NAME"; then
        echo "Container '$CONTAINER_NAME' is already running."
    else
        echo "Container '$CONTAINER_NAME' is not running."

        if $podman ps -a --filter "name=^${CONTAINER_NAME}$" --format "{{.Names}}" | grep -wq "$CONTAINER_NAME"; then
            echo "Starting existing container '$CONTAINER_NAME'..."
            $podman start "$CONTAINER_NAME"
        else
            echo "Creating and running new container '$CONTAINER_NAME'..."
            $podman run -d --name "$CONTAINER_NAME" -p 4000:3306 -e MYSQL_ALLOW_EMPTY_PASSWORD=yes mysqldev:8.0.35
        fi
    fi
}


os_name=$(uname)

if [ "$os_name" = "Darwin" ]; then
    echo "Executing build script for MAC"
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk use java 17.0.14-zulu
    setupMysql
else
  export JAVA_HOME=/opt/java/zulu$JAVA_VERSION
fi

echo "############## Build started ##############\n"

echo "JAVA_HOME : ${JAVA_HOME}"


# Find and stop Spring Boot app more safely
SPRING_PID=$(ps aux | grep "root-app-0.0.1-SNAPSHOT.jar" | grep -v grep | awk '{print $2}' | head -n 1)
if [ -n "$SPRING_PID" ]; then
  echo "Stopping Spring Boot app (PID: $SPRING_PID)..."
  kill -TERM $SPRING_PID
  sleep 3
  # Force kill only if still running
  if kill -0 $SPRING_PID 2>/dev/null; then
    echo "Force stopping Spring Boot app..."
    kill -9 $SPRING_PID
  fi
  sleep 2
fi

./mvnw clean package -DskipTests || { echo "Maven build failed"; exit 1; }

sh run.sh


echo "${GREEN}############## Build completed ##############${NC}\n"
