package com.jbm.cluster.api.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

/**
 * 系统用户-登录账号
 *
 * @author wesley.zhang
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TableName("base_release_info")
public class BaseReleaseInfo extends MasterDataIdEntity {

    @ApiModelProperty(value = "发布日期")
    private Date releaseTime;

    @ApiModelProperty(value = "打包日期")
    private Date packageTime;

    @Column(columnDefinition = "TEXT")
    @ApiModelProperty(value = "发布内容")
    private String releaseContent;

    @ApiModelProperty(value = "发布人")
    private String userName;

    @ApiModelProperty(value = "版本号")
    private String versionNumber;

}
