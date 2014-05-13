function LiveCtrl($scope, $rootScope, $location, $window, $routeParams, UIService) {
    $scope.choiceNbResults = [ 10, 50, 100, 1000, 10000 ];
    $scope.nbResults = 50

    $scope.ctrlPath = "live";
    // Used to handle criterias when the user manually close the websocket
    $scope.manuallyClosed = false;
    $scope.isLiveOn = false;
    $scope.isError = false;
    $scope.nbConnected = 0;
    $scope.liveData = [];
    $scope.showFilter = false;
    $scope.hostname = $location.host();
    $scope.port = $location.port();

    // leave the page -> close websocket
    $scope.$on('$locationChangeStart', function () {
        if ($scope.isLiveOn == true && angular.isDefined($scope.socketLive)) {
            console.log("Websocket force closed");
            $scope.socketLive.close();
        }
    });

    var receiveEvent = function (event) {
        var data = JSON.parse(event.data);
        // Handle errors
        if (data.error || data.kind == "error") {
            if (typeof socket != 'undefined') {
                socket.close()
            }
            if (data.error) {
                $scope.errorInfo = data.error;
            } else if (data.kind == "error") {
                $scope.errorInfo = data.message;
            }
            $scope.isError = true;
            $scope.$apply();
            return
        }

        if (data.kind == "talkRequestData") {
            $scope.liveData.push(data.message)
        }

        $scope.nbConnected = data.members.length - 1;// substract Robot
        $scope.$apply();
    };

    $scope.stopWS = function () {
        if ($scope.isLiveOn == true && angular.isDefined($scope.socketLive)) {
            $scope.socketLive.close();
            $scope.manuallyClosed = true;
            console.log("Websocket closed")
        } else {
            console.log("Websocket already closed")
        }
        // The websocket has been manually closed by the user (click on the stop button)
        $scope.manuallyClosed = true;
        $scope.isLiveOn = false;
    };
    $scope.startWS = function () {
        if ($scope.isLiveOn == false) {
            $scope.isLiveOn = true;
            var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
            if ($scope.manuallyClosed == false) {
                // The websocket is initialized using parameters from the routes
                var url = "ws://" + $location.host() + ":" + $location.port() + "/live/socket/"+$routeParams.groups+"/"+$routeParams.environment+
                                                          "/"+$routeParams.serviceaction+"/"+$routeParams.code;
            } else {
                // The websocket has been manually restart, the websocket is initialized using scope
                var url = "ws://" + $location.host() + ":" + $location.port() + "/live/socket/"+$routeParams.groups+"/"+$scope.$$childHead.environment+
                                                          "/"+$routeParams.serviceaction+"/"+$scope.$$childHead.code;
            }
            $scope.socketLive = new WS(url);
            console.log("Websocket started");
            $scope.socketLive.onmessage = receiveEvent;
        } else {
            console.log("Websocket already started");
        }
    };

    if ($routeParams.search) {
        $scope.filterSearch = $routeParams.search;
    }

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
    $rootScope.$broadcast("showGroupsFilter", $routeParams.groups, "LiveCtrl");

    $scope.startWS();

    $scope.$on("ReloadPage", function (event) {
            UIService.reloadPage($scope, true, "live");
        });
}
