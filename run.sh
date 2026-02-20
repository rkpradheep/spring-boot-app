#!/bin/bash


echo "APP_SERVER_HOME : ${APP_SERVER_HOME}"

. $APP_SERVER_HOME/set_variables.sh

exec > "$APP_SERVER_HOME/nohup.out" 2>&1

echo "APP_SERVER_HOME : ${APP_SERVER_HOME}"


echo "##############" $(date +"%Y-%m-%d %r") " ==> STARTING SPRING BOOT ##############"

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
  if [ -d "/opt/java/zulu$JAVA_VERSION" ]; then
    export JAVA_HOME=/opt/java/zulu$JAVA_VERSION
  elif [ -d "/home/test/rkp/jdk" ]; then
       export JAVA_HOME=/home/test/rkp/jdk
  else
    export JAVA_HOME=/home/sas/rkp/jdk
  fi
fi

cd $APP_SERVER_HOME

sh shutdown.sh

JAR_FILE="$APP_SERVER_HOME/spring-boot-web-app.jar"

if [ ! -f "$JAR_FILE" ]; then
  echo "JAR file not found: $JAR_FILE"
  exit 1m
fi

JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Djdk.http.auth.tunneling.disabledSchemes= -Djdk.http.auth.proxying.disabledSchemes= -Duser.timezone=Asia/Kolkata -javaagent:instrumentation.jar --add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED --add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED --add-exports=java.base/sun.net.www.http=ALL-UNNAMED -Djava.protocol.handler.pkgs=com.server.protocol -Xbootclasspath/a:protocol.jar -Dspring.profiles.active=custom"


#Placeholder
JAVA_OPTS_CUSTOM_VALUE=

if [ -n "$JAVA_OPTS_CUSTOM_VALUE" ]; then
  JAVA_OPTS="$JAVA_OPTS_CUSTOM_VALUE"
fi


echo "Using JAVA_OPTS: $JAVA_OPTS"
#nohup caffeinate -di $JAVA_HOME/bin/java $JAVA_OPTS -jar "$JAR_FILE" &
nohup $JAVA_HOME/bin/java $JAVA_OPTS -jar "$JAR_FILE" &


echo "##############" $(date +"%Y-%m-%d %r") " ==> SPRING BOOT STARTED ##############"
