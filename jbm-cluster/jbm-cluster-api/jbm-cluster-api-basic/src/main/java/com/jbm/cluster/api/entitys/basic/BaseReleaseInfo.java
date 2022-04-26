package com.jbm.cluster.api.entitys.basic;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
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

    @ApiModelProperty(value = "包下载地址")
    private String packageUrl;

    @ApiModelProperty(value = "关联APPID")
    private Long appId;


}
