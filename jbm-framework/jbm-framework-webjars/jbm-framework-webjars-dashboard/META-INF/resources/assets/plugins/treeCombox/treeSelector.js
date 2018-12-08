/**
 * Created with IntelliJ IDEA. User: tom Date: 14-10-16 Time: 下午4:35 To change
 * this template use File | Settings | File Templates.
 */

(function($) {
	$.fn.comboBox = function(params) {
		var me = this;
		//$(".menuContent").remove();
		var config = $.extend({
			data : [],
			values : [],
			url : '',
			type : 'post',
			params : null,
			input : $("#departmentIds"),
			radio: false
		}, params);
		var setting = {
			check : {
				enable : true,
				chkboxType:{ "Y" : "ps", "N" : "s" }
			},
			view : {
				dblClickExpand : false
			},
			data : {
				simpleData : $.extend({
					enable : true,
					idKey : "id",
					pIdKey : "pid",
					rootPId : 0
				}, params.simpleDataConfig),
				key : $.extend({
					checked : "isChecked",
					children : "",
					title : ''
				}, params.keyConfig)

			},
			callback : {
				onCheck : onCheck
			}
		};
		//setting.check.chkboxType = { "Y" : "ps", "N" : "ps" };
		if(config.radio){
			setting.check.chkStyle = "radio";
			setting.check.radioType = "all";
		}
		me.click(function() {
			var offset = me.offset();
			me.siblings(".menuContent").css({
				left : offset.left + "px",
				top : offset.top + me.outerHeight() + "px"
			}).slideDown("fast");
			$("body").bind("mousedown", onBodyDown);
		});
		var uid = generateUID();
		var treeId = uid + "_comboTree";
		var _w = config.width || me.width();
		var _h = config.height || 100;
		if (config.values.length == 0) {
			config.values = me.val().split(',');
			// alert(config.values);
		}
		for (var i = 0; i < config.values.length; i++) {
			config.values[i] = parseInt(config.values[i]);
		}
		
		
		$("div[data-ref='"+me.attr("id")+"']").remove();
				
		var treePanel = '<div  class="menuContent" data-ref="'+me.attr("id")+'" style="display:none; position: absolute;"><ul id="'
				+ treeId
				+ '" class="ztree" '
				+ ' style="margin-top:0; width:'
				+ _w
				+ 'px; min-height: 100px;max-height:300px;height:'
				+ _h
				+ 'px;" ></ul><input type="hidden" name="'
				+ me.attr("fieldName")
				+ '" value="'
				+ config.values.join()
				+ '"/></div>;'
		$(treePanel).insertAfter(this);
		me.treePanel = $(treePanel);

		if (config.data.length == 0) {
			if (config.url) {
				// for (var i = 0; i < data.length; i++) {
				asyncJson(config);
				// }
			}
		}

		function asyncJson(config, param) {
			$.ajax({
				type : config.type,
				dataType : 'json',
				data : param ? param : config.params,
				async : false,
				url : config.url,
				success : function(data) {
					if (!config.data)
						config.data = [];
					Array.prototype.push.apply(config.data, data);
					for (var i = 0; i < data.length; i++) {
						asyncJson(config, {
							pid : data[i].id
						})
					}
					// config.data.concat(data);
				}
			});
		}

		initValues(config.values, config.data);
		
		me.commboTree = $.fn.zTree.init($("#" + treeId), setting, config.data);
		//setCheck();

		function initValues(values, data) {
			//var departmentObj = $("#departmentIds");
			if (isNaN(values[0])) {
				config.input.attr("value", "");
			} else {
				config.input.attr("value", values);
			}
			var allNodes = data, names = [];
			for (var i = 0; i < allNodes.length; i++) {
				var node = allNodes[i];
				if ($.inArray(node.id, values) >= 0) {
					node.isChecked = true;
					names.push(node.name);
				}
			}
			me.val(names);
		}
		function hideMenu() {
			$(".menuContent").fadeOut("fast");
			$("body").unbind("mousedown", onBodyDown);
		}
		function onBodyDown(event) {
			if (!(event.target.id == "menuBtn"
					|| event.target.id == me.attr("id")
					|| event.target.id == "menuContent" || $(event.target)
					.parents(".menuContent").length > 0)) {
				hideMenu();
			}
		}
		function onCheck(e, treeId, treeNode) {
			var zTree = $.fn.zTree.getZTreeObj(treeId);
			//zTree.setting.check.chkboxType = { "Y" : "ps", "N" : "ps" };
			/*if(config.single){
				var allNodes = zTree.getNodes();
				for (var i=0, l=allNodes.length; i < l; i++) {
					if(treeNode!=allNodes[i]){   //
						if(allNodes[i] && allNodes[i]!=null) {
							zTree.checkNode(allNodes[i], false);
						}
					}
				}
			}*/

			var nodes = zTree.getCheckedNodes(true), values = [], names = [];
			for (var i = 0, l = nodes.length; i < l; i++) {
				var node = nodes[i];
				if(node.getCheckStatus().half)
					continue;
				values.push(node.id);
				names.push(node.name);
			}
			me.val(names.join(","));
			me.siblings(".menuContent").find("input[name='" + me.attr("fieldName") + "']").val(values.join(","));
			config.input.attr("value", values);
			config.afterCheck&&config.afterCheck();
		}
	
		return me;
	};
})(jQuery);
