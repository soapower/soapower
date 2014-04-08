/*global define */

'use strict';

spApp.directive('spCriterias', ['$filter', function ($filter) {
    return {
        restrict: 'E',
        scope: {
            soapactions: '='
        },
        controller: function ($scope, $element, $attrs, $transclude, $location, $routeParams, EnvironmentsService, SoapactionsService, CodesService, UIService) {
            EnvironmentsService.findAllAndSelect($scope, $routeParams.environment, $routeParams.groups, null, true);
            CodesService.findAllAndSelect($scope, $routeParams);
            $scope.ctrlPath = $scope.$parent.ctrlPath;

            $scope.mindate = UIService.getInputCorrectDateFormat($routeParams.mindate);
            $scope.maxdate = UIService.getInputCorrectDateFormat($routeParams.maxdate);

            // Initialise the calendars to today's date
            $scope.mindatecalendar = new Date();
            $scope.maxdatecalendar = new Date();

            $scope.showSoapactions = false;
            if ($attrs.soapactions == "yes") {
                $scope.showSoapactions = true;
                SoapactionsService.findAllAndSelect($scope, $routeParams);
            }

            // Called when the mindate datetimepicker is set
            $scope.onMinTimeSet = function (newDate, oldDate) {
                $scope.showmindate = false;
                $scope.mindate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
            }
            // Called when the maxdate datetimepicker is set
            $scope.onMaxTimeSet = function (newDate, oldDate) {
                $scope.showmaxdate = false;
                $scope.maxdate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
            }

            $scope.changeCriteria = function () {
                // Check that the date inputs format are correct and that the mindate is before the maxdate
                if (UIService.checkDatesFormatAndCompare($scope.mindate, $scope.maxdate)) {
                    UIService.reloadPage($scope, $scope.showSoapactions);
                } else {
                    // Else, mindate and maxdate are set to yesterday's and today's dates
                    $scope.mindate = UIService.getInputCorrectDateFormat(UIService.getDay("yesterday"));
                    $scope.maxdate = UIService.getInputCorrectDateFormat(UIService.getDay("today"));
                }
            };
        },
        templateUrl: 'partials/common/criterias.html',
        replace: true
    }
}]);

spApp.directive('spGroups', function () {
    return {
        restrict: 'E',
        scope: {
            soapactions: '='
        },
        controller: function ($scope, $rootScope, $routeParams, GroupsService) {
            $scope.showSelect = false;
            $scope.showGroup = false;
            $scope.$on("showGroupsFilter", function (event, groups) {
                $scope.showGroup = (groups != false);
                $scope.groups = [];
                // TODO Split group with "|" if many groups in url
                $scope.groups.push(groups);
            });

            $scope.changeGroup = function () {
                $scope.showSelect = false;
                $rootScope.group = $scope.groups;
                $rootScope.$broadcast("ReloadPage", $scope.groups);
            };

            $scope.selectGroupsOptions = {
                'multiple': true,
                'simple_tags': true,
                'tags': []
            };

            $scope.loadGroups = function() {
                GroupsService.findAll().success(function(groups) {
                    $scope.selectGroupsOptions["tags"] = groups.values;
                });
            };
            $scope.loadGroups()
        },
        templateUrl: 'partials/common/group.html',
        replace: true
    }
});

spApp.directive('spGraphtimes', function () {
    return {
        restrict: 'E',
        scope: { // attributes bound to the scope of the directive
            graph: '='
        },
        link: function ($scope, element, attrs) {
            var n = 120,
                duration = 1200,
                now = new Date(Date.now() - duration),
                data = d3.range(n).map(function () {
                    return 0;
                });

            var margin = {top: 6, right: 0, bottom: 20, left: 40},
                width = 760 - margin.right,
                height = 120 - margin.top - margin.bottom;

            $scope.$on("stopMonitor", function (event, value) {
                console.log("stopping monitor");
                svg.remove();
                svg = null;
            });

            $scope.$on(attrs.graph, function (event, newValue) {
                tick(newValue);
            });

            function tick(newValue) {
                if (svg == null) {
                    console.log("exit tick, svg is already removed");
                    return;
                }
                // update the domains
                now = new Date();
                x.domain([now - (n - 2) * duration, now - duration]);
                y.domain([0, d3.max(data)]);

                // push the accumulated count onto the back, and reset the count
                //data.push(Math.min(30, count));
                data.push(newValue)
                //  count = 0;

                // redraw the line
                svg.select(".line")
                    .attr("d", line)
                    .attr("transform", null);

                // slide the x-axis left
                axis.transition()
                    .duration(duration)
                    .ease("linear")
                    .call(x.axis);

                // slide the line left
                path.transition()
                    .duration(duration)
                    .ease("linear")
                    .attr("transform", "translate(" + x(now - (n - 1) * duration) + ")")
                    .each("end", tick);

                // pop the old data point off the front
                data.shift();
            };

            var x = d3.time.scale()
                .domain([now - (n - 2) * duration, now - duration])
                .range([0, width]);

            var y = d3.scale.linear()
                .range([height, 0]);

            var line = d3.svg.line()
                .interpolate("basis")
                .x(function (d, i) {
                    if (isNaN(i)) {
                        return 0;
                    } else {
                        return x(now - (n - 1 - i) * duration);
                    }

                })
                .y(function (d, i) {
                    if (isNaN(d)) {
                        return 0;
                    } else {
                        return y(d);
                    }

                });

            var svg = d3.select(element[0])
                .append("svg")
                .attr("width", width + margin.left + margin.right)
                .attr("height", height + margin.top + margin.bottom)
                .style("margin-left", -margin.left + "px")
                .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            svg.append("defs").append("clipPath")
                .attr("id", "clip")
                .append("rect")
                .attr("width", width)
                .attr("height", height);

            var axis = svg.append("g")
                .attr("class", "x axis")
                .attr("transform", "translate(0," + height + ")")
                .call(x.axis = d3.svg.axis().scale(x).orient("bottom"));

            var path = svg.append("g")
                .attr("clip-path", "url(#clip)")
                .append("path")
                .data([data])
                .attr("class", "line");
        }
    }
});

spApp.directive('activeLink', ['$location', function (location) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var clazz = attrs.activeLink;
            var path = "/" + attrs.link;
            scope.location = location;
            scope.$watch('location.path()', function (newPath) {
                if (newPath.indexOf(path) == 0) {
                    element.addClass(clazz);
                } else {
                    element.removeClass(clazz);
                }
            });
        }
    };
}]);

spApp.directive('spReplay', function () {
    return {
        restrict: 'E',
        replace: true,
        templateUrl: "partials/common/cellReplayTemplate.html"
    }
});
spApp.directive('spRequest', function () {
    return {
        restrict: 'E',
        replace: true,
        templateUrl: "partials/common/cellRequestTemplate.html"
    }
});
spApp.directive('spResponse', function () {
    return {
        restrict: 'E',
        replace: true,
        templateUrl: "partials/common/cellResponseTemplate.html"
    }
});
spApp.directive('spStatus', function () {
    return {
        restrict: 'E',
        replace: true,
        templateUrl: "partials/common/cellStatusTemplate.html",
        scope: {
            status: '='
        }
    }
});
spApp.directive('spShowresults', function () {
    return {
        restrict: 'E',
        replace: true,
        templateUrl: "partials/common/showResults.html",
        controller: function ($scope) {
            $scope.choiceNbResults = [ 10, 50, 100, 1000, 10000 ];
            $scope.nbResults = 50
        }
    }
});
spApp.directive('spReplayEdit', function () {
    return {
        restrict: 'E',
        replace: true,
        templateUrl: "partials/common/replay.html",
        controller: function ($scope, ReplayService) {
            $scope.replayReq = function (row) {
                $scope.idSelected = row.id;
                ReplayService.beforeReplay(row.id).then(function (data) {
                    $scope.replayContent = data;
                    $('#myModal').modal('show')
                });
            };

            $scope.sendReplayReq = function () {
                ReplayService.replay($scope.idSelected, $scope.replayContent);
                $('#myModal').modal('hide')
            };
        }
    }
});
