/*jshint smarttabs:true */
define([], function() {
  var Controller = function($scope, $routeParams, $location, 
            versionService, Notification, $uibModal, homeService,$mdDialog) {
    window.debugScope = $scope;


    var currLocation = $location.path();

    var path = "";

    if(typeof $routeParams.name !== "undefined"){
      path = $routeParams.name;
    }
    
    console.log("Path: " + path);

    $scope.formData = {
      restorem : undefined
    };
    
    $scope.partials = [];
    
    function loadPartials(path){
		homeService.getBreadcrumb(path).success(function(data){
			var l = data.response.length;
			console.log("Lenght: " + l);
			$scope.partials = data.response.splice(0, l - 1);
		});
	}
    
    $scope.currentView = function(){
    	var view = currLocation
    				.split('/')
    				.filter(isNotEmpty)[0]; //home or shares
    	console.log("View: " + view);
    	
    	return view;
    }
    
    var isNotEmpty = function(str){
		return (str && str.trim() !== "");
	};

    function loadVersions(p){
      versionService.getVersions(p).success(function(data){
        $scope.versions = data.response;
        $scope.name = $scope.versions[0].name;

        if($scope.versions.length > 1) {
          $scope.pathId = $scope.versions[1].pathId;
          $scope.formData.restorem = $scope.versions[1].version;
        }
      }).error(function(data, status, header, config) {
        Notification.error(data.firstError.userMessage);
        console.log("Error: ");
        console.log( data.errors );
      });
    }

    $scope.restore = function() {
      versionService.restoreVersion($scope.pathId, $scope.formData.restorem)
        .success(function (data, status, header, config) {
          Notification.success(data.message);
          $scope.versions = data.response;
          $scope.formData.restorem = $scope.versions[1].version;
        }).error(function (data, status, header, config) {
        Notification.error(data.firstError.userMessage);
        console.log("Error: ");
        console.log(data.errors);
      });
    };
   $scope.cancel = function(){
    $location.path($scope.partials[$scope.partials.length-1].path);
   };
   
	$scope.showFile = function(file){
		var showFileController = function($scope, $mdDialog){
			$scope.file = file;
			
			$scope.url = "/download/version/" + file.pathId + "?version=" + file.version;

			$scope.cancel = function() {
        $mdDialog.cancel('cancel');
				// $uibModalInstance.dismiss('cancel');
			};
		};
		showFileController.$inject = ['$scope', '$mdDialog'];
		
		var modalInstance = $mdDialog.show({
			templateUrl : 'showFile.html',
			controller : showFileController
		});
	};

	loadPartials(path);
    loadVersions(path);
  };
  Controller.$inject = [ '$scope',
    '$routeParams',
    '$location',
    'versionService',
    'Notification',
    '$uibModal',
    'homeService',
    '$mdDialog'];
  return Controller;
});