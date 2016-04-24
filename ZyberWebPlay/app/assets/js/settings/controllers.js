/*global define */

define(['./accountController',
		'../controllers/sidebarController',
		'../controllers/headerController'
		],
        function(accountController,
        	 	sidebarController,
        	 	headerController
        	 	) {

	/* Controllers */

	var controllers = {};
	
	controllers.accountController = accountController;
	controllers.sidebarController = sidebarController;
	controllers.headerController = headerController;

	return controllers;
});