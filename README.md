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


Jenkins Integration
===================
- Create a job, with a shell execution :
`rm *.xml ; wget http://<server>:<port>/statsAsJunit/yesterday/yesterday/ -O result.xml`
- Add a publish test Junit to view all SoapAction's result time. If a SoapAction's response time is > threshold, a failed test is generate.
- Url parameters : `http://<server>:<port>/statsAsJunit/<minDate>/<maxDate>/` with minDate / maxDate like : 2012-11-29, yesterday, today

Trello
=======
https://trello.com/b/Gd11S2zp

Installation
=======
Requirements
-----------
* JDK >= 1.6, add JAVA_HOME to your path
* Mysql 5 with a `soapower` user (with password `soapower`, associated to a `soapower` database. You can choose another
name or password by editing the key `db.default.url` in `application.conf`
* Play Framework 2.1.1
** Download and unzip : http://downloads.typesafe.com/play/2.1.1/play-2.1.1.zip
** Add PLAY_HOME to your path
* Git

Compilation / Development
-----------
* Make soapower directory : `mkdir -p /opt/soapower/build && cd /opt/soapower/build`
* Checkout the source code : `git clone https://github.com/soapower/soapower.git`
* Play Compilation : `cd /opt/soapower/build/soapower && play compile`

You can ajust configuration by editing `/opt/soapower/build/soapower/conf/application.conf`

* Play run on default port 9000 : `cd /opt/soapower/build/soapower && play run`
* Go to http://localhost:9000/

Run in Production with source code on master branch
-----------
* Set Soapower Home : `export SOAPOWER_HOME="/opt/soapower/"` (optional if your home is /opt/soapower/)
* Set Soapower Http Listen Port : `export SOAPOWER_PORT=9010` (optional if your default port is 9010)
* Run build.sh : `chmod +x /opt/soapower/build/soapower/build.sh && /opt/soapower/build/soapower/build.sh`
* Run deploy.sh : `/opt/soapower/build/soapower/deploy.sh`

build.sh : Git pull code from https://github.com/soapower/soapower, make shell script executable and build Soapower (play dist)

deploy.sh : Get Soapower package (from build), create a directory SOAPOWER_HOME/date and unzip the package file into it, stop and start Soapower.
The current Soapower is in directory /opt/soapower/current (symlink). Please clean manually old deployed directories in /opt/soapower/.

restart.sh : stop and restart Soapower.

Example :
`/opt/soapower$ ls -l
total 8
drwxr-xr-x  4 yvonnickesnault  admin  136  7 avr 18:50 20130407_185006
drwxr-xr-x  4 yvonnickesnault  admin  136  7 avr 18:50 20130407_185051
drwxr-xr-x  3 yvonnickesnault  admin  102  7 avr 16:34 build
lrwxr-xr-x  1 yvonnickesnault  admin   52  7 avr 18:47 current -> /opt/soapower/20130407_185051/soapower-1.0-SNAPSHOT/`


Development
=======
Soapower developed with Play Framework 2, with scala / java / js / html5
Current version build on Play Framework 2.1.1

Licence
=======
This software is licensed under the Apache 2 license, quoted below.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
