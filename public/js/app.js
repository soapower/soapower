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
                    templateUrl: 'partials/home.html'
                })
                .when('/live', {
                    templateUrl: 'partials/live.html',
                    controller: LiveCtrl
                })
                .when('/admin', {
                    templateUrl: 'partials/admin.html',
                    controller: AdminCtrl
                })
                .when('/search', {
                    templateUrl: 'partials/search.html',
                    controller: SearchCtrl
                })
                .when('/search/:environment/:soapaction/:mindate/:maxdate/:code', {
                    templateUrl: 'partials/search.html',
                    controller: SearchCtrl
                })
                .when('/analysis', {
                    templateUrl: 'partials/analysis.html',
                    controller: AnalysisCtrl
                })
                .when('/services', { controller: ServicesCtrl, templateUrl: 'js/services/views/list.html'})
                .when('/services/:serviceId', {controller: ServiceEditCtrl, templateUrl: 'js/services/views/detail.html'})
                .when('/services/new', {controller: ServiceNewCtrl, templateUrl: 'js/services/views/detail.html'})
                .when('/stats', {
                    templateUrl: 'partials/stats.html',
                    controller: StatsCtrl
                })
                .when('/statistics/:environment/:soapaction/:mindate/:maxdate/:code', {
                    templateUrl: 'partials/stats.html',
                    controller: StatsCtrl
                })
                .otherwise({
                    redirectTo: '/home'
                });
        });
        angular.bootstrap(document, ['spApp']);
    });
