angular.module('zyber.services', [])

.service('HomeService', function($http, $window, HomeFactory) {
  var serverApi = "http://192.168.1.4:9000";
  this.serverApi = serverApi;
  this.authenticate = function(user, pass){
    return new Promise(function(fulfill, reject){
      if($window.sessionStorage.token){
        fulfill();
      }else{
        $http.post(serverApi + "/api/authenticate", {username:user, password:pass})
        .success(function(data){
          $window.sessionStorage.token = data.bearer;
          fulfill();
        }).error(function (data, status, headers, config) {
          // Erase the token if the user fails to log in
          delete $window.sessionStorage.token;
          reject(data);
        });
      }
    });
  };

  this.checkDownload = function(file){
    return $http.get(serverApi + '/api/checkdownload/'+ file.uuid);
  };

  this.getFiles = function(path, showHidden, ord, asc){
    var viewPar = HomeFactory.view() ? "&view=" + HomeFactory.view() : "";
    var url = serverApi + "/api/files"  + "?path=" + path + "&showHidden="+showHidden+"&ord=" + ord + "&t=" + asc + viewPar;
    console.log(url);
    return $http.get(url);
  };

  this.getBreadcrumb = function(pathId){
      var viewPar = HomeFactory.view() ? "&view=" + HomeFactory.view() : "";
      return $http.get(serverApi + "/api/breadcrumb" + "?path=" + pathId + viewPar);
    };

  this.createFolder = function(path, folder){
      var viewPar = HomeFactory.view() ? "?view=" + HomeFactory.view() : "";
      return $http.post(serverApi + "/api/createFolder" + viewPar, {path: path, folderName: folder});
  };
})
.factory("HomeFactory", function(){
    var sorting = "name";
    var view;
    var asc = true;
    
    return {
      sorting: function(){ 
        return sorting;
      },
      setSorting: function(newSorting){
        sorting = newSorting;
      },
      asc: function(){ 
        return asc;
      },
      setAsc: function(na){
        asc = na;
      },
      view : function(){
        return view;
      },
      setView : function(v){
        view = v;
      }
    };
  });
