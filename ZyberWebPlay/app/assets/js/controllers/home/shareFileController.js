define([], function() {
	var Controller = function($scope, $mdDialog,$http,file) {
		//Suppress IDEA warnings with dummy assignments
		//noinspection SillyAssignmentJS
		file.sharing = file.sharing;
		//noinspection SillyAssignmentJS
		file.shareId = file.shareId;
		//noinspection SillyAssignmentJS
		file.shares = file.shares;

		window.debugShareScope = $scope;
		window.debugShareScope.file = file;

		console.log("File is " + JSON.stringify(file));
		function usersToString(theUsers) {
			return _.map(theUsers, function (i) {
				return i.userEmail;
			}).join();
		}
		$scope.shareForm = {
			password:"",
			shareType : file.sharing,
			users : usersToString(file.shares)
		};
		if($scope.shareForm.shareType === "") {
			$scope.shareForm.shareType = "public";
		}

		$scope.share = function() {
			$scope.error = "";
			$scope.info="Saving";
			$http({
				method: 'POST',
				url: "/api/sharing/file/" + file.uuid,
				data: $scope.shareForm
			})
			.then(function success(response) {
				$scope.info ="";
				for(var k in response.data) { //noinspection JSUnfilteredForInLoop
					file[k]=response.data[k];
				}
				$mdDialog.cancel($scope.shareForm.shareType != "revoke");
			}, function failure(response) {
				$scope.info ="";
				$scope.error = response.statusText;
			});
		};

		$scope.validate = function(){
			var currentAccess = $scope.shareForm.shareType;
			if(file.sharing == "public" && currentAccess == "public") return false;
			if(file.sharing == "users" && currentAccess == "users") return $scope.shareForm.users && usersToString(file.shares) != $scope.shareForm.users;

			if(currentAccess == "password") {
				return $scope.shareForm.password;
			}
			if(currentAccess == "users") {
				return $scope.shareForm.users;
			}
			return true;
		};

		$scope.save = function(name) {
			$mdDialog.cancel(name);
		};

		$scope.cancel = function() {
			$mdDialog.cancel('cancel');
		};
		
		$scope.open = function($event) {
				$event.preventDefault();
				$event.stopPropagation();
				$scope.opened = true;
		};

		$scope.fileLink = function() {
			if($scope.shareForm.shareType == "public") {
				return location.origin + "/public/download/" + file.shareId;
			}
			if($scope.shareForm.shareType == "password") {
				return location.origin + "/restricted/p/download/" + file.shareId;
			}
			if($scope.shareForm.shareType == "users") {
				return location.origin + "/restricted/download/" + file.shareId;
			}
		};

		$scope.passwordText = function() {
			if(file.sharing == "password"){
				return "Change password";
			} else {
				return "Enter password";
			}
		};

		$scope.shareText = function() {
			if($scope.shareForm.shareType == "revoke"){
				return "Revoke";
			}
			return "Share";
		};

		/*var newUUID = function() {
			var r = crypto.getRandomValues(new Uint8Array(1))[0]%16|0, v = c == 'x' ? r : (r&0x3|0x8);
			return v.toString(16);
		};*/
	};
	Controller.$inject = [ '$scope', '$mdDialog','$http','file'];
	return Controller;
});