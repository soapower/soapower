#!/usr/bin/env sh


#
# This script intend to stop Soapower. It uses the "RUNNING_PID" Netty's file in order to kill the server (gently, more strongly if it wasn't enough).
#
# This script shouldn't be used directly. Indeed some environment may not have been properly set and some checks not performed. Use ./soapower.sh stop instead.
#
# DO NOT MODIFY THIS SCRIPT UNLESS YOU KNOW WHAT YOU ARE DOING
#




#
# Kill the process denoted by the given pid. It does it softly first, and then if it wasn't enough more hardly.
#
# Parameter :
#   $1 = process to kill pid
#
killPid(){

    pidToKill=$1;

    echo "Stopping process ${pidToKill}";

    # Kill the process
    kill -15 ${pidToKill};

    # Wait it has been shutdown
    sleep 10 ;

    # Force kill if necessary
    checkPid ${pidToKill};
    if [ $? != 0 ]
    then
        echo "Killing process ${pidToKill}"
        kill -9 ${pidToKill}
    fi
}

#
# Main
#

echo "Stopping Soapower"
for pid in `cat ${SOAPOWER_HOME}/current/RUNNING_PID`; do killPid ${pid}; done;