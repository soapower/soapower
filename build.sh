#!/bin/bash

echo "Build Soapower"

if [[ -z "${SOAPOWER_HOME}" ]]; then
    export SOAPOWER_HOME="/opt/soapower"
fi

export SOAPOWER_SRC="${SOAPOWER_HOME}/build/soapower"

echo "Soapower Home: ${SOAPOWER_HOME}"

mkdir -p ${SOAPOWER_HOME}/build/ && cd ${SOAPOWER_HOME}/build/

if [ $? -ne 0 ]; then
    echo "Failed to make dir ${SOAPOWER_HOME}/build/"
    exit 1
fi

cd ${SOAPOWER_SRC}
echo "Git pull code from https://github.com/soapower/soapower to ${SOAPOWER_SRC}"
git pull

echo "Add +x to shell scripts"
chmod +x *.sh

echo "Play Compilation and Dist"
play dist

