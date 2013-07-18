function ServicesCtrl($scope, $routeParams, ServicesService) {

    $scope.ctrlPath = "services";

    console.log("fetch services");

    ServicesService.findAll($routeParams.group).
        success(function (services) {
            $scope.services = services.data;
        })
        .error(function (resp) {
            console.log("Error with ServicesService.findAll" + resp);
        });

    $scope.filterOptions = {
        filterText: "",
        useExternalFilter: false
    };

    $scope.gridOptions = {
        data: 'services',
        showGroupPanel: true,
        showFilter: false,
        filterOptions: $scope.filterOptions,
        columnDefs: [
            {field: 'env', displayName: 'Environment'},
            {field: 'description', displayName: 'Description'},
            {field: 'localTarget', displayName: 'Local Target'},
            {field: 'remoteTarget', displayName: 'Remote Target'},
            {field: 'timeoutInMs', displayName: 'Timeout in ms'},
            {field: 'recordXmlData', displayName: 'Record Xml Data'},
            {field: 'recordData', displayName: 'Record Data'},
            {field: 'edit', displayName: 'Edit', cellTemplate: '<div class="ngCellText" ng-class="col.colIndex()"><span ng-cell-text><a href="#/services/edit/{{ row.getProperty(\'id\') }}"><i class="icon-pencil"></i></a></span></div>'}
        ]
    };
}

function ServiceEditCtrl($scope, $routeParams, $location, Service, EnvironmentsService, UIService) {

    var self = this;

    $scope.hostname = $location.host();
    $scope.port = $location.port();

    Service.get({serviceId: $routeParams.serviceId}, function (service) {
        self.original = service;
        $scope.service = new Service(self.original);
        $scope.service.recordXmlData = UIService.fixBooleanReverse($scope.service.recordXmlData);
        $scope.service.recordData = UIService.fixBooleanReverse($scope.service.recordData);

        EnvironmentsService.findAllAndSelect($scope, null, $scope.service);

    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.service);
    }

    $scope.destroy = function () {
        self.original.destroy(function () {
            $location.path('/services');
        });
    };

    $scope.save = function () {
        $scope.service.update(function () {
            $location.path('/services');
        });
    };
}

function ServiceNewCtrl($scope, $location, Service, EnvironmentsService) {

    EnvironmentsService.findAllAndSelect($scope);

    $scope.service = new Service({id:'-2'});
    $scope.service.recordXmlData = "yes";
    $scope.service.recordData = "yes";

    $scope.save = function () {
        $scope.service.update(function () {
            $location.path('/services/');
        });
    }

}