soapower - Monitoring Webservices
========

Soapower provides a GUI for 
 - viewing webservices requests (live and search page),
 - download the data from the request and response, 
 - getting response time,
 - viewing 90percentiles response times, etc ...

Soapower allows monitoring several applications across multiple environments. 

It is also possible to set a threshold response time on soapaction, 
the alerts are rising if the receiver does not send the response back in time.

Administration interface is available for
 - Configure environments and webservices (local / remote target / timeout)
 - Set the thresholds for response time for each soapaction
 - Import / export service configurations & settings
 - Set the purges data XML Data requests & Answers, All Data /  environment
 - Monitoring CPU / Heap Memory / logs file

<img src='https://raw.github.com/soapower/soapower/master/public/images/soapower/ucDiagram.png' display='float:left'>
<img src='https://raw.github.com/soapower/soapower/master/public/images/soapower/seqDiagram.png' display='float:right'>
<img src='https://raw.github.com/soapower/soapower/master/public/images/soapower/classDiagram.png' display='float:left'>


Soapower developed with Play Framework 2, with scala / java / js / html5


Jenkins Integration
===================
If a SoapAction's response time is > threshold, a failed test is generate and visible on Jenkins.

- Create a job, with a shell execution :
`rm *.xml ; wget http://<server>:<port>/statsAsJunit/yesterday/yesterday/ -O result.xml`
- Add a publish test Junit to view all SoapAction's result time. 
- Url parameters : `http://<server>:<port>/statsAsJunit/<minDate>/<maxDate>/` with minDate / maxDate like : 2012-11-29, yesterday, today


Run in Production with a stable version
=======
Current stable version of Soapower : 1.0

Requirements
-----------
* JDK >= 1.6, add JAVA_HOME to your path
* Mysql 5 with a `soapower` user (with password `soapower`, associated to a `soapower` database. You can choose another
name or password by editing the key `db.default.url` in `application.conf` and add `-Dconfig.file=/full/path/to/conf/application-prod.conf` it to the restart.sh script (last line of file)

Installation & Run in Production with stable version
-----------

```
# Set Soapower Home (optional if your home is /opt/soapower/)
$ export SOAPOWER_HOME="/opt/soapower/"

# Set Soapower Http Listen Port (optional if your default port is 9010)
$ export SOAPOWER_PORT=9010

mkdir -p /opt/soapower && cd /opt/soapower
wget http://dl.bintray.com/content/soapower/soapower/soapower-1.0.zip?direct -O soapower-1.0.zip
unzip soapower-1.0.zip
ln -s soapower-1.0 current && cd current
wget --no-check-certificate http://raw.github.com/soapower/soapower/v1.0-play2.1.1/restart.sh
wget --no-check-certificate http://raw.github.com/soapower/soapower/v1.0-play2.1.1/conf/logger-prod.xml
chmod +x restart.sh
./restart.sh
```

* Go to http://localhost:9010/


Soapower Home directory after theses steps :
```
$ ls -lart
total 64408
-rw-r--r--  1 yvonnickesnault  admin  32970177  7 avr 19:36 soapower-1.0.zip
drwxr-xr-x@ 4 yvonnickesnault  admin       136  8 avr 22:22 ..
lrwxr-xr-x  1 yvonnickesnault  admin        12  8 avr 22:25 current -> soapower-1.0
drwxr-xr-x  5 yvonnickesnault  admin       170  8 avr 22:25 .
drwxr-xr-x  8 yvonnickesnault  admin       272  8 avr 22:26 soapower-1.0
```

With the current directory :`
```
/opt/soapower/current$ ls -lart
total 32
-rwxr-xr-x   1 yvonnickesnault  admin  2992  7 avr 19:33 start
drwxr-xr-x  56 yvonnickesnault  admin  1904  7 avr 19:33 lib
drwxr-xr-x   5 yvonnickesnault  admin   170  8 avr 22:25 ..
drwxr-xr-x   3 yvonnickesnault  admin   102  8 avr 22:26 logs
-rwxr-xr-x   1 yvonnickesnault  admin  1050  8 avr 22:26 restart.sh
-rw-r--r--   1 yvonnickesnault  admin     4  8 avr 22:26 RUNNING_PID
-rw-r--r--   1 yvonnickesnault  admin   921  8 avr 22:32 logger-prod.xml
```

The `RUNNING_PID` file contain the pid of Soapower (wich is running here)


Installation & Run in Production with master branch
=======

Requirements
-----------
* JDK >= 1.6, add JAVA_HOME to your path
* Mysql 5 with a `soapower` user (with password `soapower`, associated to a `soapower` database.
* Play Framework 2.1.1. Download and unzip : http://downloads.typesafe.com/play/2.1.1/play-2.1.1.zip, add PLAY_HOME to your path
* Git


Init
-----

```
# Make soapower directory : 
$ mkdir -p /opt/soapower/build && cd /opt/soapower/build

# Checkout the source code : 
$ git clone https://github.com/soapower/soapower.git

# Play Compilation : 
$ cd /opt/soapower/build/soapower && play compile

# Play run on default port 9000 : 
$ cd /opt/soapower/build/soapower && play run

```

* Go to http://localhost:9000/


Run in Production with source code on master branch
-----------

```
# Set Soapower Home (optional if your home is /opt/soapower/)
$ export SOAPOWER_HOME="/opt/soapower/"

# Set Soapower Http Listen Port (optional if your default port is 9010)
$ export SOAPOWER_PORT=9010

# Run build.sh : Git pull code from https://github.com/soapower/soapower, 
# then make shell script executable and build Soapower (play dist)
$ chmod +x /opt/soapower/build/soapower/build.sh && /opt/soapower/build/soapower/build.sh

# Run deploy.sh : Get Soapower package (from build), create a directory SOAPOWER_HOME/date 
# and unzip the package file into it, stop and start Soapower.
$ /opt/soapower/build/soapower/deploy.sh

```

* Go to http://localhost:9010/


To avoid any conflicts with build.sh script, do not make any change in directory /opt/soapower/build/soapower.

The current Soapower is in directory /opt/soapower/current (symlink). Please clean manually old deployed directories in /opt/soapower/.

restart.sh : stop and restart Soapower.

Example of Soapower Home directory :
```
/opt/soapower$ ls -l
total 8
drwxr-xr-x  4 yvonnickesnault  admin  136  7 avr 18:50 20130407_185006
drwxr-xr-x  4 yvonnickesnault  admin  136  7 avr 18:50 20130407_185051
drwxr-xr-x  3 yvonnickesnault  admin  102  7 avr 16:34 build
lrwxr-xr-x  1 yvonnickesnault  admin   52  7 avr 18:47 current -> /opt/soapower/20130407_185051/soapower-1.0-SNAPSHOT/
```


Development
==========

Requirements
-----------
* JDK >= 1.6, add JAVA_HOME to your path
* Mysql 5 with a `soapower` user (with password `soapower`, associated to a `soapower` database. You can choose another
name or password by editing the key `db.default.url` in `application.conf`
* Play Framework 2.1.1. Download and unzip : http://downloads.typesafe.com/play/2.1.1/play-2.1.1.zip, add PLAY_HOME to your path
* Git
* An IDE (scala-ide, intellij), or an editor of your choice...

If you are a java developer (and not scala), please read at least http://www.codecommit.com/blog/scala/scala-for-java-refugees-part-1 (part 1 to 6)

If you don't know Play Framework, please read http://www.playframework.com/documentation/2.1.1/ScalaTodoList

If you don't know Git... go to http://try.github.io and read how to make a pull request here : https://help.github.com/articles/using-pull-requests

Trello : https://trello.com/b/Gd11S2zp

[![Build Status](https://buildhive.cloudbees.com/job/soapower/job/soapower/badge/icon)](https://buildhive.cloudbees.com/job/soapower/job/soapower/)

Licence
=======
This software is licensed under the Apache 2 license, quoted below.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
