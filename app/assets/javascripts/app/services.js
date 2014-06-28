'use strict';

/***************************************
 *        GROUPS
 ****************************************/

spApp.factory('Group', function ($resource) {
    return $resource('/groups/:groupId',
        { groupId: '@_id.$oid'},
        { update: {method: 'PUT'},
            create: {method: 'POST'}}
    );
});

spApp.factory("GroupsService", function ($http) {
    return {
        findAll: function () {
            return $http.get('/groups/findAll');
        }
    }
});

/***************************************
 *  ENVIRONMENTS
 ***************************************/
spApp.factory('Environment', function ($resource) {
    return $resource('/environments/:environmentId',
        { environmentId: '@_id.$oid'},
        { update: {method: 'PUT'},
            create: {method: 'POST'}}
    );
});


spApp.factory("EnvironmentsService", function ($http) {
    return {
        findAll: function (group) {
            return $http.get('/environments/' + group + '/findall');
        },
        findOptions: function (group) {
            return $http.get('/environments/' + group + '/options');
        },
        findAllAndSelect: function ($scope, environmentName, groups, myService, addAll) {
            $http.get('/environments/' + groups + '/options')
                .success(function (environments) {
                    $scope.environments = environments;
                    if (environments.length == 0) {
                        console.log("No environments have been founded");
                        $scope.environments.unshift({id: "error", name: "error. No Env with this group"});
                    } else if (addAll) {
                        $scope.environments.unshift({id: "all", name: "all"});
                    }
                    if (environmentName != null || myService != null) {
                        angular.forEach($scope.environments, function (value, key) {
                            if (environmentName != null && value.name == environmentName) {
                                // Used in Live and Search pages, we only need the name of the environment for those pages
                                $scope.environment = value.name;
                            }
                            if (myService != null && value.id == myService.environmentId) {
                                myService.environment = value;
                            }
                        });
                    }
                })
                .error(function (resp) {
                    console.log("Error with EnvironmentsService.findAllAndSelect" + resp);
                });
        }
    }
});

/***************************************
 *  SERVICES
 ***************************************/
spApp.factory('Service', function ($resource) {
    return $resource('/services/:environmentName/:serviceId',
        { environmentName: '@environmentName', serviceId: '@_id.$oid'},
        { update: {method: 'PUT'},
            create: {method: 'POST'}}
    );
});

spApp.factory("ServicesService", function ($http) {
    return {
        findAll: function (environmentId) {
            return $http.get('/services/' + environmentId + '/findall');
        }
    }
});


/***************************************
 *  MOCK GROUPS
 ***************************************/

spApp.factory('MockGroup', function ($resource) {
    return $resource('/mockgroups/:mockgroupId',
        { mockgroupId: '@_id.$oid'},
        { update: {method: 'PUT'},
            create: {method: 'POST'}}
    );
});

spApp.factory("MockGroupsService", function ($http) {
    return {
        findAll: function (group) {
            return $http.get('/mockgroups/' + group + '/findall');
        }
    }
});


/***************************************
 *  MOCKS
 ***************************************/
spApp.factory('Mock', function ($resource) {
    return $resource('/mocks/:mockGroupName/:mockId',
        { mockGroupName: '@mockGroupName', mockId: '@_id.$oid'},
        { update: {method: 'PUT'},
            create: {method: 'POST'}}
    );
});

spApp.factory("MocksService", function ($http) {
    return {
        findAll: function (mockGroupId) {
            return $http.get('/mocks/' + mockGroupId + '/findall');
        }
    }
});


/***************************************
 *  SERVICE ACTION
 ***************************************/

spApp.factory('ServiceAction', function ($resource) {
    return $resource('/serviceactions/:serviceActionId',
        { serviceActionId: '@_id.$oid'},
        { update: {method: 'PUT'}}
    );
});

spApp.factory("ServiceActionsService", function ($http) {
    return {
        findAll: function (groups) {
            return $http.get('/serviceactions/' + groups + '/findall');
        },
        findAllAndSelect: function ($scope, serviceaction, groups) {
            // We only retrieve serviceactions names
            $http.get('/serviceactions/' + groups + '/findallname')
                .success(function (serviceActions) {
                    $scope.serviceactions = serviceActions.data;
                    if (serviceaction != "all") {
                        angular.forEach($scope.serviceactions, function (value) {

                            if (value == serviceaction) $scope.serviceaction = value;
                        });
                    } else $scope.serviceaction = "all";

                    $scope.serviceactions.unshift("all");

                })
                .error(function (error) {
                    console.log("Error with ServiceActionsService.findAllAndSelect" + error);
                });
        },
        regenerate: function () {
            return $http.get('/serviceactions/regenerate');
        }
    }
});

/***************************************
 *         SERVICE INDEX
 ****************************************/

spApp.factory("IndexService", function ($http) {
    return {
        getBuildInfo: function () {
            return $http.get('/index/buildinfo');
        }
    }
});

/*************************************** */
/*************************************** */
/*************************************** */
/*************************************** */
/*************************************** */


spApp.factory("AnalysisService", function ($http) {
    return {
        findAll: function () {
            return $http.get('/serviceactions/listDatatable');
        }
    }
});

spApp.factory("CodesService", function ($http) {
    return {
        findAllAndSelect: function ($scope, $routeParams) {
            $http.get('/status/findall')
                .success(function (codes) {
                    $scope.codes = codes.values;
                    $scope.codes.unshift("all");
                    angular.forEach($scope.codes, function (value) {
                        if (value != "all") {
                            var valNot = "NOT_" + value;
                            $scope.codes.push(valNot);
                            if (valNot == $routeParams.code) $scope.code = valNot;
                        }
                        if (value == $routeParams.code) $scope.code = value;
                    });
                })
                .error(function (resp) {
                    console.log("Error with CodesService.findAllAndSelect" + resp);
                });
        }
    }
});


spApp.factory("LoggersService", function ($http) {
    return {
        findAll: function () {
            return $http.get('/loggers');
        },
        changeLevel: function (loggerName, newLevel) {
            return $http.get('/loggers/change/' + loggerName + '/' + newLevel);
        }
    }
});


spApp.factory("UIService", function ($location, $filter, $routeParams, $rootScope) {
    return {
        reloadPage: function ($scope, showServiceactions, page) {

            var environment = "all", serviceaction = "all", mindate = "yesterday", maxdate = "today", code = "all", live="false";

            if ($scope.environment) environment = $scope.environment;

            if ($scope.live) live = $scope.live;

            if ($scope.serviceaction) {
                serviceaction = encodeURIComponent($scope.serviceaction);
            }

            if ($scope.mindate && $scope.mindate != "" && $scope.mindate != "all") {
                mindate = this.initDayToUrl(this.getURLCorrectDateFormat($scope.mindate), "yesterday");
            }
            if ($scope.maxdate && $scope.maxdate != "" && $scope.maxdate != "all") {
                maxdate = this.initDayToUrl(this.getURLCorrectDateFormat($scope.maxdate), "today");
            }

            if ($scope.code) code = $scope.code;

            var path = $scope.ctrlPath + '/' + $scope.groups;

            if (page == "search") {
                path = path + "/" + environment + "/" + serviceaction + "/" + mindate + "/" + maxdate + "/" + code;
                // Add the search parameters to the query string
                if ($scope.search) {
                    var search = {'search': $scope.search, 'request': $scope.request.toString(), 'response': $scope.response.toString()}
                    $location.path(path).search(search);
                }
                else $location.path(path)
                console.log("UIService.reloadPage : Go to " + path);
            }
            else if (page == "live") {
                path = path + "/" + environment + "/" + serviceaction + "/" + "live/live/" + code;
                console.log("UIService.reloadPage : Go to " + path);
                $location.path(path);
            }
            else if (page == "serviceactions") {
                console.log("UIService.reloadPage : Go to " + path);
                $location.path(path)
            }
            else if (page == "statistics") {
                path = path + "/" + environment + "/" + mindate+"/"+maxdate+"/"+live
                console.log("UIService.reloadPage : Go to " + path);
                $location.path(path)
            }
            else if (page == "analysis") {
                path = path + "/" + environment + "/" + serviceaction + "/" + mindate + "/" + maxdate + "/" + live;
                console.log("UIService.reloadPage : Go to " + path);
                $location.path(path)
            }
        },
        /*
         /* Transform a string in the format "yyyy-mm-ddThh:mm" to the
         /* format "yyyy-mm-dd hh:mm" used to display the date
         */
        getInputCorrectDateFormat: function (indate) {
            if (indate && indate != "all") {
                if (indate == "yesterday" || indate == "today") {
                    indate = this.getDay(indate);
                }
                // split to get the date and the time
                var dateAndTime = indate.split('T');
                // The date is set to a string in the following format : yyyy-mm-dd hh:mm
                return dateAndTime[0] + " " + dateAndTime[1];
            }
        },
        /*
         /* Transform a string in the format "yyyy-mm-dd hh:mm" to the
         /* format "yyyy-mm-ddThh:mm" used to pass a date in the URL
         */
        getURLCorrectDateFormat: function (indate) {
            if (indate && indate != "all") {
                // Allow the user to enter "today" or "yesterday" in the date input field
                if (indate == "yesterday" || indate == "today") {
                    indate = this.getDay(indate);
                    return indate;
                }
                else {
                    var dateAndTime = indate.split(' ');
                    return dateAndTime[0] + "T" + dateAndTime[1];
                }
            }
        },
        initDayToUrl: function (val, defaultValue) {
            var dayInit = null;
            if (val == defaultValue) return val;
            if (val == "")  return defaultValue;

            if (val == this.getDay("today")) {
                dayInit = "today";
            } else if (val == this.getDay("yesterday")) {
                dayInit = "yesterday";
            } else {
                dayInit = val;
            }

            return dayInit;
        },
        getDay: function (sDay) {
            var mDate = new Date();
            var month = mDate.getMonth() + 1;
            var nb = -1;
            var time = "00:00";
            switch (sDay) {
                case "today" :
                    nb = 0;
                    time = "23:59";
                    break;
                case "yesterday" :
                default :
                    nb = -1;
            }
            var day = mDate.getDate() + nb;
            if (month < 10) {
                month = "0" + month;
            }
            if (day < 10) {
                day = "0" + day;
            }
            return mDate.getFullYear() + "-" + month + "-" + day + "T" + time;
        },
        checkDatesFormatAndCompare: function (mindate, maxdate) {
            if (mindate && maxdate) {
                mindate = new Date(mindate);
                maxdate = new Date(maxdate);

                return !!mindate.getTime() && !!maxdate.getTime() && mindate <= maxdate;
            }
        },
        stringToBoolean: function (string) {
            if (string == "true") return true;
            else if (string == "false") return false;
            else return true;
        }
    }
});

spApp.factory('ReplayService', function ($http, $rootScope, $location, Service) {
    return {
        beforeReplay: function (id) {
            var url = "/download/request/" + id + "/false";
            return $http.get(url);
        },
        replay: function (id, environmentName, serviceId, contentType, data) {
            Service.get({environmentName: environmentName, serviceId: serviceId}, function (service) {
                    var url = '';
                    var contentTypeForRequest = '';
                    if (service.typeRequest == "SOAP") {
                        // SOAP service
                        url = '/replay/soap/' + id;
                        contentTypeForRequest = "application/xml"
                    } else if (service.typeRequest == "REST") {
                        // REST service
                        url = '/replay/rest/' + id;
                        if (service.httpMethod == "get" || service.httpMethod == "delete") {
                            // The content is just the HTTP method and the URL in text format
                            contentTypeForRequest = "text/html";
                        } else {
                            contentTypeForRequest = contentType;
                        }
                    } else {
                        alert("Error with type Request : " + service.typeRequest)
                    }
                    // perform the request
                    $http({ method: "POST",
                        url: url,
                        data: data.data,
                        headers: { "Content-Type": contentTypeForRequest }
                    }).success(function () {
                        console.log("Success replay id" + id);
                        $rootScope.$broadcast('refreshSearchTable');
                    }).error(function (resp) {
                        console.log("Error replay id" + id);
                        console.log(resp);
                        console.log("location:" + $location.path());
                        $rootScope.$broadcast('refreshSearchTable');
                    });
                }, function () {
                    // Called when the resource cannot get retrieve
                    console.log("Error replay id" + id);
                }
            )
        }
    }
});
