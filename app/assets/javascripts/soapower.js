/*global define, angular */

'use strict';

var spApp = angular.module('spApp', [ 'ui.bootstrap', 'ngRoute', 'ngResource', 'ngUpload', 'ngTable', 'ui.bootstrap.datetimepicker', 'ui.select2']);

spApp.config(function ($routeProvider) {
    $routeProvider
        .when('/home', { templateUrl: 'partials/home/home.html' })
        .when('/live', { controller: LiveCtrl, templateUrl: 'partials/live/live.html' })
        .when('/live/:search', { controller: LiveCtrl, templateUrl: 'partials/live/live.html' })
        .when('/search', { redirectTo: '/search/all/all/all/yesterday/today/all'})
        .when('/search/:groups/:environment/:soapaction/:mindate/:maxdate/:code', {
            controller: SearchCtrl, templateUrl: 'partials/search/search.html'
        })
        .when('/analysis', {
            redirectTo: '/analysis/all/all/all/yesterday/today/all'
        })
        .when('/analysis/:groups/:environment/:soapaction/:mindate/:maxdate/:code', {
            controller: AnalysisCtrl, templateUrl: 'partials/analysis/analysis.html'
        })
        .when('/stats', { redirectTo: '/stats/all/all/yesterday/today/all' })
        .when('/stats/:groups/:environment/:mindate/:maxdate/:code', { controller: StatsCtrl, templateUrl: 'partials/stats/stats.html' })

        .when('/monitor', { controller: MonitorCtrl, templateUrl: 'partials/admin/monitor/monitor.html' })
        .when('/loggers', { controller: LoggersCtrl, templateUrl: 'partials/admin/monitor/loggers.html'})
        .when('/admin', { controller: AdminCtrl, templateUrl: 'partials/admin/admin.html' })

        .when('/services', {  redirectTo: '/services/list/all'})
        .when('/services/new/:groups', {controller: ServiceNewCtrl, templateUrl: 'partials/admin/services/detail.html'})
        .when('/services/edit/:groups/:serviceId', {controller: ServiceEditCtrl, templateUrl: 'partials/admin/services/detail.html'})
        .when('/services/list/:groups', { controller: ServicesCtrl, templateUrl: 'partials/admin/services/list.html'})

        .when('/environments', {  redirectTo: '/environments/list/all'})
        .when('/environments/new', {controller: EnvironmentNewCtrl, templateUrl: 'partials/admin/environments/detail.html'})
        .when('/environments/edit/:environmentId', {controller: EnvironmentEditCtrl, templateUrl: 'partials/admin/environments/detail.html'})
        .when('/environments/list/:groups', { controller: EnvironmentsCtrl, templateUrl: 'partials/admin/environments/list.html'})

        .when('/mockgroups', {  redirectTo: '/mockgroups/list/all'})
        .when('/mockgroups/new', {controller: MockGroupNewCtrl, templateUrl: 'partials/admin/mockgroups/detail.html'})
        .when('/mockgroups/edit/:mockGroupId', {controller: MockGroupEditCtrl, templateUrl: 'partials/admin/mockgroups/detail.html'})
        .when('/mockgroups/list/:groups', { controller: MockGroupsCtrl, templateUrl: 'partials/admin/mockgroups/list.html'})

        .when('/mocks', {  redirectTo: '/mockgroups/list/all'})
        .when('/mocks/new/:mockGroup', {controller: MockNewCtrl, templateUrl: 'partials/admin/mocks/detail.html'})
        .when('/mocks/edit/:mockId', {controller: MockEditCtrl, templateUrl: 'partials/admin/mocks/detail.html'})
        .when('/mocks/list/:mockGroup', { controller: MocksCtrl, templateUrl: 'partials/admin/mocks/list.html'})

        .when('/soapactions', {  redirectTo: '/soapactions/list'})
        .when('/soapactions/edit/:soapActionId', {controller: SoapActionEditCtrl, templateUrl: 'partials/admin/soapactions/detail.html'})
        .when('/soapactions/list', { controller: SoapActionsCtrl, templateUrl: 'partials/admin/soapactions/list.html'})

        .otherwise({ redirectTo: '/home' });
});

spApp.run(['$location', '$rootScope', function ($location, $rootScope) {
    $rootScope.namePattern = /^\w*$/;
    $rootScope.$on('$routeChangeSuccess', function (event, current, previous) {
        $rootScope.$broadcast("showGroupsFilter", false, "Soapower");
    });
}]);

