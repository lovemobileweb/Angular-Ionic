/*global define */

define([ 'angular', '../services/home/homeService',"../services/version/versionService","../services/trash/trashService" ],
		function(angular, homeService, versionService,trashService) {

	/* Services */
	
	var services = {};
	
	services.homeService = homeService;
	services.versionService = versionService;
	services.trashService = trashService;
	
	return services;

});