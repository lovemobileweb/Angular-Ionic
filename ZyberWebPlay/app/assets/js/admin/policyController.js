define([], function() {
	var controller = function($scope,$location, Notification,$http){
		window.debugPolicyScope = $scope;

		$scope.policy = {};
		$http.get("/api/admin/policy").then(
			function success(response) {
				$scope.policy = response.data.response;
				console.log("success");
			}, function error() {
				console.log("error");
			}
		);

		$scope.pushPolicy = function(){
			console.log("Debug"); 
			$http.post("/api/admin/policy",$scope.policy).then(
				function success(data) {
					Notification.success("Updated password policy");
					console.log("success updating");
				}, function error() {
					Notification.error(data);
					console.log("error updating");
				});
		};

	};

	controller.$inject = ['$scope', '$location',

															//'$routeParams',
													'Notification',
													'$http'];
	
	return controller;
});