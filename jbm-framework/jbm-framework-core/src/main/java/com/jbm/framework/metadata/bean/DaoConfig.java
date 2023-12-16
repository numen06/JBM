package com.jbm.framework.metadata.bean;

import org.apache.commons.collections.FastHashMap;
import org.mapstruct.ap.shaded.freemarker.template.utility.ClassUtil;

import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: Joe Xie Date: 14-9-3 Time: 下午4:46
 *
 * @author Wesley
 */
public class DaoConfig {

    // private static Map<String, ClsInfo> config;

    private Boolean generate = true;

    private Map<Class<?>, ClsInfo> classConfigMap;

    // public Map<String, ClsInfo> getConfig() {
    // return config;
    // }

    public Map<Class<?>, ClsInfo> getClassConfigMap() {
        return classConfigMap;
    }

    @SuppressWarnings("unchecked")
    private final void changeToClassMap(Map<String, ClsInfo> config) {
        classConfigMap = new FastHashMap();
        for (String key : config.keySet()) {
            try {
                classConfigMap.put(ClassUtil.forName(key), config.get(key));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (LinkageError e) {
                e.printStackTrace();
            }
        }
    }

    public final void setConfig(Map<String, ClsInfo> config) {
        changeToClassMap(config);
    }

    // public static ClsInfo getClsInfo(String cls) {
    // return classConfigMap.get(cls);
    // }

    public <T> ClsInfo findClsInfo(T entity) {
        return findClsInfo(entity.getClass());
    }

    public <T> ClsInfo findClsInfo(Class<T> clazz) {
        ClsInfo clsInfo = this.classConfigMap.get(clazz);
        return clsInfo;
    }

    public String findPrimaryKey(Class<?> clazz) {
        ClsInfo clsInfo = findClsInfo(clazz);
        if (clsInfo == null)
            return null;
        return clsInfo.getIdColumn();
    }

    public String findPrimaryKey(Object entity) {
        ClsInfo clsInfo = findClsInfo(entity);
        if (clsInfo == null)
            return null;
        return clsInfo.getIdColumn();
    }

    public Boolean getGenerate() {
        return generate;
    }

    public void setGenerate(Boolean generate) {
        this.generate = generate;
    }

}
