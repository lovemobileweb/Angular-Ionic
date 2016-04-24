define([], function(){
	var Controller = function($scope, $http, Notification, 
			$routeParams, homeService, homeFactory,
			$mdDialog, $location){
		
		$scope.queryText = null;
		$scope.search = $routeParams.name;
		var path = $location.search().p;
		
		console.log("searchController");
		console.log($scope.searchText);
		console.log("Home factory: " + homeFactory.view());
		console.log($location.search());
		console.log($location.search().p);
		
		$scope.view = homeFactory.view();
		
		$scope.selectedItem = {};
		$scope.searchResult = [];
		
		
		function searchFiles(name,showHidden,hiddenOnly){
			if(!name || name.trim() === "") return [];
			console.log("!!!Searching " + name);
			homeService.searchFiles(name,path,homeFactory.view(),false,false, false).then(function(result){
				$scope.searchResult = result;
			});
		};
				
		$scope.searchFiles = function(name,showHidden,hiddenOnly){
			if(!name || name.trim() === "") return [];
			console.log("!!!Searching " + name);
			return homeService.searchFiles(name,path,homeFactory.view(),false,false);
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
//		    	$scope.selected = [];
		    });
		};
		
		$scope.gotoSearch = function(query){
			if(!query || query.trim() === "") return;
			console.log("doing search for: " + query);
			var autoChild = document.getElementById('searchText2').firstElementChild;
		    var el = angular.element(autoChild);
		    el.scope().$mdAutocompleteCtrl.hidden = true;
//		    el.scope().$mdAutocompleteCtrl.blur();

//			var mask = $(".md-scroll-mask");
//			console.log(mask);
//			mask.remove();
		    
		    var pathParam = path ? {p:path} : {};
	        $location.path(homeFactory.view() + "/search/" + query).search(pathParam);
		};
		
		searchFiles($routeParams.name, null, null);
	};
	Controller.$inject = ["$scope", "$http", "Notification", "$routeParams", 
	                      "homeService", "homeFactory", "$mdDialog", "$location"];
	return Controller;
});