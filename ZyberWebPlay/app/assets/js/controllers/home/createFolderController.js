// define([], function() {
// 	var Controller = function($scope, $uibModalInstance) {
// 		$scope.save = function(name) {
// 			$uibModalInstance.close(name);
// 		};

// 		$scope.cancel = function() {
// 			$uibModalInstance.dismiss('cancel');
// 		};
// 	};
// 	Controller.$inject = [ '$scope', '$uibModalInstance'];
// 	return Controller;
// });

define([], function() {
	var Controller = function($scope, $mdDialog,partials,currentView) {
		$scope.hide = function() {
		    $mdDialog.hide();
		  };
			//   $scope.save = function(name) {
			// 	$uibModalInstance.close(name);
			// };
		  $scope.cancel = function() {
		    $mdDialog.cancel();
		  };
		  $scope.answer = function(answer) {
		  	console.log(answer);
		    $mdDialog.hide(answer);
		  };
		  $scope.partials=partials;
		  $scope.currentView=currentView;
	};
	Controller.$inject = [ '$scope', '$mdDialog','partials','currentView'];
	return Controller;
});