// Ionic Starter App

// angular.module is a global place for creating, registering and retrieving Angular modules
// 'starter' is the name of this angular module example (also set in a <body> attribute in index.html)
// the 2nd parameter is an array of 'requires'
// 'starter.services' is found in services.js
// 'starter.controllers' is found in controllers.js
angular.module('zyber', ['ionic','ionic.service.core', 'zyber.controllers', 'zyber.services', 
  'pascalprecht.translate', 'ngCordova'])
.run(function($ionicPlatform) {
  $ionicPlatform.ready(function() {
    // Hide the accessory bar by default (remove this to show the accessory bar above the keyboard
    // for form inputs)
    if (window.cordova && window.cordova.plugins && window.cordova.plugins.Keyboard) {
      cordova.plugins.Keyboard.hideKeyboardAccessoryBar(true);
      cordova.plugins.Keyboard.disableScroll(true);
    }
    if (window.StatusBar) {
      // org.apache.cordova.statusbar required
      StatusBar.styleDefault();
    }
  });
})
.run(function($rootScope, HomeFactory, HomeService, $ionicModal, $window) {
  $rootScope.$on('$stateChangeStart', function(event, toState, toParams, fromState, fromParams) {
    console.log("To state: ");
    console.log(toState.name);
    if(toState.name === "tab.home"){
      HomeFactory.setView("home");
    }else if(toState.name === "tab.shares"){
      HomeFactory.setView("shares");
    }
  });
   $ionicModal.fromTemplateUrl('modalLogin.html', {
      scope: $rootScope,
      animation: 'slide-in-up',
      backdropClickToClose : false,
      hardwareBackButtonClose : false,
      focusFirstInput :true
    }).then(function(modal) {
      $rootScope.modal = modal;
      if (!$window.sessionStorage.token) {
        $rootScope.modal.show();
      }
    });

    $rootScope.signIn = function(user){
      HomeService.authenticate(user.username, user.password).then(function(){
      $rootScope.modal.hide();
      window.location.reload(true);
    }, function(error){
      $ionicPopup.alert({
          template: 'Invalid credentials',
          okType: 'button-assertive'
      });
    });
    };
})
.config(function($stateProvider, $urlRouterProvider) {
  $stateProvider

  // setup an abstract state for the tabs directive
    .state('tab', {
    url: '',
    abstract: true,
    templateUrl: 'templates/tabs.html'
  })

  // Each tab has its own nav history stack:

  .state('tab.home', {
    cache: false,
    url: '/home/:name',
    views: {
      'tab-home': {
        templateUrl: 'templates/tab-home.html',
        controller: 'HomeCtrl'
      }
    }
  })

  .state('tab.shares', {
      cache: false,
      url: '/shares/:name',
      views: {
        'tab-shares': {
          templateUrl: 'templates/tab-home.html',
          controller: 'HomeCtrl'
        }
      }
    });

  $urlRouterProvider.otherwise('/home/');

})
.config(['$translateProvider',  
                function($translateProvider) {
    	$translateProvider.useUrlLoader('http://192.168.1.4:9000/api/usermessages');
      $translateProvider.preferredLanguage("_");//endpoint returns users language
      $translateProvider.useSanitizeValueStrategy('escape');
}])
.factory('authInterceptor', function ($rootScope, $q, $window) {
  return {
    request: function (config) {
      config.headers = config.headers || {};
      if ($window.sessionStorage.token) {
        config.headers.bearer = $window.sessionStorage.token;
      }
      return config;
    },
    response: function (response) {
      console.log("Response: ");
      console.log(response);
      if (response.status === 401) {
        // handle the case where the user is not authenticated
        console.log("Showing login?");
        //$rootScope.modal.show();
      }
      return response || $q.when(response);
    }
  };
})
.config(function ($httpProvider) {
  $httpProvider.interceptors.push('authInterceptor');
});


