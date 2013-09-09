function SearchCtrl ($scope, $rootScope, $http, $location, $routeParams, $window, ReplayService, UIService) {
    $scope.ctrlPath = "search";

    $scope.showTips = false;
    $scope.hostname = $location.host();
    $scope.port = $location.port();

    $scope.totalServerItems = 0;
     
    $scope.reloadTable = function () {
        var group = $routeParams.group ? $routeParams.group : 'all';
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
            'sSearch=' +
            '&iDisplayStart=' + 1 +
            '&iDisplayLength=' + 10000 +
            '&call=' + new Date();

        $http({
            method: 'GET',
            url: url,
            cache: false
        }).success(function (largeLoad) {
            $scope.data = largeLoad.data;
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

    $rootScope.$broadcast("showGroupsFilter", $routeParams.group);

    $scope.$on("ReloadPage", function (event) {
        UIService.reloadPage($scope, true);
    });    
}
