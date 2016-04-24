/*global require, requirejs */
/*jshint smarttabs:true */
requirejs.config({
	paths : {
		'angular' : [ '/bowerassets/bower/angularjs/angular' ],
		'angular-animate' : [ '/bowerassets/bower/angular-animate/angular-animate'],
		'angular-aria' : [ '/bowerassets/bower/angular-aria/angular-aria'],
		'angular-route' : [ '/bowerassets/bower/manual/angular-route' ],
		'ui-bootstrap': [ '/bowerassets/bower/manual/ui-bootstrap-tpls' ],
		'angular-ui-contextMenu': [ '/bowerassets/lib/angular-ui-contextMenu' ],
		'angular-messages': [ '/bowerassets/bower/angular-messages/angular-messages'],
		'angular-ui-notification': [ '/bowerassets/bower/angular-ui-notification/angular-ui-notification'],
		'angular-translate' : [ '/bowerassets/bower/angular-translate/angular-translate' ],
		'angular-translate-loader-url' : [ '/bowerassets/bower/manual/angular-translate-loader-url' ],
		'angular-material' : [ '/bowerassets/bower/angular-material/angular-material'],
		'angular-material-data-table' : [ '/bowerassets/bower/angular-material-data-table/md-data-table.min'],
		'ng-flow' : [ '/bowerassets/bower/manual/ng-flow-standalone' ],
		'ng-context-menu' : [ '/bowerassets/bower/ng-context-menu/ng-context-menu' ],
		'angular-tree-control' : [ '/bowerassets/lib/angular-tree-control/angular-tree-control' ],
		'mousetrap' : [ '/assets/lib/mousetrap/mousetrap' ] //no suitable bower component

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
			deps : [ 'angular' ],
			exports : 'angular'
		},
		'ui-bootstrap' : {
			deps : [ 'angular' ]
		},
		'angular-ui-contextMenu' : {
			deps : [ 'angular' ]
		},
		'angular-ui-notification' : {
			deps : [ 'angular' ]
		},
		'angular-translate' : {
			deps : [ 'angular' ]
		},
		'angular-translate-loader-url' : {
			deps : [ 'angular-translate' ]
		},
		'angular-material' : {
			deps : [ 'angular' ]
		},
		'angular-material-data-table' : {
			deps : [ 'angular-material' ]
		},
		'ng-flow' : {
			deps : [ 'angular' ]
		},
		'ng-context-menu' : {
			deps : [ 'angular' ]
		},
		'angular-tree-control' : {
			deps : [ 'angular']
		}
	}
});

require([ 'angular', './home/controllers', './home/services',  './home/factories',
          'angular-route', 'ui-bootstrap', 'angular-ui-contextMenu',
          'angular-ui-notification','angular-messages',
          'angular-translate', 'angular-translate-loader-url','angular-animate','angular-aria',
          'angular-material','angular-material-data-table','ng-flow','ng-context-menu',
          'angular-tree-control', 'mousetrap'], 
		function(angular, controllers, services, factories, angularRoute, uiBootstrap, angularAnimate,
				angularAria,angularMaterial,angularMaterialDataTable,ngContextMenu) {

	'use strict';

	// Declare app level module

	var app = angular.module(
			'Home',
			[ 'ngRoute', 'ui.bootstrap', 
			  'ui.bootstrap.contextMenu',
			  'pascalprecht.translate',
			  'ui-notification',
			  'ngAnimate',
			  'ngAria',
			  'md.data.table',
			  'ngMaterial',
			  'flow',
			  'ng-context-menu',
			  'treeControl',
			]);
	//configuration
	app.config(function($routeProvider, $locationProvider) {
		$locationProvider.html5Mode(true);
		$routeProvider.when('/home', {
			controller : controllers.homeController,
			templateUrl : '/assets/partials/home.html'
		}).when('/home/:name', {
			controller : controllers.homeController,
			templateUrl : '/assets/partials/home.html'
		}).when('/shares', {
			controller : controllers.homeController,
			templateUrl : '/assets/partials/home.html'
		}).when('/shares/:name', {
			controller : controllers.homeController,
			templateUrl : '/assets/partials/home.html'
		}).when('/trash', {
			controller : controllers.trashController,
			templateUrl : '/assets/partials/trash.html'
		}).when('/trash/:name*', {
			controller : controllers.trashController,
			templateUrl : '/assets/partials/trash.html'
		}).when('/home/versions/:name', {
			controller : controllers.versionController,
			templateUrl : '/assets/partials/manage-versions.html'
		}).when('/shares/versions/:name', {
			controller : controllers.versionController,
			templateUrl : '/assets/partials/manage-versions.html'
		}).when('/home/search/:name', {
			controller : controllers.searchController,
			templateUrl : '/assets/partials/search-results.html'
		}).when('/shares/search/:name', {
			controller : controllers.searchController,
			templateUrl : '/assets/partials/search-results.html'
		}).otherwise({
			redirectTo: '/home'
		});
	});
	
	app.config(function(NotificationProvider) {
        NotificationProvider.setOptions({
            positionX: 'right'
        });
    });
	
	app.config(['$translateProvider',  
                function($translateProvider) {
    	$translateProvider.useUrlLoader('/api/usermessages');
        $translateProvider.preferredLanguage("_");//endpoint returns users language
        $translateProvider.useSanitizeValueStrategy('escape');
    }]);
	
	//registering controllers:
	app.controller("createFolderController", controllers.createFolderController);
	app.controller("uploadController", controllers.uploadController);
	app.controller("shareFileController", controllers.shareFileController);
	app.controller("shareFolderController", controllers.shareFolderController);
	app.controller("versionController", controllers.versionController);
	app.controller("trashController", controllers.trashController);
	app.controller("securityController", controllers.securityController);
	app.controller("homectrl", controllers.homeController);
	app.controller("sidebarctrl", controllers.sidebarController);
	app.controller("headerctrl", controllers.headerController);
	app.controller("selectFolderController", controllers.selectFolderController);
	app.controller("showFileController", controllers.showFileController);

	//registering services:
	app.service('homeService', services.homeService);
	app.service('versionService', services.versionService);
	app.service('trashService', services.trashService);

	//TODO Not used
	app.filter('encode', function() {
		  return window.encodeURIComponent;
	});
	
	//registering factories:
	app.factory('homeFactory', factories.homeFactory);
	app.factory('uploadFactory', factories.uploadFactory);

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
	
	app.directive('focusMe', function($timeout, $parse) {
		  return {
		    //scope: true,   // optionally create a child scope
		    link: function(scope, element, attrs) {
		      var model = $parse(attrs.focusMe);
		      scope.$watch(model, function(value) {
		        if(value === true) { 
		          $timeout(function() {
		            element[0].focus(); 
		          });
		        }
		      });
		    }
		  };
		});
	//TODO
	app.directive( 'whenActive', function ( $location ) {
	    return {
	        scope: true,
	        link: function ( scope, element, attrs ) {
	            scope.$on( '$routeChangeSuccess', function () {
	                if ( $location.path() == element.attr( 'href' ) ) {
	                    element.addClass( 'active' );
	                }
	                else {
	                    element.removeClass( 'active' );
	                }
	            });
	        }
	    };
	});
	
	//extra config
	app.config(['flowFactoryProvider', function (flowFactoryProvider) {
	    flowFactoryProvider.defaults = {
	    		//UUID generator on client.
	    		generateUniqueIdentifier: function(){
	    			return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
	    			    var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
	    			    return v.toString(16);
	    			});
	    		},
	    		permanentErrors: [403, 404, 415, 500]//501
	    	};
	    }]);
	
	app.run(['$rootScope','homeFactory', function($rootScope, homeFactory) {
		$('.loading').animate({'opacity': 0},400, function() {
		    $('.loading').remove();
		    console.info('Loading complete.');
		});
		
		$rootScope.$on("$routeChangeSuccess", function(event, next, current) {
//			console.log(next.$$route);
			//TODO improve
			if(next.$$route.originalPath.match(/^\/home\/(?:(.+?))$|^\/home$/))
				homeFactory.setView("home");
			else if(next.$$route.originalPath.match(/^\/shares\/(?:(.+?))$|^\/shares/))
				homeFactory.setView("shares");
		}
		)}]);
	app.filter('bytes', function() {
		return function(bytes, precision) {
			if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) return '-';
			if (typeof precision === 'undefined') precision = 1;
			var units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'],
				number = Math.floor(Math.log(bytes) / Math.log(1024));
			return (bytes / Math.pow(1024, Math.floor(number))).toFixed(precision) +  ' ' + units[number];
		}
	});
	app.filter('renameTrashToArchive', function() {
		return function(crumb) {
			if (crumb != 'Trash') {return crumb;}
			else {return 'Archive';}
		}
	});
	angular.bootstrap(document, [ 'Home' ]);
	
});