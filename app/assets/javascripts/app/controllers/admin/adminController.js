function AdminCtrl($scope, EnvironmentsService, $http, $filter, UIService) {
    $scope.mindatecalendar = new Date();
    $scope.maxdatecalendar = new Date();

    // Called when the maxdate datetimepicker is set
    $scope.onMaxTimeSet = function (newDate, oldDate) {
        $scope.showmaxdate = false;
        $scope.maxtimecalendar = newDate;
        $scope.maxdate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
    };

    // Called when the mindate datetimepicker is set
    $scope.onMinTimeSet = function (newDate, oldDate) {
        $scope.showmindate = false;
        $scope.mindatecalendar = newDate;
        $scope.mindate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
    };

    $scope.submitDelete = function () {

        if (UIService.checkDatesFormatAndCompare($scope.mindate, $scope.maxdate)) {
            $scope.showRunningDelete = true;
            $scope.showResponseDelete = false;
            $scope.deleteForm.environmentName = $scope.envir;
            // Send dates in String format to avoid problem with timezones
            $scope.deleteForm.minDate = $scope.mindate;
            $scope.deleteForm.maxDate = $scope.maxdate;

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
            // Else, mindate and maxdate are set to yesterday's and today's dates
            $scope.mindate = UIService.getInputCorrectDateFormat(UIService.getDay("yesterday"));
            $scope.maxdate = UIService.getInputCorrectDateFormat(UIService.getDay("today"));

            $scope.mindatecalendar = new Date($scope.mindate);
            $scope.maxdatecalendar = new Date($scope.maxdate);
        }
    };

    EnvironmentsService.findAllAndSelect($scope, null, 'all', null, true);
}