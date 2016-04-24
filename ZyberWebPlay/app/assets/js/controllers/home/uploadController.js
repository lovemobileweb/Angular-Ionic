/*jshint smarttabs:true */
define([], function() {
	var Controller = function($scope, path, partials, currentView, $mdDialog, uploadFactory) {
		$scope.partials=partials;
		$scope.currentView=currentView;

		$scope.flowObject = uploadFactory.flowObject();
		
		$scope.flowObject.off("uploadStart");

		$scope.hide = function() {
			$mdDialog.cancel('cancel');
		};
		$scope.cancel = function() {
			$mdDialog.hide();
		};
		$scope.answer = function(answer) {
		    $mdDialog.hide(answer);
		  };
		  
		$scope.open = function($event) {
				$event.preventDefault();
				$event.stopPropagation();
				$scope.opened = true;
		};
		
		$scope.uploadStarted = function(){
			console.log("Upload started event");
			$mdDialog.hide();
		};
		uploadFactory.setPath(path);
		
	};
	Controller.$inject = [ '$scope', 'path', 'partials','currentView','$mdDialog','uploadFactory'];

	return Controller;
});