package com.jbm.dic.test;

import com.jbm.framework.ann.dic.Constant;

/**
 * 充电桩状态-字典
 * Created by youg.gao on 2016/5/24.
 */
@Constant(namespaces = "evop_pile_deal_status")
public enum PileDealStatusDict {
    /**
     * 空闲中
     */
    OnLineFree,
    /**
     * 使用中
     */
    OnLineUsing,
    /**
     * 预约中
     */
    OnLineBooking,
    /**
     * 故障中
     */
    OnLineFault,
    /**
     * 离线
     */
    OffLine,
}
