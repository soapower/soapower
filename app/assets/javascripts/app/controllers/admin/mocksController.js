function MocksCtrl($scope, $rootScope, $filter, $routeParams, MocksService, UIService, ngTableParams) {

    // Looking for mocks with their groups and adding all informations to $scope.mocks var
    MocksService.findAll($routeParams.mockGroup).
        success(function (mocks) {
            $scope.mocks = mocks.data;

            $scope.tableParams = new ngTableParams({
                page: 1,            // show first page
                count: 10,          // count per page
                sorting: {
                    'name': 'asc'     // initial sorting
                }
            }, {
                total: $scope.mocks.length, // length of data
                getData: function ($defer, params) {
                    var datafilter = $filter('customAndSearch');
                    var mocksData = datafilter($scope.mocks, $scope.tableFilter);
                    var orderedData = params.sorting() ? $filter('orderBy')(mocksData, params.orderBy()) : mocksData;
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
            console.log("Error with MocksService.findAll" + resp);
        });

    $rootScope.$broadcast("showGroupsFilter", $routeParams.groups);

    $scope.$on("ReloadPage", function (event, group) {
        $scope.ctrlPath = "mocks";
        UIService.reloadAdminPage($scope, group);
    });

    $scope.mockGroup = $routeParams.mockGroup;
}

function MockEditCtrl($scope, $routeParams, $location, Mock, MockGroupsService) {
    var self = this;

    $scope.title = "Edit a Mock";

    Mock.get({mockId: $routeParams.mockId}, function (mock) {
        self.original = mock;
        $scope.mock = new Mock(self.original);
        MockGroupsService.findAll("all").
            success(function (mockGroups) {
                $scope.mockGroups = mockGroups.data;
                angular.forEach(mockGroups.data, function (mockGroup) {
                    if (mockGroup.id == $scope.mock.mockGroupId) {
                        $scope.mock.mockGroup = mockGroup;
                        return false;
                    }
                });
            })
            .error(function (resp) {
                console.log("Error with MockGroupsService.findAll:" + resp);
            });
    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.mock);
    };

    $scope.destroy = function () {
        self.original.destroy(function () {
            $location.path('/mocks');
        }, function (response) { // error case
            alert(response.data);
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

function MockNewCtrl($scope, $location, Mock, MockGroupsService, $routeParams) {

    $scope.title = "Insert new Mock";

    $scope.mock = new Mock({id: '-1'});
    $scope.mock.name = "";
    $scope.mock.description = "";
    $scope.mock.timeoutms = 0;
    $scope.mock.httpStatus = 200;
    $scope.mock.response = "";
    $scope.mock.criteria = "*";

    MockGroupsService.findAll("all").
        success(function (mockGroups) {
            $scope.mockGroups = mockGroups.data;
            angular.forEach(mockGroups.data, function (mockGroup) {
                if (mockGroup.name == $routeParams.mockGroup) {
                    $scope.mock.mockGroup = mockGroup;
                    return false;
                }
            });
        })
        .error(function (resp) {
            console.log("Error with MockGroupsService.findAll:" + resp);
        });

    $scope.save = function () {
        $scope.mock.update(function () {
            $location.path('/mocks/');
        }, function (response) { // error case
            alert(response.data);
        });
    }

}