function SearchCtrl($scope, $rootScope, $http, $location, $routeParams, $filter, ngTableParams, UIService) {
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
