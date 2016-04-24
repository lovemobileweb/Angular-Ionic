/*global define */

define(['./usersController', 
        './activityController',
        './loginActivityController',
        './policyController',
        './termStoreController',
        '../controllers/sidebarController',
        '../controllers/headerController'],
        function(usersController, activityController, 
        		loginActivityController,policyController,
				 termStoreController,
        		sidebarController,
        		headerController) {

	/* Controlers */

	var controllers = {};

	controllers.usersController = usersController;
	controllers.sidebarController = sidebarController;
	controllers.headerController = headerController;
	controllers.activityController = activityController;
	controllers.loginActivityController = loginActivityController;
	controllers.termStoreController = termStoreController;
	controllers.policyController = policyController;

	controllers.adminController = function($stateParams){
	};
	
	controllers.activityController = activityController;
	controllers.loginActivityController = loginActivityController;
	
	controllers.detailsController = function($scope, $stateParams, 
             usersService, Notification,$mdDialog){
		$scope.user = {};
		
		function getUser(uuid){
			usersService.getUser(uuid).success(function(data){
				$scope.user = data.response;
			}).error(function(data, status, header, config){
				Notification.error(data.firstError.userMessage);
				console.error("Error retrieving user: ");
				console.error(data.errors);
			});
		}
		
		getUser($stateParams.userId);
	};
		
	controllers.groupsController = function($scope, groupsService, $uibModal, Notification,$mdDialog){
		$scope.groups = [];
		
		function loadUserGroups(){
			groupsService.getGroups().success(function(data){
				$scope.groups = data.response;
			}).error(function(data, status, header, config){
				console.error("Error loading groups");
				console.error(data.errors);
			});
		}
		
		$scope.createGroup = function(){
			var controller = function($scope, $mdDialog){
				$scope.cancel = function() {
					$mdDialog.cancel('cancel');
				};
				
				$scope.save = function(name){
					groupsService.createGroup(name).success(function(data){
						Notification.success(data.response);
						$mdDialog.cancel('created');
						loadUserGroups();
					}).error(function(data, status, header, config){
						console.error("Error creating group");
						console.error(data.errors);
						Notification.error(data.firstError.userMessages);
					});
				};
			};
			
			controller.$inject = ['$scope', '$mdDialog']; 
			
			var modalInstance = $mdDialog.show({
				templateUrl : 'createGroup.html',
				controller : controller
			});
		};
	
		loadUserGroups();
	};
	
	controllers.groupMembersController = function($scope, groupsService, 
         $uibModal, Notification, $stateParams, $state, $mdDialog){
		
		$scope.group = {};
		$scope.members = {};
		
		function loadGroup(groupId){
			groupsService.getGroup(groupId).success(function(data){
				$scope.group = data.response;
			}).error(function(data, status, header, config){
				console.error("Error retrieving group");
				console.error(data.errors);
			});
		}
		
		function loadMembers(groupId){
			groupsService.getGroupMembers(groupId).success(function(data){
				$scope.members = data.response;
			}).error(function(data, status, header, config){
				console.error("Error retrieving groupMembers");
				console.error(data.errors);
			});
		}
		
		$scope.showAddMembers = function(){
			var group = $scope.group;
			var controller = function($scope, $mdDialog){				
				$scope.saveMembers = function(emails){
					$scope.error = "";
					$scope.info="Saving";
					groupsService.addMembersToGroup(group.uuid,
							emails).success(function(data){
                       Notification.success(data.response);
                       loadMembers(group.uuid);
                       $mdDialog.hide(true);
					}).error(function(data, status, header, config){
						console.error("Error adding members");
						console.error(data.errors);
						$scope.info ="";
						$scope.error = data.firstError.userMessage;
					});
				};
				$scope.emails = [];
				$scope.selected = {};
				
				$scope.selectedItemChange = function(item){
					console.log("selectedItemChange");
					console.log(item);
					$scope.selected = item;
				};
				
				$scope.addEmail = function(item){
					console.log("addEmail");
					console.log(item);
					if(!_.contains($scope.emails, item.email)){
						$scope.emails.push(item.email);
					}
					$scope.searchText = '';
				};
				
				$scope.removeEmail = function(email){
					console.log("Removing email");
					$scope.emails = _.without($scope.emails, email);
				};
				
				$scope.searchText = '';
				$scope.users = [];
				
				$scope.querySearch = function(searchText){
					groupsService.getUsersLike(searchText).success(function(data){
						$scope.users = data.response;
					}).error(function(data){
						console.error("Error getting users like name");
						console.error(data);
						$scope.info = "";
						$scope.error = data.firstError.userMessage;
					});
				};
				
				$scope.cancel = function() {
					$mdDialog.cancel('cancel');
				};
			};
			
			controller.$inject = ['$scope', '$mdDialog'];
			
			var modalInstance = $mdDialog.show({
				templateUrl : 'addMembers.html',
				controller : controller
			});
		};
		
		$scope.deleteGroup = function(){
			groupsService.removeGroup($scope.group.uuid).success(function(data){
                Notification.success(data.response);
                $state.go("admin.groups.list");
			}).error(function(data, status, header, config){
				console.error("Error removing group");
				console.error(data.errors);
			});
		};
		
		$scope.removeMember = function(member){
			var groupId = $scope.group.uuid
			groupsService.removeMember(groupId, member.memberId).success(function(data){
                Notification.success(data.response);
                loadMembers(groupId);
			}).error(function(data, status, header, config){
				console.error("Error removing member");
				console.error(data);
			});
		};
		
		$scope.editGroup = function(name){
			var group = $scope.group;
			var controller = function($scope, $mdDialog){
				$scope.groupName = name;
				$scope.cancel = function() {
					$mdDialog.cancel('cancel');
				};
				
				$scope.save = function(name){
					groupsService.updateGroup(group.uuid, name).success(function(data){
						loadGroup(group.uuid);
						Notification.success(data.response);
						$mdDialog.cancel('created');
					}).error(function(data, status, header, config){
						console.error("Error creating group");
						console.error(data);
						Notification.error(data.firstError.userMessage);
					});
				};
			};
			
			controller.$inject = ['$scope', '$mdDialog']; 
			
			var modalInstance = $mdDialog.show({
				templateUrl : 'createGroup.html',
				controller : controller
			});
		};
		
		loadGroup($stateParams.groupId);
		loadMembers($stateParams.groupId);
	};
	
	controllers.permissionsController = function($scope, $http){
		$scope.permissionSet = [];
		
		function loadPermissionSets(){
			$http.get("/api/security/permissions").success(function(data){
				$scope.permissionSet = data.response;
			}).error(function(data) {
				Notification.error(data.firstError.userMessage);
				console.log("Error: ");
				console.log( data.errors );
			});
		}
		loadPermissionSets();
	};
	
	controllers.groupsController.$inject = ['$scope', 'groupsService', '$uibModal', 'Notification','$mdDialog'];
	
	controllers.groupMembersController.$inject = ['$scope', 'groupsService', '$uibModal', 
                                                  'Notification', '$stateParams', '$state','$mdDialog'];
	
	controllers.adminController.$inject = ['$stateParams', '$scope', 'usersService', 'Notification', 
                                           '$uibModal','$mdDialog'];
	
	controllers.detailsController.$inject = ['$scope', '$stateParams', 'usersService', 
                                             'Notification'];
	
	controllers.permissionsController.$inject = ['$scope', '$http'];

	return controllers;
});