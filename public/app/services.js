/*global define */

'use strict';

define(['angular'], function (angular) {

    /* Services */
    angular.module('spApp.services', [])
        .factory("EnvironmentsService", function ($http) {
            return {
                findAllAndSelect: function ($scope, $routeParams) {
                    $http.get('/environments/findall')
                        .success(function (environments) {
                            $scope.environments = environments;
                            angular.forEach($scope.environments, function (value, key) {
                                if (value.name == $routeParams.environment) $scope.environment = value;
                            });

                        })
                        .error(function (resp) {
                            console.log("Error with EnvironmentsService.findAllAndSelect" + resp);
                        });
                }
            }
        })
        .factory("SoapActionsService", function ($http) {
            return {
                findAllAndSelect: function ($scope, $routeParams) {
                    $http.get('/soapactions/findall')
                        .success(function (soapactions) {
                            $scope.soapactions = soapactions;
                            angular.forEach($scope.soapactions, function (value, key) {
                                if (value.name == $routeParams.soapaction) $scope.soapaction = value;
                            });

                        })
                        .error(function (resp) {
                            console.log("Error with SoapActionsService.findAllAndSelect" + resp);
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
                reloadPage: function ($scope) {
                    var environment = "all", soapaction = "all", mindate = "all", maxdate = "all", code = "all";

                    if ($scope.environment) environment = $scope.environment.name;
                    if ($scope.soapaction) soapaction = $scope.soapaction.name;
                    if ($scope.mindate && $scope.mindate != "" && $scope.mindate != "All") {
                        mindate = $filter('date')($scope.mindate, 'yyyy-MM-dd');
                    }
                    if ($scope.maxdate && $scope.maxdate != "" && $scope.maxdate != "All") {
                        maxdate = $filter('date')($scope.maxdate, 'yyyy-MM-dd');
                    }
                    if ($scope.code) code = $scope.code.name;

                    var path = $scope.ctrlPath + '/'
                        + environment + "/"
                        + soapaction + "/"
                        + mindate + "/"
                        + maxdate + "/"
                        + code;
                    console.log("UIService.reloadPage : Go to " + path);
                    $location.path(path);
                },
                getDateFromParam: function (indate) {
                    if (indate && indate != "all") {
                        var elem = indate.split('-');
                        return new Date(elem[0], elem[1] - 1, elem[2]);
                    }
                }
            }
        }).factory('ReplayService',function ($http, $rootScope, $location, $window) {
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
        }).factory("AdminService", function ($http) {
            return {
                downloadConfiguration: function () {

                }
            }
        })
        /*
         .factory('SharedService', function($rootScope) {
         var sharedService = {};

         sharedService.message = '';

         sharedService.prepForBroadcast = function(msg) {
         this.message = msg;
         this.broadcastItem();
         };

         sharedService.broadcastItem = function() {
         $rootScope.$broadcast('handleBroadcast');
         };

         return sharedService;
         })*/;

});