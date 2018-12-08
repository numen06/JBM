//注册ajax全局变量和事件
$.ajaxSetup({

	timeout : 60000
// 默认请求超时标准是5秒钟

});

window.startCoding = function(callback) {
	$(function() {

		callback && callback();

	});
};

/**
 * 获取表单数据 allFiled=true 表示需要所有字段，（不忽略空值） existArray= true
 * 表示对象中可以存在数组，否则会自动将数组转化成字符串的形式，如hobby:'2,3,5' 不传参数 表示默认只取有值的字段，并且自动将数组拼接成字符串
 */

$.fn.getValues = function(allFiled, existArray) {
	var o = {};
	var a = this.serializeArray();
	$.each(a, function() {
		if (o[this.name]) { // 表示已经存在了
			if (!o[this.name].push) {
				o[this.name] = [ o[this.name] ]; // 变身为数组
			}
			o[this.name].push(this.value || '');
		} else {
			if (allFiled) { // 如果不忽略空值
				o[this.name] = this.value || '';
			} else {
				this.value && (o[this.name] = this.value || '');
			}
		}
	});
	if (!existArray) {
		for ( var item in o) {
			if (o[item].push) { // 如果是数组
				o[item] = o[item].join();
			}
		}
	}

	return o;
}

$.fn.setStaticValues = function(obj) {
	var me = $(this);
	for ( var o in obj) {
		me.find(".data-value[name='" + o + "']").html(obj[o]);
	}
}

$.fn.getNameById = function(data) {
	if (!data) {
		return;
	}
	if (data.result.list.length < 1) {
		return;
	}
	var list = data.result.list;
	var exp = data.result.exp;
	var dataExp = {};
	for (var j = 0; j < list.length; j++) {
		$.each(exp, function(index, value) {
			dataExp[index] = value[list[j][index]];
		});
		list[j]["_exp"] = dataExp;
		dataExp = {};
	}
}

/*
 * 初始化j静态面板 传入url,dataFilter,loading,type
 */
$.fn.initStaticPanel = function(url, dataFilter, loading, type) {
	var panel = $(this);
	var time = (new Date()).getTime();
	var _config = {
		url : url,
		type : type || "get",
		dataFilter : dataFilter,
		dataType : "json",
		success : function(data) {
			panel.setStaticValues(data["result"]);
		}
	};
	if (loading == "no")
		loading = false;
	else {
		loading = true;
	}
	if (loading) {
		_config.beforeSend = function() {
			window["loading_" + time] = layer.load('正在加载...');
		}
		_config.complete = function() {
			layer.close(window["loading_" + time]);
		}
	}
	$.ajax(_config);

}

/**
 * 初始化表单业务数据
 */

$.fn.initValues = function(obj) {
	var me = $(this);

	// 初始化所有 input
	for ( var name in obj) {
		var _input = me.find("input[name='" + name + "']");
		if (_input.attr("type") != "radio" && _input.attr("type") != "checkbox") {
			_input.val(obj[name]);
		}
	}
	// 初始化所有 textarea
	for ( var name in obj) {
		var _textarea = me.find("textarea[name='" + name + "']");

		_textarea.val(obj[name]);

	}

	// 初始化所有 select
	for ( var name in obj) {
		// me.find("select[name='"+name+"']").val(obj[name]);
		me.find("select[name='" + name + "']").each(function(index, item) {
			// me.chosen("destroy");
			var cur_select = $(item);
			// cur_select.initSelectValue(obj[name]);
			// console.info(typeof obj[name],888,cur_select,cur_select.html());

			cur_select.val(obj[name] + "");
			cur_select.select2();
			// cur_select.initSelectValue(obj[name]);
		});
	}
	// 初始化所有 radio
	var radios = [];
	for ( var name in obj) {
		me.find("input[type='radio'][name='" + name + "']").each(function(index, item) {
			if (obj[name] == $(item).attr("value")) {
				$(item).attr("checked", "checked");
			}
		});
	}
	// 初始化所有 checkbox
	var checkboxs = [];
	for ( var name in obj) {
		me.find("input[type='checkbox'][name='" + name + "']").each(function(index, item) {
			// 找到了name等于hobby的字段
			var str = obj[name].toString();
			var values = str.split(","); // 将字段值分隔成数组

			if ($.inArray($(item).attr("value"), values) != -1) { // 表示该checkbox要被选中
				$(item).prop("checked", true);
			}
		});
	}
}

function generateUID() {
	return new Date().getTime() + Math.floor(Math.random() * 100);
}

Date.prototype.format = function(fmt) {
	if(fmt == undefined){
		fmt = 'yyyy-MM-dd hh:mm:ss';
	}
	var o = {
		"M+" : this.getMonth() + 1, // 月份
		"d+" : this.getDate(), // "+getI18n("public.common.date")+"
		"h+" : this.getHours(), // 小时
		"m+" : this.getMinutes(), // 分
		"s+" : this.getSeconds(), // "+getI18n("public.common.second")+"
		"q+" : Math.floor((this.getMonth() + 3) / 3), // 季度
		"S" : this.getMilliseconds()
	// 毫秒
	};
	if (/(y+)/.test(fmt))
		fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
	for ( var k in o)
		if (new RegExp("(" + k + ")").test(fmt))
			fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
	return fmt;
}

/**
 * init select value
 */
$.fn.initSelectValue = function(value) {
	var me = $(this);
	me.find("option").removeAttr("selected");
	me.find("option[value='" + value + "']").attr("selected", "selected");
	// me.find("option").each(function(index,item){
	// var mee= $(this);
	// if(mee.attr("value")==value){
	// mee.attr("selected","selected");
	// return false;
	// }
	// });
}

/**
 * 对form dom 进行基础数据 初始化, 如 select checkbox
 */
$.fn.initFormBase = function() {
	var me = $(this);
	// 初始化所有 select
	me.find("select").each(function(index, item) {

		var cur_select = $(item);
		if (cur_select.attr("jbm-data")) { // 如果select有本地option数据，优先用本地数据 如
			// <select
			// data="window.nationList"></select>
			var dataList = eval(cur_select.attr("jbm-data"));
			cur_select.initSelectOptions(dataList, {
				valueName : cur_select.attr("jbm-valuename"),
				textName : cur_select.attr("jbm-textname")
			});
		} else if (cur_select.attr("jbm-url")) { // width:
			// cur_select.attr("width")?cur_select.attr("width"):'auto'
			// console.info("标签:",cur_select.attr("jbm-valueName"),cur_select.attr("jbm-textName"));
			var datakey = cur_select.attr("jbm-datakey");
			if (!datakey)
				datakey = null;

			cur_select.initSelectOptionsByURL(cur_select.attr("jbm-url"), {
				dataKey : datakey,
				value : cur_select.attr("jbm-value"),
				tipName : cur_select.attr("jbm-TipName"),
				showTip : cur_select.attr("jbm-showTip"),
				valueName : cur_select.attr("jbm-valuename"),
				textName : cur_select.attr("jbm-textname"),
				async : false,
				chosen : false
			});
		}
	});

}
/*
 * 初始化表单 传入url,dataFilter,loading,type 如果url等于空 表示不需要初始化业务数据
 */
$.fn.initForm = function(url, dataFilter, loading, type) {
	var form = $(this);
	var time = (new Date()).getTime();
	var _config = {
		url : url,
		type : type || "get",
		dataFilter : dataFilter,
		dataType : "json",
		success : function(data) {
			form.initFormBase();// 进行第一步初始化 如 select
			form.initValues(data["result"]); // 进行第二步初始化

		}
	};
	if (loading == "no")
		loading = false;
	else {
		loading = true;
	}
	if (loading) {
		_config.beforeSend = function() {
			window["loading_" + time] = layer.load('正在加载...');
		}
		_config.complete = function() {
			layer.close(window["loading_" + time]);
		}
	}
	if (url) {
		$.ajax(_config);
	} else {
		form.initFormBase();// 进行第一步初始化 如 select
	}

}

/*
 * 初始化下拉列表的option数据 用法:第一个参数data传的是数组类型的值，第二个参数就是传的一个配置信息，如，value，valueName等
 * 优先级： 动态配置优先于标签配置
 */
$.fn.initSelectOptions = function(data, config) {
	var me = $(this);
	var _config = $.extend({
		value : '',
		valueName : 'value',
		textName : 'text',
		showTip : true,
		useChosen : true,
		width : me.width()
	}, config);
	_config.value = _config.value || me.attr("value");
	var optionHTML = "";
	if (_config.showTip) {
		optionHTML = "<option value=''>请选择</option>";
	}
	_.each(data, function(item, index) {
		if (_config.value == item[_config.valueName]) {
			optionHTML += '<option value="' + item[_config.valueName] + '" selected>' + item[_config.textName] + '</option>';
		} else {
			optionHTML += '<option value="' + item[_config.valueName] + '">' + item[_config.textName] + '</option>';
		}

	});
	var firstOption = me.find("option:first-child");
	if (firstOption.size() > 0 && _.isEmpty(firstOption.attr("value"))) {
		optionHTML = "<option value=''>" + firstOption.html() + "</option>" + optionHTML;
	}
	me.html(optionHTML);
	// if( _config.width.indexOf("p")==-1){
	// _config.width += "px";
	// }
	_config.useChosen && me.chosen($.extend({
		allow_single_deselect : true,
		disable_search : true
	}, _config));
}
/*
 * 初始化下拉列表的option数据 用法:第一个参数data传的是字符串类型，第二个参数就是传的一个配置信息，如，value，valueName等
 * 第三个参数是回调函数 优先级： 动态配置优先于标签配置
 */
$.fn.initSelectOptionsByURL = function(url, config, callback) {
	var me = $(this);
	var _config = $.extend({
		value : '',
		tipName : '请选择',
		valueName : 'value',
		textName : 'text',
		async : true,
		showTip : false,
		dataKey : 'result',
		disable_search : true,
		chosen : true,
		width : me.width() + ""
	}, config);

	if (!_config.value) {
		_config.value = me.attr("val");
		me.removeAttr("val");
	}
	_config.width += "";
	if (_config.width.indexOf("p") == -1) {
		_config.width += "px";
	}
	var optionHTML = "";
	if (_config.showTip) {
		optionHTML = "<option value=''>" + _config.tipName + "</option>";
	}
	$.ajax({
		url : url,
		type : _config.type || "get",
		dataType : "json",
		async : _config.async,
		success : function(resp) {
			var data;
			if (_config.dataKey == null) {
				data = resp;
			} else {
				data = eval("resp." + _config.dataKey);
				// data= resp[_config.dataKey];
			}
			$.each(data, function(index, item) {
				if (_config.value == item[_config.valueName]) {
					optionHTML += '<option value="' + item[_config.valueName] + '" selected>' + item[_config.textName] + '</option>';
				} else {
					optionHTML += '<option value="' + item[_config.valueName] + '">' + item[_config.textName] + '</option>';
				}
			});
			var firstOption = me.find("option:first-child");
			if (firstOption.size() > 0 && firstOption.attr("value") != "") {
				optionHTML = "<option value=''>" + firstOption.html() + "</option>" + optionHTML;
			}
			me.html(optionHTML);
			if (_config.chosen) {
				me.trigger('chosen:updated');
				me.chosen({
					allow_single_deselect : true,
					disable_search : _config.disable_search,
					width : _config.width
				});
			}
			callback && callback(resp);
		}
	});

}
/*
 * 保存表单 传入url,success回调函数，或只传一个回调也行
 */

$.fn.save = function(a, b) {

	var form = $(this);

	// $("#user_form").validate

	var time = (new Date()).getTime();
	var url, success;
	if (typeof (a) == 'string') { // 表示a是字符串url
		url = a;
		success = b;
	} else if (typeof (a) == 'function') {
		success = a;
	}

	var _config = {
		url : url || form.attr("action"),
		data : form.getValues(),
		type : form.attr("method") || "post",
		dataType : 'json',
		success : function(data) {
			success && success(data);
		}
	};
	if (form.attr("loading") != "no") {
		_config.beforeSend = function() {
			window["loading_" + time] = layer.load('正在加载...');
		}
		_config.complete = function() {
			layer.close(window["loading_" + time]);
		}
	}
	$.ajax(_config);
}

/*
 * 固定表头 传入table的id
 */

$.fn.fixHeader = function() {

	var _table = $(this);// $("#"+tableId);
	var td_width = 0, td_widths = [];
	// 将表头的各列宽度存储在table上
	_table.find("thead th").each(function(index, item) {
		td_widths.push($(item).attr("width"));
	});
	_table.data("td_widths", td_widths);
	$("<table  class='data-header'  cellpadding='0'  cellspacing='0'></table>").html(_table.find("thead")).insertBefore(_table);

}

/**
 * 加载list数据的简化方法
 *
 */
$.fn.loadNewList = function(config) {

	var time = (new Date()).getTime();
	var _config = $.extend({
		url : '',
		type : 'post',
		dataType : 'json',
		pageSize : 10,
		height : '',
		loading : true,
		table : $(this),
		template : '',
		templateFun : {},
		listKey : 'list',
		totalKey : 'total',
		// tdHeight: '20',
		showGoInput : true,
		showGoButton : true,
		params : {},
		callback : function() {
		},
		pagination : true
	// true 表示需要分页，否则表示不需要
	}, config);
	_config.context = _config.table;
	_config.beforeSend = function(data) {
		config.beforeSend && config.beforeSend(data, $(this)); // 请求开始前的动作
		if (_config.loading) {
			window["loading_" + time] = layer.load('正在加载...');
		}
	}
	_config.complete = function(data) {
		config.complete && config.complete(data, $(this)); // 请求结束后的动作
		if (_config.loading) {
			layer.close(window["loading_" + time]);
		}
	}
	var me = $(this);
	_config.success = function(data) {
		var table = me;
		config.success && config.success(data, table); // 请求成功后的动作
		// config.afterRender&&config.afterRender(); //界面渲染后的动作
		// 对列表的公共处理
		if ($("#" + _config.template).size() == 0) {
			alert("not find template !");
			return;
		}

		if (data['result'][_config.listKey].length == 0) { // 处理没有拿到数据的情况，提示暂无数据
			var tdNum;
			if (table.data("td_widths")) {
				tdNum = table.data("td_widths").length;
			} else {
				tdNum = table.find("thead tr th").size();
			}

			table.find("tbody").html("<td colspan='" + tdNum + "'><div class='no-data-tip'>暂时没有数据哦！</div></td>");
		} else {

			// data['result'][_config.listKey]=
			// data['result'][_config.listKey].concat(data['result'][_config.listKey]);
			// data['result'][_config.listKey]=
			// data['result'][_config.listKey].concat(data['result'][_config.listKey]);
			table.find("tbody").html(juicer($("#" + _config.template).html(), $.extend({
				list : data['result'][_config.listKey]
			}, _config.templateFun)));
			// _config.tdHeight
			// table.find("tbody tr td").height(_config.tdHeight);
		}

		_config.callback();

		dealTableHeader();
		// if(!_config.height){
		// _config.height= 999;
		// }
		// table.parent().height(_config.height);
		table.find("tr:even").addClass("tr-even");
		table.find("tbody tr").hover(function() {
			$(this).addClass("tr-hover");
		}, function() {
			$(this).removeClass("tr-hover");
		});
	}
	// 处理表头
	function dealTableHeader() {
		if (_config.table.hasClass("fix-header") && !_config.table.data("fixed")) { // 表示需要固定表头，但还未固定

			_config.table.fixHeader();// 固定表头
			_config.table.data("fixed", true);
		} else if (_config.table.hasClass("fix-header")) { // 表示已经固定

			_config.table.find("tbody tr td").each(function(index, item) {
				$(item).attr("width", _config.table.data("td_widths")[index]);
			});
		}
	}

	function loadData(currPage, paginationCt, pageSize) {
		_config.currPage = currPage;
		_config.paginationCt = paginationCt;
		_config.pageSize = pageSize;
		_config.data = $.extend(_config.params, {
			currPage : currPage + 1,
			pageSize : pageSize
		});
		// $.ajax(_config);
		_config.paginationCt.pagination({
			dataSource : _config.url,
			ajax : _config,
			pageSize : 10,
			locator : 'result.list',
			showGoInput : _config.showGoInput,
			showGoButton : _config.showGoButton,
			callback : _config.success
		});
	}
	var gid = 'pagination_' + (new Date()).getTime();
	if (_config.pagination) { // 如果需要处理分页

		_config.table.after("<div id='" + gid + "' class='pagination'> </div>");
		loadData(0, $("#" + gid), _config.pageSize);
	}
	dealTableHeader();
	var panel = _config.table.closest(".panel");
	// alert(panel.height());
	var table_ct_height = panel.height() - panel.find(".panel-top-banner").height() - panel.find(".panel-title").height();
	table_ct_height = table_ct_height - panel.find(".data-header").height();
	// alert(table_ct_height-75);
	if (!_config.height) {
		_config.height = table_ct_height - 75;
	}
	// _config.table.layout(_config.height); //适配尺寸并加滚动条

	return function(params) { // 闭包返回，因为只有闭包才能访问内部的数据
		if (params) {
			for ( var key in params) {
				if (!params[key]) {
					delete _config.data[key];
				}

			}
			_config.data = $.extend(_config.data, params);
		}
		loadData(0, $("#" + gid), _config.pageSize);
	}
}

function clickTabelHead(obj, config) {
	$(obj).find("th").each(function(index, item) {
		$(item).click(function() {
			if (!$(this).attr("column") || $(this).attr("column") == "") {
				return;
			}
			var sort = $(this).attr("column") + ":asc";
			var div = '<div class="ngSortButtonDown" ></div>';
			if ($(this).find(".ngSortButtonDown").length > 0) {
				div = '<div class="ngSortButtonUp" ></div>';
				sort = $(this).attr("column") + ":desc";
			}
			config.params["sortRule"] = sort;
			$(".ngSortButtonDown").remove();
			$(".ngSortButtonUp").remove();
			$(this).append(div);
			$(obj).loadList(config);
		});
	});
}

/**
 * 加载list数据的简化方法
 *
 */

$.fn.loadList = function(config) {

	var time = (new Date()).getTime();
	var _config = $.extend({
		url : '',
		type : 'post',
		dataType : 'json',
		pageSize : 10,
		height : '',
		loading : true,
		table : $(this),
		template : '',
		templateFun : {},
		useTp : true,
		listKey : 'list',
		totalKey : 'total',
		// tdHeight: '20',
		showGoInput : true,
		showGoButton : true,
		params : {},
		isUseMemory : true,
		isPopUp : false,
		callback : function() {
		},
		pagination : true
	// true 表示需要分页，否则表示不需要
	}, config);
	// 处理表头点击
	clickTabelHead($(this), config);
	_config.context = _config.table;
	_config.beforeSend = function(data) {
		config.beforeSend && config.beforeSend(data, $(this)); // 请求开始前的动作
		if (_config.loading) {
			window["loading_" + time] = layer.load('正在加载...');
		}
	}
	_config.complete = function(data) {
		config.complete && config.complete(data, $(this)); // 请求结束后的动作
		if (_config.loading) {
			layer.close(window["loading_" + time]);
		}
	}
	var me = $(this);
	_config.success = function(data) {
		var table = me;
		config.success && config.success(data, table); // 请求成功后的动作
		// 对列表的公共处理
		if (_config.useTp && $("#" + _config.template).size() == 0) {
			alert("not find template !");
			return;
		}

		if (data['result'][_config.listKey].length == 0) { // 处理没有拿到数据的情况，提示暂无数据
			var tdNum;
			if (table.data("td_widths")) {
				tdNum = table.data("td_widths").length;
			} else {
				tdNum = table.find("thead tr th").size();
			}

			table.find("tbody").html("<td colspan='" + tdNum + "'><div class='no-data-tip'>暂时没有数据哦！</div></td>");
		} else {

			// data['result'][_config.listKey]=
			// data['result'][_config.listKey].concat(data['result'][_config.listKey]);
			// data['result'][_config.listKey]=
			// data['result'][_config.listKey].concat(data['result'][_config.listKey]);
			if (_config.useTp) {
				table.find("tbody").html(juicer($("#" + _config.template).html(), $.extend({
					list : data['result'][_config.listKey]
				}, _config.templateFun)));
			} else {
				table.find("tbody").html(juicer(_config.template, $.extend({
					list : data['result'][_config.listKey]
				}, _config.templateFun)));
			}
			// _config.tdHeight
			// table.find("tbody tr td").height(_config.tdHeight);
		}

		_config.callback();

		dealTableHeader();
		// if(!_config.height){
		// _config.height= 999;
		// }
		// table.parent().height(_config.height);
		table.find("tr:even").addClass("tr-even");
		table.find("tbody tr").hover(function() {
			$(this).addClass("tr-hover");
		}, function() {
			$(this).removeClass("tr-hover");
		});
		config.afterRender && config.afterRender(_config.context); // 界面渲染后的动作
	}
	// 处理表头
	function dealTableHeader() {
		if (_config.table.hasClass("fix-header") && !_config.table.data("fixed")) { // 表示需要固定表头，但还未固定

			_config.table.fixHeader();// 固定表头
			_config.table.data("fixed", true);
		} else if (_config.table.hasClass("fix-header")) { // 表示已经固定

			_config.table.find("tbody tr td").each(function(index, item) {
				$(item).attr("width", _config.table.data("td_widths")[index]);
			});
		}
	}

	function loadData(currPage, paginationCt, pageSize) {
		_config.currPage = currPage;
		_config.paginationCt = paginationCt;
		_config.pageSize = pageSize;
		_config.data = $.extend(_config.params, {
			currPage : currPage + 1,
			pageSize : pageSize
		});
		// $.ajax(_config);
		// if ($(".paginationjs").length > 0) {
		// 	$(".paginationjs").remove();
		// 	// return;
		// }
		$(paginationCt).next().remove();
		_config.paginationCt.pagination({
			dataSource : _config.url,
			ajax : _config,
			pageSize : pageSize,
			locator : 'result.list',
			showGoInput : _config.showGoInput,
			showGoButton : _config.showGoButton,
			callback : _config.success,
			isUseMemory : _config.isUseMemory,
			isPopUp : _config.isPopUp
		});
	}
	var gid = 'pagination_' + (new Date()).getTime();
	if (_config.pagination) { // 如果需要处理分页

		if ($(".ps-container").length > 0) {
			$(".ps-container").after("<div id='" + gid + "' class='pagination'> </div>");
		} else {
			_config.table.after("<div id='" + gid + "' class='pagination'> </div>");
		}

		loadData(0, $("#" + gid), _config.pageSize);
	}
	dealTableHeader();
	var panel = _config.table.closest(".panel");
	// alert(panel.height());
	var table_ct_height = panel.height() - panel.find(".panel-top-banner").height() - panel.find(".panel-title").height();
	table_ct_height = table_ct_height - panel.find(".data-header").height();
	// alert(table_ct_height-75);
	if (!_config.height) {
		_config.height = table_ct_height - 75;
	}
	if ($(".ps-scrollbar-x-rail").length == 0) {
		// _config.table.layout(_config.height); //适配尺寸并加滚动条
	}

	return function(params) { // 闭包返回，因为只有闭包才能访问内部的数据
		if (params) {
			for ( var key in params) {
				if (!params[key]) {
					delete _config.data[key];
				}

			}
			_config.data = $.extend(_config.data, params);
		}
		loadData(0, $("#" + gid), _config.pageSize);
	}

	// var time= (new Date()).getTime();
	// var _config= $.extend({
	// url: '',
	// type: 'post',
	// dataType: 'json',
	// pageSize:10,
	// height: '',
	// loading: true,
	// table: $(this),
	// template: '',
	// templateFun: {},
	// listKey: 'list',
	// totalKey: 'total',
	// // tdHeight: '20',
	// params:{},
	// callback: function(){},
	// pagination: true // true 表示需要分页，否则表示不需要
	// },config);
	// _config.context= _config.table;
	// _config.beforeSend= function(data){
	// config.beforeSend&&config.beforeSend(data,$(this)); //请求开始前的动作
	// if(_config.loading){
	// window["loading_"+time]= layer.load('正在加载...');
	// }
	// }
	// _config.complete= function(data){
	// config.complete&&config.complete(data,$(this)); //请求结束后的动作
	// if(_config.loading){
	// layer.close(window["loading_"+time]);
	// }
	// }
	// _config.success= function(data){
	// var table= $(this);
	// config.success&&config.success(data,table); //请求成功后的动作
	// //config.afterRender&&config.afterRender(); //界面渲染后的动作
	// //对列表的公共处理
	// if($("#"+_config.template).size()==0){
	// alert("not find template !");
	// return ;
	// }
	// if(!data['status']){
	// alert("查询失败！");
	// return false;
	//
	// }
	//
	//
	// if(data['result'][_config.listKey].length==0){ //处理没有拿到数据的情况，提示暂无数据
	// var tdNum;
	// if(table.data("td_widths"))
	// {
	// tdNum= table.data("td_widths").length;
	// }else{
	// tdNum =table.find("thead tr th").size();
	// }
	//
	// table.find("tbody").html("<td colspan='"+tdNum+"'><div
	// class='no-data-tip'>暂时没有数据哦！</div></td>");
	// }else{
	//
	// // data['result'][_config.listKey]=
	// data['result'][_config.listKey].concat(data['result'][_config.listKey]);
	// //data['result'][_config.listKey]=
	// data['result'][_config.listKey].concat(data['result'][_config.listKey]);
	// table.find("tbody").html(juicer($("#"+_config.template).html(),
	// $.extend({list:data['result'][_config.listKey]},_config.templateFun)));
	// // _config.tdHeight
	// //table.find("tbody tr td").height(_config.tdHeight);
	// }
	//
	// _config.callback();
	//
	// if(_config.pagination){ //如果需要处理分页
	//
	// _config.paginationCt.pagination({total: data['result'][_config.totalKey],
	// current_page: _config.currPage,items_per_page:_config.pageSize,callback:
	// loadData });
	// }
	// dealTableHeader();
	// // if(!_config.height){
	// // _config.height= 999;
	// // }
	// // table.parent().height(_config.height);
	// table.find("tr:even").addClass("tr-even");
	// table.find("tbody tr").hover(function(){
	// $(this).addClass("tr-hover");
	// },function(){
	// $(this).removeClass("tr-hover");
	// });
	// }
	// //处理表头
	// function dealTableHeader(){
	// if(_config.table.hasClass("fix-header")&&!_config.table.data("fixed")){
	// //表示需要固定表头，但还未固定
	//
	// _config.table.fixHeader();//固定表头
	// _config.table.data("fixed",true);
	// }else if(_config.table.hasClass("fix-header")){ //表示已经固定
	//
	// _config.table.find("tbody tr td").each(function(index,item){
	// $(item).attr("width",_config.table.data("td_widths")[index]);
	// });
	// }
	// }
	//
	// function loadData(currPage, paginationCt,pageSize){
	// _config.currPage= currPage;
	// _config.paginationCt= paginationCt;
	// _config.pageSize= pageSize;
	// _config.data= $.extend(_config.params,{currPage: currPage+1,pageSize:
	// pageSize});
	// $.ajax(_config);
	// }
	// var gid= 'pagination_'+(new Date()).getTime();
	// if(_config.pagination){ //如果需要处理分页
	// _config.table.after("<div id='"+gid+"' class='pagination'> </div>");
	// loadData(0,$("#"+gid),_config.pageSize);
	// }
	// dealTableHeader();
	// var panel= _config.table.closest(".panel");
	// //alert(panel.height());
	// var table_ct_height=
	// panel.height()-panel.find(".panel-top-banner").height()-panel.find(".panel-title").height();
	// table_ct_height= table_ct_height- panel.find(".data-header").height();
	// //alert(table_ct_height-75);
	// if(!_config.height){
	// _config.height= table_ct_height-75;
	// }
	// _config.table.layout(_config.height); //适配尺寸并加滚动条
	//
	//
	// return function(params){ //闭包返回，因为只有闭包才能访问内部的数据
	// if(params){
	// for(var key in params){
	// if(!params[key]){
	// delete _config.data[key];
	// }
	//
	// }
	// _config.data= $.extend(_config.data,params);
	// }
	// loadData(0,$("#"+gid),_config.pageSize);
	// }
}

Date.prototype.Format = function(fmt) {
	if(fmt == undefined){
		fmt = 'yyyy-MM-dd hh:mm:ss';
	}
	var o = {
		"M+" : this.getMonth() + 1, // 月份
		"d+" : this.getDate(), // "+getI18n("public.common.date")+"
		"h+" : this.getHours(), // 小时
		"m+" : this.getMinutes(), // 分
		"s+" : this.getSeconds(), // "+getI18n("public.common.second")+"
		"q+" : Math.floor((this.getMonth() + 3) / 3), // 季度
		"S" : this.getMilliseconds()
	// 毫秒
	};
	if (/(y+)/.test(fmt))
		fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
	for ( var k in o)
		if (new RegExp("(" + k + ")").test(fmt))
			fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
	return fmt;
}

function getFormDatas(form1, param) {
	// filterForm();

	$("input[type='text']").each(function() {
		if ($(this).attr("liger")) {
			$(this).attr("oldValue", $(this).val());
			$(this).attr("value", $(this).ligerComboBox().getValue());
		}
	});

	var formDatas = $(form1).serializeArray();
	/*
	 * var s=""; $.each(formDatas, function(i,field){
	 * s+="(key:"+field.name+",value:"+field.value+")"; }); alert(s);
	 */

	$.each(formDatas, function(i, field) {
		var thisValue = field.value;
		if ($("input[name='" + field.name + "']").attr("liger")) {
			// thisValue = $(field).ligerComboBox().getValue();
		}
		// if(!param[field.name]){
		if (!param.hasOwnProperty(field.name)) {
			param[field.name] = thisValue;
		} else {
			param[field.name] += "," + thisValue;
		}
	});

	$("input[type='text']").each(function() {
		if ($(this).attr("liger")) {
			$(this).attr("value", $(this).attr("oldValue"));
		}
	});
	return param;
}

$.fn.submitForm = function(config,param) {

	var me= this;
	if (typeof(config)=== 'function'){ //传进来的是success函数
		config.success = config;
	}
	var _config= $.extend({
		ajax: true,
		type: "post",
		dataType: "json",
		autoClose: false,
		data: '', //额外在参数
		loadName:"正在保存...",
		loading: true,
		validate:function(){return true},
		beforeSend: function(){},
		success: function(){}
	},config);

	if(_config.autoClose){  //自动关闭当前窗口
		// _config.complete= function()
	}
	var time= (new Date()).getTime();
	_config.beforeSend= function(data){

		// if(config.beforeSend){
		// 	if(config.beforeSend(data,$(this))== false){  //请求开始前的动作
		// 		return false;
		// 	}
		// }
		// if(_config.loading){
		// 	// console.info("");
		// 	window["loading_"+time]= layer.load(_config.loadName);
		// }
	}
	_config.complete= function(data){
		// config.complete&&config.complete(data,$(this)); //请求结束后的动作
		// if(_config.loading){
		// 	setTimeout(function(){
		// 		layer.close(window["loading_"+time]);
		// 		if(_config.autoClose){
		// 			Win.closeMyself();
		// 		}
		// 	},1000);
		// }
        if($(".formLoadBtn"))
        {
            $(".formLoadBtn").button("reset");
        }
	}


	if(_config.ajax){
//        $(me).validate({
//            submitHandler: function(form){
//                //console.info("ajax form ....");
//                $(form).ajaxSubmit(_config);  // form ajax 异步提交
//            }
//        });

		$(me).html5Validate(function() {

            if($(".formLoadBtn"))
            {
                $(".formLoadBtn").button("loading");
            }
			if(!param)
			{
				param={};
				param = getFormDatas(me, param);
			}
//            var param = {};
			if(!me.attr("formAction"))
			{
				$(me).ajaxSubmit(_config);
			}
			else
			{
				$.ajax({
					url : me.attr("formAction"),
					type : "POST",
					data : JSON.stringify(param),
					dataType : "json",
					contentType: "application/json; charset=utf-8",
					success :  _config.success,
                    complete: function (data) {
                        _config.complete(data)
                    }
				});
			}

		}, {
			validate:_config.validate
		});



	}else{
		$(me).validate(); //一般form表单提交，跳转页面
	}
	$(me).submit();
}

$.fn.submitFileForm= function(config){

    var me= this;
    if (typeof(config)=== 'function'){ //传进来的是success函数
        config.success = config;
    }
    var _config= $.extend({
        ajax: true,
        type: "post",
        dataType: "json",
        autoClose: false,
        data: '', //额外在参数
        loadName:"正在保存...",
        loading: true,
        validate:function(){return true},
        beforeSend: function(){},
        success: function(){}
    },config);

    if(_config.autoClose){  //自动关闭当前窗口
        // _config.complete= function()
    }
    var time= (new Date()).getTime();
    _config.beforeSend= function(data){

        if(config.beforeSend){
            if(config.beforeSend(data,$(this))== false){  //请求开始前的动作
                return false;
            }
        }

    }
    _config.complete= function(data){
        if($(".formLoadBtn"))
        {
            $(".formLoadBtn").button("reset");
        }
        config.complete&&config.complete(data,$(this)); //请求结束后的动作

    }


    if (_config.ajax) {

        $(me).html5Validate(function() {

            if($(".formLoadBtn"))
            {
                $(".formLoadBtn").button("loading");
            }
            var url =   $(me).attr('action'),
                data = new FormData($(me)[0])

            $.ajax({
                url:url,
                type: 'post',
                data: data,
                myType:"file",
                processData: false,
                contentType: false,

                success: function (data) {

                    _config.success(data);
                },
                complete: function (data) {
                     _config.complete(data)
                }


            });


            // $(me).ajaxSubmit(_config); // form ajax 异步提交

        }, {
            validate:_config.validate
        });
    } else {
        $(me).validate(); // 一般form表单提交，跳转页面
    }
    $(me).submit();
}

/*
 * 省市区 三级级联
 */

$.fn.areaSelection = function(config) {
	var me = $(this);
	var _config = $.extend({
		provinceName : "province",
		cityName : "city",
		townName : "town",
		province : '',
		city : '',
		town : '',
		validate : false,
		parentCode : '',
		privinceURL : basePath + "/org/getSystemCodeList.action?type=province",
		cityURL : basePath + "/org/getSystemCodeList.action?type=city",
		townURL : basePath + "/org/getSystemCodeList.action?type=county",
		validateProvince : false,
		validateCity : false,
		validateTown : false,
		isShowMap : false,
		callBack : function() {
		}
	// parentCode:4
	// cityURL: pathMap.resourcePath+'/data/cityList.json',
	// townURL: pathMap.resourcePath+'/data/townList.json'
	}, config);
	if (_config.parentCode) {
		_config.privinceURL += "&parentCode=" + _config.parentCode;
	}
	var provinceSelection, citySelection, townSelection;
	// 添加省份控件
	me.append('<select num="0" id="provinceId" style="display: none;"  name="' + _config.provinceName + '"  val="' + _config.province
		+ '" ><option value="">-省份-</option></select>');
	provinceSelection = me.find("select[name='" + _config.provinceName + "']");
	provinceSelection.initSelectOptionsByURL(_config.privinceURL, {
		showTip : false,
		dataKey : null,
		valueName : 'codeCd',
		textName : 'codeDisplay',
		width : '88px',
		disable_search : false,
		type : 'post'
	}, function() {
		// 监听省份控件的change事件
		provinceSelection.change(function(event, obj) {
			if (provinceSelection.val() == "") {

				_config.change && _config.change({
					queryType : 1,
					areaId : $("#query_local").val()
				});
				destroySelect(me.find("select[name='" + _config.cityName + "']"));
				destroySelect(me.find("select[name='" + _config.townName + "']"));
				return;
			}
			(!obj) && (obj = {
				selected : provinceSelection.val()
			});
			if (_config.validateProvince) {
				if (!obj.selected) {
					provinceSelection.next().find("a").addClass("error-input");
				} else {
					provinceSelection.next().find("a").removeClass("error-input");
				}
			}
			if (_config.isShowMap) {
				enlargeMap($(this).find("option:selected").text(), 10);

				if ($(this).attr("num") == "0") {
					_config.callBack();
				}
				// $(this).attr("num","1");
			}

			// 如果下级select已经存在了，就删除掉
			destroySelect(me.find("select[name='" + _config.cityName + "']"));
			destroySelect(me.find("select[name='" + _config.townName + "']"));

			me.append('<select  num="0" id="cityId" style="display: none;"  name="' + _config.cityName + '"   val="' + _config.city + '"><option value="">-城市-</option></select>');
			citySelection = me.find("select[name='" + _config.cityName + "']");
			citySelection.initSelectOptionsByURL(_config.cityURL + "&parentCode=" + obj.selected, {
				showTip : false,
				dataKey : null,
				valueName : 'codeCd',
				textName : 'codeDisplay',
				width : '88px',
				disable_search : false,
				type : 'post'
			}, function() {
				// 监听城市控件的change事件
				citySelection.change(function(event, obj) {
					if (_config.isShowMap) {
						enlargeMap($(this).find("option:selected").text(), 10);

						if ($(this).attr("num") == "0") {
							_config.callBack();
						}
						// $(this).attr("num","1");
					}
					if (citySelection.val() == "") {
						_config.change && _config.change({
							queryType : 1,
							provinceId : provinceSelection.val()
						});
						destroySelect(me.find("select[name='" + _config.townName + "']"));
						return;
					}
					(!obj) && (obj = {
						selected : citySelection.val()
					});
					if (_config.validateCity) {
						if (!obj.selected) {
							citySelection.next().find("a").addClass("error-input");
						} else {
							citySelection.next().find("a").removeClass("error-input");
						}
					}
					// 如果下级select已经存在了，就删除掉
					destroySelect(me.find("select[name='" + _config.townName + "']"));
					me.append('<select id="townId"  num="0" style="display: none;"  name="' + _config.townName + '"  val="' + _config.town
						+ '"><option value="">-区/县-</option></select>');
					townSelection = me.find("select[name='" + _config.townName + "']");
					townSelection.initSelectOptionsByURL(_config.townURL + "&parentCode=" + obj.selected, {
						showTip : false,
						dataKey : null,
						valueName : 'codeCd',
						textName : 'codeDisplay',
						width : '90px',
						disable_search : false,
						type : 'post'
					});
					townSelection.change(function(event, obj) {
						if (_config.isShowMap) {
							enlargeMap($(this).find("option:selected").text(), 14);
							if ($(this).attr("num") == "0") {
								_config.callBack();
							}
							// $(this).attr("num","1");

						}
						if (townSelection.val() == "") {
							_config.change && _config.change({
								queryType : 1,
								cityId : citySelection.val()
							});
							return;
						}
						if (_config.validateTown) {
							if (!obj.selected) {
								townSelection.next().find("a").addClass("error-input");
							} else {
								townSelection.next().find("a").removeClass("error-input");
							}
						}
						_config.change && _config.change({
							queryType : 1,
							countyId : townSelection.val()
						});
					});
					_config.change && _config.change({
						queryType : 1,
						cityId : citySelection.val()
					});
				});
				if (_config.town) { // 如果有初始化的城市数据

					citySelection.trigger("change");
					delete _config.town;
				}
			});
			_config.change && _config.change({
				queryType : 1,
				provinceId : provinceSelection.val()
			});
		});

		// 省份数据已经初始化完毕！
		if (_config.city) { // 如果有初始化的城市数据
			provinceSelection.trigger("change");
			delete _config.city;
		}

	});

	function destroySelect(select) {
		$(select).chosen("destroy");
		$(select).remove();
	}

}

function preloadPages(urlMap) {
	$.preloadPages(urlMap);
}

function getRealMh(value, fix) {
	var unit = "kWh";
	var pe = 0;
	if (value)
		pe = parseFloat(value);
	if (!fix)
		fix = 2;
	if (pe > 1000) {
		pe = pe / 1000;
		unit = "MWh";
	}
	if (pe > 1000) {
		pe = pe / 1000;
		unit = "GWh";
	}
	pe = pe.toFixed(fix);
	return {
		value : pe,
		unit : unit
	};
}

$.fn.serializeObject = function() {
	var o = {};
	var a = this.serializeArray();
	$.each(a, function() {
		if (o[this.name] !== undefined) {
			if (!o[this.name].push) {
				o[this.name] = [ o[this.name] ];
			}
			o[this.name].push(this.value || '');
		} else {
			if ($.trim(this.value) == '')
				return;
			o[this.name] = this.value || '';
		}
	});
	return o;
};

$.filterEmpty = function() {
	var o = {};
	var a = this.serializeArray();
	$.each(a, function() {
		if (o[this.name] !== undefined) {
			if (!o[this.name].push) {
				o[this.name] = [ o[this.name] ];
			}
			o[this.name].push(this.value || '');
		} else {
			if ($.trim(this.value) == '')
				return;
			o[this.name] = this.value || '';
		}
	});
	return o;
};

$.rest = function(config) {
	var _conf = {
		type : "POST",
		dataType : "json",
		contentType : "application/json; charset=utf-8",
	};
	jQuery.extend(_conf, config);
	var _param = _conf.data;
	_conf.data = JSON.stringify(_param);
	return $.ajax(_conf);
}
