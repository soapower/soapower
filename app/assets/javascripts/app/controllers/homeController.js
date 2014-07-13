function HomeCtrl($scope, $http, $timeout, AuthenticationService) {
    $scope.error = "";
    /**
     * Login using the given credentials as (email,password).
     * The server adds the XSRF-TOKEN cookie which is then picked up by Play.
     */
    $scope.login = function (credentials) {
        if (AuthenticationService.login(credentials).then(function (currentUser) {
            if (currentUser) {
                $scope.user = currentUser;
            } else {
                $scope.error = "Please Check your login and password ";
                console.log("Error with login");
            }
        }));
    };

    /**
     * Invalidate the token on the server.
     */
    $scope.logout = function () {
        AuthenticationService.logout();
        $scope.user = undefined;
    };

    /**
     * Pings the server. When the request is not authorized, the $http interceptor should
     * log out the current user.
     */
    $scope.ping = function () {
        $http.get("/ping").then(function () {
            $scope.ok = true;
            $timeout(function () {
                $scope.ok = false;
            }, 3000);
        });
    };

    /** Subscribe to the logout event emitted by the $http interceptor. */
    $scope.$on("InvalidToken", function () {
        console.log("InvalidToken!");
        $scope.user = undefined;
    });

    $scope.$on("reloadAuthentication", function (event, action) {
        console.log("loginController on reloadAuthentication" + event);
        $scope.load();
    });

    $scope.user = false;
    $scope.load = function () {
        if (AuthenticationService.isLoggedInPromise().then(function (currentUser) {
            if (currentUser) {
                console.log("Home : currentUser:" + currentUser);
                $scope.user = currentUser
            } else {
                console.log("no logged user");
            }
        }));
    };
    $scope.load();

}

function HomeAdminCtrl($scope) {
    $("#menuAdmin").css("display", "block");
}