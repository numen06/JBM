// Dashboard Box controller
'use strict';
app.controller('DashboardCtrl', [
	'$rootScope',
	'$scope',
	'$timeout',
	function($rootScope, $scope, $timeout) {
		var stompClient = null;

		$scope.data = {
			// 连接状态
			connected : false,
			// 消息
			message : '',
			rows : []
		};

		// 连接
		$scope.connect = function() {
			var socket = new SockJS('/websocket');
			stompClient = Stomp.over(socket);
			stompClient.connect({}, function(frame) {
				// 注册监控消息
				stompClient.subscribe('/topic/monitor', function(msg) {
					var monitorData = JSON.parse(msg.body);
					$timeout(function() {
						$scope.monitorModel = monitorData;
						$scope.dataFormatAdd(monitorData);
						$scope.showData();
					})
					if($("#memRatio_char").data('easyPieChart')){
						$("#memRatio_char").data('easyPieChart').update(monitorData.memRatio);
					}
				});
			});
		};

		$scope.disconnect = function() {
			if (stompClient != null) {
				stompClient.disconnect();
			}
			$scope.data.connected = false;
		}

		$scope.send = function() {
			stompClient.send("/server/send", {}, JSON.stringify({
				'message' : $scope.data.message
			}));
		}

		var cpuData = [];
		var memData = [];

		var oneDay = 24 * 3600 * 1000;
		$scope.monitorModel = {
			memRatio : 0
		};

		var cpuChart = echarts.init(document.getElementById('main'));
		var memChart = echarts.init(document.getElementById('main2'));

		$scope.init = function() {
			$.rest({
				async : false,
				url : 'monitor/all',
				success : function(data) {
					var monitorDatas = data.result;
					for (var i = 0; i < monitorDatas.length; i++) {
						$scope.dataFormatAdd(monitorDatas[i]);
					}
					$scope.connect();
				}
			});
		};

		$scope.dataFormatAdd = function(monitorData) {
			if (cpuData.length > 300) {
				cpuData.shift();
			}
			if (memData.length > 300) {
				memData.shift();
			}
			var now = new Date(monitorData.monitorTime);
			cpuData.push({
				name : now.toString(),
				value : [ [ now.getFullYear(), now.getMonth() + 1, now.getDate() ].join('/') + " " + [ now.getHours(), now.getMinutes(), now.getSeconds() ].join(':'),
					monitorData.cpuRatio.toFixed(2) ]
			});
			memData.push({
				name : now.toString(),
				value : [ [ now.getFullYear(), now.getMonth() + 1, now.getDate() ].join('/') + " " + [ now.getHours(), now.getMinutes(), now.getSeconds() ].join(':'),
					(monitorData.usedMemory / 1024).toFixed(0) ]
			});
		}

		var cpuOption = {
			title : {
				text : 'DEVOPS CPU'
			},
			tooltip : {
				trigger : 'axis',
				formatter : function(params) {
					params = params[0];
					return params.value[1] + '%';
				},
				axisPointer : {
					animation : false
				}
			},
			xAxis : {
				type : 'time',
				splitLine : {
					show : false
				}
			},
			yAxis : {
				type : 'value',
				boundaryGap : [ 0, '100%' ],
				splitLine : {
					show : false
				},
				max : 100
			},
			series : [ {
				name : '系统CPU',
				type : 'line',
				showSymbol : false,
				hoverAnimation : false,
				data : cpuData,
				markLine : {
					data : [ {
						type : 'average',
						name : '平均值'
					}, [ {
						symbol : 'none',
						x : '90%',
						yAxis : 'max'
					}, {
						symbol : 'none',
						label : {
							normal : {
								position : 'start',
								formatter : '最大值'
							}
						},
						type : 'max',
						name : '最高点'
					} ] ]
				},
				areaStyle : {
					normal : {
						color : new echarts.graphic.LinearGradient(0, 0, 0, 1, [ {
							offset : 0,
							color : 'rgb(255, 158, 68)'
						}, {
							offset : 1,
							color : 'rgb(255, 70, 131)'
						} ])
					}
				},
			} ]
		};

		var memOption = {
			title : {
				text : 'DEVOPS MEM'
			},
			tooltip : {
				trigger : 'axis',
				formatter : function(params) {
					params = params[0];
					return (params.value[1] / 1024).toFixed(0) + 'GB<br/>' + (params.value[1]*1).toFixed(0) + 'MB';
				},
				axisPointer : {
					animation : false
				}
			},
			xAxis : {
				type : 'time',
				splitLine : {
					show : false
				}
			},
			yAxis : {
				type : 'value',
				boundaryGap : [ 0, '100%' ],
				splitLine : {
					show : false
				}
			},
			series : [ {
				name : '系统内存',
				type : 'line',
				data : memData,
				showSymbol : false,
				hoverAnimation : false,
				showSymbol : false,
				hoverAnimation : false,
				markLine : {
					data : [ {
						type : 'average',
						name : '平均值'
					}, [ {
						symbol : 'none',
						x : '90%',
						yAxis : 'max'
					}, {
						symbol : 'none',
						label : {
							normal : {
								position : 'start',
								formatter : '最大值'
							}
						},
						type : 'max',
						name : '最高点'
					} ] ]
				},
				areaStyle : {
					normal : {
						color : new echarts.graphic.LinearGradient(0, 0, 0, 1, [ {
							offset : 0,
							color : 'rgb(255, 158, 68)'
						}, {
							offset : 1,
							color : 'rgb(255, 70, 131)'
						} ])
					}
				},
			} ]
		};

		cpuChart.setOption(cpuOption);
		memChart.setOption(memOption);

		$scope.showData = function() {
			cpuChart.setOption({
				series : [ {
					data : cpuData
				} ]
			});
			memChart.setOption({
				series : [ {
					data : memData
				} ]
			});
		};

		$scope.init();

	} ]);