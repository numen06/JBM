app.controller('DocumentListCtrl', [
	'$scope',
	'$http',
	'$compile',
	'toaster',
	'$modal',
	function($scope, $http, $compile, toaster, $modal) {
		$scope.fileInfo = {
			expiredTime : -1,
			originaId : ""
		};
		$scope.toaster = toaster;

		$scope.folderPath = null;

		$scope.addHoverDom = function(treeId, treeNode) {
			var aObj = $("#" + treeNode.tId + "_a");
			var path = treeNode.path.replace(/\\/g, "\/");
			if (!treeNode.folder) {
				if ($("#diyBtn_" + treeNode.id).length > 0)
					return;
				if ($("#diyBtn2_" + treeNode.id).length > 0)
					return;
				if ($("#diyDeleteBtn_" + treeNode.id).length > 0)
					return;
				if ($("#diyEditBtn_" + treeNode.id).length > 0)
					return;
				var editStr = "<a id='diyBtn_" + treeNode.id + "' ng-click='downloadFile(\"" + path + "\")' style='margin:0 0 0 5px;'>下载</a>" + "<a id='diyBtn2_" + treeNode.id
					+ "' ng-click='previewFile(\"" + path + "\")' style='margin:0 0 0 5px;'>预览</a>" + "<a id='diyDeleteBtn_" + treeNode.id + "' ng-click='deleteFile(\"" + path
					+ "\")' style='margin:0 0 0 5px;'>删除</a>";
				if (treeNode.edit) {
					editStr += "<a id='diyEditBtn_" + treeNode.id + "' ng-click='editFile(\"" + path + "\")' style='margin:0 0 0 5px;'>编辑</a>";
				}
			} else {
				if ($("#diyUploadBtn_" + treeNode.id).length > 0)
					return;
				var editStr = "<a id='diyUploadBtn_" + treeNode.id + "' ng-click='readPath(\"" + path + "\")' style='margin:0 0 0 5px;'>上传</a>" + "<a id='diyDirBtn_" + treeNode.id
					+ "' ng-click='makeDir(\"" + path + "\")' style='margin:0 0 0 5px;'>新建文件夹</a>";
				if (!treeNode.rootFolder) {
					editStr += "<a id='diyDeleteBtn_" + treeNode.id + "' ng-click='deleteFile(\"" + path + "\")' style='margin:0 0 0 5px;'>删除</a>"
				} else {
					editStr += "<a id='diyRenameBtn_" + treeNode.id + "' ng-click='renameFile(\"" + path + "\")' style='margin:0 0 0 5px;'>重命名</a>"
				}
			}
			aObj.append(editStr);
			$html = $compile(aObj)($scope);
		}

		$scope.removeHoverDom = function(treeId, treeNode) {
			if (!treeNode.folder) {
				$("#diyBtn_" + treeNode.id).unbind().remove();
				$("#diyBtn2_" + treeNode.id).unbind().remove();
				$("#diyDeleteBtn_" + treeNode.id).unbind().remove();
				if (treeNode.edit) {
					$("#diyEditBtn_" + treeNode.id).unbind().remove();
				}
			} else {
				$("#diyUploadBtn_" + treeNode.id).unbind().remove();
				$("#diyDirBtn_" + treeNode.id).unbind().remove();
				if (!treeNode.rootFolder) {
					$("#diyDeleteBtn_" + treeNode.id).unbind().remove();
				} else {
					$("#diyRenameBtn_" + treeNode.id).unbind().remove();
				}
			}
		}

		$scope.editFile = function(path) {
			$.ajax({
				url : "file/readFile",
				type : "POST",
				dataType : "json",
				headers : {
					'Content-Type' : 'application/json;charset=utf-8'
				},
				data : JSON.stringify({
					'filepath' : path
				}),
				success : function(resp) {
					if (resp.status) {
						var modalInstance = $modal.open({
							templateUrl : "edit.html",
							controller : 'EditCtrl',
							resolve : {
								'path' : function() {
									return path;
								},
								'scopeMain' : function() {
									return $scope;
								},
								'text' : function() {
									return resp.result;
								}
							}
						});
					}
				}
			});
		}

		$scope.recovery = function(path) {
			bootbox.confirm('你确定要删除此进程?', function(result) {
				if (!result) {
					return;
				}
				$.ajax({
					url : "program/recovery",
					type : "POST",
					dataType : "json",
					headers : {
						'Content-Type' : 'application/json;charset=utf-8'
					},
					data : JSON.stringify({
						'filepath' : path
					}),
					success : function(resp) {
						if (resp.status) {
							$scope.search();
							$scope.$apply();
							toaster.pop("success", "刪除进程", resp.message);
						} else {
							toaster.pop("error", "刪除进程", resp.message);
						}
					}
				});
			});
		}

		$scope.deleteFile = function(path) {

			bootbox.confirm('你确定要删除此文件?', function(result) {
				if (!result) {
					return;
				}
				$.ajax({
					url : "file/deleteFile",
					type : "POST",
					dataType : "json",
					data : {
						'path' : path
					},
					success : function(resp) {
						if (resp.status) {
							$scope.initTree();
							$scope.$apply();
							toaster.pop("success", "刪除", resp.message);
						} else {
							toaster.pop("error", "刪除", resp.message);
						}
					}
				});
			});
		}

		$scope.upload = null;

		$scope.readPath = function(path) {
			var modalInstance = $modal.open({
				templateUrl : "upload.html",
				controller : 'UploadCtrl',
				resolve : {
					'path' : function() {
						return path;
					},
					'scopeMain' : function() {
						return $scope;
					}
				}
			});
		}

		$scope.renameFile = function(path) {
			var modalInstance = $modal.open({
				templateUrl : "renameFile.html",
				controller : 'RenameCtrl',
				resolve : {
					'path' : function() {
						return path;
					},
					'scopeMain' : function() {
						return $scope;
					}
				}
			});
		}

		$scope.makeDir = function(path) {
			var modalInstance = $modal.open({
				templateUrl : "makeDir.html",
				controller : 'MakeDirCtrl',
				resolve : {
					'path' : function() {
						return path;
					},
					'scopeMain' : function() {
						return $scope;
					}
				}
			});
		}

		$scope.downloadFile = function(path) {
			// download("file/downloadFile", {
			// 'path' : path
			// })
			window.open("file/downloadFile?path=" + path);
		}

		$scope.previewFile = function(path) {
			// download("file/downloadFile", {
			// 'path' : path
			// })
			window.open("file/previewFile?path=" + path);
		}

		var setting = {
			view : {
				'addHoverDom' : $scope.addHoverDom,
				'removeHoverDom' : $scope.removeHoverDom
			}
		};

		$scope.toFileTree = function(folderPath) {
			$scope.folderPath = folderPath;
			$scope.initTree();
		}

		$scope.initTree = function() {
			$.ajax({
				url : "file/getTreeFile",
				type : "POST",
				dataType : "json",
				headers : {
					'Content-Type' : 'application/json;charset=utf-8'
				},
				data : JSON.stringify({
					'folderPath' : $scope.folderPath
				}),
				success : function(resp) {
					$scope.tree = resp.result;
					$.fn.zTree.init($("#treeDemo"), setting, $scope.tree);
				}
			});
		}

		// $scope.initTree();

		$scope.kill = function(folder) {
			$.alert({
				title : '关闭进程',
				icon : 'glyphicon glyphicon-eject',
				theme : 'modern',
				content : function() {
					var self = this;
					// self.setContent('正在启动中');
					return $.rest({
						url : '/program/kill',
						async : false,
						data : {
							folder : folder
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
					$scope.search();
				}
			});

		}

		$scope.scanning = function(folder) {
			$.alert({
				title : '扫描进程',
				icon : 'glyphicon glyphicon-eject',
				theme : 'modern',
				content : function() {
					var self = this;
					// self.setContent('正在启动中');
					return $.rest({
						url : '/program/scanning',
						async : false,
						data : {
							folder : folder
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
					$scope.search();
				}
			});
		}

		$scope.start = function(folder) {
			$.alert({
				title : '启动进程',
				icon : 'glyphicon glyphicon-play-circle',
				theme : 'modern',
				content : function() {
					var self = this;
					// self.setContent('正在启动中');
					return $.rest({
						url : '/program/start',
						data : {
							folder : folder
						}
					}).done(function(response) {
						var zt = response.success ? 'success' : 'danger';
						self.setIcon(zt + ' ' + self.icon);
						self.setContent(response.message);
					}).fail(function() {
						self.setContent('Something went wrong.');
					});
				},
				onContentReady : function() {
					$scope.search();
				}
			});
		}

		$scope.toProgram = function(folder, start) {
			if (start) {
				$scope.start(folder);
			} else {
				$scope.kill(folder);
			}
		}

		$scope.toGuard = function(folder, guard) {
			$.rest({
				url : '/program/toGuard',
				async : false,
				data : {
					folder : folder,
					guard : guard
				},
				success : function(data) {
					toaster.pop("success", "守护操作", data.message);
					$scope.search();
				},
			});
		}

		$scope.structure = function(mianFile, folder) {
			$.rest({
				url : '/program/structure',
				data : {
					mianFile : mianFile,
					folder : folder
				},
				success : function(data) {
					toaster.pop("success", "初始化程序", data.message);
					// $scope.search();
				},
			});
		}
		$scope.pids = [];

		$scope.lookgc = function(folder) {
			$.rest({
				url : 'program/getGcUtilMap',
				timeout : 0,
				data : {
					'folder' : folder
				},
				success : function(data) {
					if (data) {
						if (data.status) {
							// toaster.pop("success", "GC", data.message);
							var modalInstance = $modal.open({
								templateUrl : "gc.html",
								controller : 'GcCtrl',
								resolve : {
									'path' : function() {
										return folder;
									},
									'scopeMain' : function() {
										return $scope;
									},
									'res' : function() {
										return data.result;
									}
								}
							});
						} else {
							toaster.pop("error", "GC", data.message);
						}
					}
				},
			});
		}

		$scope.search = function() {
			$scope.pids = [];
			var data = $("#searchForm").serializeObject();
			$("#table").loadList({
				url : '/program/search',
				timeout : 60000,
				params : data,
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

		// 上傳
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
					$scope.initTree();
					$scope.fileInfo = {};
				},
				error : function(XMLHttpRequest, textStatus, errorThrown) {
					alert("上传错误");
				}
			});
		};

	} ]);

app.controller('EditCtrl', function($scope, $modalInstance, $state, toaster, path, scopeMain, text) {

	$scope.fileText = text;

	$scope.ok = function() {
		$.ajax({
			url : "file/writeFile",
			type : "POST",
			dataType : "json",
			headers : {
				'Content-Type' : 'application/json;charset=utf-8'
			},
			data : JSON.stringify({
				'text' : $scope.fileText,
				'filepath' : path
			}),
			success : function(resp) {
				if (resp.status) {
					$modalInstance.close();
					// scopeMain.initTree();
					$scope.$apply();
					toaster.pop("success", "编辑", "保存成功");
				} else {
					toaster.pop("error", "编辑", resp.message);
				}
			}
		});
	}

	$scope.cancel = function() {
		$modalInstance.close();
	}
});

app.controller('RenameCtrl', function($scope, $modalInstance, $state, toaster, path, scopeMain) {
	$scope.dirPath = path;
	$scope.dirName = null;

	$scope.ok = function() {
		$.ajax({
			url : "file/renameDir",
			type : "POST",
			dataType : "json",
			headers : {
				'Content-Type' : 'application/json;charset=utf-8'
			},
			data : JSON.stringify({
				'dirname' : $scope.dirName,
				'dirpath' : $scope.dirPath
			}),
			success : function(resp) {
				if (resp.status) {
					$modalInstance.close();
					scopeMain.toFileTree(resp.result);
					scopeMain.search();
					// scopeMain.initTree();
					toaster.pop("success", "修改目录", resp.message);
					$scope.$apply();
				} else {
					toaster.pop("error", "修改目录", resp.message);
				}
			}
		});

	}
});

app.controller('GcCtrl', function($scope, $http, $compile, $modalInstance, $state, toaster, path, scopeMain, res) {

	$scope.chartOptions = {
		series : {
			pie : {
				innerRadius : 0.9,
				show : true

			}
		},
		legend : {
			backgroundColor : "rgba(255,255,255,0)",
			labelFormatter : function(label, series) {
				return "<div style='    width: 140px;text-align: -webkit-right;'>" + label
					+ "<span style='color: blue;margin-right: 10px;font-size: 16px;display: -webkit-inline-box;width: 40px'>" + series.data[0][1] + "</span>"
					+ "<span>%</span></div>"
			},
			margin : [ 20, 80 ]
		}
	};

	// $scope.typeChartData = [];

	$scope.edenData = [];
	$scope.survivor1 = [];
	$scope.survivor2 = [];
	$scope.old = [];

	$scope.$watch('$viewContentLoaded', function() {
		var free = 100;
		$scope.edenData.push({
			label : "伊甸园区",
			data : res['E'],
			color : "#F2AD68"
		});
		$scope.edenData.push({
			label : "空闲",
			data : (free - res['E']).toFixed(2),
			color : "#FAEBD7"
		});
		$("#eden").plot($scope.edenData, $scope.chartOptions);

		$scope.survivor1.push({
			label : "幸存一区",
			data : res['S0'],
			color : "#36ADD7"
		});
		$scope.survivor1.push({
			label : "空闲",
			data : (free - res['S0']).toFixed(2),
			color : "#FAEBD7"
		});
		$("#survivor1").plot($scope.survivor1, $scope.chartOptions);

		$scope.survivor2.push({
			label : "幸存二区",
			data : res['S1'],
			color : "#83CF74"
		});
		$scope.survivor2.push({
			label : "空闲",
			data : (free - res['S1']).toFixed(2),
			color : "#FAEBD7"
		});
		$("#survivor2").plot($scope.survivor2, $scope.chartOptions);

		$scope.old.push({
			label : "老年代区",
			data : res['O'],
			color : "#F80400"
		});
		$scope.old.push({
			label : "空闲",
			data : (free - res['O']).toFixed(2),
			color : "#FAEBD7"
		})
		$("#old").plot($scope.old, $scope.chartOptions);

		// $scope.typeChartData.push({label:"幸存一区",data:res['S0'],color:"#36ADD7"});
		// $scope.typeChartData.push({label:"幸存二区",data:res['S1'],color:"#83CF74"});
		// $scope.typeChartData.push({label:"伊甸园区",data:res['E'],color:"#F2AD68"});
		// $scope.typeChartData.push({label:"老年代区",data:res['O'],color:"#F80400"});
		// var free = 100 - res['S0'] - res['S1'] - res['E'] - res['O'];
		// $scope.typeChartData.push({label:"空闲",data:free.toFixed(2),color:"#FAEBD7"});
		// $("#chartPie").plot($scope.typeChartData, $scope.chartOptions);
	});

	$scope.ok = function() {
		$modalInstance.close();
	}
});

app.controller('MakeDirCtrl', function($scope, $modalInstance, $state, toaster, path, scopeMain) {
	$scope.dirPath = path;
	$scope.dirName = null;

	$scope.ok = function() {
		$.ajax({
			url : "file/makeDir",
			type : "POST",
			dataType : "json",
			headers : {
				'Content-Type' : 'application/json;charset=utf-8'
			},
			data : JSON.stringify({
				'dirname' : $scope.dirName,
				'dirpath' : $scope.dirPath
			}),
			success : function(resp) {
				if (resp.status) {
					$modalInstance.close();
					scopeMain.initTree();
					$scope.$apply();
				} else {
					toaster.pop("error", "创建文件夹", "创建失败");
				}
			}
		});

	}
});

app.controller('UploadCtrl', function($scope, $modalInstance, $state, toaster, path, scopeMain) {
	$scope.files = [];
	$scope.uploadFile = function() {
		$scope.processDropzone();
	};

	$scope.reset = function() {
		$scope.resetDropzone();
	};
	$scope.toaster = toaster;

	$scope.uploadPath = path;

	// $("#uploadPath").val(path);

	$scope.beforeLoadFile = function() {
		if (!$scope.uploadPath) {
			toaster.pop("error", "上传", "请选择一个目录");
			$scope.$apply();
			return false;
		}
		return true;
	}

	$scope.dropzoneCallback = function(data) {
		if (data.json.status) {
			scopeMain.initTree();
			$scope.$apply();
			toaster.pop("success", "上传文件", data.json.message);
		} else {
			toaster.pop("error", "上传文件", data.json.message);
		}
	}

	$scope.ok = function() {
		$modalInstance.close();
	}
});
