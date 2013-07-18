function SoapActionsCtrl($scope, SoapactionsService) {

    $scope.ctrlPath = "soapactions";
    $scope.btnRegenerateDisabled = false;
    $scope.info = "";

    SoapactionsService.findAll().
        success(function (soapActions) {
            $scope.soapActions = soapActions.data;
        })
        .error(function (resp) {
            console.log("Error with SoapActionsService.findAll" + resp);
        });

    $scope.filterOptions = {
        filterText: "",
        useExternalFilter: false
    };

    $scope.gridOptions = {
        data: 'soapActions',
        showGroupPanel: true,
        showFilter: false,
        filterOptions: $scope.filterOptions,
        columnDefs: [
            {field: 'name', displayName: 'SoapAction Name'},
            {field: 'thresholdms', displayName: 'SoapAction Threshold in ms'},
            {field: 'edit', displayName: 'Edit', cellTemplate: '<div class="ngCellText" ng-class="col.colIndex()"><span ng-cell-text><a href="#/soapactions/edit/{{ row.getProperty(\'id\') }}"><i class="icon-pencil"></i></a></span></div>'}
        ]
    };

    $scope.regenerate = function () {
        $scope.info = "Running Generation...";
        $scope.btnRegenerateDisabled = true;

        SoapactionsService.regenerate().success(function (resp) {
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
