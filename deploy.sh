#!/bin/bash

echo "Deploy Soapower"

if [[ -z "${SOAPOWER_HOME}" ]]; then
    export SOAPOWER_HOME="/opt/soapower"
fi

if [[ -z "${SOAPOWER_HTTP_PORT}" ]]; then
    export SOAPOWER_HTTP_PORT=9010
fi

export SOAPOWER_SRC="${SOAPOWER_HOME}/build/soapower"

echo "Soapower Home: ${SOAPOWER_HOME}, port : ${SOAPOWER_HTTP_PORT}"

cd ${SOAPOWER_SRC}

PACKAGE_FILE=`ls ${SOAPOWER_SRC}/dist/ | grep soapower- | grep zip`

if [ $? -ne 0 ]; then
    echo "Please package soapower, you can use build.sh, read it before"
    exit 1;
fi

export MYDATE_TIME=`date +%Y%m%d_%H%M%S`

mkdir -p ${SOAPOWER_HOME}/${MYDATE_TIME}

if [ $? -ne 0 ]; then
    echo "Failed to make dir ${SOAPOWER_HOME}/${MYDATE_TIME}"
    exit 1;
fi

echo "Move ${PACKAGE_FILE} to ${SOAPOWER_HOME}/${MYDATE_TIME}"

mv ${SOAPOWER_SRC}/dist/${PACKAGE_FILE} ${SOAPOWER_HOME}/${MYDATE_TIME}

if [ $? -ne 0 ]; then
    echo "Failed to move file ${PACKAGE_FILE} to ${SOAPOWER_HOME}/${MYDATE_TIME}";
    exit 1;
fi

cd ${SOAPOWER_HOME}/${MYDATE_TIME} && unzip ${PACKAGE_FILE}
if [ $? -ne 0 ]; then
    echo "Failed to unzip ${PACKAGE_FILE} in ${SOAPOWER_HOME}/${MYDATE_TIME}";
    exit 1;
fi

export DIR_DEPLOY=`ls -d ${SOAPOWER_HOME}/${MYDATE_TIME}/*/`

echo "Copy from ${SOAPOWER_SRC}/conf/logger-prod.xml to ${DIR_DEPLOY}/logger-prod.xml"
cp ${SOAPOWER_SRC}/conf/logger-prod.xml ${DIR_DEPLOY}/logger-prod.xml
if [ $? -ne 0 ]; then
    echo "Failed to copy from ${SOAPOWER_SRC}/conf/logger-prod.xml to ${DIR_DEPLOY}/logger-prod.xml";
    exit 1;
fi

echo "Copy from ${SOAPOWER_SRC}/restart.sh to ${DIR_DEPLOY}/restart.sh"
cp ${SOAPOWER_SRC}/restart.sh ${DIR_DEPLOY}/restart.sh

chmod +x ${DIR_DEPLOY}/restart.sh ${DIR_DEPLOY}/start
if [ $? -ne 0 ]; then
    echo "Failed to chmod +x ${DIR_DEPLOY}/restart.sh ${DIR_DEPLOY}/start";
    exit 1;
fi

echo "Stopping Soapower..."
ps -ef | grep java | grep soapower | grep -v grep | while read a b c; do kill -15 $b ; done

sleep 4

# kill if necessary
ps -ef | grep java | grep soapower | grep -v grep | while read a b c; do kill -9 $b ; done

echo "Delete symlink ${SOAPOWER_HOME}/current"
rm -f ${SOAPOWER_HOME}/current

echo "Make link from ${DIR_DEPLOY} to ${SOAPOWER_HOME}/current"
ln -s ${DIR_DEPLOY} ${SOAPOWER_HOME}/current

echo "Starting Soapower on port 9010"
cd ${SOAPOWER_HOME}/current && nohup start -Dlogger.file=logger-prod.xml -Dhttp.port=${SOAPOWER_HTTP_PORT} -DapplyEvolutions=true >/dev/null 2>&1

