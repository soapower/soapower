function VisualizeCtrl($scope, $http, $location, $routeParams, $window, UIService) {

    $scope.requestOrResponse = $routeParams.requestorresponse;

    $http.get("/visualize/content/" + $scope.requestOrResponse + "/" + $routeParams.id)
        .success(function (data, status, headers) {
            $scope.displayInCorrectFormat(data, headers);
        })
        .error(function (data) {
            $scope.data = "Empty content";
        });

    $http.get("/visualize/headers/" + $scope.requestOrResponse + "/" + $routeParams.id)
        .success(function (data, status) {
            $scope.headers = data;
        })
        .error(function (data) {
            $scope.headers = "Empty headers";
        });

    $http.get("/visualize/details/" + $routeParams.id)
        .success(function (data, status) {
            $scope.environment = data.environmentName;
            $scope.serviceId = data.serviceId;
            $scope.sender = data.sender;
            $scope.timeInMillis = data.timeInMillis;
            $scope.status = data.status;
            $scope.purged = data.purged;
            $scope.isMock = data.isMock;
        })
        .error(function (data) {
            $scope.headers = "Empty headers";
        });

    $scope.getRaw = function () {
        var url = "/visualize/content/" + $scope.requestOrResponse + "/" + $routeParams.id;
        $window.open(url)
    };

    $scope.getDownload = function () {
        var url = "/download/" + $scope.requestOrResponse + "/" + $routeParams.id;
        $window.open(url)
    };

    $scope.goOther = function (goTorequestOrResponse) {
        var url = "/visualize/" + goTorequestOrResponse + "/" + $routeParams.id;
        $location.path(url);
    };

    // Pretty print the data in the correct format
    $scope.displayInCorrectFormat = function (data, headers) {
        if (UIService.startsWith(headers("Content-Type"), "application/json")) {
            $scope.content = angular.toJson(data, true);
        } else if (UIService.startsWith(headers("Content-Type"), "application/xml") || UIService.startsWith(headers("Content-Type"), "text/xml")) {
            $scope.content = data.replace(/\"/g, '');
        } else {
            $scope.content = data;
        }
    }
}
