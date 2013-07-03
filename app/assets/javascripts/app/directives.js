/*global define */

'use strict';

define(['angular'], function (angular) {

    /* Directives */

    angular.module('spApp.directives', []).
        directive('spCriterias', function () {
            return {
                restrict: 'E',
                scope: false,
                controller: function ($scope, $element, $attrs, $transclude, $location, $routeParams, EnvironmentsService, SoapactionsService, CodesService, UIService) {

                    EnvironmentsService.findAllAndSelect($scope, $routeParams);
                    SoapactionsService.findAllAndSelect($scope, $routeParams);
                    CodesService.findAllAndSelect($scope, $routeParams);

                    $scope.mindate = UIService.getDateFromParam($routeParams.mindate);
                    $scope.maxdate = UIService.getDateFromParam($routeParams.maxdate);

                    $scope.changeCriteria = function () {
                        UIService.reloadPage($scope);
                    };

                    $scope.$watch('mindate', function () {
                        if ($scope.mindate) {
                            $scope.showmindate = false;
                            if ($scope.mindate > $scope.maxdate) {
                                $scope.maxdate = $scope.mindate;
                            }
                        }
                    });
                    $scope.$watch('maxdate', function () {
                        if ($scope.maxdate) {
                            $scope.showmaxdate = false;
                            if ($scope.mindate > $scope.maxdate) {
                                $scope.maxdate = $scope.mindate;
                            }
                        }
                    });
                },
                templateUrl: 'app/common/views/criterias.html',
                replace: true
            }
        });
});