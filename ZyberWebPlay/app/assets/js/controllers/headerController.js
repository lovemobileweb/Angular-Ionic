define([], function() {
	var Controller = function(
			$scope,$rootScope,$location,$timeout,
			homeService,homeFactory,$mdDialog,
			$location) {
		
		$scope.location = $location;
		$scope.searchedFiles = [];
		
		$scope.searchFiles = function(name,showHidden,hiddenOnly){
			if(!name || name.trim() === "") return [];
			console.log("Searching for in header controller " + name);
			console.log(homeFactory.path());
			var searchFiles = homeService.searchFiles(name,homeFactory.path(),homeFactory.view(),$scope.$parent.showHidden,false);
			return searchFiles;
		};
		$scope.extend = function  () {
	        $('body').toggleClass('extended');
	        $('.sidebar').toggleClass('ps-container');	
	        $rootScope.$broadcast('resize');
		};
		$scope.addSearch = function(){
			$('#header').toggleClass('nav-search-box');
		}
		$scope.gotoSearch = function(query){
			if(!query || query.trim() === "") return;
			console.log("doing search for: " + query);
			var autoChild = document.getElementById('searchText').firstElementChild;
		    var el = angular.element(autoChild);
		    el.scope().$mdAutocompleteCtrl.hidden = true;
		    
//			var mask = $(".md-scroll-mask");
//			console.log(mask);
//			mask.remove();
		    var pathParam = homeFactory.path() ? {p:homeFactory.path()} : {};
	        $location.path(homeFactory.view() + "/search/" + query).search(pathParam);
		};
		$scope.showFile = function(file,ev){
//			$scope.file = file;
			
			var modalInstance = $mdDialog.show({
				templateUrl : 'showFile.html',
				controller : 'showFileController',
				parent: angular.element(document.body),
				targetEvent: ev,
				clickOutsideToClose:true,
				size : "lg",
				locals : {
					file : file
                }
			}).finally(function(){
//		   $scope.selected = [];
		    });
		};
	}
	Controller.$inject = [ '$scope',
							'$rootScope',
	                       	'$location',
	                       	'$timeout',
	                       	'homeService',
	                       	'homeFactory',
	                       	'$mdDialog',
	                       	'$location'
	                      ];
	return Controller;
});