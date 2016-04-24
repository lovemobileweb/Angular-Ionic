define([], function(){
	var Controller = function($scope, $mdDialog, $http, Notification, path,$log){
		$scope.principals = [];
		$scope.newPrincipal = {};
		$scope.permissionSets = [];
		$scope.permissionsFor = {};
		$scope.selectedPrincipal = null;
		$scope.emailsList = [];
		
		function loadPrincipals(selectedPrinc){
			$http.get("/api/security/permissions/" + path.uuid).success(function(data){
				$scope.principals = data.response;
				
				console.log("Size: " + data.length);
				var toSelect = _.find($scope.principals, function(princ){
					return selectedPrinc && selectedPrinc.principalId === princ.principalId;
				});
				if(toSelect){
					$scope.selectedPrincipal = toSelect;
				}else if($scope.principals.length > 0){
					$scope.selectedPrincipal = $scope.principals[0];
				}else{
					$scope.selectedPrincipal = null;
				}
			}).error(function(data /*, status, header, config*/) {
				Notification.error(data.firstError.userMessage);
				console.log("Error: ");
				console.log( data.errors );
			});
		}
		$scope.getPrincipals = function(val) {
		   return $http.get('/api/query/principals', {
		     params: {
		       name: val
		     }
		   }).then(function(response){
			   var result = response.data.response.map(function(item){
				     return item.name;
				   });
			   $scope.emailsList=result;
		       return result;
		   });
		 };
		$scope.addPrincipal = function(principal){
			if(principal){
				$http.post("/api/security/permissions/" + path.uuid + "/principal",  
						{principal_name : principal, set_recursively: $scope.shareModel.recursively} ).success(function(data){
							loadPrincipals(data.response);
							$scope.newPrincipal = {};
							Notification.success(data.message);
	
							$scope.selectedItem = null;
							$scope.searchText = '';
						}).error(function(data /*, status, header, config*/) {
							Notification.error(data.firstError.userMessage);
							console.log("Error: ");
							console.log( data.errors );
						});
			}
		};
		
		
		$scope.setSelected = function(principal, permissionName){
			$scope.selectedPrincipal = principal;
			console.log("Selected permission: " + permissionName);
			$scope.updatePermissions(principal, permissionName);
		};
		
		$scope.updatePermissions = function(principal, permissionName){
			if(!permissionName){ return; }
			if(principal.permissions.name && principal.permissions.name === permissionName) return;
			var selected = _.find($scope.permissionSets, function(e){
				return e.name === permissionName;
			});
			if(!selected) return;
			
			$http.put("/api/security/permissions/" + 
					path.uuid + "/principal/"+ principal.principalId,
					_.extend(selected, {set_recursively: $scope.shareModel.recursively})).success(function(data){
						Notification.success(data.response);
					}).error(function(data) {
						Notification.error(data.firstError.userMessage);
						console.log("Error: ");
						console.log( data.errors );
					});
		};
		
		$scope.removePrincipal = function(principal){
			$http.delete("/api/security/permissions/" + 
					path.uuid + "/principal/"+ principal.principalId,
					{params: {set_recursively:$scope.shareModel.recursively}}).success(function(data){
						Notification.success(data.response);
						$scope.selectedPrincipal = null;
						loadPrincipals(null);
					}).error(function(data) {
						Notification.error(data.firstError.userMessage);
						console.log("Error: ");
						console.log( data.errors );
					});
		};
		
		function loadPermissionSets(){
			$http.get("/api/security/permissions").success(function(data){
				$scope.permissionSets = data.response;
			}).error(function(data) {
				Notification.error(data.firstError.userMessage);
				console.log("Error: ");
				console.log( data.errors );
			});
		}
		
		loadPrincipals(null);
		loadPermissionSets();

	    // list of `state` value/display objects
	    $scope.querySearch   = querySearch;
	    $scope.selectedItemChange = selectedItemChange;
	    $scope.searchTextChange   = searchTextChange;
	    // ******************************
	    // Internal methods
	    // ******************************
	    /**
	     * Search for states... use $timeout to simulate
	     * remote dataservice call.
	     */
	    function querySearch (query) {
	    	$scope.getPrincipals(query);

	      var results = query ? $scope.emailsList.filter( createFilterFor(query) ) : $scope.emailsList,
	          deferred;
	      if ($scope.simulateQuery) {
	        deferred = $q.defer();
	        $timeout(function () { deferred.resolve( results ); }, Math.random() * 1000, false);
	        return deferred.promise;
	      } else {
	      	var emailListVar = [];

	      	angular.forEach(results, function(email) {
	      	  	emailListVar.push({"email" : email})
	      	});
	      	console.log('emailListVar',emailListVar);
	        return emailListVar;
	      }
	    }
	    function searchTextChange(text) {
	      $log.info('Text changed to ' + text);
	      $scope.getPrincipals(text);
	    }
	    function selectedItemChange(item) {
		  $log.info('Item changed to ' + JSON.stringify(item));
		  if(item)
			  $scope.newPrincipal.value = item.email;
	    }
	    /**
	     * Create filter function for a query string
	     */
	    function createFilterFor(query) {
	      var lowercaseQuery = angular.lowercase(query);
	      return function filterFn(emailsList) {
	      	console.log(emailsList.indexOf(lowercaseQuery) === 0);
	        return (emailsList.indexOf(lowercaseQuery) === 0);
	      };
	    }
	    $scope.done = function(/*selectedPrincipal, permissionsFor*/){
//	    	$scope.updatePermissions(selectedPrincipal, permissionsFor);
	    	$mdDialog.hide();
	    };
	    $scope.cancel = function(){
	    	$mdDialog.cancel();
	    };
	};
	Controller.$inject = ["$scope", "$mdDialog", "$http", "Notification", "path","$log"];
	return Controller;
});