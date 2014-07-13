/*global define, angular */

'use strict';

var spApp = angular.module('spApp', [ 'ui.bootstrap', 'ngRoute', 'ngCookies', 'ngResource', 'ngTable', 'ui.bootstrap.datetimepicker', 'ui.select2', 'hljs']);

spApp.config(function ($routeProvider) {
    $routeProvider
        .when('/home', { controller: HomeCtrl, templateUrl: 'partials/home/home.html' })
        .when('/live', { redirectTo: '/live/all/all/all/live/live/all' })
        .when('/live/:groups/:environment/:serviceaction/live/live/:code', { controller: LiveCtrl, templateUrl: 'partials/live/live.html' })

        .when('/search', { redirectTo: '/search/all/all/all/yesterday/today/all' })
        .when('/search/:groups/:environment/:serviceaction/:mindate/:maxdate/:code', { controller: SearchCtrl, templateUrl: 'partials/search/search.html' })
        .when('/visualize/:requestorresponse/:id', { controller: VisualizeCtrl, templateUrl: 'partials/search/requestorresponse.html'})

        .when('/analysis', { redirectTo: '/analysis/all/all/all/yesterday/today' })
        .when('/analysis/:groups/:environment/:serviceaction/:mindate/:maxdate/:live?', {  controller: AnalysisCtrl, templateUrl: 'partials/analysis/analysis.html' })

        .when('/stats', { redirectTo: '/stats/all/all/yesterday/today' })
        .when('/stats/:groups/:environment/:mindate/:maxdate/:live?', { controller: StatsCtrl, templateUrl: 'partials/stats/stats.html' })

        .when('/monitor', { controller: MonitorCtrl, templateUrl: 'partials/admin/monitor/monitor.html' })
        .when('/loggers', { controller: LoggersCtrl, templateUrl: 'partials/admin/monitor/loggers.html'})

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


spApp.config(function ($httpProvider) {
    $httpProvider.interceptors.push('httpRequestInterceptor');
    $httpProvider.responseInterceptors.push('httpResponseInterceptor');
});

spApp.factory('httpResponseInterceptor', function ($rootScope, $q, $location) {
    var success = function (response) {
        return response;
    };

    var error = function (response) {
        if (response.status === 401 && response.config.url != "/login") {
            //redirect them back to login page
            $location.path('/home');
            return $q.reject(response);
        } else {
            return $q.reject(response);
        }
    };

    return function (promise) {
        return promise.then(success, error);
    };
});

spApp.factory('httpRequestInterceptor', function () {
    return {
        request: function (config) {
            if (config.method == "GET") {
                /*var token = new Date().getTime();
                 if(config.url.search("tooltip-html-unsafe-popup") == -1
                 && config.url.search("confirm.html") == -1
                 && config.url.search("backdrop.html") == -1
                 && config.url.search("window.html") == -1){
                 config.url = config.url + "?cacheSlayer=" + token.toString();
                 }*/
            }
            return config;
        }
    };
});

spApp.run(['$location', '$rootScope', '$route', 'AuthenticationService', function ($location, $rootScope, $route, AuthenticationService) {

    $rootScope.namePattern = /^\w*$/;
    // enumerate routes that don't need authentication
    var routesThatDontRequireAuth = ['/login'];
    var routesForAdmin = ['/admin'];

    // check if current location matches route
    var routeClean = function (route) {
        return _.find(routesThatDontRequireAuth,
            function (noAuthRoute) {
                return _.str.startsWith(route, noAuthRoute);
            });
    };

    // check if current location matches route
    var routeAdmin = function (route, routesList) {
        return _.find(routesList,
            function (adminRoute) {
                return _.str.startsWith(route, adminRoute);
            });
    };

    $rootScope.$on("reloadAuthentication", function (event, action) {
        console.log("loginController on reloadAuthentication" + event);
        if (action == 'login') {
            $route.reload();
        }
        if (action == 'logout') {
            $rootScope.userConnected = false;
            $rootScope.isAdmin = false;
        }
    });

    $rootScope.$on('$locationChangeStart', function (ev, to, toParams, from, fromParams) {

        AuthenticationService.isLoggedInPromise()
            .then(function (userConnected) { // success
                $rootScope.userConnected = userConnected;
                $rootScope.isAdmin = eval(userConnected.profile == 'ADMIN');
                if ((routeAdmin($location.url(), routesForAdmin) && !$rootScope.isAdmin) || !$rootScope.isAdmin) {
                    // redirect back to login
                    ev.preventDefault();
                    console.log("$locationChangeStart call to error");
                    $location.path("/home");
                    $route.reload();
                }
            }, function (reason) { // failed
                $scope.userConnected = null;
            });
    });

}]);