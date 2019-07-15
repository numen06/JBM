package jbm.framework.boot.autoconfigure.mybatis.handler;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

public class MasterdataObjectHandler implements MetaObjectHandler {
    /**
     * 新增时填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        if (metaObject.hasSetter("createTime"))
            metaObject.setValue("createTime", DateUtil.date());
        if (metaObject.hasSetter("updateTime"))
            metaObject.setValue("updateTime", DateUtil.date());
    }

    /**
     * 更新时填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        if (metaObject.hasSetter("updateTime"))
            setFieldValByName("updateTime", DateUtil.date(), metaObject);
    }
}
