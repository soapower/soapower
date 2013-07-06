
function SearchCtrl ($scope, $http, $location, $routeParams, $window, ReplayService) {
    $scope.ctrlPath = "search";

    $scope.showTips = false;
    $scope.hostname = $location.host();
    $scope.port = $location.port();

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
            '&iDisplayStart=' + (page) +
            '&iDisplayLength=' + pageSize +
            '&call=' + new Date();

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
            {field: 'status', displayName: 'Status', width: '60px', cellTemplate: 'partials/common/cellStatusTemplate.html'},
            {field: 'env', displayName: 'Environment', width: '100px'},
            {field: 'sender', displayName: 'Sender',  width: '100px'},
            {field: 'soapAction', displayName: 'SoapAction'},
            {field: 'startTime', displayName: 'StartTime', width: '200px' },
            {field: 'time', displayName: 'TimeInMillis', width: '100px'},
            {field: 'purged', displayName: 'Request', width: '80px', cellTemplate: 'partials/common/cellRequestTemplate.html'},
            {field: 'purged', displayName: 'Response', width: '80px', cellTemplate: 'partials/common/cellResponseTemplate.html'},
            {field: 'purged', displayName: 'Replay', width: '80px', cellTemplate: 'partials/common/cellReplayTemplate.html'}
        ]

    };
}
