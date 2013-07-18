/*global define */

'use strict';

define(['angular'], function (angular) {

    /* Directives */

    angular.module('spApp.directives', []).
        directive('spCriterias', function () {
                      return {
                          restrict: 'E',
                          scope: {
                              soapactions: '='
                          },
                          controller: function ($scope, $element, $attrs, $transclude, $location, $routeParams, EnvironmentsService, SoapactionsService, CodesService, UIService) {

                              EnvironmentsService.findAllAndSelect($scope, $routeParams.environment);
                              CodesService.findAllAndSelect($scope, $routeParams);
                              $scope.ctrlPath = $scope.$parent.ctrlPath;

                              $scope.showSoapactions = false;
                              if ($attrs.soapactions == "yes") {
                                  $scope.showSoapactions = true;
                                  SoapactionsService.findAllAndSelect($scope, $routeParams);
                              }

                              $scope.mindate = UIService.getDateFromParam($routeParams.mindate);
                              $scope.maxdate = UIService.getDateFromParam($routeParams.maxdate);

                              $scope.changeCriteria = function () {
                                  UIService.reloadPage($scope);
                              };

                              $scope.$watch('mindate', function () {
                                  if ($scope.mindate) {
                                      $scope.showmindate = false;
                                      if ($scope.mindate > $scope.maxdate) {
                                          $scope.maxdate = $scope.mindate;
                                      }
                                  }
                              });
                              $scope.$watch('maxdate', function () {
                                  if ($scope.maxdate) {
                                      $scope.showmaxdate = false;
                                      if ($scope.mindate > $scope.maxdate) {
                                          $scope.maxdate = $scope.mindate;
                                      }
                                  }
                              });
                          },
                          templateUrl: 'partials/common/criterias.html',
                          replace: true
                      }
                  })
                  .directive('spGroups', function () {
                              return {
                                  restrict: 'E',
                                  scope: {
                                      groups: '='
                                  },
                                  controller: function ($scope, $element, $attrs, $transclude, $location, $routeParams, GroupsService, UIService) {

                                      GroupsService.findAllAndSelect($scope, $routeParams.group);

                                      $scope.changeGroup = function () {
                                          UIService.reloadPage($scope);
                                      };


                                  },
                                  templateUrl: 'partials/common/group.html',
                                  replace: true
                              }
                          })
        .directive('spGraphtimes', function () {
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
        })
        .directive('activeLink', ['$location', function(location) {
            return {
                restrict: 'A',
                link: function(scope, element, attrs, controller) {
                    var clazz = attrs.activeLink;
                    var path = "/" + attrs.link;
                    scope.location = location;
                    scope.$watch('location.path()', function(newPath) {
                        if (path === newPath) {
                            element.addClass(clazz);
                        } else {
                            element.removeClass(clazz);
                        }
                    });
                }

            };

        }]);
});