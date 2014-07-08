function SearchCtrl($scope, $rootScope, $http, $location, $routeParams, $window, $filter, ngTableParams, UIService) {
    $scope.ctrlPath = "search";
    $scope.showTips = false;
    $scope.hostname = $location.host();
    $scope.port = $location.port();
    $scope.totalServerItems = 0;
    $scope.waitForData = false;

    $scope.reloadTable = function () {
        var groups = $routeParams.groups ? $routeParams.groups : 'all';
        var environment = $routeParams.environment ? $routeParams.environment : 'all';
        var serviceaction = $routeParams.serviceaction ? $routeParams.serviceaction : 'all';
        var mindate = $routeParams.mindate ? $routeParams.mindate : 'all';
        var maxdate = $routeParams.maxdate ? $routeParams.maxdate : 'all';
        var code = $routeParams.code ? $routeParams.code : 'all';
        var search = $routeParams.search ? $routeParams.search : '';
        $scope.waitForData = true;
        var request = "true";
        var response = "true";

        if ($routeParams.request === "false") {
            request = "false";
        }
        if ($routeParams.response === "false") {
            response = "false";
        }

        $scope.tableParams = new ngTableParams({
            page: 1,            // show first page
            count: 10,          // count per page
            sorting: {
                'startTime': 'desc'     // initial sorting
            }
        }, {
            total: 0,//largeLoad.data.length, // length of data
            getData: function ($defer, params) {
                var orderedData = params.sorting();
                var sortKey = Object.keys(orderedData)[0];
                var sortVal = orderedData[sortKey];

                var url = '/search/' + groups +
                    '/' + environment +
                    '/' + encodeURIComponent(serviceaction) +
                    '/' + mindate +
                    '/' + maxdate +
                    '/' + code +
                    '/listDatatable?' +
                    'sSearch=' + search +
                    '&request=' + request +
                    '&response=' + response +
                    '&page=' + params.page() +
                    '&pageSize=' + params.count() +
                    '&sortKey=' + sortKey +
                    '&sortVal=' + sortVal +
                    '&call=' + new Date();

                $http({
                    method: 'GET',
                    url: url,
                    cache: false
                }).success(function (newLoad) {
                    $scope.waitForData = false;
                    $scope.totalSize = newLoad.totalDataSize;
                    params.total(newLoad.totalDataSize);
                    $defer.resolve(newLoad.data);
                });
            }
        });
    };

    $scope.reloadTable();

    $scope.dlRequest = function (asFile, row) {
        if (row.purged == "true") {
            $window.alert("Sorry, Request already purged...");
        } else {
            if(asFile) {
                var url = "/download/request/" + row._id.$oid;
            }
            else {
                var url = "#/visualize/request/" + row._id.$oid;
            }
            $window.open(url);
        }
    };

    $scope.dlResponse = function (asFile, row) {
        if (row.purged == "true") {
            $window.alert("Sorry, Response already purged...");
        } else {
            if(asFile) {
                var url = "/download/response/" + row._id.$oid;
            }
            else {
                var url = "#/visualize/response/" + row._id.$oid
            }
            $window.open(url);
        }
    };

    $scope.$on('refreshSearchTable', function (event) {
        console.log("Receive Broadcast event : refreshSearchTable");
        $scope.reloadTable();
    });

    $rootScope.$broadcast("showGroupsFilter", $routeParams.groups, "SearchCtrl");

    $scope.$on("ReloadPage", function (event, newGroups) {
        if (newGroups) $scope.groups = newGroups;
        UIService.reloadPage($scope, true, "search");
    });
}

function VisualizeCtrl($scope, $rootScope, $http, $location, $routeParams, $window, UIService, $filter) {

    var requestOrResponse = $routeParams.requestorresponse;

    if(requestOrResponse == "request") {
        $http.get("/visualize/request/" + $routeParams.id)
            .success(function(data, status, headers) {
                $scope.displayInCorrectFormat(data, headers);
            })
            .error(function(data) {
                $scope.data = "Empty content";
            });
        $scope.getRaw = function() {
            var url = "/visualize/request/" + $routeParams.id;
            $window.open(url)
        };
        $scope.getDownload = function() {
            var url = "/download/request/" + $routeParams.id;
            $window.open(url)
        };
    }
    else if (requestOrResponse == "response") {
        $http.get("/visualize/response/" + $routeParams.id)
            .success(function(data, status, headers) {
                $scope.displayInCorrectFormat(data, headers);
            })
            .error(function(data) {
                $scope.data = "Empty content";
            });
        $scope.getRaw = function() {
            var url = "/visualize/response/" + $routeParams.id;
            $window.open(url)
        };
        $scope.getDownload = function() {
            var url = "/download/response/" + $routeParams.id;
            $window.open(url)
        };
    }
    else {
        $window.open("#/search");
    }


    // Pretty print the data in the correct format
    $scope.displayInCorrectFormat = function(data, headers) {
        if(UIService.startsWith(headers("Content-Type"), "application/json")) {
            $scope.data = angular.toJson(data, true);
        }
        else if (UIService.startsWith(headers("Content-Type"), "application/xml") || UIService.startsWith(headers("Content-Type"), "text/xml")) {
            $scope.data = data;
        }
        else {
            $scope.data = data
        }
    }
}

