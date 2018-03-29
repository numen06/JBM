app.controller('RecoveryListCtrl', [ '$scope', 'toaster', '$modal', function($scope, toaster, $modal) {

	$scope.toaster = toaster;

	$scope.pathFormat = function(path) {
		return path.replace(/\\/g, '\/');
	}

	$scope.model = {};
	$scope.reduction = function(recoveryPath, sourcePath, replace, type) {
		bootbox.confirm('你确定要还原此进程?', function(result) {
			if (!result) {
				return;
			}
			$.rest({
				url : "recovery/reduction",
				data : {
					'recoveryPath' : recoveryPath,
					'sourcePath' : sourcePath,
					'replace' : replace,
					'type' : type
				},
				success : function(resp) {
					if (resp.status) {
						$scope.search();
						toaster.pop("success", "还原", resp.message);
					} else {
						toaster.pop("error", "还原", resp.message);
					}
				}
			});
		});
	}

	$scope.search = function() {
		var data = $("#searchForm").serializeObject();
		$("#table").loadList({
			url : '/recovery/search',
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

	$scope.remove = function(recoveryPath, sourcePath, replace, type) {
		bootbox.confirm('你确定要彻底删除此文件吗?', function(result) {
			if (!result) {
				return;
			}
			$.rest({
				url : "recovery/remove",
				data : {
					'recoveryPath' : recoveryPath,
					'sourcePath' : sourcePath,
					'replace' : replace,
					'type' : type
				},
				success : function(resp) {
					if (resp.status) {
						$scope.search();
						toaster.pop("success", "彻底删除", resp.message);
					} else {
						toaster.pop("error", "彻底删除", resp.message);
					}
				}
			});
		});
	}

	$scope.search();

} ]);
