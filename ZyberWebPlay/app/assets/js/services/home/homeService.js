define([], function() {
	var service = function($http, homeFactory, $translate){
		this.getFiles = function(path, showHidden, ord, asc){
			var viewPar = homeFactory.view() ? "&view=" + homeFactory.view() : "";
			var url = "/api/files"  + "?path=" + path + "&showHidden="+showHidden+"&ord=" + ord + "&t=" + asc + viewPar;
			
			console.log(url);
			return $http.get(url);
		};
		
		this.createFolder = function(path, folder){
			var viewPar = homeFactory.view() ? "?view=" + homeFactory.view() : "";
			return $http.post("/api/createFolder" + viewPar, {path: path, folderName: folder});
		};
		
		this.renameFile = function(uuid, name){
			return $http.post("/api/rename", {uuid: uuid, name: name});
		};
		
		this.deleteFile = function(uuid){
			return $http.delete("/api/delete/" + uuid);
		};
		
		this.deleteFiles = function(files){
			var fileUuids = _.map(files, function(file){return file.uuid;})
			console.log("DeleteFiles service");
			console.log(fileUuids);
			return $http.post("/api/files/delete", fileUuids);
		};

		this.restoreFile = function(uuid){
			return $http.post("/api/undelete/" + uuid);
		};

		this.searchFiles = function(name, path, view, showHidden,hiddenOnly, limit){
//			return $http.get("/api/search?name="+name+"&showHidden="+showHidden+"&hiddenOnly="+hiddenOnly);
			var params = {name: name, spath: path, view: view,
				      showHidden: showHidden,
				      hiddenOnly: hiddenOnly, limit: limit
					 };
			console.log(params);
			var searchFiles = $http.get("/api/search",
					{params: params});
			return searchFiles.then(function(response){ 
				console.log("Response is " + JSON.stringify(response.data)); 
				return response.data.response; 
			})
		};
		
		this.downloadFileJs = function(file){
			//There is no need for the fileDownload plugin, we can check download (api endpoint) 
			//first to show message and then just do normal download 
			//(in browser we use cookie for authentication so this works with normal download)
			$.fileDownload('/api/download/' + file.uuid, {
				failCallback: function (html, url) {
					console.error("File download error: ");
					console.error(html);
					console.error(url);
					$translate("permission_cannot_view_file").then(function (msg) {
						Notification.error(msg);
					  });
				} 
		    });
		};
		
		this.getActivity = function(path,showHidden){
			var viewPar = homeFactory.view() ? "&view=" + homeFactory.view() : "";
            console.log("getActivities for path: " + path +"&showHidden="+showHidden+viewPar);
            return $http.get("/api/activity?path=" + path +"&showHidden="+showHidden+viewPar);
		};
		
		this.getTermStore = function(){
			return $http.get("/api/termstore");
		};
		
		this.getTerms = function(name){
			return $http.get("/api/termstore/" + name + "/terms");
		};
		
		this.getMetadata = function(pathId){
			return $http.get("/api/metadata?uuid="+pathId);
		};
		
		this.updateMetadata = function(pathId, metadata){
			return $http.put("/api/metadata?uuid=" + pathId, metadata);
		};
		
		this.deleteMetadata = function(pathId, key){
			return $http.delete("/api/metadata?uuid=" + pathId + "&key=" + key);
		};
		
		this.getBreadcrumb = function(pathId){
			var viewPar = homeFactory.view() ? "&view=" + homeFactory.view() : "";
			return $http.get("/api/breadcrumb" + "?path=" + pathId + viewPar);
		};
	};
	
	service.$inject = ['$http', 'homeFactory', '$translate'];
	
	return service;
});