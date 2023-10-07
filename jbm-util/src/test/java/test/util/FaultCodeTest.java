package test.util;

import com.jbm.util.FaultCodeUtil;
import org.junit.Test;

public class FaultCodeTest {

    @Test
    public void makeFaultCode() {
        String ret = FaultCodeUtil.makeFaultCode("1", "0000", "1", "0100");
        System.out.println(ret);
    }
}
