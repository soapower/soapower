/*global define, angular */

'use strict';

var spApp = angular.module('spApp', [ 'ui.bootstrap', 'ngRoute', 'ngResource', 'ngUpload', 'ngTable', 'ui.bootstrap.datetimepicker', 'ui.select2']);

spApp.config(function ($routeProvider) {
    $routeProvider
        .when('/home', { templateUrl: 'partials/home/home.html' })
        .when('/live', {
            redirectTo: '/live/all/all/all/live/live/all'
        })
        .when('/live/:groups/:environment/:serviceaction/live/live/:code', {
            controller: LiveCtrl, templateUrl: 'partials/live/live.html'
        })
        .when('/search', { redirectTo: '/search/all/all/all/yesterday/today/all'})
        .when('/search/:groups/:environment/:serviceaction/:mindate/:maxdate/:code', {
            controller: SearchCtrl, templateUrl: 'partials/search/search.html'
        })
        .when('/analysis', {
            redirectTo: '/analysis/all/all/all/yesterday/today'
        })
        .when('/analysis/:groups/:environment/:serviceaction/:mindate/:maxdate/:live?', {
            controller: AnalysisCtrl, templateUrl: 'partials/analysis/analysis.html'
        })
        .when('/stats', { redirectTo: '/stats/all/all/yesterday/today' })
        .when('/stats/:groups/:environment/:mindate/:maxdate/:live?', { controller: StatsCtrl, templateUrl: 'partials/stats/stats.html' })

        .when('/monitor', { controller: MonitorCtrl, templateUrl: 'partials/admin/monitor/monitor.html' })
        .when('/loggers', { controller: LoggersCtrl, templateUrl: 'partials/admin/monitor/loggers.html'})
        .when('/admin', { controller: AdminCtrl, templateUrl: 'partials/admin/admin.html' })

        .when('/services', { redirectTo: '/services/list/all'})
        .when('/services/new/:environmentName/:groups', {controller: ServiceNewCtrl, templateUrl: 'partials/admin/services/detail.html'})
        .when('/services/edit/:environmentName/:serviceId/:groups', {controller: ServiceEditCtrl, templateUrl: 'partials/admin/services/detail.html'})
        .when('/services/list/:environmentName/:groups', { controller: ServicesCtrl, templateUrl: 'partials/admin/services/list.html'})

        .when('/environments', {  redirectTo: '/environments/list/all'})
        .when('/environments/new/:groups', {controller: EnvironmentNewCtrl, templateUrl: 'partials/admin/environments/detail.html'})
        .when('/environments/edit/:environmentId/:groups', {controller: EnvironmentEditCtrl, templateUrl: 'partials/admin/environments/detail.html'})
        .when('/environments/list/:groups', { controller: EnvironmentsCtrl, templateUrl: 'partials/admin/environments/list.html'})

        .when('/mockgroups', {  redirectTo: '/mockgroups/list/all'})
        .when('/mockgroups/new/:groups', {controller: MockGroupNewCtrl, templateUrl: 'partials/admin/mockgroups/detail.html'})
        .when('/mockgroups/edit/:mockGroupId/:groups', {controller: MockGroupEditCtrl, templateUrl: 'partials/admin/mockgroups/detail.html'})
        .when('/mockgroups/list/:groups', { controller: MockGroupsCtrl, templateUrl: 'partials/admin/mockgroups/list.html'})

        .when('/mocks', {  redirectTo: '/mockgroups/list/all'})
        .when('/mocks/new/:mockGroupName/:groups', {controller: MockNewCtrl, templateUrl: 'partials/admin/mocks/detail.html'})
        .when('/mocks/edit/:mockGroupName/:mockId/:groups', {controller: MockEditCtrl, templateUrl: 'partials/admin/mocks/detail.html'})
        .when('/mocks/list/:mockGroupName/:groups', { controller: MocksCtrl, templateUrl: 'partials/admin/mocks/list.html'})

        .when('/serviceactions/edit/:serviceActionId', {controller: ServiceActionEditCtrl, templateUrl: 'partials/admin/serviceactions/detail.html'})
        .when('/serviceactions', {  redirectTo: '/serviceactions/all'})
        .when('/serviceactions/:groups', { controller: ServiceActionsCtrl, templateUrl: 'partials/admin/serviceactions/list.html'})

        .otherwise({ redirectTo: '/home' });
});

spApp.run(['$location', '$rootScope', function ($location, $rootScope) {
    $rootScope.namePattern = /^\w*$/;
    $rootScope.$on('$routeChangeSuccess', function (event, current, previous) {
        $rootScope.$broadcast("showGroupsFilter", false, "Soapower");
    });
}]);

