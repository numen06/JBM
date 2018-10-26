package com.jbm.dic.test;

import com.jbm.framework.ann.dic.Constant;

/**
 * Created by youg.gao on 2016/5/24.
 */
@Constant(namespaces = "evop_pile_deal_status")
public interface PileDealStatusDictValue {
    Integer OnLineFree = 0;
    Integer OnLineUsing = 1;
    Integer OnLineBooking = 2;
    Integer OnLineFault = 3;
    Integer OffLine = 4;
}
