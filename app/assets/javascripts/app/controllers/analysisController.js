function AnalysisCtrl($scope, $rootScope, $routeParams, $http, UIService) {
    $scope.ctrlPath = "analysis";

    var group = $routeParams.group ? $routeParams.group : 'all';
    var environment = $routeParams.environment ? $routeParams.environment : 'all';
    var serviceaction = $routeParams.serviceaction ? $routeParams.serviceaction : 'all';
    var mindate = $routeParams.mindate ? $routeParams.mindate : 'all';
    var maxdate = $routeParams.maxdate ? $routeParams.maxdate : 'all';
    var code = $routeParams.code ? $routeParams.code : 'all';
    var url = '/analysis/' + group +
        '/' + environment +
        '/' + encodeURIComponent(serviceaction) +
        '/' + mindate +
        '/' + maxdate +
        '/' + code +
        '/true/load?call=' + new Date();

    ///analysis/:environment/:serviceAction/:minDate/:maxDate/:status/:statsOnly/load

    $http({
        method: 'GET',
        url: url,
        cache: false
    }).success(function (largeLoad) {
            testdata = largeLoad.map(function (series) {
                series.values = series.values.map(function (d) {
                    return {x: d[0], y: d[1] }
                });
                return series;
            });

            var chart;

            nv.addGraph(function () {
                chart = nv.models.lineChart();

                chart.x(function (d, i) {
                    return i
                })

                chart.xAxis.tickFormat(function (d) {
                    var dx = testdata[0].values[d] && testdata[0].values[d].x || 0;
                    return dx ? d3.time.format('%x')(new Date(dx)) : '';
                })
                    .showMaxMin(false);

                chart.yAxis
                    .tickFormat(d3.format(',f'));

                chart.showXAxis(true);

                d3.select('#chart1 svg')
                    .datum(testdata)
                    .transition().duration(500)
                    .call(chart);

                //TODO: Figure out a good way to do this automatically
                nv.utils.windowResize(chart.update);
                //nv.utils.windowResize(function() { d3.select('#chart1 svg').call(chart) });

                chart.dispatch.on('stateChange', function (e) {
                    nv.log('New State:', JSON.stringify(e));
                });

                $scope.$apply();
                return chart;
            });

        });

    $rootScope.$broadcast("showGroupsFilter", $routeParams.group);

    $scope.$on("ReloadPage", function (event) {
        UIService.reloadPage($scope, true);
    });
}
