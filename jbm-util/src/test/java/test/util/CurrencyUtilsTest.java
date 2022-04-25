package test.util;

import com.jbm.util.CurrencyUtils;
import org.junit.Test;

import java.text.NumberFormat;

public class CurrencyUtilsTest {

    /**
     * 测试方法入口
     *
     * @author
     * @version 1.00.00
     * @date 2018年1月18日
     */
    @Test
    public void test() throws Exception {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        System.out.println(currencyFormat.format(222222222222L));
        //554,545.4544;
        System.out.println(CurrencyUtils.yuan2fen(120.445));
        System.out.println(CurrencyUtils.formatAmountCny(1200.35));
        System.out.println(CurrencyUtils.formatAmountCny(12000.35));
        System.out.println(CurrencyUtils.formatAmountCny(120000.35));
        System.out.println(CurrencyUtils.formatAmountCny(1200000.35));
        System.out.println(CurrencyUtils.formatAmountCny(12000000.35));
        System.out.println(CurrencyUtils.formatAmountCny(120000000.35));
        System.out.println(CurrencyUtils.formatAmountCny(1200000000.35));
    }
}
