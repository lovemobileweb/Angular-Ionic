define([], function() {
  var service = function($http){
    this.getVersions = function(path){
      console.log("getVersions path: " + window.encodeURIComponent(path));
      return $http.get("/api/versions?path=" + window.encodeURIComponent(path));
    };

    this.restoreVersion = function(uuid,version){
      console.log("restoreVersion uuid: " + uuid + " version " + version);
      return $http.post("/api/restore?uuid="+uuid+"&version="+version);
    };
  };

  service.$inject = ['$http'];
  return service;
});