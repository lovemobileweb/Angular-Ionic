/*jshint smarttabs:true */
define([], function() {
	var Controller = function($scope, $routeParams, $location, $uibModal, 
			$window, homeService, homeFactory, Notification, $translate , 
			$mdEditDialog, $q, $timeout, $mdDialog, uploadFactory, $http,$mdSidenav) {
	
				
		var currLocation = $location.path();
		
		var path = "";
		if(typeof $routeParams.name !== "undefined"){
			path = $routeParams.name;
		}
		homeFactory.setPath(path);
		
		String.prototype.supplant = function (o) {
		    return this.replace(/{([^{}]*)}/g,
		        function (a, b) {
		            var r = o[b];
		            return typeof r === 'string' || typeof r === 'number' ? r : a;
		        }
		    );
		};
		var confirmDeletionMsg = null;
		var confirmDeletionMsg_files = null;
		$translate('confirm_file_deletion').then(function (msg) {
			confirmDeletionMsg = msg;
		  });
		$translate('confirm_files_deletion').then(function (msg) {
			confirmDeletionMsg_files = msg;
		  });
		
		$scope.getConfirmMsg = function(){
			if($scope.selected.length > 1 ){
				return confirmDeletionMsg_files.supplant({files: $scope.selected.length});
			}else if($scope.selected.length === 1){
				return confirmDeletionMsg.supplant({file: $scope.selected[0].name});
			}
		};
		
		window.debugScope = $scope;

		//Upload Part ###############################################################
		var confirmUploadCancel = null;
		$translate('confirm_upload_cancel').then(function (msg) {
			confirmUploadCancel = msg;
		});
		
		uploadFactory.setPath(path);
		$scope.flowObject = uploadFactory.flowObject();
		$scope.flowObject.off("filesSubmitted");
		
		$scope.fileSuccess = function(file, message, chunk){
			console.log("File success");
			var total = 0;
			$scope.flowObject.files.forEach(function(f){
				if(f.isComplete()){
					total += 1;
				}
			});
			$scope.flowObject.completedUploads = total;
		};

		$scope.filesSubmitted = function($flow, $files){
			console.log("filesSubmitted");
			$files.forEach(function(file){console.log(file);});
			uploadFactory.checkUpload(path).success(function(data){
				$flow.upload();
			}).error(function(data){
				$files.forEach(function(file){$flow.removeFile(file);});
				Notification.error(data.firstError.userMessage);
			});
		};
		
		$scope.closeUpload = function(){
			if($scope.flowObject.isUploading() || uploadsIncomplete($scope.flowObject)){
				if($window.confirm(confirmUploadCancel)){
					$scope.flowObject.cancel();
					$scope.flowObject.completedUploads = 0;
				}
			}else{
				$scope.flowObject.cancel();
				$scope.flowObject.completedUploads = 0;
				$scope.flowObject.files.forEach(function(f){
					$scope.flowObject.removeFile(f);
				});
			}
		};
		
		function uploadsIncomplete($flow){
			return _.some($flow.files, function(f){
				return !f.isComplete();
			});
		}
		
		$scope.isHidePanel = homeFactory.isHideUploadPanel;		
		$scope.toggleUploadPanel = homeFactory.toggleUploadPanel;
		//Upload part ###################################################################
		
		$scope.files = [];
		if(!$scope.$parent.showHidden) {
			$scope.$parent.showHidden = false;
		}

		$scope.hashSelected = $location.hash();
		
		$scope.selectedFile = null;
		
		$scope.setSelected = function(file){
			if($scope.selectedFile !== file){
				$scope.selectedFile = file;
				$scope.editing = false;
				$scope.newName.value = file.name;
			}
		};
		
		$scope.editing = false;
		$scope.initEdit = function(){
			$scope.newName.value = $scope.selectedFile.name;
			$scope.editing = true;
			$scope.close();
		};
		$scope.newName = {value: ""};
		$scope.changeName = function(file){
			var newName = $scope.newName.value;
			if(file.name !== newName){
				homeService.renameFile(file.uuid, newName).success(function(data){
					Notification.success(data.response);
					loadFiles(path);
					$scope.selected = [];
				}).error(function(data /*, status, header, config*/) {
					Notification.error(data.firstError.userMessage);
					console.log("Error: ");
					console.log( data.errors );
				});
			}
			$scope.editing = false;
		};
		
		
		$scope.asc = homeFactory.asc;
		
		$scope.setSorting = function(no){
			homeFactory.setSorting(no);
			homeFactory.setAsc(!homeFactory.asc());
			loadFiles(path);
			$scope.selected = [];
		};
		
		$scope.setAsc = homeFactory.setAsc;
		
		$scope.getSorting = homeFactory.sorting;

		$scope.partials = [];
		
		$scope.currentView = function(){
			return homeFactory.view();
		};
		
		$scope.isHome = function(){
			return homeFactory.view() === "home";
		};
		
		function loadPartials(path){
			homeService.getBreadcrumb(path).success(function(data){
				$scope.partials = data.response;
			});
		}
		
		$scope.getLocation = function(){
			var locationNames = _.map($scope.partials, function(p){
				return p.name;
			});
			return locationNames.join(" / ");
		};

		$scope.currentPath = function(){
			var frag = currLocation.split('/');
			var sanitLoc = "";
			var len = frag.length;
			for(var i = 0; i < len; i++){
				if(frag[i])
					sanitLoc = sanitLoc + (window.encodeURIComponent(frag[i]) + "/");
			}
			return sanitLoc;
		};

		function loadFiles(p){
			//$scope.loading = true;

			var t = homeFactory.asc() ? "asc" : "desc";
			homeService.getFiles(p, $scope.showHidden, homeFactory.sorting(), t).success(function(data){
				$scope.files = data.response;
				if($scope.hashSelected) {
					console.log("Selected hash is " + $scope.hashSelected);
					var selected = _.find(debugScope.files,function(f){return f.name === $scope.hashSelected});
					if(selected)$scope.selected = [selected];
				}
			}).error(function(data /*, status, header, config */) {
				Notification.error({message: data.firstError.userMessage, positionX: 'center'});
				console.log("Error: ");
				console.log( data.errors);
			}).finally(function(){
				$scope.loading = false;
				$scope.selected = [];
			});
		}
		$scope.reloadFiles = function(){
			loadFiles(path);
		};
		
		$scope.openCreateFolder = function(ev) {
			$mdDialog.show({
		      controller: 'createFolderController',
		      templateUrl: 'createFolder.html',
		      parent: angular.element(document.body),
		      targetEvent: ev,
		      clickOutsideToClose:true,
		      locals : {
                    partials : $scope.partials,
                    currentView : $scope.currentView
                }
		    })
		    .then(function(name) {
		    	console.log("Folder name: " + name);
		      	homeService.createFolder(path, name).success(function(data){
					Notification.success(data.response);
					loadFiles(path);
				}).error(function(data, status, header, config) {
					Notification.error(data.firstError.userMessage);
					console.log("Error: ");
					console.log( data );
				});
		    }, function() {
		    	console.log("Folder name failed: ");
		    }).finally(function(){
		    	$scope.selected = [];
		    });

		};

		$scope.openUploadFile = function(ev) {
		    $scope.closePanel = false;
			$mdDialog.show({
		      controller: 'uploadController',
		      templateUrl: 'uploadFile.html',
		      clickOutsideToClose:true,
		      targetEvent: ev,
		      resolve: {
					path: function(){return path;}
				},
				locals: {
					partials : $scope.partials,
					currentView : $scope.currentView
					// path: path,
					// onSuccess: $scope.onSuccess
				}

		    })
		    .then(function(name) {
		    	console.log($scope.flowObject);
		    	console.log("success");
		    }, function() {
		    	console.log("failed");
		    }).finally(function(){
		    	$scope.selected = [];
		    });

		};

		$scope.openFileSharing = function(file,ev) {
			$scope.items = file;
			var modalInstance = $mdDialog.show({
				templateUrl : '/assets/partials/shareFile.html',
				controller : 'shareFileController',
				parent: angular.element(document.body),
				targetEvent: ev,
				clickOutsideToClose:false,
				resolve: {
					file: function () {
						return file;
					}
				}
			}).then(function(result) {
				if(shared) {
					Notification.success({message: 'File has been shared', positionX: 'center'});
				} else {
					Notification.success({message: 'File sharing has been revoked', positionX: 'center'});
				}				
			}).finally(function(){
		    	$scope.selected = [];
		    	$scope.close();
		    });
		};

		$scope.openFolderSharing = function(file,ev) {
//			$scope.items = file;
			var modalInstance = $mdDialog.show({
				templateUrl : '/assets/partials/sharingFolder.html',
				controller : 'shareFolderController',
				parent: angular.element(document.body),
				targetEvent: ev,
				clickOutsideToClose:false,
				locals: {
					file: file,
					event: ev,
					parentScope: $scope,
					onRefresh: function(){
						loadFiles(path);
				    	$scope.selected = [];
					}
				}
			});
			$scope.close();
		}
		
		$scope.deleteSelectedFiles = function(){
			console.log("Selected files");
			console.log($scope.selected);
			if($scope.selected.length > 1 ){
				console.log("Deleting multiple files");
				homeService.deleteFiles($scope.selected).success(function(data){
					loadFiles(path);
					$scope.selected = [];
					Notification.success(data.response.deleted);
					$scope.close();
				}).error(function(data /*, status, header, config*/) {
					Notification.error(data.firstError.userMessage);
					console.log("Error: ");
					console.log( data.errors );
//					$scope.selected = [];
				});
			}else if($scope.selectedFile){
				deleteFile($scope.selectedFile);
			}
			
		};
		
		function deleteFile(file){
			console.log(file);
			homeService.deleteFile(file.uuid).success(function(data){
				loadFiles(path);
				$scope.selected = [];
				Notification.success(data.response);
				$scope.close();
			}).error(function(data /*, status, header, config*/) {
				Notification.error(data.firstError.userMessage);
				console.log("Error: ");
				console.log( data.errors );
			});
		}

		$scope.restoreFile = function(file){
			homeService.restoreFile(file.uuid).success(function(data){
				loadFiles(path);
				$scope.selected = [];
				Notification.success(data.message);
			}).error(function(data /*, status, header, config*/) {
				console.log("Error: ");
				console.log( data );
			});
		};
		
		$scope.fileHistory = function(file){
           var path = "/" + $scope.currentView() + "/versions/" + file.uuid;
           
           console.log("The path is " + path);
           $location.path(path);
		};
		
		var isNotEmpty = function(str){
			return (str && str.trim() !== "");
		};

		$scope.searchedFiles = [];

		$scope.searchFiles = function(name){
			if(!name || name.trim() === "") return [];
			console.log("!!!Searching for wtf " + name);
			var searchFiles = homeService.searchFiles(name,path,homeFactory.view(),$scope.$parent.showHidden,false);
			return searchFiles;
		};
	
		$scope.gotoSearch = function(query){
			if(!query || query.trim() === "") return;
			console.log("doing search for: " + query);
			var autoChild = document.getElementById('searchText').firstElementChild;
		    var el = angular.element(autoChild);
		    el.scope().$mdAutocompleteCtrl.hidden = true;
		    
//			var mask = $(".md-scroll-mask");
//			console.log(mask);
//			mask.remove();
		    var pathParam = path ? {p:path} : {};
	        $location.path(homeFactory.view() + "/search/" + query).search(pathParam);
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

		$scope.metadata = {};
		$scope.selectedStore = {};
		$scope.termStore = [];
		$scope.availableTermStore = [];

		function loadTermStore(){
			homeService.getTermStore().success(function(data){
				$scope.termStore = data;
			}).error(function(data, status, header, config) {
				Notification.error("Can't retrieve term store");
				console.error("Error retrieving TermStore");
				console.error(data);
			});
		}

		function loadMetadata(pathId){
			homeService.getMetadata(pathId).success(function(data){
				$scope.metadata = data;
				
				$scope.availableTermStore = _.filter($scope.termStore, function(ts){
					return !(ts.name in data);
				});
				$scope.selectedStore = {};
				
			}).error(function(data, status, header, config) {
				Notification.error("Can't retrieve file metadata");
				console.error("Can't retrieve file metadata");
				console.error(data);
			});
		}

		function addToMetadata(file, termStore, elements){
			if(!elements.length)return;
			var ob = {key: termStore, value: elements};
			homeService.updateMetadata(file.uuid, ob).success(function(data){
				loadMetadata(file.uuid);
				Notification.success("Metadata updated");
			}).error(function(data, status, header, config) {
				Notification.error("Can't update file metadata");
				console.error("Can't update file metadata");
				console.error(data);
			});
		}
		
	
		
		$scope.downloadFile = function(file){
			homeService.downloadFileJs(file);
		};

		$scope.newMetadata = function(file, name){
			openMetadata(file, name, []);
		};
		
		$scope.editMetadata = function(file, name, elements){
			openMetadata(file, name, elements);
		};
		
		function openMetadata(file, name, e){
			var elements = [];
			if(e)
				elements = e;
			
			var termStore = _.find($scope.termStore, function(ts){
				return ts.name === name;
			});
			
			var metadataController = function($inScope, $mdDialog){
				$inScope.termStore = termStore;
				$inScope.newElement = {};
				$inScope.elements = elements;

				$inScope.cancel = function() {
					$mdDialog.cancel('cancel');
				};

				$inScope.addElement = function(element){
					if(element && !_.contains($inScope.elements, element)){
						$inScope.elements.push(element);
					}
					$inScope.newElement = {};
				};

				$inScope.remove = function(element){
					$inScope.elements = _.without($inScope.elements, element);
				};

				$inScope.save = function(){
					addToMetadata(file, $inScope.termStore.name, $inScope.elements);
					$mdDialog.cancel('save');
				};

				function getTerms(){
					if(!$inScope.termStore.allowCustomTerms){
						homeService.getTerms($inScope.termStore.name).success(function(data){
							$inScope.terms = data;
							console.log("Data:");
							console.log(data);
						}).error(function(data, status, header, config) {
							Notification.error("Can't retrieve terms");
							console.error("Error retrieving TermStore");
							console.error(data);
						});
					}
				}
				getTerms();
			};

			metadataController.$inject = ['$scope', '$mdDialog'];

			var modalInstance = $mdDialog.show({
				templateUrl : 'metadata.html',
				controller : metadataController
			});
		}
		
		$scope.deleteMetadata = function(file, key){
			homeService.deleteMetadata(file.uuid, key).success(function(data){
				loadMetadata(file.uuid);
				Notification.success(data);
			}).error(function(data, status, header, config) {
				Notification.error(data);
				console.error("Error deleting metadata");
				console.error(data);
			});
		};
		
		loadFiles(path);
		loadTermStore();
		loadPartials(path);
		uploadFactory.setControllerListener({
			update: function(){loadFiles(path);},
			path: path
		})

		$scope.selected = [];
		$scope.$watchCollection('selected', function (newValue, oldValue) {
			if($scope.selected && $scope.selected.length === 1){
				$scope.setSelected($scope.selected[0]);
			}else{
				$scope.selectedFile = null;
			}
		});
		
		$scope.$watch("selectedFile", function(newValue, oldValue){
			console.log("SelectedFile: " + newValue);
			if($scope.selectedFile){				
				homeService.getActivity($scope.selectedFile.uuid, $scope.$parent.showHidden).success(function(data){
			        $scope.selectedFileActivity = data.response;
			        console.log("selectedFileVersions: ");
			        console.log(data.response);
			      }).error(function(data, status, header, config) {
			        Notification.error(data.firstError.userMessage);
			        console.log("Error: ");
			        console.log( data.errors );
			      });
				loadMetadata($scope.selectedFile.uuid);
			}
		});
		
		$scope.showContext = false;
		$scope.onShow = function(file){
			$scope.selected = [file];

			$scope.showContext = true;
		};
		
		$scope.onSelect = function(file){
			if ($scope.ctrlDown){
				if(_.contains($scope.selected, file)){
					$scope.selected = _.without($scope.selected, file);
				}else{
					$scope.selected.push(file);
				}
		    }else{
				$scope.selected = [file];
		    }
		};
		
		$scope.isFileSelected = function(file){
			return _.contains($scope.selected, file);
		};
		
		$scope.query = {
		   order: 'name',
		   limit: 5,
	       page: 1
		};
  
  
		//TODO ??
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
  
	  $scope.demo = {
	    showTooltip : false
	  };
  
	  $scope.$watch('demo.tipDirection',function(val) {
	    if (val && val.length ) {
	      $scope.demo.showTooltip = true;
	    }
	  });
 
  	$scope.message = [
  		['qwerty', function(){
	  		console.log('qewr');
	  	}],
	  	['qwerty1', function(){
	  		console.log('qewr1');
	  	}]
	];	
	$scope.onClose = function(){
		$scope.showContext = false;
	};
	
	$scope.hidePanel = false;
	$scope.closePanel = false;
	$scope.onResize = function(){
		$timeout(function(){
		var windowHeight = $window.innerHeight - 5;
		var topHeaderHeight = parseInt(angular.element("#top-header").css("height"));
		if ($(window).width()<840)
			var tableHeight = windowHeight - topHeaderHeight - 55 - 45 - 30 - 20 - 50;
		else
			var tableHeight = windowHeight - topHeaderHeight - 55 - 45 - 30 - 20;
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

		//#####################Ctrl + key combinations (ie: select all)
		$scope.ctrlDown = false;
		
	    Mousetrap.bind(['command', 'ctrl'], function() { 
	    	$scope.ctrlDown = true;
		    $scope.$apply();
	    }, 'keydown');
	    
	    Mousetrap.bind(['command', 'ctrl'], function() { 
	    	$scope.ctrlDown = false;
		    $scope.$apply();
	    }, 'keyup');
	    
	    Mousetrap.bind(['command+a', 'ctrl+a'], function() {
	        $scope.selected = $scope.files.slice();
	        // return false to prevent default browser behavior
	        // and stop event from bubbling
	        return false;
	    });
	
	    Mousetrap.bind('esc', function() { 
	    	$scope.selected = [];
		    $scope.$apply();
	    }, 'keyup');
		//#####################
	    
	    
	    /*######################## Move/copy files #######################*/
		$scope.showMoveFiles = function(files,ev){
			var modalInstance = $mdDialog.show({
				templateUrl : 'selectFolder.html',
				controller : 'selectFolderController',
				parent: angular.element(document.body),
				targetEvent: ev,
				clickOutsideToClose:true,
				size : "lg",
				locals : {
					foldersUrl : $scope.isHome() ? '/api/hometree' : '/api/sharestree'
                }
			}).then(function(selected){
				if(selected){
					var aFiles = _.map(files, function(f){return f.uuid;});			
					$http.post('/api/moveto', {paths: aFiles, dstPath: selected.uuid}).
					success(function(data){
						loadFiles(path);
						Notification.success(data.response);
					}).error(function(data){
						Notification.error(data.firstError.userMessage);
						console.log(data);
					});
				}
		    });
		};
		
		$scope.showCopyFiles = function(files,ev){			
			
			var modalInstance = $mdDialog.show({
				templateUrl : 'selectFolder.html',
				controller : 'selectFolderController',
				parent: angular.element(document.body),
				targetEvent: ev,
				clickOutsideToClose:true,
				size : "lg",
				locals : {
					foldersUrl : '/api/folderstree'
                }
			}).then(function(selected){
				if(selected){
					var aFiles = _.map(files, function(f){return f.uuid;});			
					$http.post('/api/copyto', {paths: aFiles, dstPath: selected.uuid}).
					success(function(data){
						loadFiles(path);
						Notification.success(data.response);
					}).error(function(data){
						Notification.error(data.firstError.userMessage);
						console.log(data);
					});
				}
		    });
		};

		$scope.toggleRight = buildToggler('right');
	    $scope.isOpenRight = function(){
	      return $mdSidenav('right').isOpen();
	    };
	    /**
	     * Supplies a function that will continue to operate until the
	     * time is up.
	     */
	    function debounce(func, wait, context) {
	      var timer;
	      return function debounced() {
	        var context = $scope,
	            args = Array.prototype.slice.call(arguments);
	        $timeout.cancel(timer);
	        timer = $timeout(function() {
	          timer = undefined;
	          func.apply(context, args);
	        }, wait || 10);
	      };
	    }
	    /**
	     * Build handler to open/close a SideNav; when animation finishes
	     * report completion in console
	     */
	    
	    function buildToggler(navID) {
	      return function() {
	        $mdSidenav(navID)
	          .toggle()
	          .then(function () {
	            console.log("toggle " + navID + " is done");
	          });
	      }
	    }
	    $scope.close = function () {
	          $mdSidenav('right').close()
	            .then(function () {
	              console.log("close RIGHT is done");
	            });
        };
	};
	Controller.$inject = [ '$scope', 
	                       '$routeParams', 
	                       '$location', 
	                       '$uibModal', 
	                       '$window', 
	                       'homeService',
	                       'homeFactory',
	                       'Notification',
	                       '$translate',
	                       '$mdEditDialog',
	                       '$q',
	                       '$timeout',
	                       '$mdDialog',
	                       'uploadFactory',
	                       '$http',
	                       '$mdSidenav'
	                       ];
	return Controller;
});