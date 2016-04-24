define([], function() {
	var Controller = function($scope,$rootScope,$location,$timeout,$window) {
		$scope.location = $location;
	 	$scope.extend = function  () {
	         $('body').toggleClass('extended');
	         $('.sidebar').toggleClass('ps-container');	
	         $rootScope.$broadcast('resize');
	 	};

	 	if ($(window).width()<840) {
 	        $('body').removeClass('extended');
 	    }
	 	if ($(window).width()<1200) {
 	        $('body').removeClass('extended');
 	    }
 	    
    	var w = angular.element($window);
      
    	w.bind('resize', function () {
	   	 	if ($(window).width()<840) {
    	        $('body').removeClass('extended');
    	    }
		 	if ($(window).width()<1200) {
	 	        $('body').removeClass('extended');
	 	    }
    	});   
	}
	Controller.$inject = [ '$scope',
							'$rootScope',
	                       	'$location',
	                       	'$timeout',
	                       	'$window'
	                      ];
	return Controller;
});