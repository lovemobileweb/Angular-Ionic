angular.module('zyber.controllers', [])

.controller('HomeCtrl', function($scope, $stateParams, $translate, $window, 
  $cordovaFileTransfer, $cordovaFileOpener2, HomeService, HomeFactory, $ionicModal, 
  $ionicPopup, $ionicLoading) {
    $scope.files = [];
    $scope.partials = [];
    $scope.folder = {};
    var directory = 'zyber_downloads';

    var path = "";
    if(typeof $stateParams.name !== "undefined"){
      path = $stateParams.name;
    }

    console.log("Path: " + path);

    function loadFiles(p){
      var t = HomeFactory.asc() ? "asc" : "desc";
      HomeService.getFiles(p, false, HomeFactory.sorting(), t).success(function(data){
        console.log(data);
        $scope.files = data.response;
      }).error(function(data /*, status, header, config */) {
        console.log("Error: ");
        console.log( data.errors);
      });
    }

    function loadPartials(path){
      HomeService.getBreadcrumb(path).success(function(data){
        $scope.partials = data.response;
        console.log("partials");
        console.log($scope.partials);
        console.log("done");
      });
    }

    $scope.currentView = function(){
      return HomeFactory.view();
    };

    function downloadFileJs(file){
      HomeService.checkDownload(file).success(function(result){
        var url = HomeService.serverApi + '/api/download/' + file.uuid;
        //Does not work on device, only browser
        //window.location.assign(url);
        
        // Save location for android
         var targetPath = null;
        if(ionic.Platform.isAndroid()){
          targetPath = cordova.file.externalCacheDirectory  + file.name;
        // Save location for ios
        }else if(ionic.Platform.isIOS()){
          targetPath = cordova.file.cacheDirectory  + file.name;
        }else{
          //Not supported platform
          return;
        }
       

        var options = {headers : {bearer: $window.sessionStorage.token}};

        $ionicLoading.show({
          template: 'Loading...'
        });

        $cordovaFileTransfer.download(url, targetPath, options, true).then(function (result) {
          console.log('Success download');
          $ionicLoading.hide();

          $cordovaFileOpener2.open(
              targetPath, // Any system location, you CAN'T use your appliaction assets folder
              file.mimeType
          ).then(function() {
              console.log('Success open');
          }, function(err) {
              console.log('An error occurred: ' + JSON.stringify(err));
          });
        }, function (error) {
            console.log('Error');
            $ionicLoading.hide();
        }, function (progress) {
            // PROGRESS HANDLING GOES HERE
        });

      }).error(function(data){
        console.log("Error");
        console.log(data);
      });
    }

    $scope.downloadFile = function(file){
      downloadFileJs(file);
    };

    $ionicModal.fromTemplateUrl('create_folder.html', {
        scope: $scope,
        animation: 'slide-in-up'
      }).then(function(modal) {
        $scope.modal = modal;
      });

    $scope.cancel = function() {
        $scope.modal.hide();
        $scope.folder = {};
    };

    $scope.openCreateFolder = function(){
      console.log("openCreateFolder");
      $scope.modal.show();
    };

    $scope.doCreateFolder = function(name){
      HomeService.createFolder(path, name).success(function(data){
        loadFiles(path);
        $scope.modal.hide();
        $scope.folder = {};
        $ionicPopup.alert({
          template: data.response
        });
      }).error(function(data, status, header, config) {
          console.log("Error: ");
          console.log( data );
          $ionicPopup.alert({
            template: data.firstError.userMessage,
            okType: 'button-assertive'
          });
      });
    };

    $scope.openUploadFile = function(){
        if(ionic.Platform.isAndroid()){
          androidUpload();
        }else if(ionic.Platform.isIOS()){
          iosUpload();
        }
       
    };


    function androidUpload(){
       window.plugins.mfilechooser.open([], function (uri) {
        console.log(uri);
        $ionicLoading.show({
            template: 'Loading...'
        });
        // File name only
        var filename = uri.split("/").pop();
        var options = {
            fileKey: "file",
            fileName: filename,
            chunkedMode: true,
            headers : {bearer: $window.sessionStorage.token}
         };
         var viewPar = HomeFactory.view() ? "?view=" + HomeFactory.view() : "";
         var url = HomeService.serverApi + '/api/upload/' + path + viewPar;

         $cordovaFileTransfer.upload(url, uri, options).then(function (result) {
            var jsonResponse = JSON.parse(result.response);
            $ionicPopup.alert({
                template: jsonResponse.response
              });
            console.log("SUCCESS: " + JSON.stringify(result.response));
            console.log("Response: " + result.response);
            $ionicLoading.hide();
            loadFiles(path);
         }, function (err) {
             console.log("ERROR: " + JSON.stringify(err));
             $ionicLoading.hide();
             $ionicPopup.alert({
                template: err.firstError.userMessage,
                okType: 'button-assertive'
              });
         }, function (progress) {
             // PROGRESS HANDLING GOES HERE
         });
      }, function (error) {
        console.log(error);
      });
    }

    function iosUpload(){
      window.FilePicker.pickFile(function(uri){
        console.log(uri);
        $ionicLoading.show({
            template: 'Loading...'
        });
        // File name only
        var filename = uri.split("/").pop();
        var options = {
            fileKey: "file",
            fileName: filename,
            chunkedMode: true,
            headers : {bearer: $window.sessionStorage.token}
         };
         var viewPar = HomeFactory.view() ? "?view=" + HomeFactory.view() : "";
         var url = HomeService.serverApi + '/api/upload/' + path + viewPar;

              
         $cordovaFileTransfer.upload(url, uri, options).then(function (result) {
            var jsonResponse = JSON.parse(result.response);
            $ionicPopup.alert({
                template: jsonResponse.response
              });
            console.log("SUCCESS: " + JSON.stringify(result.response));
            console.log("Response: " + result.response);
            $ionicLoading.hide();
            loadFiles(path);
         }, function (err) {
             console.log("ERROR: " + JSON.stringify(err));
             $ionicLoading.hide();
             $ionicPopup.alert({
                template: err.firstError.userMessage,
                okType: 'button-assertive'
              });
         }, function (progress) {
             // PROGRESS HANDLING GOES HERE
         });
      },function(error){
        console.log(error);
      });
    }

    loadFiles(path);
    loadPartials(path);
    // HomeService.authenticate().then(function(){
    //   loadFiles(path);
    //   loadPartials(path);
    // }, function(error){
    //   console.error("Promise failed");
    // });
});
