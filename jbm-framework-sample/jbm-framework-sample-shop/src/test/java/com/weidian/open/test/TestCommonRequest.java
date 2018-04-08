package com.weidian.open.test;

import com.weidian.open.sdk.request.CommonRequest;
import org.junit.Test;

/**
 * Created by wangyadi on 16/7/7.
 */
public class TestCommonRequest extends BaseTest{

    @Test
    public void testVdianOrderListGet() throws Exception {
        String result = new CommonRequest(token).vdianOrderListGet("1", "2", null, null, null, "2015-11-12 16:36:08", "2016-11-12 16:36:08");
        LOGGER.debug("response:{}\n", result);
    }
}
