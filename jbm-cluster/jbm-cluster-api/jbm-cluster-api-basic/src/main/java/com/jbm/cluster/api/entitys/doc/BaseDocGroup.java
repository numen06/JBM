package com.jbm.cluster.api.entitys.doc;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import com.jbm.framework.masterdata.usage.entity.MasterDataIdEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("文档分组管理")
@TableName
public class BaseDocGroup extends MasterDataEntity {

    @Id
    @TableId(type = IdType.ASSIGN_UUID)
    @ApiModelProperty(value = "文档ID")
    private String groupId;
    @ApiModelProperty(value = "分组路径")
    private String groupPath;
    @ApiModelProperty(value = "文档过期时间")
    private Date expirationTime;
    @ApiModelProperty(value = "是否自动清理")
    private Boolean autoClear;
    @ApiModelProperty(value = "最大条目数")
    private Integer maxQuantity;
    @ApiModelProperty(value = "文档操作token")
    private String tokenKey;
    @ApiModelProperty(value = "分组名称")
    private String docGroupName;
}
