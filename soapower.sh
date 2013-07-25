#!/usr/bin/env sh
#
# This script is the entry point of all Soapower exploit actions. It can start, stop and restart Soapower.
# It perform some verifications, set some environment variables and launch subscript start.sh and stop.sh.
#



#
# You can modify the environment variables below in order to tune your Soapower instance, but it will be better to not touch
# anything else unless you know what you are doing
#

# This variable contains the soapower installation directory
# export SOAPOWER_HOME=/opt/soapower

# Soapower's http listening port
# export SOAPOWER_HTTP_PORT=9010




##############################################################################
#                                                                            #
# DO NOT TOUCH ANYTHING BELOW THIS LINE UNLESS YOU KNOW WHAT YOU ARE DOING   #
#                                                                            #
##############################################################################

export SOAPOWER_CURRENT="${SOAPOWER_HOME}/current"

#
# Check that the process denoted by the given pid exists
# Parameter :
#   $1 = process to check pid
# Return
#   1 = Process hasn't been founded
#   0 = Process has been founded
#
checkPid(){

    pidToCheck=$1

    ps --pid ${pidToCheck} > /dev/null

    if [ $? != 0 ]
    then
        return 1
    else
        return 0
    fi

}

#
# Check that soapower process is running. Return 0 if yes, no otherwise.
#
checkSoapowerAliveness(){
      # Checking if Soapower RUNNING_PID exists
     if [ -r ${SOAPOWER_CURRENT}/RUNNING_PID ]
     then

        checkPid `cat ${SOAPOWER_CURRENT}/RUNNING_PID`

        if [ $? != 0 ]
        then
            echo "Soapower PID's file exists, but there isn't any process corresponding. Check that Soapower hasn't crash and then use "./soapower.sh force" to remove PID's file";
            exit 1;
        else
            echo "Soapower is running";
            return 0;
        fi
     else
        echo "Soapower is not running"
        return 1;
     fi

}



#
# This operation check Soapower installation and environment variables. It exists if something goes wrong
#
checkSoapower(){

    echo "Checking Soapower installation and environment"

    # Checking environment variables
    if [ -z "${SOAPOWER_HOME}" ]; then
        export SOAPOWER_HOME="/opt/soapower"
    fi

    if [ -z "${SOAPOWER_HTTP_PORT}" ]; then
        export SOAPOWER_HTTP_PORT=9010
    fi



    echo "Soapower Home: ${SOAPOWER_HOME}, port : ${SOAPOWER_HTTP_PORT}"
    echo "Soapower Current: ${SOAPOWER_CURRENT}"

    # Checking files
    chmod +x ${SOAPOWER_CURRENT}/start.sh
    if [ $? -ne 0 ]; then
        echo "Failed to chmod +x start.sh, please check your installation"
        exit 1;
    fi

     chmod +x ${SOAPOWER_CURRENT}/stop.sh
     if [ $? -ne 0 ]; then
         echo "Failed to chmod +x stop.sh, please check your installation"
         exit 1;
     fi

     if [ ! -f ${SOAPOWER_CURRENT}/logger-prod.xml ]; then
        echo "${SOAPOWER_CURRENT}/logger-prod.xml not exist, please check your installation"
        exit 1;
     fi


     checkSoapowerAliveness
}



#
# This function start Soapower, it hasn't any parameters and return 0 if all was well.
#
startSoapower(){
    checkSoapower
    . ./start.sh
}


#
# This function stop Soapower, it hasn't any parameters and return 0 if all was well.
#
stopSoapower(){
    checkSoapower
    . ./stop.sh
}


#
# This function restart Soapower, it hasn't any parameters and return 0 if all was well.
#
restartSoapower(){
    checkSoapower
    stopSoapower
    startSoapower
}

#
# Remove RUNNING_PID files if exists
#
forceSoapower(){

    echo "Forcing soapower"
    if [ -f ${SOAPOWER_CURRENT}/RUNNING_PID ]
    then
        rm -f ${SOAPOWER_CURRENT}/RUNNING_PID
    fi
    echo "Soapower's RUNNING_PID file has been remove properly";
}


##########################
# Main                   #
##########################

case "$1" in
'start')
        startSoapower
        ;;
'stop')
        stopSoapower
        ;;
'force')
        forceSoapower
        ;;
'check')
        checkSoapower
        ;;
*)
        echo "Usage $0 { start | stop | force | check}"
        ;;
esac




