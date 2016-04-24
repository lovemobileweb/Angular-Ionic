define([], function() {
	var controller = function($scope, $uibModal, 
			$window, usersService, Notification, $stateParams,$mdDialog){
		
		$scope.users = [];
		
		function loadUsers(){
			usersService.getUsers().success(function(data){
				$scope.users = data.response;
			}).error(function(data, status, header, config) {
				Notification.error(data.firstError.userMessage);
				console.log("Error: ");
				console.log( data.errors );
			});
		}
		
		$scope.createAccount = function(){
			var modalInstance = $mdDialog.show({
				templateUrl : 'editUser.html',
				controller : function($scope, $mdDialog){
					$scope.isEdit = false;
					$scope.model = {};
					$scope.languages = [];
					$scope.roles = [];
					$scope.save = function(model){
						model.language = model.selectedLanguage.cod;
						model.userRole = model.selectedRole.uuid;
						usersService.createUser(model).success(function(data){
							Notification.success(data.response);
							loadUsers();
						}).error(function(data, status, header, config) {
							Notification.error(data.firstError.userMessage);
							console.log("Error: ");
							console.log( data.errors );
						});
						$mdDialog.cancel();
					};
					
					$scope.cancel = function() {
						$mdDialog.cancel('cancel');
					};
					function loadLanguages(){
						usersService.getLanguages().success(function(data){
							$scope.languages = data.response;
							console.log("languages");
							console.log(data.response);
						}).error(function(data, status, header, config){
							Notification.error(data.firstError.userMessage);
						});
					}
					
					function loadUserRoles(){
						usersService.getUserRoles().success(function(data){
							$scope.roles = data.response;
							console.log("languages");
							console.log(data.response);
						}).error(function(data, status, header, config){
							Notification.error(data.firstError.userMessage);
						});
					}
					loadUserRoles();
					loadLanguages();
				}
			});
		};
		
		$scope.editAccount = function(user){
			user.selectedLanguage = {cod:user.language, value: ""};
			console.log("User role");
			console.log(user.userRole);
			user.selectedRole = {uuid:user.userRole, name: ""}
			var modalInstance = $mdDialog.show({
				templateUrl : 'editUser.html',
				controller : function($scope, $mdDialog){
					$scope.isEdit = true;
					$scope.model = user;
					$scope.languages = [];
					$scope.roles = [];
					$scope.save = function(model){
						model.language = model.selectedLanguage.cod;
						if(model.isResetPassword){
							model.resetPassword = model.password;
						}
						console.log("Selected role");
						console.log(model.selectedRole.uuid);
						model.userRole = model.selectedRole.uuid;
						console.log()
						console.log("Model: ");
						console.log(model);
						usersService.updateUser(model).success(function(data){
							loadUsers();
							Notification.success(data.response);
						}).error(function(data, status, header, config) {
							Notification.error(data.firstError.userMessage);
							console.log("Error: ");
							console.log( data.errors );
						});
						$mdDialog.cancel();
					};
					$scope.cancel = function() {
						$mdDialog.cancel('cancel');
					};
					
					$scope.delete = function(uuid){
						usersService.deleteUser(uuid).success(function(data){
							loadUsers();
							Notification.success(data.response);
						}).error(function(data, status, header, config) {
							Notification.error(data.firstError.userMessage);
							console.log("Error: ");
							console.log( data.errors );
						});
						$mdDialog.cancel();
					};
					function loadLanguages(){
						usersService.getLanguages().success(function(data){
							$scope.languages = data.response;
							var found = _.find($scope.languages, function(language) {
								return language.cod == user.selectedLanguage.cod;
							});
							if(found) {
								user.selectedLanguage = found;
							}
							console.log("languages");
							console.log(data);
						}).error(function(data, status, header, config){
							Notification.error(data.firstError.userMessage);
						});
					}
					
					$scope.clearPasswords = function(){
						$scope.model.password = "";
						$scope.model.confirm = "";
					};
					
					function loadUserRoles(){
						usersService.getUserRoles().success(function(data){
							$scope.roles = data.response;
							var found = _.find($scope.roles, function(role) {
								return role.uuid == user.selectedRole.uuid;
							});
							if(found) {
								user.selectedRole = found;
							}
							console.log("User roles");
							console.log(data.response);
						}).error(function(data, status, header, config){
							Notification.error(data.firstError.userMessage);
						});
					}
					
					loadUserRoles();
					loadLanguages();
				}
			});
		};
		
		loadUsers();
	};
	
	controller.$inject = ['$scope', '$uibModal', 
                          '$window', 'usersService', 'Notification', 
                          '$stateParams','$mdDialog'];
	
	return controller;
});