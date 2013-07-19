/*global define */

'use strict';

define(['angular'], function (angular) {

    /* Services */
    angular.module('spApp.services', ['ngResource'])

        .factory('Service', function ($resource, UIService) {
            var Service = $resource('/services/:serviceId',
                { serviceId: '@id'},
                { update: {method: 'POST'} }
            );

            Service.prototype.update = function (cb) {
                this.recordXmlData = UIService.fixBoolean(this.recordXmlData);
                this.recordData = UIService.fixBoolean(this.recordData);
                this.environmentId = parseInt(this.environment.id);
                this.id = parseInt(this.id);

                return Service.update({serviceId: this.id},
                    angular.extend({}, this, {serviceId: undefined}), cb);
            };

            Service.prototype.destroy = function (cb) {
                return Service.remove({serviceId: this.id}, cb);
            };

            return Service;
        })

        .factory('Environment', function ($resource, UIService) {
            var Environment = $resource('/environments/:environmentId',
                { environmentId: '@id'},
                { update: {method: 'POST'} }
            );

            Environment.prototype.update = function (cb) {
                this.recordXmlData = UIService.fixBoolean(this.recordXmlData);
                this.recordData = UIService.fixBoolean(this.recordData);
                this.id = parseInt(this.id);
                this.groupId =  parseInt(this.group.id);

                return Environment.update({environmentId: this.id},
                    angular.extend({}, this, {environmentId: undefined}), cb);
            };

            Environment.prototype.destroy = function (cb) {
                return Environment.remove({environmentId: this.id}, cb);
            };

            return Environment;
        })
        .factory('Group', function ($resource, UIService) {
            var Group = $resource('/groups/:groupId',
                { groupId: '@id'},
                { update: {method: 'POST'} }
            );

            Group.prototype.update = function (cb) {
                this.id = parseInt(this.id);

                return Group.update({groupId: this.id},
                    angular.extend({}, this, {groupId: undefined}), cb);
            };

            Group.prototype.destroy = function (cb) {
                return Group.remove({groupId: this.id}, cb);
            };

            return Group;
        })
        .factory('SoapAction', function ($resource) {
            var SoapAction = $resource('/soapactions/:soapActionId',
                { soapActionId: '@id'},
                { update: {method: 'POST'} }
            );

            SoapAction.prototype.update = function (cb) {
                this.id = parseInt(this.id);
                return SoapAction.update({soapActionId: this.id},
                    angular.extend({}, this, {soapActionId: undefined}), cb);
            };

            return SoapAction;
        })
        .factory("SoapactionsService", function ($http) {
            return {
                findAll: function () {
                    return $http.get('/soapactions/listDatatable');
                },
                findAllAndSelect: function ($scope, $routeParams) {
                    $http.get('/soapactions/findall')
                        .success(function (soapactions) {
                            $scope.soapactions = soapactions;
                            $scope.soapactions.unshift({id: "all", name: "all"});
                            angular.forEach($scope.soapactions, function (value, key) {
                                if (value.name == $routeParams.soapaction) $scope.soapaction = value;
                            });

                        })
                        .error(function (resp) {
                            console.log("Error with SoapActionsService.findAllAndSelect" + resp);
                        });
                },
                regenerate: function () {
                    return $http.get('/soapactions/regenerate');

                }
            }
        })
        .factory("AnalysisService", function ($http) {
            return {
                findAll: function () {
                    return $http.get('/soapactions/listDatatable');
                }
            }
        })
        .factory("ServicesService", function ($http) {
            return {
                findAll: function (group) {
                    return $http.get('/services/'+group+'/listDatatable');
                }
            }
        })
        .factory("EnvironmentsService", function ($http) {
            return {
                findAll: function (group) {
                    return $http.get('/environments/'+group+'/listDatatable');
                },
                findAllAndSelect: function ($scope, environmentName, environmentGroup, myService) {
                    console.log("Selected environment group : " + environmentGroup)
                    $http.get('/environments/'+environmentGroup+'/options')
                        .success(function (environments) {
                            $scope.environments = environments;
                             if(environments.length==0){
                                console.log("No environments have been founded")
                                $scope.environments.unshift({id: "none", name: "none"});
                            }else{
                                 $scope.environments.unshift({id: "all", name: "all"});
                            }


                            if (environmentName != null || myService != null) {
                                angular.forEach($scope.environments, function (value, key) {
                                    if (environmentName != null && value.name == environmentName) {
                                        $scope.environment = value;
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
        })
        .factory("GroupsService", function ($http) {
            return {
                findAll: function () {
                    return $http.get('/groups/listDatatable');
                },
                findAllAndSelect: function ($scope, groupName, myEnvironment) {
                    $http.get('/groups/options')
                        .success(function (groups) {
                            $scope.groups = groups;
                            $scope.groups.unshift({id: "all", name: "all"});
                            if (groupName != null || myEnvironment != null) {
                                angular.forEach($scope.groups, function (value, key) {
                                    if (groupName != null && value.name == groupName) {
                                        $scope.group = value;
                                    }
                                    if (myEnvironment != null && value.id == myEnvironment.groupId) {
                                        myEnvironment.group = value;
                                    }
                                });
                            }

                        })
                        .error(function (resp) {
                            console.log("Error with GroupsService.findAllAndSelect" + resp);
                        });
                }
            }
        })
        .factory("CodesService", function ($http) {
            return {
                findAllAndSelect: function ($scope, $routeParams) {
                    $http.get('/status/findall')
                        .success(function (codes) {
                            $scope.codes = codes;
                            $scope.codes.unshift({id: "all", name: "all"});
                            angular.forEach($scope.codes, function (value, key) {
                                if (value.name == $routeParams.code) $scope.code = value;
                            });

                        })
                        .error(function (resp) {
                            console.log("Error with SoapActionsService.findAllAndSelect" + resp);
                        });
                }
            }
        })
        .factory("UIService",function ($location, $filter) {
            return {
                reloadPage: function ($scope, $routeParams) {
                    var group ="all", environment = "all", soapaction = "all", mindate = "all", maxdate = "all", code = "all";

                    if ($scope.environment) environment = $scope.environment.name;

                    if ($routeParams.group) group = $routeParams.group;

                    if ($scope.group) group = $scope.group.name;



                    if ($scope.showSoapactions) {
                        if ($scope.soapaction) soapaction = $scope.soapaction.name;
                    }
                    if ($scope.mindate && $scope.mindate != "" && $scope.mindate != "All") {
                        mindate = this.initDayToUrl($filter('date')($scope.mindate, 'yyyy-MM-dd'));

                    }
                    if ($scope.maxdate && $scope.maxdate != "" && $scope.maxdate != "All") {
                        maxdate = this.initDayToUrl($filter('date')($scope.maxdate, 'yyyy-MM-dd'));
                    }
                    if ($scope.code) code = $scope.code.name;

                    var path = $scope.ctrlPath + '/' + group + "/" + environment + "/";

                    if ($scope.showSoapactions) path = path + soapaction + "/";

                    path = path + mindate + "/" + maxdate + "/" + code;

                    console.log("UIService.reloadPage : Go to " + path);
                    $location.path(path);
                },
                 reloadAdminPage: function ($scope) {
                                    var group ="all";
                                    if ($scope.group) group = $scope.group.name;

                                    var path = $scope.adminPath + '/' + group ;

                                    console.log("UIService.reloadAdminPage : Go to " + path);
                                    $location.path(path);
                 },
                getDateFromParam: function (indate) {
                    if (indate && indate != "all") {
                        if (indate == "yesterday" || indate == "today") {
                            indate = this.getDay(indate);
                        }
                        var elem = indate.split('-');
                        return new Date(elem[0], elem[1] - 1, elem[2]);
                    }
                },
                initDayToUrl: function(val, defaultValue) {
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
                    switch (sDay) {
                        case "today" :
                            nb = 0;
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
                    return mDate.getFullYear() + "-" + month + "-" + day;
                },
                fixBoolean: function (val) {
                    return (val == "yes" || val == true) ? true : false;
                },
                fixBooleanReverse: function (val) {
                    return (val == "true" || val == true) ? "yes" : "no";
                }
            }
        }).factory('ReplayService', function ($http, $rootScope, $location) {
            return {
                replay: function (id) {
                    $http.get('/replay/' + id)
                        .success(function (data) {
                            console.log("Success replay id" + id);
                            $rootScope.$broadcast('refreshSearchTable');
                        })
                        .error(function (resp) {
                            console.log("Error replay id" + id);
                            console.log("location:" + $location.path())
                            $rootScope.$broadcast('refreshSearchTable');
                        });
                }
            }
        })
});