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
            '&iDisplayStart=' + 1 +
            '&iDisplayLength=' + 10 +
            '&call=' + new Date();
        $http({
            method: 'GET',
            url: url,
            cache: false
        }).success(function (largeLoad) {
            $scope.data = largeLoad.data;
            $scope.totalSize = largeLoad.totalDataSize
            $scope.waitForData = false;
            $scope.tableParams = new ngTableParams({
                page: 1,            // show first page
                count: 10,          // count per page
                sorting: {
                    'startTime': 'desc'     // initial sorting
                }
            }, {
                total: largeLoad.data.length, // length of data
                getData: function ($defer, params) {
                    // At each changes on page or on number of displayed request
                    // The datas are retrieved, and the number of data to get is (page number) * (number of requests per page)
                        var numberRequestToLoad = params.page() * params.count()
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
                            '&iDisplayStart=' + 1 +
                            '&iDisplayLength=' + numberRequestToLoad +
                            '&call=' + new Date();
                        $http({
                            method: 'GET',
                            url: url,
                            cache: false
                        }).success(function(newLoad) {
                            var datafilter = $filter('customAndSearch');
                            var requestsData = datafilter(newLoad.data, $scope.tableFilter);
                            var orderedData = params.sorting() ? $filter('orderBy')(requestsData, params.orderBy()) : requestsData;
                            var res = orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count());
                            params.total(newLoad.totalDataSize)
                            $defer.resolve(res);
                        });

                },
                $scope: { $data: {} }
            });

            $scope.$watch("tableFilter", function () {
                $scope.tableParams.reload()
            });

        });
    };

    $scope.reloadTable();

    $scope.dlRequest = function (asFile, row) {
        if (row.purged == "true") {
            $window.alert("Sorry, Request already purged...");
        } else {
            var url = "/download/request/" + row._id.$oid + "/" + asFile;
            $window.open(url);
        }
    };

    $scope.dlResponse = function (asFile, row) {
        if (row.purged == "true") {
            $window.alert("Sorry, Response already purged...");
        } else {
            var url = "/download/response/" + row._id.$oid + "/" + asFile;
            $window.open(url);
        }
    };

    $scope.$on('refreshSearchTable', function (event) {
        console.log("Receive Broadcast event : refreshSearchTable");
        $scope.reloadTable();
    });

    $rootScope.$broadcast("showGroupsFilter", $routeParams.groups, "SearchCtrl");

    $scope.$on("ReloadPage", function (event, newGroups) {
        if(newGroups) $scope.groups = newGroups;
        UIService.reloadPage($scope, true, "search");
    });
}
