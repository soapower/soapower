function GroupsCtrl($scope, GroupsService) {

    GroupsService.findAll().
        success(function (groups) {
            $scope.groups = groups.data;
        })
        .error(function (resp) {
            console.log("Error with GroupsService.findAll" + resp);
        });

    $scope.filterOptions = {
        filterText: "",
        useExternalFilter: false
    };

    $scope.gridOptions = {
        data: 'groups',
        showGroupPanel: true,
        showFilter: false,
        filterOptions: $scope.filterOptions,
        columnDefs: [
            {field: 'groupName', displayName: 'Name'},
            {field: 'edit', displayName: 'Edit', cellTemplate: '<div class="ngCellText" ng-class="col.colIndex()"><span ng-cell-text><a href="#/groups/{{ row.getProperty(\'groupId\') }}"><i class="icon-pencil"></i></a></span></div>'}
        ]
    };
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
        });
    };
}

function GroupNewCtrl($scope, $location, Group, GroupsController) {

    GroupsController.findAllAndSelect($scope);

    $scope.group = new Group({id:'-1'});


    $scope.save = function () {
        $scope.group.update(function () {
            $location.path('/groups/');
        });
    }

}