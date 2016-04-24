define([], function() {
	var controller = function($scope,$location,activityService, Notification){

		$scope.path = "";
		$scope.parentPath = "";
		$scope.mode = "file";

		window.debugActivitiesScope = $scope;

		$scope.partialPaths = function() {
			var partialPaths =
					[{
						name:"Home",
						path:""
					}];

			var frag = $scope.path.split('/');
			var len = frag.length;
			for(var i = 1; i < len; i++){
				var partial = {
					name: frag[i],
					path: partialPaths[i-1].path + "/" + window.encodeURIComponent(frag[i])
				};
				partialPaths.push(partial);
			}
			partialPaths[0].path="home";
			return partialPaths;
		};

		//TODO maybe this shouldn't be a function
		$scope.partials = $scope.partialPaths();

		var update = function() {
			activityService.listFiles($scope.path,$scope.mode == "time",$scope.$parent.showHidden).then(
					function success(response) {
						console.log("Response");
						console.log(response);
						$scope.files = response.data.response.paths;
						$scope.activities = response.data.response.activities;
						console.log("Paths are " + JSON.stringify($scope.files));
						console.log("success");
					}, function error() {
						console.log("error");
					}
			);
		};

		$scope.filterPath = function(toAppend) {
			$scope.parentPath = $scope.path;
			$scope.path=$scope.path+"/"+toAppend;
			update();
		};

		$scope.reset = function() {
			update();
		};

		update();
	};
	
	controller.$inject = ['$scope', '$location', 'activityService','Notification'];
	
	return controller;
});