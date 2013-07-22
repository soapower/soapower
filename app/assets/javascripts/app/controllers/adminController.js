function AdminCtrl ($scope, EnvironmentsService, $http) {
    $scope.urlDlConfig = "/admin/downloadConfiguration";
    $scope.urlDlRequestDataStatsEntries = "/admin/downloadRequestDataStatsEntries";
    $scope.urlUploadConfiguration = "/admin/uploadConfiguration";
    $scope.typeAction = "xml-data";

    $scope.showResponseUpload = false;
    $scope.showUploadRunning = false;
    $scope.uploadComplete = function (content, completed) {
        if (completed) {
            $scope.response = content;
            $scope.showUploadRunning = false;
            $scope.showResponseUpload = true;
        } else {
            $scope.showUploadRunning = true;
        }
    };

    $scope.$watch('minDate', function () {
        if ($scope.minDate) {
            $scope.showminDate = false;
            if ($scope.minDate > $scope.maxDate) {
                $scope.maxDate = $scope.minDate;
            }
        }
    });
    $scope.$watch('maxDate', function () {
        if ($scope.maxDate) {
            $scope.showmaxDate = false;
            if ($scope.minDate > $scope.maxDate) {
                $scope.maxDate = $scope.minDate;
            }
        }
    });

    $scope.submitDelete = function () {
        $scope.showRunningDelete = true;
        $scope.showResponseDelete = false;
        $scope.deleteForm.environmentName = $scope.deleteForm.environment.name;
        $scope.deleteForm.minDate = $scope.minDate;
        $scope.deleteForm.maxDate = $scope.maxDate;

        $http.post('/admin/delete', $scope.deleteForm)
            .success(function (resp) {
                $scope.responseDelete = resp;
                $scope.showRunningDelete = false;
                $scope.showResponseDelete = true;
            })
            .error(function (resp) {
                $scope.responseDelete = resp;
                $scope.showRunningDelete = false;
                $scope.showResponseDelete = true;
            });
    }

    EnvironmentsService.findAllAndSelect($scope, null, 'all', null, true);

}