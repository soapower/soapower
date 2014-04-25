function AdminCtrl($scope, EnvironmentsService, $http, $filter, UIService) {
    $scope.urlDlConfig = "/admin/downloadConfiguration";
    $scope.urlDlRequestDataStatsEntries = "/admin/downloadRequestDataStatsEntries";
    $scope.urlUploadConfiguration = "/admin/uploadConfiguration";
    $scope.typeAction = "xml-data";

    $scope.showResponseUpload = false;
    $scope.showUploadRunning = false;

    $scope.startUpload = function () {
        $scope.showUploadRunning = true;
        $scope.showResponseUpload = false;
    };

    $scope.uploadComplete = function (content) {
        $scope.response = content;
        $scope.showUploadRunning = false;
        $scope.showResponseUpload = true;
    };
    $scope.mindatecalendar = new Date();
    $scope.maxdatecalendar = new Date();

     // Called when the maxdate datetimepicker is set
     $scope.onMaxTimeSet = function (newDate, oldDate) {
        $scope.showmaxdate = false;
        $scope.maxdate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
     };

     // Called when the mindate datetimepicker is set
     $scope.onMinTimeSet = function (newDate, oldDate) {
        $scope.showmindate = false;
        $scope.mindate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
        console.log($scope.mindatecalendar);
     };

    $scope.submitDelete = function () {
        if (UIService.checkDatesFormatAndCompare($scope.mindate, $scope.maxdate)) {
            // Dates are in correct format, perform the deletion
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
        } else {
            // Else wrong format, mindate and maxdate are reset to yesterday's and today's dates
            $scope.mindate = UIService.getInputCorrectDateFormat(UIService.getDay("yesterday"));
            $scope.maxdate = UIService.getInputCorrectDateFormat(UIService.getDay("today"));
        }
    };

    EnvironmentsService.findAllAndSelect($scope, null, 'all', null, true);
}