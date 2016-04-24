/*global require, requirejs */
/*jshint smarttabs:true */
requirejs.config({
	paths : {
		'angular' : [ '/assets/bower/angularjs/angular' ],
		'angular-animate' : [ '/assets/bower/angular-animate/angular-animate'],
		'angular-aria' : [ '/assets/bower/angular-aria/angular-aria'],
		'angular-route' : [ '/assets/bower/manual/angular-route' ],
		'ui-bootstrap': [ '/assets/bower/manual/ui-bootstrap-tpls' ],
		'angular-messages': [ '/assets/bower/angular-messages/angular-messages'],
		'angular-ui-notification': [ '/assets/bower/angular-ui-notification/angular-ui-notification'],
		'ui-router' : [ '/assets/bower/angular-ui-router/angular-ui-router' ],
		'angular-translate' : [ '/assets/bower/angular-translate/angular-translate' ],
		'angular-translate-loader-url' : [ '/assets/bower/manual/angular-translate-loader-url' ],
		'angular-material' : [ '/assets/bower/angular-material/angular-material'],
		'angular-material-data-table' : [ '/assets/bower/angular-material-data-table/md-data-table.min']
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
		}
	}
});

require([ 'angular', './settings/controllers', './admin/services', './home/services',
           './home/factories', 'ui-bootstrap',
          'angular-ui-notification','angular-messages', 'ui-router', 
          'angular-translate', 'angular-animate','angular-aria','angular-translate-loader-url','angular-sanitize','angular-material'], 
		function(angular, controllers, services, homeServices, factories,angularAnimate,angularAria,angularMaterial /*, angularRoute, uiBootstrap*/) {
	'use strict';

	// Declare app level module

	var app = angular.module(
			'Settings',
			[ 'ui.bootstrap', 
			  'ui-notification', 
			  'ui.router',
			  'ngAnimate',
			  'ngAria',
			  'ngMessages',
			  'pascalprecht.translate',
			  'ngSanitize',
			  'ngMaterial']);
	//configuration
	app.config([ "$stateProvider", "$urlRouterProvider", "$locationProvider",
              function($stateProvider, $urlRouterProvider /*, $locationProvider*/) {
//		$locationProvider.html5Mode(true);
		$urlRouterProvider.when("/", "/account");
		$urlRouterProvider.otherwise("/account");
		$stateProvider.state('settings', {
			url : "/",
			templateUrl : '/assets/partials/settings/main.html'
		}).state('settings.account',{
			url : 'account',
			templateUrl : '/assets/partials/settings/account.html',
			controller: controllers.accountController
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
	app.controller("accountController", controllers.accountController);
	app.controller("sidebarctrl", controllers.sidebarController);
	app.controller("headerctrl", controllers.headerController);

	//registering services:
	app.service('usersService', services.usersService);
	app.service('homeService', homeServices.homeService);
	
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
	
	app.run(function(){
		$('.loading').animate({'opacity': 0},400, function() {
		    $('.loading').remove();
		    console.info('Loading complete.');
		});
	});
	 
	app.directive("compareTo", compareTo);
	
	angular.bootstrap(document, [ 'Settings' ]);
	
});