'use strict';

spApp.directive('spCriterias', ['$filter', function ($filter) {
    return {
        restrict: 'E',
        scope: {
            serviceactions: '='
        },
        controller: function ($scope, $element, $attrs, $transclude, $location, $routeParams, EnvironmentsService, ServiceActionsService, CodesService, UIService) {
            EnvironmentsService.findAllAndSelect($scope, $routeParams.environment, $routeParams.group, null, true);

            CodesService.findAllAndSelect($scope, $routeParams);
            $scope.ctrlPath = $scope.$parent.ctrlPath;

            $scope.mindate = UIService.getInputCorrectDateFormat($routeParams.mindate);
            $scope.maxdate = UIService.getInputCorrectDateFormat($routeParams.maxdate);

            // Initialise the calendars to today's date
            $scope.mindatecalendar = new Date();
            $scope.maxdatecalendar = new Date();

            $scope.showServiceactions = $attrs.serviceactions == "yes";

            // Called when the mindate datetimepicker is set
            $scope.onMinTimeSet = function (newDate, oldDate) {
                $scope.showmindate = false;
                $scope.mindate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
            };
            // Called when the maxdate datetimepicker is set
            $scope.onMaxTimeSet = function (newDate, oldDate) {
                $scope.showmaxdate = false;
                $scope.maxdate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
            };

            $scope.changeCriteria = function () {
                // Check that the date inputs format are correct and that the mindate is before the maxdate
                if (UIService.checkDatesFormatAndCompare($scope.mindate, $scope.maxdate)) {
                    UIService.reloadPage($scope, $scope.showServiceactions);
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
            $scope.showGroup = false;
            $scope.lastGroupSelected = [];
            $scope.$on("showGroupsFilter", function (event, groups, caller) {

                console.log("caller " + caller + " showGroupsFilter with groups: " + groups);
                $scope.showGroup = (groups != false);
                if (groups && groups != false) {
                    $scope.groupsSelected = groups.split(',');
                    // if there is "all" and an other group, keep "all" only
                    if ($scope.groupsSelected.length > 1 && $scope.groupsSelected.indexOf("all") > -1) {
                        $scope.groupsSelected = ["all"];
                    }

                    // Check if all groups in URL exists in list of groups (except ALL)
                    // Url to test : http://localhost:9000/#/services/new
                    $scope.loadGroups(function () {
                        angular.forEach($scope.groupsSelected, function (g) {
                            if (g != "all" && $scope.groups.indexOf(g) == -1) {
                                console.log("Group not exist:" + g);
                                $scope.groupsSelected.splice($scope.groupsSelected.indexOf(g), 1);
                            }
                        });
                        if ($scope.groupsSelected.length == 0) $scope.groupsSelected = ["all"];
                    });
                }
            });

            $scope.changeGroup = function () {
                // if user select "All" and no all before, reset groupsSelected to all only
                if ($scope.lastGroupSelected && $scope.groupsSelected) {
                    if ($scope.lastGroupSelected.indexOf("all") == -1 && $scope.groupsSelected.indexOf("all") > -1) {
                        $scope.groupsSelected = ["all"];
                    } else if ($scope.lastGroupSelected.indexOf("all") > -1 &&
                        $scope.groupsSelected.indexOf("all") > -1 &&
                        $scope.groupsSelected.length > 1) {
                        // if user select not "All" but have "All" before, delete "All" from list
                        $scope.groupsSelected.splice($scope.lastGroupSelected.indexOf("all"), 1)
                    }
                }
                $scope.lastGroupSelected = $scope.groupsSelected;
                if ($scope.showGroup) $rootScope.$broadcast("ReloadPage", $scope.groupsSelected, "spGroups.change()");
            };

            $scope.loadGroups = function (callBack) {
                GroupsService.findAll().success(function (groups) {
                    $scope.groups = groups;
                    $scope.groups.unshift("all");
                    if (callBack) callBack();
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
                data.push(newValue);
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
            }

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

spApp.directive('spBuildInfo', function () {
    return {
        restrict: 'E',
        replace: true,
        template: "<span>{{info}}</span>",
        controller: function ($scope, $attrs, IndexService) {
            IndexService.getBuildInfo().success(function (infos) {
                $scope.info = infos.projectName + " " + infos.version;
            });
        }
    }
});

spApp.directive('spDocumentation', function () {
    return {
        restrict: 'E',
        replace: true,
        template: "<a href='http://soapower.readthedocs.org/en/{{version}}'><i class='fa fa-book'></i> </i> Documentation </a>",
        controller: function ($scope, IndexService) {
            IndexService.getBuildInfo().success(function (infos) {
                $scope.version = infos["versionDoc"];
            });
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
                $scope.idSelected = row._id.$oid;
                $scope.serviceId = row.serviceId.$oid;
                $scope.contentType = row.contentType;
                $scope.environmentName = row.environmentName;

                ReplayService.beforeReplay($scope.idSelected).then(function (data) {
                    if ($scope.contentType == "application/json")
                        data.data = JSON.stringify(data.data);
                    $scope.replayContent = data;
                    $('#myModal').modal('show');
                });
            };

            $scope.sendReplayReq = function () {
                ReplayService.replay($scope.idSelected, $scope.environmentName, $scope.serviceId, $scope.contentType, $scope.replayContent);
                $('#myModal').modal('hide')
            };
        }
    }
});

spApp.directive('spFilter', function ($http, $filter) {
    return {
        restrict: 'E',
        scope: {
            page: '@page'
        },
        controller: function ($scope, $element, $attrs, $transclude, $location, $routeParams, EnvironmentsService, ServiceActionsService, CodesService, UIService) {
            // Scope initialization
            EnvironmentsService.findAllAndSelect($scope, $routeParams.environment, $routeParams.groups, null, true);
            CodesService.findAllAndSelect($scope, $routeParams);
            ServiceActionsService.findAllAndSelect($scope, $routeParams.serviceaction, $routeParams.groups);

            if ($scope.page == "live") {
                // The directive was called from the Live Page
                $scope.showresearch = true;
                $scope.showstatus = true;
                $scope.showserviceactions = true;

                $scope.request = true;
                $scope.response = true;
                $scope.search = "";


                $scope.changeStatus = function () {
                    // If the user set a new status
                    $http({
                        method: "POST",
                        url: "/live/changeCriteria",
                        data: {key: "code", value: $scope.code},
                        headers: {'Content-Type': 'application/json'}
                    })
                };

                $scope.changeEnvironment = function () {
                    // If the user set a new Environment
                    $http({
                        method: "POST",
                        url: "/live/changeCriteria",
                        data: {key: "environment", value: $scope.environment},
                        headers: {'Content-Type': 'application/json'}
                    })
                };

                $scope.changeRequest = function () {
                    if ($scope.request == false && $scope.response == false) {
                        $scope.search = "";
                        $scope.changeSearch();
                    }
                    // If the user check or uncheck the request box
                    $http({
                        method: "POST",
                        url: "/live/changeCriteria",
                        data: {key: "request", value: $scope.request.toString()},
                        headers: {'Content-Type': 'application/json'}
                    })
                };

                $scope.changeServiceAction = function () {
                    $http({
                        method: "POST",
                        url: "/live/changeCriteria",
                        data: {key: "serviceAction", value: $scope.serviceaction},
                        headers: {'Content-Type': 'application/json'}
                    })
                };

                $scope.changeResponse = function () {
                    if ($scope.request == false && $scope.response == false) {
                        $scope.search = "";
                        $scope.changeSearch();
                    }
                    // If the user check or uncheck the response box
                    $http({
                        method: "POST",
                        url: "/live/changeCriteria",
                        data: {key: "response", value: $scope.response.toString()},
                        headers: {'Content-Type': 'application/json'}
                    })
                };

                // Called when the user change the textarea
                $scope.changeSearch = function () {
                    $http({
                        method: "POST",
                        url: "/live/changeCriteria",
                        data: {key: "search", value: $scope.search},
                        headers: {'Content-Type': 'application/json'}
                    })
                };
            } else {
                // Search, Analysis and Statistic need calendars and filter button
                $scope.showcalendars = true;
                $scope.showfilterbutton = true;
                $scope.showstatus = true;
                $scope.showserviceactions = true;

                $scope.mindate = UIService.getInputCorrectDateFormat($routeParams.mindate);
                $scope.maxdate = UIService.getInputCorrectDateFormat($routeParams.maxdate);

                // Initialise the calendars to today's date
                $scope.mindatecalendar = new Date();
                $scope.maxdatecalendar = new Date();

                // Called when the mindate datetimepicker is set
                $scope.onMinTimeSet = function (newDate, oldDate) {
                    $scope.showmindate = false;
                    $scope.mindate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
                };
                // Called when the maxdate datetimepicker is set
                $scope.onMaxTimeSet = function (newDate, oldDate) {
                    $scope.showmaxdate = false;
                    $scope.maxdate = $filter('date')(newDate, "yyyy-MM-dd HH:mm");
                };

                $scope.changeCriteria = function () {
                    // Check that the date inputs format are correct and that the mindate is before the maxdate
                    if (UIService.checkDatesFormatAndCompare($scope.mindate, $scope.maxdate)) {
                        $scope.groups = $routeParams.groups;
                        UIService.reloadPage($scope, $scope.showServiceactions, $scope.page);
                    } else {
                        // Else, mindate and maxdate are set to yesterday's and today's dates
                        $scope.mindate = UIService.getInputCorrectDateFormat(UIService.getDay("yesterday"));
                        $scope.maxdate = UIService.getInputCorrectDateFormat(UIService.getDay("today"));
                    }
                };

                if ($scope.page == "search") {

                    $scope.showresearch = true;
                    $scope.request = $routeParams.request ? UIService.stringToBoolean($routeParams.request) : true;
                    $scope.response = $routeParams.response ? UIService.stringToBoolean($routeParams.response) : true;
                    $scope.search = $routeParams.search ? $routeParams.search : "";

                    // Called when the user check or uncheck the request checkbox
                    $scope.changeRequest = function () {
                        if ($scope.request == false && $scope.response == false) {
                            $scope.search = "";
                        }
                    }

                    // Called when the user check or uncheck the response checkbox
                    $scope.changeResponse = function () {
                        if ($scope.request == false && $scope.response == false) {
                            $scope.search = "";
                        }
                    }
                }
                else if ($scope.page == "statistics") {
                    $scope.showserviceactions = false;
                    $scope.showlive = true
                    $scope.showstatus = false;
                    $scope.live = $routeParams.live ? UIService.stringToBoolean($routeParams.live) : false;
                }
                else if ($scope.page == "analysis") {
                    $scope.showserviceactions = false;
                    $scope.showlive = true
                    $scope.showstatus = false;
                    $scope.showserviceactions = true;
                    $scope.live = $routeParams.live ? UIService.stringToBoolean($routeParams.live) : false;
                }
            }
            $scope.ctrlPath = $scope.$parent.ctrlPath;
        },
        templateUrl: 'partials/common/filter.html',
        replace: true
    }
});