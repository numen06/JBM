app.controller('SourceListCtrl', [ '$scope', 'toaster', '$modal', function($scope, toaster, $modal) {
	$scope.fileInfo = {
		expiredTime : -1,
		originaId : ""
	};
	$scope.toaster = toaster;
	$scope.files = [];
	$scope.uploadFile = function() {
		$scope.processDropzone();
	};

	$scope.reset = function() {
		$scope.resetDropzone();
	};

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

	$scope.releaseStart = function(path, sourceInfoName) {
		var modalInstance = $modal.open({
			templateUrl : "release.html",
			controller : 'ReleaseCtrl',
			resolve : {
				'sourceInfoName' : function() {
					return sourceInfoName;
				},
				'path' : function() {
					return path;
				},
				'scopeMain' : function() {
					return $scope;
				},
				'url' : function() {
					return 'source/releaseDirStart';
				}
			}
		});
		/*
		 * $.rest({ url : 'source/releaseStart', data : { sourceInfoName :
		 * sourceInfoName }, success : function(data) { toaster.pop("success",
		 * "发布程序", data.message); $scope.search(); }, });
		 */
	}

	$scope.release = function(path, sourceInfoName) {
		var modalInstance = $modal.open({
			templateUrl : "release.html",
			controller : 'ReleaseCtrl',
			resolve : {
				'sourceInfoName' : function() {
					return sourceInfoName;
				},
				'path' : function() {
					return path;
				},
				'scopeMain' : function() {
					return $scope;
				},
				'url' : function() {
					return 'source/releaseDir';
				}
			}
		});
	}

	$scope.groupDir = [];

	$scope.model = {};

	$scope.initGroupDir = function() {
		$.rest({
			url : 'source/getSourceDir',
			success : function(data) {
				if (data.status) {
					$scope.groupDir = data.result;
				}
			}
		});
	}

	$scope.initGroupDir();

	$scope.remove = function(path, sourceInfoName) {
		bootbox.confirm('你确定要删除此资源包?', function(result) {
			if (!result) {
				return;
			}
			$.rest({
				url : 'source/remove',
				data : {
					'path' : path,
					'sourceInfoName' : sourceInfoName
				},
				success : function(data) {
					toaster.pop("success", "删除程序", data.message);
					$scope.search();
				},
			});
		});
	}

	$scope.search = function() {
		var data = $("#searchForm").serializeObject();
		// alert($scope.model);
		$("#table").loadList({
			url : '/source/search',
			params : $scope.model,
			pageSize : 20,
			template : 'program_list_tpl',
			loading : false,
			success : function(data, table) {
				$scope.list = data.result.list;
				toaster.pop("success", "查询列表", data.message);
			},
			afterRender : function(dom) {
				var $div = dom[0];
				angular.element(document).injector().invoke(function($compile) {
					$compile($div)($scope);
				});
			}
		});
	}

	$scope.search();

} ]);

app.controller('ReleaseCtrl', function($scope, $modalInstance, $state, toaster, sourceInfoName, path, scopeMain, url) {

	$scope.toaster = toaster;

	var setting = {
		callback : {
			onClick : zTreeOnClick
		}
	};

	function zTreeOnClick(event, treeId, treeNode) {
		if (treeNode.rootFolder) {
			return;
		}
		$scope.dirname = treeNode.name;
		$scope.$apply();
	}
	;

	$scope.dirname = 'default';
	$scope.open = true;

	$scope.init = function() {
		$.ajax({
			url : "source/getRootDir",
			type : "POST",
			dataType : "json",
			headers : {
				'Content-Type' : 'application/json;charset=utf-8'
			},
			data : JSON.stringify({
				'path' : path,
				'sourceInfoName' : sourceInfoName
			}),
			success : function(resp) {
				$scope.tree = resp.result;
				$.fn.zTree.init($("#treeDemo"), setting, $scope.tree);
			}
		});
	}
	$scope.init();

	$scope.ok = function() {
		$.alert({
			title : '发布程序',
			icon : 'fa fa-paper-plane-o',
			theme : 'modern',
			content : function() {
				var self = this;
				// self.setContent('正在启动中');
				$.rest({
					'url' : url,
					data : {
						'path' : path,
						'dirname' : $scope.dirname,
						'open' : $scope.open,
						'sourceInfoName' : sourceInfoName
					},
				}).done(function(response) {
					var zt = response.success ? 'success' : 'danger';
					self.setIcon(zt + ' ' + self.icon);
					self.setContent(response.message);
				}).fail(function() {
					self.setContent('Something went wrong.');
				});
			},
			onContentReady : function() {
				scopeMain.search();
			}
		});
		$modalInstance.close();
	}

	$scope.cancel = function() {
		$modalInstance.close();
	}
});
