soapower
========

Soapower provides a GUI for 
 - viewing webservices requests, 
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

<img src='https://raw.github.com/soapower/soapower/master/public/images/soapower/soapower.png' width='50%' display='float:left'>

Trello
=======
https://trello.com/board/soapower/

Development
=======
Soapower developed with Play Framework 2, with scala / java / js / html5


Licence
=======
This software is licensed under the Apache 2 license, quoted below.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.