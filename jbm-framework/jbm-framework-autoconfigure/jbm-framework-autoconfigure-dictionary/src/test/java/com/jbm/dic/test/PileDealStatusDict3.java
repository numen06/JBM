package com.jbm.dic.test;

/**
 * 充电桩状态-字典
 * Created by youg.gao on 2016/5/24.
 */
//@Constant(namespaces = "evop_pile_deal_status")
public enum PileDealStatusDict3 {
    /**
     * 空闲中
     */
    OnLineFree("空闲中"),
    /**
     * 使用中
     */
    OnLineUsing("使用中"),
    /**
     * 预约中
     */
    OnLineBooking("预约中"),
    /**
     * 故障中
     */
    OnLineFault("故障中"),
    /**
     * 离线
     */
    OffLine("离线"),
    ;

    private String test;

    PileDealStatusDict3(String test) {
        this.test = test;
    }


}
