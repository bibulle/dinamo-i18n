angular
		.module('property', [ 'propertyBackend', 'uploadManager', 'ngSanitize' ])
		.config(function($routeProvider) {
			$routeProvider.when('/', {
				controller : ListCtrl,
				templateUrl : '/assets/partials/list.html'
			}).otherwise({
				redirectTo : '/'
			});
		}).directive('propertyChange', function() {
			return function(scope, elem, attrs) {
				scope.$watch(attrs.propertyChange, function(newVal, oldVal) {
					scope.saveProperty(newVal, oldVal);
				}, true);
			};
		}).directive('message', function($timeout, $log) {
			return {
				controller : function($scope, $element, $rootScope) {
					$scope.message = {
						text : "",
						type : "info",
						date : new Date()
					}
					$element.alert();

					$scope.$watch('message', function(newVal, oldVal) {
						if (newVal.text != "") {
							if (angular.isDefined($scope.messageTimout)) {
								$timeout.cancel($scope.messageTimout);
							}
							// $element.show();
							if ($scope.message.type == 'error') {
								$scope.messageTimout = $timeout(function() {
									$scope.message.text = "";
								}, 25000, true);
							} else {
								$scope.messageTimout = $timeout(function() {
									$scope.message.text = "";
								}, 5000, true);
							}
						}
					}, true);
				},
			}
		}).directive('valueBlur', function($timeout) {
			return function(scope, elem, attrs) {
				elem.bind('blur', function() {
					if (!scope.$$phase) {
						scope.$apply(attrs.valueBlur);
					} else {
						$timeout(function() {
							elem[0].focus();
						}, 0, false);
					}
				});
			};
		}).directive('valueFocus', function valueFocus($timeout) {
			return function(scope, elem, attrs) {
				scope.$watch(attrs.valueFocus, function(newVal) {
					if (newVal) {
						$timeout(function() {
							elem[0].focus();
						}, 0, false);
					}
				});
			};
		}).directive('upload', [ 'uploadManager', function factory(uploadManager) {
			return {
				restrict : 'A',
				link : function(scope, element, attrs) {
					$(element).fileupload({
						dataType : 'text',
						add : function(e, data) {
							uploadManager.add(data);
						},
						progressall : function(e, data) {
							var progress = parseInt(data.loaded / data.total * 100, 10);
							uploadManager.setProgress(progress);
							if (progress == 100) {
								angular.element('#ImportForm').modal('hide');
							}
						},
						done : function(e, data) {
							uploadManager.setProgress(0);
						}
					});
				}
			};
		} ]).run(
				function($rootScope, $location, $log, $timeout) {
					$rootScope.conf = {
						nb_langage_by_row : 3,
						langages : [ "English", "French", "Spanish", "German", "Japanese",
								"Korean", "Portuguese", "Italian", "Chinese" ],
						locals : [ "en", "fr", "es", "de", "ja", "ko", "pt", "it", "zh" ],
					}

					$rootScope.setOrder = function(key) {
						if ($rootScope.order != key) {
							$rootScope.reverse = false;
						} else {
							$rootScope.reverse = !$rootScope.reverse;
						}
						$rootScope.order = key;
					};
					$rootScope.setMessageSuccess = function(message) {
						$rootScope.message.text = message;
						$rootScope.message.date = new Date();
						$rootScope.message.type = 'success';
					};
					$rootScope.setMessageInfo = function(message) {
						$rootScope.message.text = message;
						$rootScope.message.date = new Date();
						$rootScope.message.type = 'info';
					};
					$rootScope.setMessageError = function(message) {
						$rootScope.message.text = message;
						$rootScope.message.date = new Date();
						$rootScope.message.type = 'error';
					};
					$rootScope.appendMessageError = function(message) {
						if ($rootScope.message.text != "") {
							$rootScope.message.text += "<BR>";
						}
						$rootScope.message.text += message;
						$rootScope.message.date = new Date();
						$rootScope.message.type = 'error';
					};

					$rootScope.importProperties = function() {
						angular.element('#ImportForm').modal('show');
					}

					$rootScope.addProperty = function() {
						$rootScope.$broadcast('addProperty');
					}
					
					// Manage the WebSocket
					$rootScope.connectWebSocket = function() {
			    	var ws = new WebSocket("ws://"+$location.host()+":"+$location.port()+"/webSocket");
			    	
			    	ws.onopen = function() {
			    		console.log("Socket has been opened");
			    		$rootScope.refreshNeeded = true;
			    		$rootScope.$broadcast('properties_refresh_needed');
			    	}
			    	ws.onclose = function() {
			    		console.log("Socket has been closed");
			    		$rootScope.setMessageError("Connection to server lost");
			    		$timeout(function() {
			    			$rootScope.connectWebSocket();
							}, 5000, true);

			    	}
			    	ws.onmessage = function(message) {
			    		var data = JSON.parse(message.data);
			    		console.log("Socket message received");
			    		if (data.action == 'noRefreshNeeded') {
			    			$rootScope.refreshNeeded = false;
			    		}
			    		$rootScope.$broadcast('properties_'+data.action, data.property);
			    	}
			    	
			    	$rootScope.$on('properties_refresh_needed', function(event) {
			    		if ($rootScope.refreshNeeded) {
				    		msg = {
				    				action: "refresh",
	                  lastUpdateDate: $rootScope.lasUpdateDate,
	                  lastUpdateId: $rootScope.lasUpdateId,
	                  count: 30
	              };
				    		ws.send(JSON.stringify(msg));
				    		
			    		}
			    	});
			    } 
					$rootScope.connectWebSocket();

				}).controller('FileUploadCtrl', [ '$scope', '$rootScope', 'uploadManager', FileUploadCtrl ]);

function FileUploadCtrl($scope, $rootScope, uploadManager) {
	$scope.files = [];
	$scope.percentage = 0;

	$scope.upload = function() {
		uploadManager.upload();
		$scope.files = [];
	};

	$rootScope.$on('fileAdded', function(e, call) {
		$scope.files.push(call);
		$scope.$apply();
	});

	$rootScope.$on('uploadProgress', function(e, call) {
		$scope.percentage = call;
		$scope.$apply();
	});
}


function ListCtrl($scope, PropertyBackend, $rootScope, $timeout, $log) {

	$scope.Math = window.Math;

	$rootScope.order = 'akey';
	$rootScope.reverse = false;
	$rootScope.lasUpdateDate = 0;

	$scope.properties = []; // PropertyBackend.query();
	$scope.properties_table_id = {};
	$scope.properties_table_key = {};
	
	// Centralized properties
	$scope.updateProperties = function(property) {
		
		// Add a new empty property
		if (property == null) {
			var newProp = {};
			newProp.akey = "";
			newProp.values = [];
			for ( var i in $rootScope.conf.langages) {
				newProp.values.push({});
				newProp.values[i].value="";
			}
			$scope.properties.push(newProp);
			return newProp;
		}
		
		// add or update a property
		oldProperty = $scope.properties_table_id[property.id];
		
		if (!angular.isUndefined(oldProperty)) {
			oldProperty.id = property.id;
			oldProperty.akey = property.akey;
			oldProperty.updateDate = property.updateDate;
			oldProperty.recent = property.recent;
			oldProperty.values = property.values;
		} else {
			$scope.properties.push(property);
			$scope.properties_table_id[property.id] = property;
			oldProperty = property;
		}

		if (property.updateDate > $rootScope.lasUpdateDate) {
			$rootScope.lasUpdateDate = property.updateDate;
			$rootScope.lasUpdateId   = property.id;
		} else if ((property.updateDate == $rootScope.lasUpdateDate) && 
						   ($rootScope.lasUpdateId < property.id)) {
			$rootScope.lasUpdateId   = property.id;
		}

		return oldProperty;
	}
	
	
	$scope.$on('properties_save', function(event, property) {
		$timeout.cancel($rootScope.loadingTimout);
		if (!$rootScope.loading) {
			$rootScope.loading = true;
			if (!$rootScope.$$phase) {
				$rootScope.$digest();
			}
		}
		$rootScope.loadingTimout = $timeout(function() {
			$rootScope.loading = false;
			if (!$rootScope.$$phase) {
				$rootScope.$digest();
			}
			if ($rootScope.refreshNeeded) {
				$timeout(function() {
					$rootScope.$broadcast('properties_refresh_needed');
				}, 0, true);
			}
		}, 10, true);
		$scope.updateProperties(property);
		

	});
	$scope.$on('properties_delete', function(event, property) {
		var found = false;
		for ( var oldProp in $scope.properties) {
			if ($scope.properties[oldProp].id == property.id) {
				found = true;
				break;
			}
		}
		if (found) {
			$scope.properties.splice(oldProp, 1);
			delete $scope.properties_table_id[property.id];
		}
		if (!$scope.$$phase) {
			$scope.$digest();
		}
	});
	
	$scope.$on ('addProperty', function(event) {
		newProp = $scope.updateProperties(null);
		$scope.editedValue = newProp;
	});

	// $scope.$watch('properties', function (newValue, oldValue, scope) {
	// alert("properties changed "+newval)
	// }, true);

	$scope.hasEdit = false;
	$scope.editedValue = null;

	$scope.editProp = function(prop) {
		$scope.editedValue = prop;
	};

	$scope.editValue = function(value) {
		$scope.editedValue = value;
	};

	$scope.doneEditing = function(property, value) {
		// Clean
		property.akey = property.akey.trim();

		if (!angular.isUndefined(value)) {
			if (angular.isUndefined(value.value) || (value.value == null)) {
				value.value = "";
			}
			value.value = value.value.trim();
		}

		// Surface check
		if (property.akey == "") {
			$scope.setMessageError("Key cannot be empty");
			$scope.editedValue = property;
			return;
		}
		
		// Ready to save
		$scope.hasEdit = true;
		$scope.editedValue = null;
		
		// save
		newProperty = PropertyBackend.save(property, function() {
			$scope.setMessageSuccess("Saved (" + property.akey + ")");
			property.id = newProperty.id;
			property.akey = newProperty.akey;
			property.recent = newProperty.recent;
			property.updateDate = newProperty.updateDate;
			property.values = newProperty.values;
			$scope.properties_table_id[property.id] = property;
		}, function() {
			$scope.setMessageError("cannot saved (" + property.akey + ")");
		});
	};

	$scope.saveProperty = function(property, oldVal) {
		// alert(property);
		// if ($scope.hasEdit) {
		// property.$save();
		// }
	}

	$scope.deleteProp = function(property, event) {
		event.stopPropagation();
		
		if (!angular.isDefined(property.id)) {
			var index = $scope.properties.indexOf(property);
			$scope.properties.splice(index, 1);
		} else {
			$scope.deletedProperty = property;
		}
	};

	$scope.reallyDeletedProperty = function(property) {
		PropertyBackend.delete(property, function() {
			$scope.deletedProperty = null;
			
			// var index = $scope.properties.indexOf(property);
			// $scope.properties.splice(index, 1);
			
			$scope.setMessageSuccess("Deleted (" + property.akey + ")");
		}, function() {
			$scope.deletedProperty = null;
			$scope.setMessageError("Cannot delete (" + property.akey + ")");
		});
	};

	$scope.$watch('deletedProperty', function(newVal, oldVal) {
		if (angular.isDefined(newVal) && (newVal != null)) {
			// $log.info("deleteProp show");
			$('#deleteValidationModal').modal('show');
		} else {
			// $log.info("deleteProp hide");
			$('#deleteValidationModal').modal('hide');
		}
	});
}
