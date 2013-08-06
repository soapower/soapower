#!/bin/bash

#
# Soapower Control Shell Script (soapowerctl.sh)
#
# This script could be used with a standard installation of soapower.
# Read installation's documentation on http://soapower.github.io/soapower
# 
# ./soapower.ctl.sh start      : start soapower
# ./soapower.ctl.sh stop       : stop soapower
# ./soapower.ctl.sh restart    : restart soapower
# ./soapower.ctl.sh configtest : check the configuration
# ./soapower.ctl.sh status     : display sopower status (port regardless)
# 
#  YOU DOESN'T NEED TO MODIFY THIS SCRIPT.
# 
# 
#############################################################

########################################
#          Init vars
# You can define SOAPOWER_HOME and SOAPOWER_HTTP_PORT
# in you env (ex: .profile). 
# Default values : 
# - SOAPOWER_HOME : /opt/soapower
# - SOAPOWER_HTTP_PORT : 9010
########################################
if [[ -z "${SOAPOWER_HOME}" ]]; then
    export SOAPOWER_HOME="/opt/soapower"
fi

if [[ -z "${SOAPOWER_HTTP_PORT}" ]]; then
    export SOAPOWER_HTTP_PORT=9010
fi

########################################
#          Display Usage
# Display how to use soapowerctl.sh
########################################
display_usage() { 
    echo -e "\nUsage:\n$0 [start|stop|restart|configtest|status] \n"
    return 0
}

########################################
#          Configtest
# Check the configuration of soapower : 
# - chmod +x start file
# - check start file with dir lib/
# - check logger-prod.xml file
# - check java and version (>=1.6)
########################################
configtest() {
    ERROR=0

    export SOAPOWER_CURRENT="${SOAPOWER_HOME}/current"

    echo "Soapower Home: ${SOAPOWER_HOME}, port : ${SOAPOWER_HTTP_PORT}"
    echo "Soapower Current: ${SOAPOWER_CURRENT}"

    echo "Checking start file..."
    chmod +x ${SOAPOWER_CURRENT}/start
    if [ $? -ne 0 ]; then
        echo "ERROR : Failed to chmod +x start, please check your installation"
        ERROR=1
    fi

    cd ${SOAPOWER_CURRENT}
    JAR_FILE=`ls lib/soapower*`
    grep $JAR_FILE start >/dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo "ERROR : start file does not match with jar in dir lib/, please check your installation"
        ERROR=1
    fi

    echo "Checking logger-prod.xml file..."
    if [ ! -f ${SOAPOWER_CURRENT}/logger-prod.xml ]; then
        echo "ERROR : ${SOAPOWER_CURRENT}/logger-prod.xml not exist, please check your installation"
        ERROR=1
    fi

    echo "Checking java version..."
    if type -p java >/dev/null 2>&1 ; then
        JAVA_VER=$(java -version 2>&1 | sed 's/java version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q')
        if [ "$JAVA_VER" -ge 16 ]; then 
            echo "ok, java is 1.6 or newer" 
        else
            echo "ERROR : java version is too old..."
            ERROR=1
        fi
    else
        echo "ERROR : no java found in PATH"
        ERROR=1
    fi

    return $ERROR
}


########################################
#          Start
# Start an instance of soapower
# on http.port ${SOAPOWER_HTTP_PORT}
# Do nothing if Soapower is already started.
# If Soapower is not started : 
# - Deleting RUNNING_PID file if necessary
# - Start with nohup command
# - Call action "status"
# - Check pid on ${SOAPOWER_HTTP_PORT}
########################################
start() {
    ERROR=0
    echo "Starting Soapower..."

    ps -ef | grep java | grep soapower | grep "http.port=${SOAPOWER_HTTP_PORT}" | grep -v grep >/dev/null 2>&1

    if [ $? -eq 0 ]; then
        echo "Soapower is already started on port ${SOAPOWER_HTTP_PORT}. Please stop before" ; 
        return 1;
    fi

    if [ -f ${SOAPOWER_CURRENT}/RUNNING_PID ]; then
        echo "WARN : there is ${SOAPOWER_CURRENT}/RUNNING_PID file. Deleting it and continue starting..."
        rm ${SOAPOWER_CURRENT}/RUNNING_PID
        if [ $? -ne 0 ]; then
            echo "ERROR : can't deleting ${SOAPOWER_CURRENT}/RUNNING_PID file. Abort Starting Soapower"
            return 1;
        fi
    fi

    CMD="${SOAPOWER_CURRENT}/start -Dlogger.file=${SOAPOWER_CURRENT}/logger-prod.xml -Dhttp.port=${SOAPOWER_HTTP_PORT} -DapplyEvolutions.default=true"
    nohup $CMD >/dev/null 2>&1 &

    if [ $? -ne 0 ]; then
        echo "ERROR while starting soapower. Please run this command and check the log file:"
        echo "$CMD"
        ERROR=1
    fi

    sleep 3
    status

    ps -ef | grep java | grep soapower | grep "http.port=${SOAPOWER_HTTP_PORT}" | grep -v grep >/dev/null 2>&1

    if [ $? -ne 0 ]; then
        echo "ERROR while starting soapower. Please run this command and check the log file:"
        echo "$CMD"
        ERROR=1
    fi

    return $ERROR
}

########################################
#          Stop
# Stop instance of soapower running 
# on http.port ${SOAPOWER_HTTP_PORT}
########################################
stop() {

    ps -ef | grep java | grep soapower | grep "http.port=${SOAPOWER_HTTP_PORT}" | grep -v grep >/dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo "Soapower is already stopped on http.port ${SOAPOWER_HTTP_PORT}"
        return 0
    fi


    echo "Stopping Soapower (on running http.port ${SOAPOWER_HTTP_PORT})..."
    ps -ef | grep java | grep soapower | grep "http.port=${SOAPOWER_HTTP_PORT}" | grep -v grep | while read a b c; do kill -15 $b ; done

    sleep 4

    # kill if necessary
    ps -ef | grep java | grep soapower | grep "http.port=${SOAPOWER_HTTP_PORT}" | grep -v grep | while read a b c; do echo "Normal TERM failed, kill -9 soapower..." kill -9 $b ; done

    return 0
}

########################################
#          Status
# Display the status of all instances of soapower
# with ps command. 
########################################
status() {
    echo "Status (all instances of soapower are scanned):"
    ps -ef | grep java | grep soapower | grep -v grep | while read a b c; do 
        PORT=$(echo $c | sed 's/\(.*\)-Dhttp.port.\([0-9]*\)\(.*\)/\2/; 1q')
        echo "Soapower is started with pid:$b and http.port:${PORT}"
    done

    ps -ef | grep java | grep soapower | grep -v grep >/dev/null 2>&1

    if [ $? -ne 0 ]; then
        echo "Soapower is stopped"
    fi
    return 0
}

########################################
#          Restart
# Run stop then start action
########################################
restart() {
    stop
    start
}

########################################
#          Action
# Run configtest before run an action. If 
# the configtest failed, return directly
# param $1 : action to run
########################################
action() {
    configtest
    if [ $? -eq 0 ]; then
        $1
        ERROR=$?
    fi
    return $ERROR
}

#########################
#          MAIN
#########################
ACMD="$1"
ARGV="$@"

ERROR=0


case $ACMD in
start|stop|restart)
    action $ACMD
    ERROR=$?
    ;;
configtest)
    configtest
    ERROR=$?
    ;;
status)
    status
    ;;
*)
    display_usage
    ERROR=$?
esac

exit $ERROR
