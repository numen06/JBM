package com.jbm.framework.masterdata.usage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@Data
@TableName
@MappedSuperclass
@EqualsAndHashCode(callSuper = true)
public class MultiPlatformIdEntity extends MultiPlatformEntity {

    @ApiModelProperty("应用ID")
    private Long appId;


    public MultiPlatformIdEntity() {
        super();
    }

    public MultiPlatformIdEntity(Long id) {
        super();
        this.id = id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty("主键ID")
    private Long id;
}
