function AdminCtrl ($scope, EnvironmentsService) {
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

    $scope.$watch('mindate', function () {
        if ($scope.mindate) {
            $scope.showmindate = false;
            if ($scope.mindate > $scope.maxdate) {
                $scope.maxdate = $scope.mindate;
            }
        }
    });
    $scope.$watch('maxdate', function () {
        if ($scope.maxdate) {
            $scope.showmaxdate = false;
            if ($scope.mindate > $scope.maxdate) {
                $scope.maxdate = $scope.mindate;
            }
        }
    });

    EnvironmentsService.findAllAndSelect($scope);
}