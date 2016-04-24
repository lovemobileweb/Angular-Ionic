/*jshint smarttabs:true */
define([], function() {
	var Controller = function($scope, $routeParams, $location, $uibModal, 
			$window, trashService, Notification,$timeout) {
		window.debugTrashScope = $scope;

		$scope.files = [];
		if(!$scope.$parent.uuidMap) {
			$scope.$parent.uuidMap = {};
		}

		$scope.selectedFile = null;
		$scope.setSelected = function(file){
			if($scope.selectedFile !== file){
				$scope.selectedFile = file;
			}
		};
		$scope.clearSelected = function(){
			$scope.selectedFile = null;
		};

		var currLocation = $location.path();

		$scope.pathTo = function(file){
			console.log("pathTo: File is " + file );
			return currLocation+"/"+file;
		};


		$scope.partialPaths = function() {
			var partialPaths =
					[{
						name:"Trash",
						path:"trash"
					}];

			var frag = currLocation.split('/');
			var len = frag.length;
			for(var i = 2; i < len; i++){
				var name = $scope.$parent.uuidMap[frag[i]];
				var partial = {
					name: name,
					path: partialPaths[i-2].path + "/" + frag[i]
				};
				partialPaths.push(partial);
			}
			partialPaths[0].path="trash";
			return partialPaths;
		};

		console.log("Partials are " + JSON.stringify($scope.partialPaths()));
		$scope.partials = $scope.partialPaths();
		$scope.end = $scope.partialPaths().slice(-1)[0].path;
		console.log("Last is " + $scope.end);

		function loadFiles(p){
			var t =  "asc";
			trashService.getFiles(p, "name", t).success(function(data){
				$scope.files = data;
				for(var i=0;i<data.length;i++) {
					$scope.$parent.uuidMap[data[i].uuid]=data[i].name;
				}
			}).error(function(data /*, status, header, config */) {
				Notification.error({message: 'An error occurred while listing files', positionX: 'center'});
				console.log("Error: ");
				console.log( data );
			});
		}
		$scope.reloadFiles = function(){
			loadFiles($scope.end);
		};

		$scope.topLevel = $scope.partialPaths().length === 1;
		$scope.restoreSelectedFile = function(){
			if($scope.selected.length > 1){
				Promise.all($scope.selected.map(function(file){
					return trashService.restoreFile(file.uuid);
				})).then(function(arrayData){
					loadFiles($scope.end);
					$scope.selectedFile = null;
					$scope.selected = [];
					console.log("All response");
					console.log(arrayData);
					Notification.success(arrayData[0].data.message);
				}).catch(function(data /*, status, header, config*/) {
					console.log("Error: ");
					console.log( data );
				});
			}else if($scope.selectedFile){
				restoreSingle($scope.selectedFile);
			}
		};
		
		function restoreSingle(file){
			trashService.restoreFile(file.uuid).success(function(/*data*/){
				loadFiles($scope.end);
				$scope.selectedFile = null;
				Notification.success({message: 'File successfully restored', positionX: 'center'});
			}).error(function(data /*, status, header, config*/) {
				console.log("Error: ");
				console.log( data );
			});
		}
		
		loadFiles($scope.end);

		$scope.query = {
		  order: 'name',
		  limit: 5,
		  page: 1
		};
		
		
		$scope.loadStuff = function () {
		  $scope.promise = $timeout(function () {
		    // loading
		  }, 2000);
		}
		
		$scope.logItem = function (item) {
		  console.log(item.name, 'was selected');
		};
		
		$scope.logOrder = function (order) {
		  console.log('order: ', order);
		};
		
		$scope.logPagination = function (page, limit) {
		  console.log('page: ', page);
		  console.log('limit: ', limit);
		}
		$scope.showContext = false;
		$scope.onShow = function(file){
			$scope.setSelected(file);
			$scope.showContext = true;
		};
		$scope.onClose = function(){
			$scope.showContext = false;
		};
		
		$scope.selected = [];
		$scope.$watchCollection('selected', function (newValue, oldValue) {
			if($scope.selected && $scope.selected.length === 1){
				$scope.setSelected($scope.selected[0]);
			}else{
				$scope.selectedFile = null;
			}
		});

		$scope.onSelect = function(file){
			$scope.selected = [file];
		};

		$scope.onResize = function(){
			$timeout(function(){
			var windowHeight = $window.innerHeight - 5;
			var topHeaderHeight = parseInt(angular.element("#top-header").css("height"));
			var tableHeight = windowHeight - 113 - 55 - 45 - 30 - 20;
			angular.element("#table-container").css("max-height",tableHeight+"px");

			console.log("topHeaderHeight",topHeaderHeight);
			console.log("windowHeight",windowHeight);
			console.log("tableHeight",tableHeight);
			console.log(windowHeight-tableHeight);
			},0)
		};
		angular.element($window).bind('resize', function() {
	        $scope.onResize();
	    });
		$scope.onResize();
	};
	Controller.$inject = [ '$scope', 
	                       '$routeParams', 
	                       '$location', 
	                       '$uibModal', 
	                       '$window', 
	                       'trashService',
	                       'Notification',
	                       '$timeout'];
	return Controller;
});