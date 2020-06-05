#! /usr/bin/env bash

# getting root directory
BASEDIR=$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)
cd ${BASEDIR}

# creating jenkins folder to store its volume
if [[ ! -d jenkins ]]; then
    echo creating jenkins folder...
    mkdir jenkins
fi