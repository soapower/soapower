/*global define */

'use strict';

define(function () {

    /* Controllers */

    var controllers = {};

    controllers.SearchCtrl = function ($scope, $http, $routeParams, $window, ReplayService) {
        $scope.ctrlPath = "search";

        $scope.filterOptions = {
            filterText: "",
            useExternalFilter: true
        };
        $scope.pagingOptions = {
            pageSizes: [5, 50, 100, 250, 500, 1000],
            pageSize: 5,
            totalServerItems: 0,
            currentPage: 1
        };
        $scope.setPagingData = function (data, page, pageSize) {
            var pagedData = data.data;
            $scope.myData = pagedData;
            $scope.pagingOptions.totalServerItems = data.iTotalDisplayRecords;
            if (!$scope.$$phase) {
                $scope.$apply();
            }
        };

        $scope.getPagedDataAsync = function (pageSize, page, searchText) {
            var environment = $routeParams.environment ? $routeParams.environment : 'all';
            var soapaction = $routeParams.soapaction ? $routeParams.soapaction : 'all';
            var mindate = $routeParams.mindate ? $routeParams.mindate : 'all';
            var maxdate = $routeParams.maxdate ? $routeParams.maxdate : 'all';
            var code = $routeParams.code ? $routeParams.code : 'all';
            var url = '/search/' + environment +
                '/' + soapaction +
                '/' + mindate +
                '/' + maxdate +
                '/' + code +
                '/listDatatable?' +
                'sSearch=' +
                '&iDisplayStart=' + (page - 1) +
                '&iDisplayLength=' + pageSize +
                '&call=' + new Date();
            console.log("URL:" + url);

            $http({
                method: 'GET',
                url: url,
                cache: false
            }).success(function (largeLoad) {
                    console.log("setPagingData");
                    $scope.setPagingData(largeLoad, page, pageSize);
                });
        };

        $scope.reloadTable = function () {
            $scope.myData = null;
            $scope.getPagedDataAsync($scope.pagingOptions.pageSize, $scope.pagingOptions.currentPage);
        }

        $scope.reloadTable();

        $scope.$watch('pagingOptions', function (newVal, oldVal) {
            if (newVal !== oldVal && newVal.currentPage !== oldVal.currentPage) {
                $scope.getPagedDataAsync($scope.pagingOptions.pageSize, $scope.pagingOptions.currentPage, $scope.filterOptions.filterText);
            }
        }, true);
        $scope.$watch('filterOptions', function (newVal, oldVal) {
            if (newVal !== oldVal) {
                $scope.getPagedDataAsync($scope.pagingOptions.pageSize, $scope.pagingOptions.currentPage, $scope.filterOptions.filterText);
            }
        }, true);

        $scope.dlRequest = function (asFile, row) {
            if (row.getProperty("purged") == "true") {
                $window.alert("Sorry, Request already purged...");
            } else {
                var url = "/download/request/" + row.getProperty("id");
                if (asFile) url += "?asFile=true";
                $window.open(url);
            }
        };

        $scope.dlResponse = function (asFile, row) {
            if (row.getProperty("purged") == "true") {
                $window.alert("Sorry, Response already purged...");
            } else {
                var url = "/download/request/" + row.getProperty("id");
                if (asFile) url += "?asFile=true";
                $window.open(url);
            }
        };

        $scope.replayReq = function (row) {
            ReplayService.replay(row.getProperty("id"))
        };

        $scope.$on('refreshSearchTable', function (event) {
            console.log("Receive Broadcast event : refreshSearchTable");
            $scope.reloadTable();
        });

        $scope.gridOptions = {
            data: 'myData',
            enablePaging: true,
            showFooter: true,
            pagingOptions: $scope.pagingOptions,
            filterOptions: $scope.filterOptions,
            columnDefs: [
                {field: 'status', displayName: 'Status', cellTemplate: 'partials/cellStatusTemplate.html'},
                {field: 'env', displayName: 'Environment'},
                {field: 'sender', displayName: 'Sender'},
                {field: 'soapAction', displayName: 'SoapAction'},
                {field: 'startTime', displayName: 'StartTime'},
                {field: 'time', displayName: 'TimeInMillis'},
                {field: 'purged', displayName: 'Request', cellTemplate: 'partials/cellRequestTemplate.html'},
                {field: 'purged', displayName: 'Response', cellTemplate: 'partials/cellResponseTemplate.html'},
                //cellTemplate: '<div ng-class="{green: row.getProperty(col.field) > 30}"><div class="ngCellText">{{row.getProperty(col.field)}}</div></div>'},
                {field: 'purged', displayName: 'Replay', cellTemplate: 'partials/cellReplayTemplate.html'}
            ]

        };
    }

    controllers.SearchCtrl.$inject = [ '$scope', '$http', '$routeParams', '$window', 'ReplayService', 'UIService'];

    controllers.StatsCtrl = function ($scope, $http, $routeParams, $window, ReplayService) {
        $scope.ctrlPath = "stats";

        $scope.filterOptions = {
            filterText: "",
            useExternalFilter: true
        };
        $scope.pagingOptions = {
            pageSizes: [5, 50, 100, 250, 500, 1000],
            pageSize: 5,
            totalServerItems: 0,
            currentPage: 1
        };
        $scope.setPagingData = function (data, page, pageSize) {
            var pagedData = data.data;
            $scope.myData = pagedData;
            $scope.pagingOptions.totalServerItems = data.iTotalDisplayRecords;
            if (!$scope.$$phase) {
                $scope.$apply();
            }
        };

        $scope.getPagedDataAsync = function (pageSize, page) {
            var environment = $routeParams.environment ? $routeParams.environment : 'all';
            var soapaction = $routeParams.soapaction ? $routeParams.soapaction : 'all';
            var mindate = $routeParams.mindate ? $routeParams.mindate : 'all';
            var maxdate = $routeParams.maxdate ? $routeParams.maxdate : 'all';
            var code = $routeParams.code ? $routeParams.code : 'all';
            var url = $scope.ctrlPath + '/' + environment +
                '/' + soapaction +
                '/' + mindate +
                '/' + maxdate +
                '/' + code +
                '/listDatatable?' +
                'sSearch=' +
                '&iDisplayStart=' + (page - 1) +
                '&iDisplayLength=' + pageSize +
                '&call=' + new Date();
            console.log("URL:" + url);

            $http({
                method: 'GET',
                url: url,
                cache: false
            }).success(function (largeLoad) {
                    console.log("setPagingData");
                    $scope.setPagingData(largeLoad, page, pageSize);
                });
        };

        $scope.reloadTable = function () {
            $scope.myData = null;
            $scope.getPagedDataAsync($scope.pagingOptions.pageSize, $scope.pagingOptions.currentPage);
        }

        $scope.reloadTable();

        $scope.$watch('pagingOptions', function (newVal, oldVal) {
            if (newVal !== oldVal && newVal.currentPage !== oldVal.currentPage) {
                $scope.getPagedDataAsync($scope.pagingOptions.pageSize, $scope.pagingOptions.currentPage, $scope.filterOptions.filterText);
            }
        }, true);
        $scope.$watch('filterOptions', function (newVal, oldVal) {
            if (newVal !== oldVal) {
                $scope.getPagedDataAsync($scope.pagingOptions.pageSize, $scope.pagingOptions.currentPage, $scope.filterOptions.filterText);
            }
        }, true);

        $scope.gridOptions = {
            data: 'myData',
            enablePaging: true,
            showFooter: true,
            pagingOptions: $scope.pagingOptions,
            filterOptions: $scope.filterOptions,
            columnDefs: [
                {field: 'env', displayName: 'Environment'},
                {field: 'soapAction', displayName: 'SoapAction'},
                {field: 'avgTime', displayName: 'Avg Time In Millis'},
                {field: 'threshold', displayName: 'Threshold'}
            ]
        };

    }
    controllers.StatsCtrl.$inject = [ '$scope', '$http', '$routeParams', '$window', 'ReplayService', 'UIService'];

    controllers.AnalysisCtrl = function ($scope) {
        $scope.ctrlPath = "analysis";
    }
    controllers.AnalysisCtrl.$inject = [ '$scope' ];

    controllers.MyCtrl2 = function () {
    }
    controllers.MyCtrl2.$inject = [];

    controllers.AdminCtrl = function ($scope, $http, $routeParams, $window, EnvironmentsService) {
        $scope.urlDlConfig = "/admin/downloadConfiguration";
        $scope.urlDlRequestDataStatsEntries = "/admin/downloadRequestDataStatsEntries";
        $scope.urlUploadConfiguration = "/admin/uploadConfiguration";

        $scope.showResponseUpload = false;
        $scope.showUploadRunning = false;
        $scope.uploadComplete = function (content, completed) {
            if (completed && content.length > 0) {
                $scope.response =  JSON.parse(content);
                $scope.showUploadRunning = false;
                $scope.showResponseUpload = true;
            } else {
                $scope.showUploadRunning = true;
            }
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

        EnvironmentsService.findAllAndSelect($scope, $routeParams);
    }
    controllers.AdminCtrl.$inject = ['$scope', '$http', '$routeParams', '$window', 'EnvironmentsService'];

    return controllers;

});