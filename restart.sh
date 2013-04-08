#!/bin/bash

echo "Restart Soapower"

if [[ -z "${SOAPOWER_HOME}" ]]; then
    export SOAPOWER_HOME="/opt/soapower"
fi

if [[ -z "${SOAPOWER_HTTP_PORT}" ]]; then
    export SOAPOWER_HTTP_PORT=9010
fi

export SOAPOWER_CURRENT="${SOAPOWER_HOME}/current"

echo "Soapower Home: ${SOAPOWER_HOME}, port : ${SOAPOWER_HTTP_PORT}"
echo "Soapower Current: ${SOAPOWER_CURRENT}"

cd ${SOAPOWER_CURRENT} && chmod +x start
if [ $? -ne 0 ]; then
    echo "Failed to chmod +x start, please use deploy.sh before"
    exit 1;
fi

echo "Stopping Soapower"
ps -ef | grep java | grep soapower | grep -v grep | while read a b c; do kill -15 $b ; done

sleep 4

# kill if necessary
ps -ef | grep java | grep soapower | grep -v grep | while read a b c; do kill -9 $b ; done

echo "Starting Soapower"
nohup start -Dlogger.file=logger-prod.xml -Dhttp.port=${SOAPOWER_HTTP_PORT} -DapplyEvolutions=true >/dev/null 2>&1

