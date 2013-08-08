function GroupsCtrl($scope, GroupsService) {

    $scope.adminPath = "groups";

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
            {field: 'name', displayName: 'Name', cellTemplate: '<div class="ngCellText">{{row.getProperty(col.field)}} <span ng-show="row.getProperty(\'id\') == 1">(Default Group)</span></div>'},
            {field: 'edit', displayName: 'Edit', cellTemplate: '<div class="ngCellText" ng-class="col.colIndex()"><span ng-cell-text><a href="#/groups/{{ row.getProperty(\'id\') }}"><i class="icon-pencil"></i></a></span></div>'}
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