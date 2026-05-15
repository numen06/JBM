package com.jbm.framework.masterdata.annotation;

import com.baomidou.mybatisplus.annotation.TableName;

import java.lang.annotation.*;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@TableName
public @interface MasterData {


}
