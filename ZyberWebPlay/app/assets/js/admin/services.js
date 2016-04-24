/*global define */

define([ 'angular' ],
		function(angular ) {

	/* Services */
	
	var services = {};
		
	services.usersService = function($http){
		this.getUsers = function(){
			return $http.get("/api/users"); 
		};
		
		this.createUser = function(user){
			return $http.post("/api/users", user); 
		};
		
		this.updateUser = function(user){
			return $http.put("/api/users", user); 
		};
		
		this.saveAccount = function(user){
			return $http.put("/api/account", user); 
		};
		
		this.deleteUser = function(uuid){
			return $http.delete("/api/users/" + uuid);
		};
		
		this.getUser = function(uuid){
			return $http.get("/api/users/" + uuid); 
		};
		
		this.currentUser = function(){
			return $http.get("/api/currentUser"); 
		};
		
		this.changeUserPassword = function(model){
			return $http.put("/api/changePassword", model); 
		};
		
		this.getLanguages = function(){
			return $http.get("/api/languages"); 
		};
		
		this.getUserRoles = function(){
			return $http.get("/api/userroles"); 
		};
	};

	services.metadataService = function($http) {
		this.addTermstore = function(model){
			return $http.post("/api/termstore", model);
		};
		
		this.updateTermstore = function(uuid, model){
			return $http.put("/api/termstore?uuid=" + uuid, model);
		};
		
		this.deleteTermstore = function(uuid){
			return $http.delete("/api/termstore?uuid=" + uuid);
		};
	};

	services.activityService = function($http) {
		this.listFiles = function (path,byTime,showHidden) {
			return $http.get("/api/admin/activity?path="+encodeURIComponent(path)+"&byTime="+byTime+"&showHidden="+showHidden);
		};
	};

	services.loginActivityService = function($http) {
		this.listLogins = function () {
			return $http.get("/api/admin/loginActivity");
		};
	};
	
	services.groupsService = function($http) {
//		this.getUsersGroups = function () {
//			return $http.get("/api/usergroups");
//		};
		this.getGroups = function () {
			return $http.get("/api/groups");
		};
		this.createGroup = function(name){
			return $http.post("/api/usersgroups", {name:name});
		};
		this.getGroup = function(groupId){
			return $http.get("/api/groups/"+groupId);
		};
		this.getGroupMembers = function(groupId){
			return $http.get("/api/groups/" + groupId + "/members");
		};
		
		this.addMembersToGroup = function(groupId, members){
			return $http.put("/api/groups/" + groupId + "/members", {members: members});
		};
		
		this.removeGroup = function(groupId){
			return $http.delete("/api/usersgroups/" + groupId);
		};
		
		this.removeMember = function(groupId, memberId){
			return $http.delete("/api/groups/" + groupId + "/members/" + memberId);
		};
		this.updateGroup = function(groupId, name){
			return $http.put("/api/usersgroups/" + groupId, {name: name});
		};
		this.getUsersLike = function(name){
			return $http.get("/api/query/users", {params: {name: name}});
		};
	};
	
	services.usersService.$inject    = ['$http'];
	services.activityService.$inject = ['$http'];
	services.metadataService.$inject = ['$http'];
	services.groupsService.$inject   = ['$http'];
	return services;

});