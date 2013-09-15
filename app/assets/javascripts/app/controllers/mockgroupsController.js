function MockGroupsCtrl($scope, $rootScope, $routeParams, MockGroupsService, UIService) {

    // Looking for mockgroups with their groups and adding all informations to $scope.mockgroups var
    MockGroupsService.findAll($routeParams.group).
        success(function (mockgroups) {
            $scope.mockgroups = mockgroups.data;
        })
        .error(function (resp) {
            console.log("Error with MockgroupsService.findAll" + resp);
        });

    $rootScope.$broadcast("showGroupsFilter", $routeParams.group);

    $scope.$on("ReloadPage", function (event, group) {
        $scope.ctrlPath = "mockgroups";
        UIService.reloadAdminPage($scope, group);
    });

    // Hide the default group in table
    $scope.isNotDefaultMockGroup = function(mockGroup) {
        return mockGroup.id > 1;
    };
}

function MockGroupEditCtrl($scope, $routeParams, $location, MockGroup, GroupsService) {
    var self = this;

    $scope.title = "Edit a Mock Group"
    MockGroup.get({mockGroupId: $routeParams.mockGroupId}, function (mockGroup) {
        self.original = mockGroup;
        $scope.mockGroup = new MockGroup(self.original);
        GroupsService.findAllAndSelect($scope, null, null, $scope.mockGroup, false);
    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.mockGroup);
    }

    $scope.destroy = function () {
        self.original.destroy(function () {
            $location.path('/mockgroups');
        });
    };

    $scope.save = function () {
        $scope.mockGroup.update(function () {
            $location.path('/mockgroups');
        }, function (response) { // error case
            alert(response.data);
        });
    };
}

function MockGroupNewCtrl($scope, $location, MockGroup, GroupsService) {

    GroupsService.findAllAndSelect($scope);

    $scope.title = "Insert new Mock Group"

    $scope.mockGroup = new MockGroup({id: '-1'});
    $scope.mockGroup.name = "";

    $scope.save = function () {
        $scope.mockGroup.update(function () {
            $location.path('/mockgroups/');
        }, function (response) { // error case
            alert(response.data);
        });
    }
}