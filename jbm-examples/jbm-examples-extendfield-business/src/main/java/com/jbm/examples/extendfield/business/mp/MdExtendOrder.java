package com.jbm.examples.extendfield.business.mp;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jbm.framework.masterdata.usage.entity.MasterDataEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "md_extend_order", autoResultMap = true)
public class MdExtendOrder extends MasterDataEntity {

    private static final long serialVersionUID = 1L;

    private String orderNo;

    private String title;
}
