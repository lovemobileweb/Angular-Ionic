define([], function() {
	var Controller = function($scope, $controller,$mdDialog, $http, 
			Notification, homeFactory, file, event, parentScope, onRefresh) {
	
		angular.extend(this, $controller('securityController', 
				{$scope: $scope, path: file, $mdDialog: $mdDialog}));
		//Suppress IDEA warnings with dummy assignments
		//noinspection SillyAssignmentJS
		$scope.file = file;
		file.sharing = file.sharing;
		//noinspection SillyAssignmentJS
		file.shares = file.shares;
		
		$scope.folderShared = homeFactory.view() === "shares";

		window.debugShareFScope = $scope;
		window.debugShareFScope.file = file;
		

		$scope.shareModel = {
				folderName : file.name,
				shared : $scope.folderShared,
				recursively: true
		};
		
		$scope.toggleShare = function(folderName){
			if(!$scope.folderShared){
				console.log("Sharing folder");
				share(folderName);
			}else if(homeFactory.view() === "shares"){
				console.log("Unsharing folder to selected path");
				showHomeStructure();
			}else{
				console.log("Unsharing folder to home");
				unShare({});
			}
		};
		
		function share(folderName) {
			$scope.error = "";
//			$scope.info="Saving";
			
			$http.post("/api/makeshare", {path: file.uuid, folderName: folderName}).success(function(data){
				Notification.success(data.response);
				$scope.folderShared = true;
				onRefresh();
			}).error(function(rs){
				$scope.shareModel.shared = false;
				$scope.info ="";
				$scope.error = rs.firstError.userMessage;
				console.error(rs.errors);
			});
		};
		
		function unShare(parent_path) {
			$scope.error = "";
//			$scope.info="Saving";
			return $http.post("/api/unshare", {src_path: file.uuid, parent_path: parent_path.uuid}).success(function(data){
				Notification.success(data.response);
				$scope.folderShared = false;
				onRefresh();
			}).error(function(rs){
				$scope.shareModel.shared = true;
				$scope.info ="";
				$scope.error = rs.firstError.userMessage;
				console.log(rs.errors);
			});
		};
		
		function showHomeStructure(){
			var controller = function($innerScope){
				$innerScope.folderData = [];
				
				$innerScope.treeOptions = {
//						nodeChildren: "children"
					    dirSelectable: true,
					    multiSelection:false,
					    allowDeselect: false
				};
				
				$innerScope.expandedNodes = [];
				
				function loadHomeStructure(){
					$http.get('/api/hometree').success(function(data){
						$innerScope.folderData = data.response;
						if($innerScope.folderData){
							$innerScope.expandedNodes = [$innerScope.folderData[0]];
						}else{
							$innerScope.expandedNodes = [];
						}
					}).error(function(data){
						console.error(data);
					});
				}
				$innerScope.selected = null;
				$innerScope.setSelected = function(node){
					$innerScope.selected = node;
				};
				
				$innerScope.accept = function(){
					if($innerScope.selected){
						unShare($innerScope.selected).success(function(data){
							$mdDialog.cancel(true);
						});
					}
				};
				
				$innerScope.cancel = function(){
					$mdDialog.cancel();
				};
				loadHomeStructure();
			};
			
			controller.$inject = ['$scope'];
			
			$mdDialog.show({
			      controller: controller,
			      templateUrl: 'selectFolder.html',
			      parent: angular.element(document.body),
//			      targetEvent: ev,
			      clickOutsideToClose:false
			    })
			    .then(function(selected) {
//			    	console.log("Selected");
//			    	console.log(selected);
			    }, function(unshared) {
			    	console.log("Selection cancelled: " + unshared);
			    	if(!unshared){
			    		parentScope.openFolderSharing(file, event);
			    	}
			    });
		}

		
		$scope.done = function(){
			$mdDialog.cancel();
		};
		$scope.cancel = function(){
			$mdDialog.cancel();
		};
		
	};
	Controller.$inject = [ '$scope', '$controller', '$mdDialog','$http',
	                       'Notification', 'homeFactory', 'file', 'event', 
	                       'parentScope', 'onRefresh'];
	return Controller;
});