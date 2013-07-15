// Modules to managed timeline
angular.module('uploadManager', []).
  factory('uploadManager', function ($rootScope) {
    var _files = [];
    return {
        add: function (file) {
            _files.push(file);
            $rootScope.$broadcast('fileAdded', file.files[0].name);
        },
        clear: function () {
            _files = [];
        },
        files: function () {
            var fileNames = [];
            $.each(_files, function (index, file) {
                fileNames.push(file.files[0].name);
            });
            return fileNames;
        },
        upload: function () {
            $.each(_files, function (index, file) {
                file.submit()
                	.error(function (jqXHR, textStatus, errorThrown) {
                		$rootScope.appendMessageError(jqXHR.responseText);
                	})
                	.complete(function (result, textStatus, jqXHR) {
                		//$rootScope.$broadcast('refreshProperties');
                	});
            });
            this.clear();
        },
        setProgress: function (percentage) {
            $rootScope.$broadcast('uploadProgress', percentage);
        }
    };
  });
