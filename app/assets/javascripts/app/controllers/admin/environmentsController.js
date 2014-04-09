function EnvironmentsCtrl($scope, $rootScope, $routeParams, EnvironmentsService, UIService, ngTableParams, $filter) {

    // Looking for environments with their groups and adding all informations to $scope.environments var
    EnvironmentsService.findAll($routeParams.groups).
        success(function (environments) {
            $scope.environments = environments.data;
            $scope.tableParams = new ngTableParams({
                page: 1,            // show first page
                count: 10,          // count per page
                sorting: {
                    'name': 'asc'     // initial sorting
                }
            }, {
                total: $scope.environments.length, // length of data
                getData: function ($defer, params) {
                    var datafilter = $filter('customAndSearch');
                    var environmentsData = datafilter($scope.environments, $scope.tableFilter);
                    var orderedData = params.sorting() ? $filter('orderBy')(environmentsData, params.orderBy()) : environmentsData;
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
            console.log("Error with EnvironmentsService.findAll" + resp);
        });

    $rootScope.$broadcast("showGroupsFilter", $routeParams.groups);

    $scope.$on("ReloadPage", function (event, group) {
        $scope.ctrlPath = "environments";
        UIService.reloadAdminPage($scope, group);
    });
}

function EnvironmentEditCtrl($scope, $routeParams, $location, Environment, GroupsService) {
    var self = this;

    GroupsService.findAll().success(function (groups) {
        $scope.groups = groups.values;
    });

    Environment.get({environmentId: $routeParams.environmentId}, function (environment) {
        self.original = environment;
        $scope.environment = new Environment(self.original);
    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.environment);
    };

    $scope.destroy = function () {
        self.original.$remove(function () {
            $location.path('/environments');
        }, function (response) { // error case
            alert(response.data);
        });
    };

    $scope.save = function () {
        $scope.environment.$update(function () {
            $location.path('/environments');
        }, function (response) { // error case
            alert(response.data);
        });
    };
}

function EnvironmentNewCtrl($scope, $location, Environment, GroupsService) {

    GroupsService.findAll().success(function (groups) {
        $scope.groups = groups.values;
        console.log($scope.groups);
    });

    $scope.environment = new Environment();
    $scope.environment.hourRecordXmlDataMin = 6;
    $scope.environment.hourRecordXmlDataMax = 22;
    $scope.environment.nbDayKeepXmlData = 2;
    $scope.environment.nbDayKeepAllData = 4;
    $scope.environment.recordXmlData = true;
    $scope.environment.recordData = true;
    $scope.environment.groups = [];

    $scope.addNewGroup = function() {
        if (!$scope.environment.groups) {
            $scope.environment.groups = [];
        }
        $scope.groups.push($scope.newGroup);
        $scope.environment.groups.push($scope.newGroup);
        console.log("Add New group " + $scope.newGroup)
        console.log($scope.environment.groups);
        console.log($scope.groups);
        $('#groups').select2("destroy").select2($('#groups').data());
    };

    $scope.save = function () {
        $scope.environment.$create(function () {
            $location.path('/environments/');
        }, function (response) { // error case
            alert(response.data);
        });
    };
}