function GroupsCtrl($scope, GroupsService, ngTableParams, $filter) {

    $scope.adminPath = "groups";

    GroupsService.findAll().
        success(function (groups) {
            $scope.groups = groups.data;

            $scope.tableParams = new ngTableParams({
                page: 1,            // show first page
                count: 10,          // count per page
                sorting: {
                    'name': 'asc'     // initial sorting
                }
            }, {
                total: $scope.groups.length, // length of data
                getData: function ($defer, params) {
                    var datafilter = $filter('customAndSearch');
                    var groupsData = datafilter($scope.groups, $scope.tableFilter);
                    var orderedData = params.sorting() ? $filter('orderBy')(groupsData, params.orderBy()) : groupsData;
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
            console.log("Error with GroupsService.findAll" + resp);
        });
}

function GroupEditCtrl($scope, $routeParams, $location, Group, UIService) {

    var self = this;

    Group.get({groupId: $routeParams.groupId}, function (group) {
        self.original = group;
        $scope.group = new Group(self.original);
    });

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.group);
    }

    $scope.destroy = function () {
        self.original.destroy(function () {
            $location.path('/groups');
        });
    };

    $scope.save = function () {
        $scope.group.update(function () {
            $location.path('/groups');
        }, function (response) { // error case
            alert(response.data);
        });
    };
}

function GroupNewCtrl($scope, $location, Group, GroupsService) {

    GroupsService.findAllAndSelect($scope);

    $scope.group = new Group({id:'-1'});

    $scope.save = function () {
        $scope.group.update(function () {
            $location.path('/groups/');
        }, function (response) { // error case
            alert(response.data);
        });
    }

}