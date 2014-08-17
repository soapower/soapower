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
