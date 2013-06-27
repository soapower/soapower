function ServicesCtrl($scope, ServicesService) {

    console.log("fetch services");
    ServicesService.findAll().
        success(function (services) {
            $scope.services = services.data;
        })
        .error(function (resp) {
            console.log("Error with ServicesService.findAll" + resp);
        });

    $scope.gridOptions = {
        data: 'services',
        showGroupPanel: true,
        showFilter: true,
        columnDefs: [
            {field: 'env', displayName: 'Environment'},
            {field: 'description', displayName: 'Description'},
            {field: 'localTarget', displayName: 'Local Target'},
            {field: 'remoteTarget', displayName: 'Remote Target'},
            {field: 'timoutInMs', displayName: 'Timeout in ms'},
            {field: 'recordXmlData', displayName: 'Record Xml Data'},
            {field: 'recordData', displayName: 'Record Data'},
            {field: 'edit', displayName: 'Edit', cellTemplate: '<div class="ngCellText" ng-class="col.colIndex()"><span ng-cell-text><a href="#/services/{{ row.getProperty(\'id\') }}"><i class="icon-pencil"></i></a></span></div>'}
        ]
    };
}

function ServiceEditCtrl($scope, $routeParams, Service, EnvironmentsService) {

    var self = this;

    EnvironmentsService.findAllAndSelect($scope);

    Service.get({id: $routeParams.serviceId}, function (service) {
        self.original = service;
        $scope.service = new Service(self.original);
    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.service);
    }

    $scope.destroy = function () {
        self.original.destroy(function () {
            $location.path('/services/list');
        });
    };

    $scope.save = function () {
        $scope.service.update(function () {
            $location.path('/services');
        });
    };
}

function ServiceNewCtrl($scope, Service, EnvironmentsService) {

    EnvironmentsService.findAllAndSelect($scope);

    $scope.save = function () {
        console.log("Call save A");

        Service.save($scope.service, function (service) {
            console.log("Call save");
            $location.path('/services/' + service._id.$oid);
        });
    }
}