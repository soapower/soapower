function ServicesCtrl($scope, $rootScope, $routeParams, ServicesService, UIService, ngTableParams, $filter) {

    $scope.groupName = $routeParams.group;

    ServicesService.findAll($routeParams.group).
        success(function (services) {
            $scope.services = services.data;

            $scope.tableParams = new ngTableParams({
                page: 1,            // show first page
                count: 10,          // count per page
                sorting: {
                    'name': 'asc'     // initial sorting
                }
            }, {
                total: $scope.services.length, // length of data
                getData: function ($defer, params) {
                    var datafilter = $filter('filter');
                    var servicesData = datafilter($scope.services, $scope.tableFilter);
                    var orderedData = params.sorting() ? $filter('orderBy')(servicesData, params.orderBy()) : servicesData;
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
            console.log("Error with ServicesService.findAll" + resp);
        });
    $rootScope.$broadcast("showGroupsFilter", $routeParams.group);

    $scope.$on("ReloadPage", function (event, group) {
        $scope.ctrlPath = "services";
        UIService.reloadAdminPage($scope, group);
    });
}

function ServiceEditCtrl($scope, $rootScope, $routeParams, $location, Service, EnvironmentsService, MockGroupsService, UIService) {

    $scope.title = "Update a service";

    var self = this;

    $scope.hostname = $location.host();
    $scope.port = $location.port();

    Service.get({serviceId: $routeParams.serviceId}, function (service) {
        self.original = service;
        $scope.service = new Service(self.original);
        $scope.service.recordXmlData = UIService.fixBooleanReverse($scope.service.recordXmlData);
        $scope.service.recordData = UIService.fixBooleanReverse($scope.service.recordData);

        EnvironmentsService.findAllAndSelect($scope, null, $routeParams.group, $scope.service, false);

        MockGroupsService.findAll($routeParams.group).
            success(function (mockGroups) {
                $scope.mockGroups = mockGroups.data;
                angular.forEach(mockGroups.data, function (mockGroup, key) {
                    if (mockGroup.id == $scope.service.mockGroupId) {
                        $scope.service.mockGroup = mockGroup;
                        return false;
                    }
                });
            })
            .error(function (resp) {
                console.log("Error with MockGroupsService.findAll:" + resp);
            });
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

function ServiceNewCtrl($scope, $rootScope, $location, $routeParams, Service, MockGroupsService, EnvironmentsService) {

    $scope.title = "Insert new service";

    EnvironmentsService.findAllAndSelect($scope, null, $routeParams.group, null, false);

    $scope.service = new Service({id: '-2'});
    $scope.service.recordXmlData = "yes";
    $scope.service.recordData = "yes";
    $scope.service.timeoutms = "60000";

    $scope.hostname = $location.host();
    $scope.port = $location.port();

    MockGroupsService.findAll("all").
        success(function (mockGroups) {
            $scope.mockGroups = mockGroups.data;
        })
        .error(function (resp) {
            console.log("Error with MockGroupsService.findAll:" + resp);
        });

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