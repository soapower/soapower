function LiveCtrl($scope, $location, $window) {
    $scope.ctrlPath = "live";
    $scope.isLiveOn = false;
    $scope.isError = false;
    $scope.nbConnected = 0;
    $scope.myData = [];
    $scope.showTips = false;

    $scope.hostname = $location.host();
    $scope.port = $location.port();

    // leave the page -> close websocket
    $scope.$on('$locationChangeStart', function (event, next, current) {
        if ($scope.isLiveOn == true && angular.isDefined($scope.socketLive)) {
            console.log("Websocket force closed")
            $scope.socketLive.close();
        }
    });

    var receiveEvent = function (event) {
        var data = JSON.parse(event.data)

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

        if (data.kind == "talkRequestData") {
           // $('#datas').dataTable().fnAddData([ data.message["0"] ]);
            $scope.myData.push(data.message["0"])
        }

        $scope.nbConnected = data.members.length - 1;// substract Robot
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
            $scope.socketLive = new WS("ws://" + $location.host() + ":" + $location.port() + "/live/socket")
            console.log("Websocket started");
            $scope.socketLive.onmessage = receiveEvent
        } else {
            console.log("Websocket already started");
        }
    }

    $scope.filterOptions = {
        filterText: "",
        useExternalFilter: true
    };

    $scope.gridOptions = {
        data: 'myData',
        enablePaging: false,
        showFooter: false,
        sortInfo : { fields: ['startTime'], directions: ['desc']},
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

    $scope.startWS();

}
