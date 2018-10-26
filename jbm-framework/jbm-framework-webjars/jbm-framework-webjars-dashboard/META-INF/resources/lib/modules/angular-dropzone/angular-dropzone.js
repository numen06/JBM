angular.module('dropzone', []).directive('dropzone', function() {

	return function(scope, element, attrs) {
		var config = {
			url : attrs.action,
			maxFilesize : attrs.maxfilesize?attrs.maxfilesize:500,
			paramName : "file",
			uploadMultiple : attrs.uploadmultiple?attrs.uploadmultiple:true,
			maxThumbnailFilesize : attrs.maxthumbnailfilesize?attrs.maxthumbnailfilesize:5,
			previewsContainer : ".dropzone .well",
			clickable : ".dropzone .well",
			callback:attrs.callback,
			beforeLoadFile:attrs.beforeloadfile,
		};
		var eventHandlers = {
			success : function(file, json) {
				var dz = this;
				if (file.status == "success") {
					if (scope.toaster != null)
						scope.toaster.pop('success', '上传文件成功', file.name);
				}
				if(config.callback)
				{
					scope[config.callback]({file:file,json:json});
				}
				if (dz.files[config.maxThumbnailFilesize] != null) {
					angular.forEach(dz.files, function(dzfile, index) {
						if (index > (dz.files.length - 5))
							return;
						if (dzfile.status == "success") {
							dz.removeFile(dzfile);
						}
					});
				}
			},
			addedfile : function(file) {
				return false;
			},
			drop : function(file) {
			},
			error : function(file, json) {
				if (file.status == "error") {
					if (scope.toaster != null)
						scope.toaster.pop('error', '上传文件失败', file.name);
				}
			}
		};
		// config.beforeLoadFile=scope[config.beforeLoadFile]();
		if(config.beforeLoadFile)
		{
			dropzone = new Dropzone(element[0], config,function () {
				return scope[config.beforeLoadFile]();
			});
		}
		else
		{
			dropzone = new Dropzone(element[0], config);
		}



		angular.forEach(eventHandlers, function(handler, event) {
			dropzone.on(event, handler);
		});
		
		scope.dropzone = dropzone;

		scope.processDropzone = function() {
			dropzone.processQueue();
		};

		scope.resetDropzone = function() {
			dropzone.removeAllFiles();
		}

	};
});