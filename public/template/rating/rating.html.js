angular.module("template/rating/rating.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("template/rating/rating.html",
    "<span ng-mouseleave=\"reset()\">\n" +
    "	<i ng-repeat=\"number in range\" ng-mouseenter=\"enter(number)\" ng-click=\"rate(number)\" ng-class=\"{'icon-star': number <= val, 'icon-star-empty': number > val}\"></i>\n" +
    "</span>\n" +
    "");
}]);
