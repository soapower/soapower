function SearchCtrl($scope, $rootScope, $http, $location, $routeParams, $window, $filter, ngTableParams, UIService) {
    $scope.ctrlPath = "search";

    $scope.showTips = false;
    $scope.hostname = $location.host();
    $scope.port = $location.port();

    $scope.totalServerItems = 0;

    $scope.reloadTable = function () {
        var groups = $routeParams.groups ? $routeParams.groups : 'all';
        var environment = $routeParams.environment ? $routeParams.environment : 'all';
        var soapaction = $routeParams.soapaction ? $routeParams.soapaction : 'all';
        var mindate = $routeParams.mindate ? $routeParams.mindate : 'all';
        var maxdate = $routeParams.maxdate ? $routeParams.maxdate : 'all';
        var code = $routeParams.code ? $routeParams.code : 'all';
        var url = '/search/' + group +
            '/' + environment +
            '/' + encodeURIComponent(soapaction) +
            '/' + mindate +
            '/' + maxdate +
            '/' + code +
            '/listDatatable?' +
            'iDisplayStart=' + 1 +
            '&iDisplayLength=' + 10000 +
            '&call=' + new Date();

        $http({
            method: 'GET',
            url: url,
            cache: false
        }).success(function (largeLoad) {
            $scope.data = largeLoad.data;

            $scope.tableParams = new ngTableParams({
                page: 1,            // show first page
                count: 10,          // count per page
                sorting: {
                    'startTime': 'desc'     // initial sorting
                }
            }, {
                total: largeLoad.data.length, // length of data
                getData: function ($defer, params) {
                    var datafilter = $filter('customAndSearch');
                    var requestsData = datafilter(largeLoad.data, $scope.tableFilter);
                    var orderedData = params.sorting() ? $filter('orderBy')(requestsData, params.orderBy()) : requestsData;
                    var res = orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count());
                    params.total(orderedData.length)
                    $defer.resolve(res);
                },
                $scope: { $data: {} }
            });

            $scope.$watch("tableFilter", function () {
                $scope.tableParams.reload()
            });

        });
    }

    $scope.reloadTable();

    $scope.dlRequest = function (asFile, row) {
        if (row.purged == "true") {
            $window.alert("Sorry, Request already purged...");
        } else {
            var url = "/download/request/" + row.id;
            if (asFile) url += "?asFile=true";
            $window.open(url);
        }
    };

    $scope.dlResponse = function (asFile, row) {
        if (row.purged == "true") {
            $window.alert("Sorry, Response already purged...");
        } else {
            var url = "/download/response/" + row.id;
            if (asFile) url += "?asFile=true";
            $window.open(url);
        }
    };

    $scope.$on('refreshSearchTable', function (event) {
        console.log("Receive Broadcast event : refreshSearchTable");
        $scope.reloadTable();
    });

    $rootScope.$broadcast("showGroupsFilter", $routeParams.groups, "SearchCtrl");

    $scope.$on("ReloadPage", function (event) {
        UIService.reloadPage($scope, true);
    });
}
