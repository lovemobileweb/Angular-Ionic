define([], function() {
	var controller = function($scope,$location,loginActivityService, Notification){

		window.debugLoginActivitiesScope = $scope;
		
		loginActivityService.listLogins().then(
			function success(response) {
				$scope.activities = response.data.response;
				console.log("success");
			}, function error() {
				console.log("error");
			}
		);
		
	};

	
	controller.$inject = ['$scope', '$location',

															//'$routeParams',
													'loginActivityService',
													'Notification'];
	
	return controller;
});