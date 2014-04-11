function MockGroupsCtrl($scope, $rootScope, $routeParams, MockGroupsService, UIService, ngTableParams, $filter) {

    // Looking for mockGroups with their groups and adding all informations to $scope.mockGroups var
    MockGroupsService.findAll($routeParams.groups).
        success(function (mockGroups) {
            $scope.mockGroups = mockGroups.data;
            $scope.tableParams = new ngTableParams({
                page: 1,            // show first page
                count: 10,          // count per page
                sorting: {
                    'name': 'asc'     // initial sorting
                }
            }, {
                total: $scope.mockGroups.length, // length of data
                getData: function ($defer, params) {
                    var datafilter = $filter('customAndSearch');
                    var mockGroupsData = datafilter($scope.mockGroups, $scope.tableFilter);
                    var orderedData = params.sorting() ? $filter('orderBy')(mockGroupsData, params.orderBy()) : mockGroupsData;
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
            console.log("Error with MockGroupsService.findAll" + resp);
        });

    $rootScope.$broadcast("showGroupsFilter", $routeParams.groups, "MockGroupsCtrl");

    $scope.$on("ReloadPage", function (event, group) {
        $scope.ctrlPath = "mockgroups";
        UIService.reloadAdminPage($scope, group);
    });
}

function MockGroupEditCtrl($scope, $routeParams, $location, MockGroup, GroupsService) {

    $scope.title = "Edit an mockGroup";

    var self = this;

    GroupsService.findAll().success(function (groups) {
        $scope.groups = groups.values;
    });

    MockGroup.get({mockgroupId: $routeParams.mockGroupId}, function (mockGroup) {
        self.original = mockGroup;
        $scope.mockGroup = new MockGroup(self.original);
    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.mockGroup);
    };

    $scope.destroy = function () {
        self.original.$remove(function () {
            $location.path('/mockgroups');
        }, function (response) { // error case
            alert(response.data);
        });
    };

    $scope.save = function () {
        $scope.mockGroup.$update(function () {
            $location.path('/mockgroups');
        }, function (response) { // error case
            alert(response.data);
        });
    };
}

function MockGroupNewCtrl($scope, $location, MockGroup, GroupsService) {

    $scope.title = "Insert new mockGroup";

    GroupsService.findAll().success(function (groups) {
        $scope.groups = groups.values;
    });

    $scope.mockGroup = new MockGroup();
    $scope.mockGroup.groups = [];

    $scope.save = function () {
        $scope.mockGroup.$create(function () {
            $location.path('/mockgroups/');
        }, function (response) { // error case
            alert(response.data);
        });
    };
}