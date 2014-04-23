function MockGroupsCtrl($scope, $rootScope, $location, $routeParams, MockGroupsService, ngTableParams, $filter) {

    $scope.groups = $routeParams.groups;

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

    $scope.$on("ReloadPage", function (event, groups) {
        console.log("Receive ReloadPage");
        var path = 'mockgroups/list/' + groups;
        $location.path(path);
    });
}

function MockGroupEditCtrl($scope, $routeParams, $location, MockGroup, GroupsService) {

    $scope.title = "Edit an mockGroup";

    var self = this;

    GroupsService.findAll().success(function (groups) {
        $scope.allGroups = groups.values;
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
            $location.path('/mockgroups/list/' + $routeParams.groups);
        }, function (response) { // error case
            alert(response.data);
        });
    };

    $scope.save = function () {
        $scope.mockGroup.$update(function () {
            $location.path('/mockgroups/list/' + $routeParams.groups);
        }, function (response) { // error case
            alert(response.data);
        });
    };
}

function MockGroupNewCtrl($scope, $location, $routeParams, MockGroup, GroupsService) {

    $scope.title = "Insert new mockGroup";

    GroupsService.findAll().success(function (groups) {
        $scope.allGroups = groups.values;
    });

    $scope.mockGroup = new MockGroup();
    $scope.mockGroup.groups = [];

    $scope.save = function () {
        $scope.mockGroup.$create(function () {
            $location.path('/mockgroups/list/' + $routeParams.groups);
        }, function (response) { // error case
            alert(response.data);
        });
    };
}