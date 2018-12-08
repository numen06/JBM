app.controller('DocumentUploadCtrl', [ '$scope', 'toaster', function($scope, toaster) {
	$scope.fileInfo = {
		expiredTime : -1,
		originaId : ""
	};
	$scope.files = [];
	$scope.uploadFile = function() {
		$scope.processDropzone();
	};

	$scope.reset = function() {
		$scope.resetDropzone();
	};
	$scope.toaster = toaster;

	$scope.dropzoneCallback = function(data) {
		if (data.json.status) {
			toaster.pop("success", "上传文件", data.json.message);
		} else {
			toaster.pop("error", "上传文件", data.json.message);
		}
	}

	$scope.uploadDocument = function() {
		$avatarForm = $("#documentForm");
		var url = $avatarForm.attr('action'), data = new FormData($avatarForm[0]), _this = this;
		$.ajax(url, {
			type : 'post',
			data : data,
			dataType : 'json',
			processData : false,
			contentType : false,
			success : function(data) {
				alert("上传文件成功:" + data.message);
				$scope.fileInfo = {};
			},
			error : function(XMLHttpRequest, textStatus, errorThrown) {
				alert("上传错误");
			}
		});
	};

	// $("#documentForm").spinbox({
	// value : -1,
	// min : -1,
	// max : 2147483648
	// });
} ]);