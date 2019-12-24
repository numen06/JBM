package com.jbm.framework.masterdata.usage.bean;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import javax.persistence.MappedSuperclass;
import java.util.Date;

/**
 * 基础类模型
 *
 * @author wesley
 */
@MappedSuperclass
@Data
public class MasterDataEntity extends AdvEntity {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}
