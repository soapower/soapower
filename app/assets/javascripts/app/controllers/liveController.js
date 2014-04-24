function LiveCtrl($scope, $location, $window, $routeParams) {
    $scope.ctrlPath = "live";
    $scope.isLiveOn = false;
    $scope.isError = false;
    $scope.nbConnected = 0;
    $scope.liveData = [];
    $scope.showTips = false;

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
            console.log("Websocket closed")
        } else {
            console.log("Websocket already closed")
        }
        $scope.isLiveOn = false;
    };

    $scope.startWS = function () {
        if ($scope.isLiveOn == false) {
            $scope.isLiveOn = true;
            var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
            $scope.socketLive = new WS("ws://" + $location.host() + ":" + $location.port() + "/live/socket");
            console.log("Websocket started");
            $scope.socketLive.onmessage = receiveEvent
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

    $scope.startWS();

}
