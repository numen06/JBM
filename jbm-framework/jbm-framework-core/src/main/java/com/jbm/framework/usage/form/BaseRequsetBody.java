package com.jbm.framework.usage.form;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.TypeUtil;
import com.alibaba.fastjson.JSON;
import com.jbm.util.StringUtils;
import com.jbm.util.json.JSONBean;
import io.swagger.annotations.ApiModel;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-02-19 21:28
 **/
@ApiModel(value = "请求实体")
public class BaseRequsetBody extends JSONBean {

    private static final long serialVersionUID = 1L;

    public BaseRequsetBody() {
        super();
    }


    public BaseRequsetBody(Map map) {
        super();
        this.putAll(map);
    }

    @Override
    public Object put(String key, Object value) {
        return super.put(key, value);
    }


    /**
     * 默认获取同类名字<br/>
     * 段如果没有会在ROOT节点下查找
     *
     * @param clazz
     * @return
     */
    public <T> T tryGet(Class<T> clazz) {
        return tryGet(StringUtils.uncapitalize(clazz.getSimpleName()), clazz);
    }


    public <T> List<T> tryGetList(Class<T> clazz) {
        String key = StringUtils.uncapitalize(clazz.getSimpleName()) + "s";
        if (this.containsKey(key)) {
            return this.getJSONArray(key).toJavaList(clazz);
        }
        return JSON.parseArray(this.toJSONString(), clazz);
    }

    public <T> List<T> getList(String key, Class<T> clazz) {
        return this.getJSONArray(key).toJavaList(clazz);
    }

    /**
     * 在文本中获取相关字段
     *
     * @param name  字段名称
     * @param clazz 转换类类型
     * @return
     */
    public <T> T tryGet(String name, Class<T> clazz) {
        if (StringUtils.isNotBlank(name) && this.containsKey(name)) {
            return JSON.toJavaObject(this.getJSONObject(name), clazz);
        }
        return JSON.toJavaObject(this, clazz);
    }

    public Map<String, Object> tryToMap() {
        return this;
    }


}
