function SoapActionsCtrl($scope, SoapactionsService, ngTableParams, $filter) {

    $scope.btnRegenerateDisabled = false;
    $scope.info = "";

    SoapactionsService.findAll().
        success(function (soapActions) {
            $scope.soapActions = soapActions.data;
            $scope.tableParams = new ngTableParams({
                page: 1,            // show first page
                count: 10,          // count per page
                sorting: {
                    'name': 'asc'     // initial sorting
                }
            }, {
                total: $scope.soapActions.length, // length of data
                getData: function ($defer, params) {
                    var datafilter = $filter('filter');
                    var soapActionsData = datafilter($scope.soapActions, $scope.tableFilter);
                    var orderedData = params.sorting() ? $filter('orderBy')(soapActionsData, params.orderBy()) : soapActionsData;
                    var res = orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count());
                    params.total(orderedData.length)
                    $defer.resolve(res);
                },
                $scope: { $data: {} }
            });

            $scope.$watch("tableFilter", function () {
                $scope.tableParams.reload()
            });
        })
        .error(function (resp) {
            console.log("Error with SoapActionsService.findAll" + resp);
        });

    $scope.regenerate = function () {
        $scope.info = "Running Generation...";
        $scope.btnRegenerateDisabled = true;

        SoapactionsService.regenerate()
            .success(function (resp) {
                $scope.info = "Success generate SoapAction list";
                $scope.btnRegenerateDisabled = false;
            })
            .error(function (resp) {
                console.log("Error with SoapActionsService.regenerate" + resp);
                $scope.info = "Error with generate SoapAction list. See server logs.";
            });

        //$scope.btnRegenerate = "Running Generation...";
    }

}

function SoapActionEditCtrl($scope, $routeParams, $location, SoapAction) {

    var self = this;

    SoapAction.get({soapActionId: $routeParams.soapActionId}, function (soapAction) {
        self.original = soapAction;
        $scope.soapAction = new SoapAction(self.original);
    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.soapAction);
    }

    $scope.save = function () {
        $scope.soapAction.update(function () {
            $location.path('/soapactions');
        });
    };
}
