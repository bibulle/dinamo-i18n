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
				function($rootScope, $location, $log) {
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

					$rootScope.isNewer = function(updateDate) {
						// $log.info("---- "+updateDate+"<->"+(new
						// Date()).getTime()+" "+(((new Date()).getTime() -
						// updateDate) < 1000*10));
						return ((new Date()).getTime() - updateDate) < 60000;
					}
					
					$rootScope.addProperty = function() {
						$rootScope.$broadcast('addProperty');
					}

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

	$scope.properties = [];
	$scope
			.$on(
					'refreshProperties',
					function(event) {
						// cancel previous timeout
						$timeout.cancel($scope.timeoutId);
						// Next in 10 secondes
						$scope.timeoutId = $timeout(function() {
							$rootScope.$broadcast('refreshProperties');
						}, 60000);
						// Load
						$timeout(
								function() {
									// $log.info('--' + new Date());
									var newProperties = PropertyBackend
											.query(function() {
												// $log.info(new Date());
												var cpt = 0;
												var found = false;
												for ( var newProp in newProperties) {
													for ( var oldProp in $scope.properties) {
														if (newProperties[newProp].id == $scope.properties[oldProp].id) {
															$scope.properties[oldProp].akey = newProperties[newProp].akey;
															$scope.properties[oldProp].updateDate = newProperties[newProp].updateDate;
															$scope.properties[oldProp].values = newProperties[newProp].values;
															cpt++;
															found = true;
															break;
														}
													}
													if (!found) {
														$scope.properties.push(newProperties[newProp]);
														cpt++;
													}
													found = false;
													// if ((cpt % 10 == 0) &&
													// (!$scope.$$phase)) {
													// $scope.$digest();
													// }
												}
												for ( var oldProp in $scope.properties) {
													found = false;
													for ( var newProp in newProperties) {
														if (newProperties[newProp].id == $scope.properties[oldProp].id) {
															found = true;
															break;
														}
													}
													if (!found) {
														if ($scope.editedValue != $scope.properties[oldProp]) {
															$scope.properties.splice(oldProp, 1);
															cpt++;
														}
													}
													// if ((cpt % 10 == 0) &&
													// (!$scope.$$phase)) {
													// $scope.$digest();
													// }
												}
												// $log.info(new Date());
												// if (!$scope.$$phase) {
												// $scope.$digest();
												// }
												// $log.info(new Date());
											});
									// $log.info('--' + new Date());
								}, 0, false);
					});

	$rootScope.$broadcast('refreshProperties');
	
	$scope.$on ('addProperty', function(event) {
			var newProp = {};
			newProp.akey = "";
			newProp.values = [];
			for ( var i in $rootScope.conf.langages) {
				newProp.values.push({});
				newProp.values[i].value="";
			}
			$scope.properties.push(newProp);
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
		PropertyBackend.save(property, function() {
			$scope.setMessageSuccess("Saved (" + property.akey + ")");
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
			
			var index = $scope.properties.indexOf(property);
			$scope.properties.splice(index, 1);
			
			$scope.setMessageSuccess("Deleted (" + property.akey + ")");
		}, function() {
			$scope.deletedProperty = null;
			$scope.setMessageError("Cannot delete (" + property.akey + ")");
		});
	};

	$scope.$watch('deletedProperty', function(newVal, oldVal) {
		if (angular.isDefined(newVal) && (newVal != null)) {
			//$log.info("deleteProp show");
			$('#deleteValidationModal').modal('show');
		} else {
			//$log.info("deleteProp hide");
			$('#deleteValidationModal').modal('hide');
		}
	});
}
