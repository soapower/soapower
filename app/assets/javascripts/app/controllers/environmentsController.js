function EnvironmentsCtrl($scope, $routeParams, EnvironmentsService) {

     $scope.ctrlPath = "environments";

    // Looking for environments with their groups and adding all informations to $scope.environments var
    EnvironmentsService.findAll($routeParams.group).
        success(function (environments) {
            $scope.environments = environments.data;
        })
        .error(function (resp) {
            console.log("Error with EnvironmentsService.findAll" + resp);
        });




    $scope.filterOptions = {
        filterText: "",
        useExternalFilter: false
    };

    $scope.gridOptions = {
        data: 'environments',
        showGroupPanel: true,
        showFilter: false,
        filterOptions: $scope.filterOptions,
        columnDefs: [
            {field: 'name', displayName: 'Name'},
            {field: 'hourRecordXmlDataMin', displayName: 'Xml Data : Start Record Hour'},
            {field: 'hourRecordXmlDataMax', displayName: 'Xml Data : End Record Hour'},
            {field: 'nbDayKeepXmlData', displayName: 'Xml Data : nb days keep'},
            {field: 'nbDayKeepAllData', displayName: 'All Data : nb days keep'},
            {field: 'recordXmlData', displayName: 'Record Xml Data'},
            {field: 'recordData', displayName: 'Record Data'},
            {field: 'edit', displayName: 'Edit', cellTemplate: '<div class="ngCellText" ng-class="col.colIndex()"><span ng-cell-text><a href="#/environments/{{ row.getProperty(\'id\') }}"><i class="icon-pencil"></i></a></span></div>'}
        ]
    };
}

function EnvironmentEditCtrl($scope, $routeParams, $location, Environment, UIService) {

    var self = this;

    Environment.get({environmentId: $routeParams.environmentId}, function (environment) {
        self.original = environment;
        $scope.environment = new Environment(self.original);
        $scope.environment.recordXmlData = UIService.fixBooleanReverse($scope.environment.recordXmlData);
        $scope.environment.recordData = UIService.fixBooleanReverse($scope.environment.recordData);
    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.environment);
    }

    $scope.destroy = function () {
        self.original.destroy(function () {
            $location.path('/environments');
        });
    };

    $scope.save = function () {
        $scope.environment.update(function () {
            $location.path('/environments');
        });
    };
}

function EnvironmentNewCtrl($scope, $location, Environment, EnvironmentsEnvironment) {

    EnvironmentsEnvironment.findAllAndSelect($scope);

    $scope.environment = new Environment({id:'-1'});
    $scope.environment.recordXmlData = "yes";
    $scope.environment.recordData = "yes";

    $scope.save = function () {
        $scope.environment.update(function () {
            $location.path('/environments/');
        });
    }

}