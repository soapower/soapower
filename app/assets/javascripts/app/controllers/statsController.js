function StatsCtrl($scope, $http, $location, $routeParams) {
    $scope.ctrlPath = "stats";

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

    $scope.getPagedDataAsync = function (pageSize, page) {
        var environment = $routeParams.environment ? $routeParams.environment : 'all';
        var mindate = $routeParams.mindate ? $routeParams.mindate : 'all';
        var maxdate = $routeParams.maxdate ? $routeParams.maxdate : 'all';
        var code = $routeParams.code ? $routeParams.code : 'all';
        var url = $scope.ctrlPath + '/' + environment +
            '/' + mindate +
            '/' + maxdate +
            '/' + code +
            '/listDatatable?' +
            'sSearch=' +
            '&iDisplayStart=' + (page - 1) +
            '&iDisplayLength=' + pageSize +
            '&call=' + new Date();

        $http({ method: 'GET', url: url, cache: false })
            .success(function (largeLoad) {
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