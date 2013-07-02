function SoapActionsCtrl($scope, SoapactionsService) {

    SoapactionsService.findAll().
        success(function (soapActions) {
            $scope.soapActions = soapActions.data;
        })
        .error(function (resp) {
            console.log("Error with SoapActionsService.findAll" + resp);
        });

    $scope.gridOptions = {
        data: 'soapActions',
        showGroupPanel: true,
        showFilter: true,
        columnDefs: [
            {field: 'name', displayName: 'SoapAction Name'},
            {field: 'thresholdms', displayName: 'SoapAction Threshold in ms'},
            {field: 'edit', displayName: 'Edit', cellTemplate: '<div class="ngCellText" ng-class="col.colIndex()"><span ng-cell-text><a href="#/soapactions/{{ row.getProperty(\'id\') }}"><i class="icon-pencil"></i></a></span></div>'}
        ]
    };

    $scope.regenerate = function () {
        console.log("TODO");
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
