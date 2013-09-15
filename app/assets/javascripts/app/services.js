'use strict';

spApp.factory('Service', function ($resource, UIService) {
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
});

spApp.factory('Environment', function ($resource, UIService) {
    var Environment = $resource('/environments/:environmentId',
        { environmentId: '@id'},
        { update: {method: 'POST'} }
    );

    Environment.prototype.update = function (cb, cbError) {
        this.recordXmlData = UIService.fixBoolean(this.recordXmlData);
        this.recordData = UIService.fixBoolean(this.recordData);
        this.id = parseInt(this.id);
        this.groupId = parseInt(this.group.id);

        return Environment.update({environmentId: this.id},
            angular.extend({}, this, {environmentId: undefined}), cb, cbError);
    };

    Environment.prototype.destroy = function (cb) {
        return Environment.remove({environmentId: this.id}, cb);
    };

    return Environment;
});

spApp.factory('Mock', function ($resource) {
    var Mock = $resource('/mocks/:mockId',
        { mockId: '@id'},
        { update: {method: 'POST'} }
    );

    Mock.prototype.update = function (cb, cbError) {
        this.id = parseInt(this.id);
        this.mockGroupId = parseInt(this.mockGroup.id);

        return Mock.update({mockId: this.id},
            angular.extend({}, this, {mockId: undefined}), cb, cbError);
    };

    Mock.prototype.destroy = function (cb) {
        return Mock.remove({mockId: this.id}, cb);
    };

    return Mock;
});

spApp.factory('MockGroup', function ($resource) {
    var MockGroup = $resource('/mockgroups/:mockGroupId',
        { mockGroupId: '@id'},
        { update: {method: 'POST'} }
    );

    MockGroup.prototype.update = function (cb, cbError) {
        this.id = parseInt(this.id);
        this.groupId = parseInt(this.group.id);

        return MockGroup.update({mockGroupId: this.id},
            angular.extend({}, this, {mockGroupId: undefined}), cb, cbError);
    };

    MockGroup.prototype.destroy = function (cb) {
        return MockGroup.remove({mockGroupId: this.id}, cb);
    };

    return MockGroup;
});

spApp.factory('Group', function ($resource, UIService) {
    var Group = $resource('/groups/:groupId',
        { groupId: '@id'},
        { update: {method: 'POST'} }
    );

    Group.prototype.update = function (cb, cbError) {
        this.id = parseInt(this.id);

        return Group.update({groupId: this.id},
            angular.extend({}, this, {groupId: undefined}), cb, cbError);
    };

    Group.prototype.destroy = function (cb) {
        return Group.remove({groupId: this.id}, cb);
    };

    return Group;
});

spApp.factory('SoapAction', function ($resource) {
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
});

spApp.factory("SoapactionsService", function ($http) {
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
                        if (encodeURIComponent(value.name) == $routeParams.soapaction) $scope.soapaction = value;
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
});

spApp.factory("AnalysisService", function ($http) {
    return {
        findAll: function () {
            return $http.get('/soapactions/listDatatable');
        }
    }
});

spApp.factory("ServicesService", function ($http) {
    return {
        findAll: function (group) {
            return $http.get('/services/' + group + '/listDatatable');
        }
    }
});

spApp.factory("EnvironmentsService", function ($http) {
    return {
        findAll: function (group) {
            return $http.get('/environments/' + group + '/listDatatable');
        },
        findAllAndSelect: function ($scope, environmentName, environmentGroup, myService, addAll) {
            $http.get('/environments/' + environmentGroup + '/options')
                .success(function (environments) {
                    $scope.environments = environments;
                    if (environments.length == 0) {
                        console.log("No environments have been founded")
                        $scope.environments.unshift({id: "error", name: "error. No Env with this group"});
                    } else if (addAll) {
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
});

spApp.factory("MocksService", function ($http) {
    return {
        findAll: function (mockGroup) {
            return $http.get('/mocks/' + mockGroup + '/listDatatable');
        }
    }
});

spApp.factory("MockGroupsService", function ($http) {
    return {
        findAll: function (group) {
            return $http.get('/mockgroups/' + group + '/listDatatable');
        }
    }
});

spApp.factory("GroupsService", function ($http) {
    return {
        findAll: function () {
            return $http.get('/groups/listDatatable');
        },
        findAllAndSelect: function ($scope, $rootScope, groupName, myEnvironment, withAll) {
            $http.get('/groups/options')
                .success(function (groups) {
                    $scope.groups = groups;
                    var groupAll = {id: "all", name: "all"}
                    if (withAll) $scope.groups.unshift(groupAll);
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
                    if ($rootScope != null && !$scope.group) {
                        $scope.group = groupAll;
                        $rootScope.group = groupAll;
                    }

                })
                .error(function (resp) {
                    console.log("Error with GroupsService.findAllAndSelect" + resp);
                });
        }
    }
});

spApp.factory("CodesService", function ($http) {
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
});

spApp.factory("UIService",function ($location, $filter, $routeParams) {
    return {
        reloadPage: function ($scope, showSoapactions) {
            var environment = "all", soapaction = "all", mindate = "all", maxdate = "all", code = "all";

            if ($scope.environment) environment = $scope.environment.name;

            if (showSoapactions && $scope.soapaction) {
                soapaction = encodeURIComponent($scope.soapaction.name);
            }

            if ($scope.mindate && $scope.mindate != "" && $scope.mindate != "All") {
                mindate = this.initDayToUrl($filter('date')($scope.mindate, 'yyyy-MM-dd'), "today");

            }
            if ($scope.maxdate && $scope.maxdate != "" && $scope.maxdate != "All") {
                maxdate = this.initDayToUrl($filter('date')($scope.maxdate, 'yyyy-MM-dd'), "today");
            }
            if ($scope.code) code = $scope.code.name;

            var path = $scope.ctrlPath + '/' + $routeParams.group + "/" + environment + "/";

            if (showSoapactions) path = path + soapaction + "/";

            path = path + mindate + "/" + maxdate + "/" + code;

            console.log("UIService.reloadPage : Go to " + path);
            $location.path(path);
        },
        reloadAdminPage: function ($scope, group) {
            var path = $scope.ctrlPath + '/' + group;
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
});

spApp.factory('ReplayService', function ($http, $rootScope, $location) {
    return {
        beforeReplay: function(id) {
            var url = "/download/request/" + id;
            return $http.get(url);
        },
        replay: function (id, data) {
            var url = '/replay/' + id;
            $http({ method: "POST",
                url: url,
                data: data.data,
                headers: { "Content-Type" : "application/xml" }
            }).success(function (data) {
                console.log("Success replay id" + id);
                $rootScope.$broadcast('refreshSearchTable');
            }).error(function (resp) {
                console.log("Error replay id" + id);
                console.log("location:" + $location.path())
                $rootScope.$broadcast('refreshSearchTable');
            });
        }
    }
});