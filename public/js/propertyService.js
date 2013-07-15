// Modules to managed timeline
angular.module('propertyBackend', ['ngResource']).
  factory('PropertyBackend', function($resource, $location) {
                                                     
    var PropertyBackend= $resource('/property/:id');
    
    return PropertyBackend;
  });
