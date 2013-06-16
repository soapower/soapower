/*global define, angular */

'use strict';

define("angular", ['webjars!angular.js'], function () {
    return angular;
});

require(['angular', './controllers', './directives', './filters', './services',
    './lib/jquery-1.10.1.min.js', './lib/ui-bootstrap-0.4.0-SNAPSHOT.js', './lib/ng-grid-2.0.6.min.js'],
    function (angular, controllers) {

        var spApp = angular.module('spApp', ['spApp.filters', 'spApp.services', 'spApp.directives', 'ui.bootstrap', 'ngGrid']);

        spApp.config(function ($routeProvider) {
            $routeProvider
                .when('/home', {
                    templateUrl: 'partials/home.html'
                })
                .when('/live', {
                    templateUrl: 'partials/live.html',
                    controller: controllers.MyCtrl2
                })
                .when('/search', {
                    templateUrl: 'partials/search.html',
                    controller: controllers.SearchCtrl
                })
                .when('/search/:environment/:soapaction/:mindate/:maxdate/:code', {
                    templateUrl: 'partials/search.html',
                    controller: controllers.SearchCtrl
                })
                .when('/analysis', {
                    templateUrl: 'partials/analysis.html',
                    controller: controllers.MyCtrl2
                })
                .when('/statistics', {
                    templateUrl: 'partials/stats.html',
                    controller: controllers.StatsCtrl
                })
                .when('/statistics/:environment/:soapaction/:mindate/:maxdate/:code', {
                    templateUrl: 'partials/stats.html',
                    controller: controllers.StatsCtrl
                })
                .otherwise({
                    redirectTo: '/home'
                });
        });
        angular.bootstrap(document, ['spApp']);
    });
