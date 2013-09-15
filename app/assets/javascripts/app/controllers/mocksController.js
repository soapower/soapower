function MocksCtrl($scope, $rootScope, $routeParams, MocksService, UIService) {

    // Looking for mocks with their groups and adding all informations to $scope.mocks var
    MocksService.findAll($routeParams.group).
        success(function (mocks) {
            $scope.mocks = mocks.data;
        })
        .error(function (resp) {
            console.log("Error with MocksService.findAll" + resp);
        });

    $rootScope.$broadcast("showGroupsFilter", false);
}

function MockEditCtrl($scope, $routeParams, $location, Mock, GroupsService) {
    var self = this;

    Mock.get({mockId: $routeParams.mockId}, function (mock) {
        self.original = mock;
        $scope.mock = new Mock(self.original);
        GroupsService.findAllAndSelect($scope, null, null, $scope.mock, false);
    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.mock);
    }

    $scope.destroy = function () {
        self.original.destroy(function () {
            $location.path('/mocks');
        });
    };

    $scope.save = function () {
        $scope.mock.update(function () {
            $location.path('/mocks');
        }, function (response) { // error case
            alert(response.data);
        });
    };
}

function MockNewCtrl($scope, $location, Mock, GroupsService) {

    GroupsService.findAllAndSelect($scope);

    $scope.mock = new Mock({id: '-1'});
    $scope.mock.name = "";
    $scope.mock.description = "";
    $scope.mock.timeout = 0;
    $scope.mock.response = "";
    $scope.mock.criterias = "";

    $scope.save = function () {
        $scope.mock.update(function () {
            $location.path('/mocks/');
        }, function (response) { // error case
            alert(response.data);
        });
    }
}