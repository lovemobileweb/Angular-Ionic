define([], function() {
	var Controller = function($scope, $mdDialog, $http, foldersUrl){
		$scope.folderData = [];
		
		$scope.treeOptions = {
			    dirSelectable: true,
			    multiSelection:false,
			    allowDeselect: false
		};
		
		$scope.expandedNodes = [];
		
		function loadDirectoryStructure(){
			$http.get(foldersUrl).success(function(data){
				$scope.folderData = data.response;
				if($scope.folderData){
					$scope.expandedNodes = [$scope.folderData[0]];
				}else{
					$scope.expandedNodes = [];
				}
			}).error(function(data){
				console.error(data);
			});
		}
		
		$scope.selected = null;
		$scope.setSelected = function(node){
			$scope.selected = node;
		};
		
		$scope.accept = function(){
			$mdDialog.hide($scope.selected);
		};
		
		$scope.cancel = function(){
			$mdDialog.cancel(null);
		};
		
		loadDirectoryStructure();
		
	};
	Controller.$inject = ['$scope', '$mdDialog', '$http', 'foldersUrl'];
	return Controller;
});