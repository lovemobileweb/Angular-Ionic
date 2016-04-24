/*global define */

define(['../controllers/home/homeController',
        '../controllers/home/createFolderController',
        '../controllers/home/uploadController',
        '../controllers/home/shareFileController',
        '../controllers/home/shareFolderController',
        '../controllers/trash/trashController',
        '../controllers/version/versionController',
        '../controllers/home/securityController',
        '../controllers/sidebarController',
        '../controllers/headerController',
        '../controllers/home/selectFolderController',
        '../controllers/home/searchController',
        '../controllers/home/mimeTypes'], function(
              homeController,
              createFolderController,
              uploadController,
			  shareFileController,
			  shareFolderController,
			  trashController,
              versionController,
              securityController,
              sidebarController,
              headerController,
              selectFolderController,
              searchController,
              mimeTypes) {

	/* Controllers */

	var controllers = {};
	
	controllers.homeController = homeController;
	controllers.createFolderController = createFolderController;
	controllers.uploadController = uploadController;
	controllers.shareFileController = shareFileController;
	controllers.shareFolderController = shareFolderController;
	controllers.versionController = versionController;
	controllers.trashController = trashController;
	controllers.securityController = securityController;
	controllers.sidebarController = sidebarController;
	controllers.headerController = headerController;
	controllers.selectFolderController = selectFolderController;
	controllers.searchController = searchController;
	
	controllers.showFileController = function($scope, $mdDialog, homeService, file){
		$scope.file = file;
		$scope.url = '/api/download/' + file.uuid;
		
		$scope.downloadFile = function(){
			homeService.downloadFileJs(file);
		};
		$scope.cancel = function() {
			$mdDialog.cancel('cancel');
		};
		
		if(_.contains(mimeTypes.groupDocTypes, file.mimeType)){
			$scope.fileType = "document";
		}else{
			$scope.fileType = "unsupported";
		}
	};
	controllers.showFileController.$inject = ['$scope', '$mdDialog', 'homeService', 'file'];
	return controllers;
});