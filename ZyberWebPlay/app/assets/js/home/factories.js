/*global define */

define([], function(angular) {

	/* Factories */
	var factories = {};
	
	factories.homeFactory = function(){
		var sorting = "name";
		var view="home";
		var asc = true;
		var mHideUploadPanel = false;
		var path = "";
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
			},
			urlViewParam : function(){
				var viewParam = view ? "?view=" + view : "";
				return viewParam;
			},
			toggleUploadPanel : function(){
				mHideUploadPanel = !mHideUploadPanel;
			},
			isHideUploadPanel : function(){
				return mHideUploadPanel;
			},
			path: function(){
				return path;
			},
			setPath: function(np){
				path = np;
			}
		};
	};
	
	factories.uploadFactory = function(Notification, homeFactory, $http){
		var flow = new Flow({
			simultaneousUploads: 1,
			target:'/api/upload',
			generateUniqueIdentifier: function(){
    			return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    			    var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
    			    return v.toString(16);
    			});
    		},
    		permanentErrors: [403, 404, 415, 501]//500 also?
		});
		
		var viewPar = homeFactory.urlViewParam();
		
		var controllerListener = null;

		var path = null;

		flow.on('fileError', function($file, $message, $flow){
			console.log($message);
			Notification.error(JSON.parse($message).firstError.userMessage);
		});

		flow.on('fileSuccess', function($file, $message, $flow){
			$file.processing = 'processing';
//			doComplete($file, $message, $flow, addVersion);

			$http.post(
					"/api/checkupload/",
					{name : $file.name, relativePath: $file.relativePath},
					{params: {path: $file.path, view: $file.view()}}
					).success(function(data){
				var addVersion = false;
				if(data.response.existent){
					addVersion = confirm(data.response.confirm_message);
				}
				doComplete($file, $message, $flow, addVersion);
			}).error(function(data){
				//TODO
				Notification.error(data.firstError.userMessage);
				$file.cancel();
			});
		});
		
		function doComplete($file, $message, $flow, addVersion){
			$http.put('/api/upload/' + $file.path + $file.viewParam, 
					{flowIdentifier: $file.uniqueIdentifier, 
				     flowFilename:$file.name, 
				     add_version: addVersion,
				     relativePath: $file.relativePath}).success(function(data){
						$file.processing = 'completed';
						if(controllerListener && $file.path === controllerListener.path){
							controllerListener.update();
						}
					}).error(function(data){
						Notification.error(data.firstError.userMessage);
					});
		}
				
		flow.on('fileAdded', function($file, $event, $flow){
			console.log("file added");
			console.log($file);
			$file.processing = null;
			$file.path = path;
			$file.view = homeFactory.view;
			$file.viewParam = homeFactory.urlViewParam();
			console.log("fileAdded to path: " + path);
		});
		
		flow.completedUploads = 0;
				
		return {
			flowObject: function(){
				return flow;
			},
			setPath: function(npath){
				path = npath;
			},
			setControllerListener: function(listener){
				controllerListener = listener;
			},
			checkUpload: function(path){
				return $http.get('/api/checkupload/', {params: {path: path, view: homeFactory.view()}}); 
			}
		};
	};
	
	factories.uploadFactory.$inject = ["Notification", 'homeFactory', '$http'];
	
	return factories;

});