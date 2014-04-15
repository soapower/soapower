function MocksCtrl($scope, $rootScope, $location, $filter, $routeParams, MocksService, ngTableParams) {

    $scope.groups = $routeParams.groups;
    $scope.mockGroupName = $routeParams.mockGroupName;

    // Looking for mocks with their groups and adding all informations to $scope.mocks var
    MocksService.findAll($routeParams.mockGroupName).
        success(function (data) {
            $scope.mocks = data.mocks;

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

    $rootScope.$broadcast("showGroupsFilter", $routeParams.groups, "MocksCtrl");

    $scope.$on("ReloadPage", function (event, groups) {
        console.log("Receive ReloadPage");
        var path = 'mocks/list/' + $scope.mockGroupName + "/" + groups;
        $location.path(path);
    });

}

function MockEditCtrl($scope, $location, $routeParams, Mock) {

    $scope.title = "Edit a Mock";
    $scope.groups = $routeParams.groups;

    var self = this;

    Mock.get({mockGroupName: $routeParams.mockGroupName, mockId: $routeParams.mockId}, function (mock) {
        mock.mockGroupName = $routeParams.mockGroupName;
        self.original = mock;
        $scope.mock = new Mock(self.original);
    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.mock);
    };

    $scope.destroy = function () {
        self.original.$remove(function () {
            $location.path("/mocks/list/" + $routeParams.mockGroupName + "/" + $routeParams.groups);
        }, function (response) { // error case
            alert(response.data);
        });
    };

    $scope.save = function () {
        $scope.mock.$update(function () {
            $location.path("/mocks/list/" + $routeParams.mockGroupName + "/" + $routeParams.groups);
        }, function (response) { // error case
            alert(response.data);
        });
    };
}

function MockNewCtrl($scope, $location, $routeParams, Mock) {

    $scope.title = "Insert new Mock";
    $scope.groups = $routeParams.groups;

    $scope.mock = new Mock({id: '-1'});
    $scope.mock.name = "";
    $scope.mock.description = "";
    $scope.mock.timeoutms = 0;
    $scope.mock.httpStatus = 200;
    $scope.mock.response = "";
    $scope.mock.criteria = "*";
    $scope.mock.mockGroupName = $routeParams.mockGroupName;

    $scope.save = function () {
        $scope.mock.$create(function () {
            $location.path("/mocks/list/" + $routeParams.mockGroupName + "/" + $routeParams.groups);
        }, function (response) { // error case
            alert(response.data);
        });
    }

}