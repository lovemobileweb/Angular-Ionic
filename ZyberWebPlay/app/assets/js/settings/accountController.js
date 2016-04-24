define([], function() {
	var controller = function($scope, usersService, 
               Notification, $translate, $uibModal, $window, $timeout,$mdDialog){
		$scope.user = {};
		$scope.languages = [];

		var $mobile = $("#mobile");
		$mobile.intlTelInput({
		});
 
		$scope.save = function(model){
			console.log("Model: ");
			console.log(model);
			model.language = $scope.user.selectedLanguage.cod;
			model.number = $mobile.intlTelInput("getNumber",intlTelInputUtils.numberFormat.NATIONAL);
			model.countryCode = $mobile.intlTelInput("getSelectedCountryData").dialCode;

			usersService.saveAccount(model).success(function(data){
				Notification.success(data.response);
				$timeout(function(){
					window.location = "/settings";
				}, 2000);
			}).error(function(data, status, header, config) {
				Notification.error(data.firstError.userMessage);
				console.log("Error: ");
				console.log( data.errors );
			});
		};
		
		function loadAccount(){
			usersService.currentUser().success(function(data){
				$scope.user = data.response;
				console.log("UserData");
				console.log(data.response);
				$scope.user.selectedLanguage = {cod:$scope.user.language, value:""};
				if($scope.user.countryCode == "") {
					$.get('http://ipinfo.io', function() {}, "jsonp").always(function(resp) {
						var countryCode = (resp && resp.country) ? resp.country : "";
						$mobile.intlTelInput("setCountry",countryCode);
					});
				}
			}).error(function(data, status, header, config){
				Notification.error(data.firstError.userMessage);
				console.log(data.errors);
			});
		}
		function loadLanguages(){
			usersService.getLanguages().success(function(data){
				$scope.languages = data.response;

				$timeout(function() {
					var found = _.find($scope.languages, function(language) {
						return language.cod == $scope.user.selectedLanguage.cod;
					});
					if(found) {
						console.log("found",found);
						$scope.user.selectedLanguage = found;
					}
				},200);
				
				console.log("languages");
				console.log(data.response);
			}).error(function(data, status, header, config){
				Notification.error(data.firstError.userMessage);
				console.log(data.errors);
			});
		}
		
		$scope.changePassword = function(){
			var editController = function($scope, $mdDialog){
				$scope.model = {};
				$scope.save = function(model){
					console.log("Model: ");
					console.log(model);
					usersService.changeUserPassword(model).success(function(data){
						Notification.success(data);
						$mdDialog.cancel();
					}).error(function(data, status, header, config) {
						Notification.error(''+ data);
						console.log("Error: ");
						console.log( data );
					});
//					$mdDialog.close();
				};
				$scope.cancel = function() {
					$mdDialog.cancel('cancel');
				};
			};
			editController.$inject = ['$scope', '$mdDialog'];
			
			var modalInstance = $mdDialog.show({
				templateUrl : 'changePassword.html',
				controller : editController
			});
		};
		
		loadAccount();
		loadLanguages();
	};
	
	controller.$inject = ['$scope', 'usersService', 'Notification', 
                          '$translate', '$uibModal', '$window', '$timeout','$mdDialog'];
	
	return controller;
});