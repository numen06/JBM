package com.jbm.micro.mysql.mp;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 扩展字段示例：继承 {@link MasterDataEntity}，{@code extend_data} 存 MySQL/H2 JSON。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "md_extend_demo", autoResultMap = true)
public class MdExtendDemo extends MasterDataEntity {

    private static final long serialVersionUID = 1L;

    private String bizCode;

    private String title;
}
