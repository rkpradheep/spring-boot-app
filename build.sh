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


sh shutdown.sh || { echo "Failed to shutdown existing server"; exit 1; }

mkdir -p "build" || { echo "Failed to create build directory"; exit 1; }

./mvnw clean package -DskipTests || { echo "Maven build failed"; exit 1; }

if [ -f "build/spring-boot-web-app.jar" ]; then
  JAR_PATH="build/spring-boot-web-app.jar"
else
  echo "spring-boot-web-app.jar not found after build" && exit 1
fi

ZIP_PATH="build/SpringBootApp.zip"
rm -f "$ZIP_PATH"

cp run.sh build


sedi() {
  if sed --version >/dev/null 2>&1; then
    # GNU sed (likely Linux)
    sed -i "$@"
  else
    # BSD sed (macOS)
    sed -i '' "$@"
  fi
}

if [ -f "custom/application-custom.properties" ]; then
  echo "Found application-custom.properties, reading JAVA_OPTS from it."
  JAVA_OPTS_CUSTOM_VALUE=$(grep "^java.opts=" custom/application-custom.properties | sed 's/^java.opts=//')
  sedi 's|JAVA_OPTS_CUSTOM_VALUE=|JAVA_OPTS_CUSTOM_VALUE="'"$JAVA_OPTS_CUSTOM_VALUE"'"|' build/run.sh
fi


zip -jq "$ZIP_PATH" "$JAR_PATH" build/run.sh shutdown.sh set_variables.sh instrumentation/target/instrumentation.jar protocol/target/protocol.jar || { echo "Failed to create SpringBootApp.zip"; exit 1; }

rm -f "build/spring-boot-web-app.jar"
rm -f "build/run.sh"

echo "${GREEN}############## Build completed ##############${NC}\n"


sh deploy.sh $(pwd)/build

