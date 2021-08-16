package jbm.framework.boot.autoconfigure.swagger.exp;

import lombok.Data;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-03-24 04:42
 **/
@Data
public class ApiJsonPropertyBean {

    /**
     * 类型
     */
    private Class type;

    /**
     * 字段名称
     */
    private String key;


    /**
     * 描述
     */
    private String description = "";

    /**
     * 是否必要
     */
    private boolean required = false;

    public ApiJsonPropertyBean() {
        
    }

    public ApiJsonPropertyBean(Class type, String description) {
        this.type = type;
        this.description = description;
    }
}
