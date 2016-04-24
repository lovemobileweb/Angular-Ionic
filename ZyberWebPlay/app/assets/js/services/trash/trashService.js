define([], function() {
    var service = function($http){
        this.getFiles = function(uuid, ord, asc){
            return $http.get("/api/" + uuid + "?ord=" + ord + "&t=" + asc);
        };

        this.restoreFile = function(uuid){
            return $http.post("/api/trash/undelete/" + uuid);
        };
    };
    service.$inject = ['$http'];
    return service;
});