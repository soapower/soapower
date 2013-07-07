function MonitorCtrl($scope, $location, $window, $http) {
    $scope.ctrlPath = "live";
    $scope.isLiveOn = false;
    $scope.isError = false;

    $scope.hostname = $location.host();
    $scope.port = $location.port();

    var initVal = d3.range(243).map(function () {
        return 0;
    });

    $scope.cpu = initVal;
    $scope.memory = initVal;
    $scope.totalRequests = 0;
    $scope.nbRequests = null;

    // leave the page -> close websocket
    $scope.$on('$locationChangeStart', function (event, next, current) {
        if ($scope.isLiveOn == true && angular.isDefined($scope.socketLive)) {
            console.log("Websocket force closed")
            $scope.$broadcast("stopMonitor", true);
            $scope.socketLive.close();
        }
    });

    $http.get('/monitor/logfile')
        .success(function (response) {
            $scope.logfile = response;
        })
        .error(function (resp) {
            $scope.errorInfo = "Failed to load logfile";
        });

    var receiveEvent = function (event) {
        //console.log(event);

        var data = event.data.split(":");
        var type = data[1];
        var newValue = parseFloat(data[0]);

        if (type == "cpu" || type == "memory") {
            $scope.$broadcast(type, newValue);
        }

        if (type == "cpu") $scope.cpuCurrentVal = newValue;
        if (type == "memory") $scope.memoryCurrentVal = newValue;
        if (type == "totalMemory") $scope.totalMemory = newValue;

        if (type == "nbReq") {
            if ($scope.nbRequests == null) {
                $scope.nbRequests = 0;
            } else {
                $scope.nbRequests = newValue - $scope.totalRequests;
            }
            $scope.totalRequests = newValue;
        }
        $scope.$broadcast("nbrequests", $scope.nbRequests);

        // Handle errors
        if (data.error || data.kind == "error") {
            socket.close()
            if (data.error) {
                $scope.errorInfo = data.error;
            } else if (data.kind == "error") {
                $scope.errorInfo = data.message;
            }
            $scope.isError = true;
            $scope.$apply();
            return
        }

        if (type != "cpu" && type != "memory" && type != "totalMemory" && type != "nbReq") {
            $('#logs').append(event.data)
            $('#logs').stop().animate({ scrollTop: $("#logs")[0].scrollHeight }, 800);
        }

        $scope.$apply();
    }

    $scope.stopWS = function () {
        if ($scope.isLiveOn == true && angular.isDefined($scope.socketLive)) {
            $scope.socketLive.close();
            console.log("Websocket closed")
        } else {
            console.log("Websocket already closed")
        }
        $scope.isLiveOn = false;
    }

    $scope.startWS = function () {
        if ($scope.isLiveOn == false) {
            $scope.isLiveOn = true;
            var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
            $scope.socketLive = new WS("ws://" + $location.host() + ":" + $location.port() + "/monitor/socket")
            console.log("Websocket started");
            $scope.socketLive.onmessage = receiveEvent
        } else {
            console.log("Websocket already started");
        }
    }

    $scope.downloadLogFile = function () {
        var url = "/monitor/downloadLogFile";
        $window.open(url);
    };

    $scope.garbage = function () {
        $http({
            url: '/gc!',
            method: 'POST'
        }).success(function (data) {
                alert("Garbage Collect : " + data);
            }).error(function (data) {
                alert("Error Garbage Collect : " + data);
            });

    };

    $scope.startWS();
}
