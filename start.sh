#!/usr/bin/env sh

#
# This script intend to launch soapower into a NettyServer. It eventually produce a RUNNING_PID file which contains
# the pid of the Netty's Java virtual machine process.
#
# This script shouldn't be used directly. Indeed some environment may not have been properly set and some checks not performed. Use ./soapower.sh start instead.
#
# # DO NOT MODIFY THIS SCRIPT UNLESS YOU KNOW WHAT YOU ARE DOING
#



# Constructing classpath
classpath=""

# Adding libs

for jarlib in `ls ${SOAPOWER_CURRENT}/lib`; do
    # If this the first that a library is added, then no ":" has to be added. $i is used because ${classpath} is inter
    if [ "${classpath}" = "" ]
    then
        classpath=${SOAPOWER_CURRENT}/lib/${jarlib};
    else
        classpath=${classpath}:${SOAPOWER_CURRENT}/lib/${jarlib};
    fi
done;

echo "Starting Soapower"

nohup java -Dlogger.file=logger-prod.xml -Dhttp.port=${SOAPOWER_HTTP_PORT} -DapplyEvolutions.default=true -cp ${classpath} play.core.server.NettyServer ${SOAPOWER_CURRENT}  >/dev/null 2>&1 &

# Waiting for Soapower aliveness
sleep 10
checkSoapowerAliveness
