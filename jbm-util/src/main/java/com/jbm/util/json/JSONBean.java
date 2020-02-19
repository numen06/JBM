package com.jbm.util.json;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.lang.Validator;
import com.alibaba.fastjson.JSONObject;
import com.jbm.util.StringUtils;

public class JSONBean extends JSONObject {


    /**
     * 获取字符串的同时去掉前后空格
     *
     * @param key
     * @return
     */
    public String getStringByTrimToNull(String key) {
        return StringUtils.trimToNull(this.getString(key));
    }

    /**
     * 获取字段的同时判断字段是否为空
     *
     * @param key
     * @return
     * @throws ValidateException
     */
    public String getNotBlankString(String key) throws ValidateException {
        String val = this.getString(key);
        if (StringUtils.isBlank(val))
            throw new ValidateException("字段为空");
        return StringUtils.trimToNull(val);
    }

    /**
     * 获取字段的同时判断是否是email
     *
     * @param key
     * @return
     * @throws ValidateException
     */
    public String getEmail(String key) throws ValidateException {
        String val = this.getString(key);
        Validator.validateEmail(val, "邮箱格式验证错误");
        return val;
    }

    /**
     * 获取字段的同时判断是不是手机号
     *
     * @param key
     * @return
     * @throws ValidateException
     */
    public String getMobile(String key) throws ValidateException {
        String val = this.getString(key);
        Validator.validateMobile(val, "手机格式验证错误");
        return val;
    }

    /**
     * 获取字段的同时判断是不是符合正则表达式
     *
     * @param key
     * @param regex
     * @param errorMsg
     * @return
     * @throws ValidateException
     */
    public String getStringByRegex(String key, String regex, String errorMsg) throws ValidateException {
        String val = this.getString(key);
        Validator.validateMatchRegex(regex, val, errorMsg);
        return val;
    }

    /**
     * 判断是否存在某一类对象
     *
     * @param clazz
     * @return
     */
    public Boolean containsClass(Class clazz) {
        return super.containsKey(StringUtils.uncapitalize(clazz.getSimpleName()));
    }

}
