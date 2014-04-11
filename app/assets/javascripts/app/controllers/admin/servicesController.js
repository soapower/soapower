function ServicesCtrl($scope, $rootScope, $routeParams, ServicesService, UIService, ngTableParams, $filter) {

    $scope.groupName = $routeParams.groups;

    ServicesService.findAll($routeParams.groups).
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
                    var datafilter = $filter('customAndSearch');
                    var servicesData = datafilter($scope.services, $scope.tableFilter);
                    var orderedData = params.sorting() ? $filter('orderBy')(servicesData, params.orderBy()) : servicesData;
                    var res = orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count());
                    params.total(orderedData.length);
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

    console.log("Ask showGroupsFilter  with group " + $routeParams.groups);
    $rootScope.$broadcast("showGroupsFilter", $routeParams.groups, "ServicesCtrl");

    $scope.$on("ReloadPage", function (event, group) {
        $scope.ctrlPath = "services";
        UIService.reloadAdminPage($scope, group);
    });
}

function ServiceEditCtrl($scope, $rootScope, $routeParams, $location, Service, EnvironmentsService, MockGroupsService) {

    $scope.title = "Update a service";

    var self = this;

    $scope.hostname = $location.host();
    $scope.port = $location.port();

    Service.get({serviceId: $routeParams.serviceId}, function (service) {
        self.original = service;
        $scope.service = new Service(self.original);

        EnvironmentsService.findOptions($routeParams.groups).success(function (environments) {
            $scope.environments = environments;
        });

        MockGroupsService.findAll($routeParams.groups).success(function (mockGroups) {
            $scope.mockGroups = mockGroups.data;
        })
    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.service);
    };

    $scope.destroy = function () {
        self.original.$remove(function () {
            $location.path('/services');
        }, function (response) { // error case
            alert(response.data);
        });
    };

    $scope.save = function () {
        $scope.service.$update(function () {
            $location.path('/services');
        }, function (response) { // error case
            alert(response.data);
        });
    };

    // not using filter on edit services
    $rootScope.$broadcast("showGroupsFilter", false, "ServiceEditCtrl");
}

function ServiceNewCtrl($scope, $rootScope, $location, $routeParams, Service, MockGroupsService, EnvironmentsService) {

    $scope.title = "Insert new service";

    $scope.showNewGroup = false;

    EnvironmentsService.findOptions($routeParams.groups).success(function (environments) {
        $scope.environments = environments;
    });

    MockGroupsService.findAll("all").success(function (mockGroups) {
        $scope.mockGroups = mockGroups.data;
    });

    $scope.service = new Service();
    $scope.service.useMockGroup = false;
    $scope.service.timeoutms = 60000;
    $scope.service.recordXmlData = true;
    $scope.service.recordData = true;

    $scope.hostname = $location.host();
    $scope.port = $location.port();

    $scope.save = function () {
        $scope.service.$create(function () {
            $location.path('/services/');
        }, function (response) { // error case
            alert(response.data);
        });
    };

    $scope.$on("ReloadPage", function (event, group, caller) {
        var path = "services/new/" + group;
        console.log("ServiceNewCtrl receive from " + caller + " ReloadPage : " + path);
        $location.path(path);
    });

    $rootScope.$broadcast("showGroupsFilter", false, "ServiceNewCtrl");
}