package com.jbm.cluster.api.entitys.doc;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.converters.StringConverter;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.util.bean.Version;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("文档管理")
@TableName
public class BaseDoc extends MasterDataEntity {

    @Id
    @TableId(type = IdType.ASSIGN_UUID)
    @ApiModelProperty(value = "文档ID")
    private String docId;

    @ApiModelProperty(value = "文档名称")
    private String docName;

    @ApiModelProperty(value = "文档大小")
    private Long size;

    @ApiModelProperty(value = "文档分组ID")
    private String docGroupId;

    @ApiModelProperty(value = "文档分组")
    private String docGroup;

    @ApiModelProperty(value = "文档路径")
    private String docPath;

    @ApiModelProperty(value = "文档状态")
    private String state;

    @ApiModelProperty(value = "文档类型")
    private String contentType;

    @ApiModelProperty(value = "有效分钟")
    private Long effectiveTime;

    @ApiModelProperty(value = "过期时间")
    private Date expirationTime;

    @ApiModelProperty(value = "文档版本")
    @Convert(converter = StringConverter.class)
    private Version version;

    @ApiModelProperty(value = "文档创建者")
    private Long creator;

//    @ApiModelProperty(value = "文档修改者")
//    private String modifier;
}
