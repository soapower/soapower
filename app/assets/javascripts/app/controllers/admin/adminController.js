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

    $scope.$watch('envir', function(newValue, oldValue) {
           console.log("new : "+newValue+", old : "+oldValue);
       });

     // Called when the maxdate datetimepicker is set
    $scope.onMaxTimeSet = function (newDate, oldDate) {
       $scope.showmaxdate = false;
       $scope.maxdate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
    };

     // Called when the mindate datetimepicker is set
     $scope.onMinTimeSet = function (newDate, oldDate) {
        $scope.showmindate = false;
        $scope.mindate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
     };

    $scope.submitDelete = function () {

        if (UIService.checkDatesFormatAndCompare($scope.mindate, $scope.maxdate)) {
            UIService.reloadPage($scope, $scope.showServiceactions);
        } else {
            // Else, mindate and maxdate are set to yesterday's and today's dates
            $scope.mindate = UIService.getInputCorrectDateFormat(UIService.getDay("yesterday"));
            $scope.maxdate = UIService.getInputCorrectDateFormat(UIService.getDay("today"));
        }

        $scope.showRunningDelete = true;
        $scope.showResponseDelete = false;
        $scope.deleteForm.environmentName = $scope.envir;
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
    };

    EnvironmentsService.findAllAndSelect($scope, null, 'all', null, true);
}