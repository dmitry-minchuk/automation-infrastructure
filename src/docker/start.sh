#! /usr/bin/env bash

# getting root directory
BASEDIR=$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)
cd ${BASEDIR}

 setup system variables
SERVER_IP=$(curl http://checkip.amazonaws.com)
echo "Found server IP: $SERVER_IP"
echo "Exporting IP to global variables as JENKINS_LOCAL_HOST"

sed 's/aws.com/'$SERVER_IP'/g' variables.env.original > variables.env

# creating jenkins folder to store its volume
if [[ ! -d jenkins ]]; then
    echo creating jenkins folder...
    mkdir jenkins
fi

# pull required docker images
docker pull selenoid/chrome
docker pull selenoid/firefox

# start
docker-compose up