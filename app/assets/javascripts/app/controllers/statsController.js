function StatsCtrl($scope, $rootScope, $http, $location, $routeParams, UIService) {
    $scope.ctrlPath = "stats";

    $scope.showTips = false;
    $scope.hostname = $location.host();
    $scope.port = $location.port();

    var group = $routeParams.group ? $routeParams.group : 'all';
    var environment = $routeParams.environment ? $routeParams.environment : 'all';
    var mindate = $routeParams.mindate ? $routeParams.mindate : 'all';
    var maxdate = $routeParams.maxdate ? $routeParams.maxdate : 'all';
    var code = $routeParams.code ? $routeParams.code : 'all';
    var url = $scope.ctrlPath +
        '/' + group +
        '/' + environment +
        '/' + mindate +
        '/' + maxdate +
        '/' + code +
        '/listDatatable?' +
        'sSearch=' +
        '&iDisplayStart=' + 0 +
        '&iDisplayLength=' + 10000 +
        '&call=' + new Date();

    $http({ method: 'GET', url: url, cache: false })
        .success(function (dataJson) {
            $scope.data = dataJson.data;
    });


    $scope.$on("ReloadPage", function (event) {
        UIService.reloadPage($scope, false);
    });
}