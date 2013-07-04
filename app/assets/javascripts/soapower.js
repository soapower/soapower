/*global define, angular */

'use strict';


define("angular", ['/assets/javascripts/angular.js'], function () {
    return angular;
});

define("$", ['/assets/javascripts/lib/jquery-1.10.1.min.js'], function () {
    return $;
});
// make global var
requirejs.config({
    shim: {
        'd3': {
            exports: 'd3'
        },
        'nv': {
            deps: ['d3'],
            exports: 'nv'
        }
    }
});

define("d3", ['/assets/javascripts/lib/d3/d3.v3.js'], function () {
    return d3;
});
define("nv", ['/assets/javascripts/lib/d3/nv.d3.js'], function () {
    return nv;
});





require(['angular', 'app/directives', 'app/services', //'app/filters',
    'app/controllers/adminController',
    'app/controllers/analysisController',
    'app/controllers/environmentsController',
    'app/controllers/liveController',
    'app/controllers/searchController',
    'app/controllers/servicesController',
    'app/controllers/soapactionsController',
    'app/controllers/statsController',
    'lib/angular-resource.min',
    'lib/ui-bootstrap-0.4.0-SNAPSHOT',
    'lib/ng-grid-2.0.7.yesnault',
    'lib/ng-upload.min',
    'main',
    'd3'
    ], function (angular) {

    console.log(angular);

        var spApp = angular.module('spApp', [ 'ngResource', 'spApp.services', 'spApp.directives', 'ui.bootstrap', 'ngGrid', 'ngUpload']);

        spApp.config(function ($routeProvider) {
            $routeProvider
                .when('/home', {
                    templateUrl: 'partials/home/home.html'
                })
                .when('/live', {
                    templateUrl: 'partials/live/live.html',
                    controller: LiveCtrl
                })
                .when('/admin', {
                    templateUrl: 'partials/admin/admin.html',
                    controller: AdminCtrl
                })
                .when('/search', {
                    templateUrl: 'partials/search/search.html',
                    controller: SearchCtrl
                })
                .when('/search/:environment/:soapaction/:mindate/:maxdate/:code', {
                    templateUrl: 'partials/search/search.html',
                    controller: SearchCtrl
                })
                .when('/analysis', {
                    templateUrl: 'partials/analysis/analysis.html',
                    controller: AnalysisCtrl
                })
                .when('/services', { controller: ServicesCtrl, templateUrl: 'partials/services/list.html'})
                .when('/services/new', {controller: ServiceNewCtrl, templateUrl: 'partials/services/detail.html'})
                .when('/services/:serviceId', {controller: ServiceEditCtrl, templateUrl: 'partials/services/detail.html'})
                .when('/environments', { controller: EnvironmentsCtrl, templateUrl: 'partials/environments/list.html'})
                .when('/environments/new', {controller: EnvironmentNewCtrl, templateUrl: 'partials/environments/detail.html'})
                .when('/environments/:environmentId', {controller: EnvironmentEditCtrl, templateUrl: 'partials/environments/detail.html'})
                .when('/soapactions', { controller: SoapActionsCtrl, templateUrl: 'partials/soapactions/list.html'})
                .when('/soapactions/:soapActionId', {controller: SoapActionEditCtrl, templateUrl: 'partials/soapactions/detail.html'})
                .when('/stats', {
                    templateUrl: 'partials/stats/stats.html',
                    controller: StatsCtrl
                })
                .when('/statistics/:environment/:soapaction/:mindate/:maxdate/:code', {
                    templateUrl: 'partials/stats/stats.html',
                    controller: StatsCtrl
                })
                .otherwise({
                    redirectTo: '/home'
                });
        });
        angular.bootstrap(document, ['spApp']);
    });
