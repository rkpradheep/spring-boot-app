#!/bin/sh

export JAVA_VERSION="17.48.15-ca-jdk17.0.10-linux_x64"

export MYSQL_VERSION="8.3.0-linux-glibc2.28-x86_64"

export MARIADB_VERSION_ONLY="11.6.1" #Need to change the version manually in mariadb-server.sh and mariadb.service file

export MARIADB_VERSION="${MARIADB_VERSION_ONLY}-linux-systemd-x86_64"

export RED='\033[0;31m'

export GREEN='\033[0;32m'

export NC='\033[0m'

export TOMCAT_VERSION="10.1.23"
