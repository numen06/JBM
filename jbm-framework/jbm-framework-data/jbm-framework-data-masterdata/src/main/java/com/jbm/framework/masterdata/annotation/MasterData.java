package com.jbm.framework.masterdata.annotation;

import com.baomidou.mybatisplus.annotation.TableName;

import javax.persistence.Entity;
import java.lang.annotation.*;

@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Entity
@TableName
public @interface MasterData {


}
