/*global define, angular */

'use strict';

define("angular", ['webjars!angular.js'], function () {
    return angular;
});

require(['angular', './directives', './filters', './services',
    './admin/adminController',
    './analysis/analysisController',
    './live/liveController',
    './search/searchController',
    './services/servicesController',
    './stats/statsController',
    './lib/jquery-1.10.1.min', './lib/ui-bootstrap-0.4.0-SNAPSHOT', './lib/ng-grid-2.0.6.min', './lib/ng-upload.min' ],
    function (angular) {

        var spApp = angular.module('spApp', ['spApp.filters', 'spApp.services', 'spApp.directives', 'ui.bootstrap', 'ngGrid', 'ngUpload']);

        spApp.config(function ($routeProvider) {
            $routeProvider
                .when('/home', {
                    templateUrl: 'app/home/views/home.html'
                })
                .when('/live', {
                    templateUrl: 'app/live/views/live.html',
                    controller: LiveCtrl
                })
                .when('/admin', {
                    templateUrl: 'app/admin/views/admin.html',
                    controller: AdminCtrl
                })
                .when('/search', {
                    templateUrl: 'app/search/views/search.html',
                    controller: SearchCtrl
                })
                .when('/search/:environment/:soapaction/:mindate/:maxdate/:code', {
                    templateUrl: 'app/search/views/search.html',
                    controller: SearchCtrl
                })
                .when('/analysis', {
                    templateUrl: 'app/analysis/views/analysis.html',
                    controller: AnalysisCtrl
                })
                .when('/services', { controller: ServicesCtrl, templateUrl: 'app/services/views/list.html'})
                .when('/services/:serviceId', {controller: ServiceEditCtrl, templateUrl: 'app/services/views/detail.html'})
                .when('/services/new', {controller: ServiceNewCtrl, templateUrl: 'app/services/views/detail.html'})
                .when('/stats', {
                    templateUrl: 'app/stats/views/stats.html',
                    controller: StatsCtrl
                })
                .when('/statistics/:environment/:soapaction/:mindate/:maxdate/:code', {
                    templateUrl: 'app/stats/views/stats.html',
                    controller: StatsCtrl
                })
                .otherwise({
                    redirectTo: '/home'
                });
        });
        angular.bootstrap(document, ['spApp']);
    });
