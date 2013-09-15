function ServicesCtrl($scope, $rootScope, $routeParams, ServicesService, UIService) {

    $scope.groupName =  $routeParams.group;

    ServicesService.findAll($routeParams.group).
        success(function (services) {
            $scope.services = services.data;
        })
        .error(function (resp) {
            console.log("Error with ServicesService.findAll" + resp);
        });
    $rootScope.$broadcast("showGroupsFilter", $routeParams.group);

    $scope.$on("ReloadPage", function (event, group) {
        $scope.ctrlPath = "services";
        UIService.reloadAdminPage($scope, group);
    });
}

function ServiceEditCtrl($scope, $rootScope, $routeParams, $location, Service, EnvironmentsService, UIService) {

    var self = this;

    $scope.hostname = $location.host();
    $scope.port = $location.port();

    Service.get({serviceId: $routeParams.serviceId}, function (service) {
        self.original = service;
        $scope.service = new Service(self.original);
        $scope.service.recordXmlData = UIService.fixBooleanReverse($scope.service.recordXmlData);
        $scope.service.recordData = UIService.fixBooleanReverse($scope.service.recordData);

        EnvironmentsService.findAllAndSelect($scope, null, $routeParams.group, $scope.service, false);
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

    // not using filter on edit services
    $rootScope.$broadcast("showGroupsFilter", false);
}

function ServiceNewCtrl($scope, $rootScope, $location, $routeParams, Service, EnvironmentsService) {

    EnvironmentsService.findAllAndSelect($scope, null, $routeParams.group, null, false);

    $scope.service = new Service({id:'-2'});
    $scope.service.recordXmlData = "yes";
    $scope.service.recordData = "yes";

    $scope.hostname = $location.host();
    $scope.port = $location.port();

    $scope.save = function () {
        $scope.service.update(function () {
            $location.path('/services/');
        });
    }

    $rootScope.$broadcast("showGroupsFilter", false);

    $scope.$on("ReloadPage", function (event, group) {
        var path = "services/new/" + group;
        $location.path(path);
    });
}