/*global define, angular */

'use strict';

var spApp = angular.module('spApp', [ 'ui.bootstrap', 'ngRoute', 'ngResource', 'ngUpload', 'ngTable', 'ui.bootstrap.datetimepicker', 'ui.select2']);

spApp.config(function ($routeProvider) {
    $routeProvider
        .when('/home', {
            templateUrl: 'partials/home/home.html'
        })
        .when('/live', {
            templateUrl: 'partials/live/live.html',
            controller: LiveCtrl
        })
        .when('/live/:search', {
            templateUrl: 'partials/live/live.html',
            controller: LiveCtrl
        })
        .when('/search', {
            redirectTo: '/search/all/all/all/yesterday/today/all'
        })
        .when('/search/:group/:environment/:soapaction/:mindate/:maxdate/:code', {
            templateUrl: 'partials/search/search.html',
            controller: SearchCtrl
        })
        .when('/analysis', {
            redirectTo: '/analysis/all/all/all/yesterday/today/all'
        })
        .when('/analysis/:group/:environment/:soapaction/:mindate/:maxdate/:code', {
            templateUrl: 'partials/analysis/analysis.html', controller: AnalysisCtrl
        })
        .when('/monitor', {
            templateUrl: 'partials/monitor/monitor.html',
            controller: MonitorCtrl
        })
        .when('/loggers', {
            templateUrl: 'partials/monitor/loggers.html',
            controller: LoggersCtrl
        })
        .when('/admin', {
            templateUrl: 'partials/admin/admin.html',
            controller: AdminCtrl
        })

        .when('/services', {  redirectTo: '/services/all'})
        .when('/services/new/:group', {controller: ServiceNewCtrl, templateUrl: 'partials/services/detail.html'})
        .when('/services/edit/:group/:serviceId', {controller: ServiceEditCtrl, templateUrl: 'partials/services/detail.html'})
        .when('/services/:group', { controller: ServicesCtrl, templateUrl: 'partials/services/list.html'})

        .when('/environments', {  redirectTo: '/environments/all'})
        .when('/environments/new', {controller: EnvironmentNewCtrl, templateUrl: 'partials/environments/detail.html'})
        .when('/environments/edit/:environmentId', {controller: EnvironmentEditCtrl, templateUrl: 'partials/environments/detail.html'})
        .when('/environments/:group', { controller: EnvironmentsCtrl, templateUrl: 'partials/environments/list.html'})

        .when('/mocks', {  redirectTo: '/mockgroups'})
        .when('/mocks/new/:mockGroup', {controller: MockNewCtrl, templateUrl: 'partials/mocks/detail.html'})
        .when('/mocks/edit/:mockId', {controller: MockEditCtrl, templateUrl: 'partials/mocks/detail.html'})
        .when('/mocks/:mockGroup', { controller: MocksCtrl, templateUrl: 'partials/mocks/list.html'})

        .when('/mockgroups', {  redirectTo: '/mockgroups/all'})
        .when('/mockgroups/new', {controller: MockGroupNewCtrl, templateUrl: 'partials/mockgroups/detail.html'})
        .when('/mockgroups/edit/:mockGroupId', {controller: MockGroupEditCtrl, templateUrl: 'partials/mockgroups/detail.html'})
        .when('/mockgroups/:group', { controller: MockGroupsCtrl, templateUrl: 'partials/mockgroups/list.html'})

        .when('/soapactions/edit/:soapActionId', {controller: SoapActionEditCtrl, templateUrl: 'partials/soapactions/detail.html'})
        .when('/soapactions', { controller: SoapActionsCtrl, templateUrl: 'partials/soapactions/list.html'})

        .when('/stats', {
            redirectTo: '/stats/all/all/yesterday/today/all'
        })
        .when('/stats/:group/:environment/:mindate/:maxdate/:code', {
            templateUrl: 'partials/stats/stats.html',
            controller: StatsCtrl
        })
        .otherwise({
            redirectTo: '/home'
        });
});

spApp.run(['$location', '$rootScope', function ($location, $rootScope) {

    $rootScope.namePattern = /^\w*$/;

    $rootScope.$on('$routeChangeSuccess', function (event, current, previous) {
        $rootScope.$broadcast("showGroupsFilter", false);
    });
}]);

