define([], function() {
	var controller = function($scope, homeService, Notification, 
           $uibModal, metadataService,$mdDialog){
		$scope.termStore = [];
		
		function loadTermStore(){
			homeService.getTermStore().success(function(data){
				$scope.termStore = data;
			}).error(function(data, status, header, config){
				Notification.error(data);
				console.error("Error retrieving user: ");
				console.error(data);
			});
		}
		
		$scope.newTerm = function(){
			var onSave = function(model){
				return metadataService.addTermstore(model);
			};
			
			openEditModal({}, [], onSave);
		};
		
		$scope.editTerm = function(termStore){
			
			var onSave = function(model){
				return metadataService.updateTermstore(termStore.uuid, model);
			};
			if(!termStore.allowCustomTerms){
				homeService.getTerms(termStore.name).success(function(data){
					var elements = _.map(data, function(e){
						return e.name;
					});
					openEditModal(termStore, elements, onSave);
				}).error(function(data, status, header, config){
					Notification.error(data);
					console.error("Error termStore terms");
					console.error(data);
				});
			}else{
				openEditModal(termStore, [], onSave);
			}
		};
		
		function openEditModal(model, elements, onSave){
			var controller = function($scope, $mdDialog){
				$scope.elements = elements;
				$scope.model = model;
				
				$scope.addElement = function(element){
					if(element && !_.contains($scope.elements, element)){
						$scope.elements.push(element);
					}
					$scope.model.newTerm = "";
				};
				
				$scope.remove = function(element){
					$scope.elements = _.without($scope.elements, element);
				};
				
				$scope.cancel = function() {
					$mdDialog.cancel('cancel');
				};
				
				$scope.save = function(model){
					
					if(model.allowCustomTerms === undefined)
						model.allowCustomTerms = false;
										
					var ob = {termStore: model, terms: $scope.elements};
					
					onSave(ob).success(function(data){
						Notification.success(data.response);
						console.log(data);
						$mdDialog.cancel('cancel');
						loadTermStore();
					}).error(function(data, status, header, config){
						Notification.error(data.firstError.userMessage);
						console.error("Error adding termStore");
						console.error(data);
					});
				};
				
				$scope.elementsCheck = function(){
					return !($scope.model.allowCustomTerms) &&
						$scope.elements.length === 0;
				};
				
				$scope.delete = function(termstore){
					metadataService.deleteTermstore(termstore.uuid).success(function(data){
						Notification.success(data.response);
						loadTermStore();
						$mdDialog.cancel('cancel');
					}).error(function(data, status, header, config){
						Notification.error(data.firstError.userMessage);
						console.error("Error deleting termstore: ");
						console.error(data);
					});
				};
			};
			controller.$inject = ['$scope', '$mdDialog'];
			
			var modalInstance = $mdDialog.show({
				templateUrl : 'editTermStore.html',
				controller : controller
			});
		}
		
		
		loadTermStore();
	};
	
	controller.$inject = ['$scope', 'homeService', 'Notification', 
                                               '$uibModal', 'metadataService','$mdDialog'];
	
	return controller;
});