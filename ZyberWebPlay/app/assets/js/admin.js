/*global require, requirejs */
/*jshint smarttabs:true */
requirejs.config({
	paths : {
		'angular' : [ '/assets/bower/angularjs/angular' ],
		'angular-animate' : [ '/assets/bower/angular-animate/angular-animate'],
		'angular-aria' : [ '/assets/bower/angular-aria/angular-aria'],
		'angular-route' : [ '/assets/bower/manual/angular-route' ],
		'ui-bootstrap': [ '/assets/bower/manual/ui-bootstrap-tpls' ],
		'angular-ui-contextMenu': [ '/assets/lib/angular-ui-contextMenu' ],
		'angular-messages': [ '/assets/bower/angular-messages/angular-messages'],
		'angular-ui-notification': [ '/assets/bower/angular-ui-notification/angular-ui-notification'],
        'ui-router' : [ '/assets/bower/angular-ui-router/angular-ui-router' ],
		'angular-translate' : [ '/assets/bower/angular-translate/angular-translate' ],
		'angular-translate-loader-url' : [ '/assets/bower/manual/angular-translate-loader-url' ],
		'angular-material' : [ '/assets/bower/angular-material/angular-material'],
		'angular-material-data-table' : [ '/assets/bower/angular-material-data-table/md-data-table.min'],
		'ng-context-menu' : [ '/assets/bower/ng-context-menu/ng-context-menu' ]
		,'angular-sanitize' : [ '/assets/bower/manual/angular-sanitize' ]
	},
	shim : {
		'angular' : {
			exports : 'angular'
		},
		'angular-animate' : {
			deps : ['angular']
		},
		'angular-aria' : {
			deps : ['angular']
		},
		'angular-route' : {
			deps : [ 'angular' ]
		},
		'angular-messages' : {
			deps : [ 'angular' ]
		},
		'ui-bootstrap' : {
			deps : [ 'angular' ]
		},
		'angular-ui-notification' : {
			deps : [ 'angular' ]
		},
		'ui-router' : {
			deps : [ 'angular' ]
		},
		'angular-translate' : {
			deps : [ 'angular' ]
		},
		'angular-translate-loader-url' : {
			deps : [ 'angular-translate' ]
		},
		'angular-sanitize' : {
			deps : ['angular']
		},
		'angular-material' : {
			deps : [ 'angular' ]
		},
		'angular-material-data-table' : {
			deps : [ 'angular-material' ]
		},
		'ng-context-menu' : {
			deps : [ 'angular' ]
		}
	}
});

require([ 'angular', './admin/controllers', './admin/services', './home/services',
           './home/factories', 'ui-bootstrap',
          'angular-ui-notification','angular-messages', 'ui-router', 
          'angular-translate','angular-animate','angular-aria','angular-translate-loader-url',
          'angular-sanitize','angular-material','angular-material-data-table', 'ng-context-menu'], 
		function(angular, controllers, services, homeServices, factories,angularAnimate,
				angularAria, angularMaterial, angularMaterialDataTable,ngContextMenu /*, angularRoute, uiBootstrap*/) {
	'use strict';

	// Declare app level module

	var app = angular.module(
			'Administration',
			[ 'ui.bootstrap', 
			  'ui-notification', 
			  'ui.router',
			  'ngAnimate',
			  'ngAria',
			  'ngMessages',
			  'pascalprecht.translate',
			  'ngSanitize',
			  'ngMaterial',
			  'md.data.table',
			  'ng-context-menu']);
	//configuration
	app.config([ "$stateProvider", "$urlRouterProvider", "$locationProvider",
              function($stateProvider, $urlRouterProvider /*, $locationProvider*/) {
//		$locationProvider.html5Mode(true);
		$urlRouterProvider.when("/", "/users");
		$urlRouterProvider.otherwise("/users");
		$stateProvider.state('admin', {
			url : "/",
			templateUrl : '/assets/partials/admin/main.html',
			controller : controllers.adminController
		}).state('admin.users',{
			url : 'users',
			abstract: true,
			templateUrl : '/assets/partials/admin/users.html'
		}).state('admin.users.list',{
			url : '',
			parent: 'admin.users',
			templateUrl : '/assets/partials/admin/listUsers.html',
			controller: controllers.usersController
		}).state('admin.users.details',{
			url : '/details?userId',
			parent: 'admin.users',
			templateUrl : '/assets/partials/admin/details.html',
			controller: controllers.detailsController
		}).state('admin.policy',{
			url : 'policy',
			templateUrl : '/assets/partials/admin/passwordPolicy.html',
			controller: controllers.policyController
		}).state('admin.permissions',{
			url : 'permissions',
			templateUrl : '/assets/partials/admin/permissions.html',
			controller: controllers.permissionsController
		}).state('admin.activity',{
			url : 'activity',
			templateUrl : '/assets/partials/admin/activity.html',
			controller: controllers.activityController
		}).state('admin.loginActivity',{
			url : 'loginActivity',
			templateUrl : '/assets/partials/admin/login-activity.html',
			controller: controllers.loginActivityController
		}).state('admin.metadata',{
			url : 'metadata',
			templateUrl : '/assets/partials/admin/termStore.html',
			controller: controllers.termStoreController
		}).state('admin.groups',{
			abstract: true,
			url : 'groups',
			template : '<ui-view/>'
		}).state('admin.groups.list',{
			url : '',
			parent: 'admin.groups',
			templateUrl : '/assets/partials/admin/groups.html',
			controller: controllers.groupsController
		}).state('admin.groups.view',{
			url : '/view?groupId',
			parent: 'admin.groups',
			templateUrl : '/assets/partials/admin/viewGroup.html',
			controller: controllers.groupMembersController
		});
	} ]);
	
	app.config(function(NotificationProvider) {
        NotificationProvider.setOptions({
            positionX: 'right'
        });
    });
	
	app.config(['$translateProvider',  
                function($translateProvider) {
    	$translateProvider.useUrlLoader('/api/usermessages');
        $translateProvider.preferredLanguage("_");
        $translateProvider.useSanitizeValueStrategy('escape');
    }]);
	
	//registering controllers:
	app.controller("adminController", controllers.adminController);
	app.controller("activityController", controllers.activityController);
	app.controller("usersController", controllers.usersController);
	app.controller("sidebarctrl", controllers.sidebarController);
	app.controller("termStoreController", controllers.termStoreController);
	app.controller("policyController", controllers.policyController);
	app.controller("loginActivityController", controllers.loginActivityController);
	app.controller("headerctrl", controllers.headerController);
	
	//registering services:
	app.service('usersService', services.usersService);
	app.service('activityService', services.activityService);
	app.service('loginActivityService', services.loginActivityService);
	app.service('homeService', homeServices.homeService);
	app.service('metadataService', services.metadataService);
	app.service('groupsService', services.groupsService);
	
	app.filter('encode', function() {
		  return window.encodeURIComponent;
	});
	
	//directives
	app.directive('ngReallyClick', [function() {
		return {
			restrict: 'A',
			link: function(scope, element, attrs) {
				element.bind('click', function() {
					var message = attrs.ngReallyMessage;
					if (message && confirm(message)) {
						scope.$apply(attrs.ngReallyClick);
					}
				});
			}
		};
	}]);
	
	//registering factories:
	app.factory('homeFactory', factories.homeFactory);
	
	var compareTo = function() {
	    return {
	        require: "ngModel",
	        scope: {
	            otherModelValue: "=compareTo"
	        },
	        link: function(scope, element, attributes, ngModel) {
	             
	            ngModel.$validators.compareTo = function(modelValue) {
	                return modelValue == scope.otherModelValue;
	            };
	 
	            scope.$watch("otherModelValue", function() {
	                ngModel.$validate();
	            });
	        }
	    };
	};
	 
	app.directive("compareTo", compareTo);
	
	app.run(function(){
		$('.loading').animate({'opacity': 0},400, function() {
		    $('.loading').remove();
		    console.info('Loading complete.');
		});
	});
	
	angular.bootstrap(document, [ 'Administration' ]);
	
});