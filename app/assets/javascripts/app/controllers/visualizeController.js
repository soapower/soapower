function VisualizeCtrl($scope, $http, $location, $routeParams, $window, UIService) {

    $scope.requestOrResponse = $routeParams.requestorresponse;

    $http.get("/visualize/" + $scope.requestOrResponse + "/" + $routeParams.id)
        .success(function (data, status, headers) {
            $scope.displayInCorrectFormat(data, headers);
        })
        .error(function (data) {
            $scope.data = "Empty content";
        });

    $scope.getRaw = function () {
        var url = "/visualize/" + $scope.requestOrResponse + "/" + $routeParams.id;
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
            $scope.data = angular.toJson(data, true);
        } else if (UIService.startsWith(headers("Content-Type"), "application/xml") || UIService.startsWith(headers("Content-Type"), "text/xml")) {
            $scope.data = data;
        } else {
            $scope.data = data
        }
    }
}
