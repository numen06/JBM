package com.jbm.test;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Test;

public class RegTest {

    @Test
    public void test() {
        String str = "SELECT\n" +
                "\t* \n" +
                "FROM\n" +
                "\t\"gatewayLogs\" \n" +
                "WHERE\n" +
                "\ttime > now() - 5m \n" +
                "\tAND path =~/ \\ / jaja / \n" +
                "\tAND apiName =~/获取/ \n" +
                "\tAND ip =~/获取/ \n" +
                "\tAND serviceId = \n" +
                "\tAND method =~/获取/ \n" +
                "\tAND httpStatus = \n" +
                "\tAND requestRealName =~/获取/ \n" +
                "\tAND requestUserId = \n" +
                "\tAND requestTime = \n" +
                "ORDER BY\n" +
                "\ttime DESC \n" +
                "\tLIMIT 100";
        //需要body和《hu之间的字符串。定义正则表达式。
        String reg = "(?<=SELECT).*?(?=FROM)";
        //上述正则表达式利用了：获取指定字符串之后：  (?<=指定字符串)
        //获取指定字符串之前： (?=指定字符串)，实现

        String ff = ReUtil.getGroup0(reg, str);
//        System.out.println(ff);
        String out = StrUtil.replace(str, ff, "test");

        String reg2 = "(?<=LIMIT).*";

        String dd = ReUtil.getGroup0(reg2, out);

        out = StrUtil.replace(str, dd, "");

        System.out.println(out);
    }

    @Test
    public void test2() {

        String str = "SELECT\n" +
                "    *\n" +
                "FROM\n" +
                "    \"gatewayLogs\"\n" +
                "WHERE\n" +
                "                \tAND apiName =~/查询/\n" +
                "                \tAND method ='GET'\n" +
                "                ORDER BY\n" +
                "    time DESC\n";
        //需要body和《hu之间的字符串。定义正则表达式。
        String reg = "(?<=WHERE).*?AND";
        //上述正则表达式利用了：获取指定字符串之后：  (?<=指定字符串)
        //获取指定字符串之前： (?=指定字符串)，实现

        String ff = ReUtil.getGroup0(reg, str);
        System.out.println(ff);
        String out = StrUtil.replaceFirst(str, ff, "test$1");
        System.out.println(out);
    }

    @Test
    public void test3() {

        String str = "SELECT\n" +
                "    *\n" +
                "FROM\n" +
                "    \"gatewayLogs\"\n" +
                "WHERE\n" +
                "                ORDER BY\n" +
                "    time DESC\n";
        //需要body和《hu之间的字符串。定义正则表达式。
        String reg = "WHERE.*?(?=ORDER BY)";
        //上述正则表达式利用了：获取指定字符串之后：  (?<=指定字符串)
        //获取指定字符串之前： (?=指定字符串)，实现

        String ff = ReUtil.getGroup0(reg, str);
        System.out.println(ff);
        String out = StrUtil.replaceFirst(str, ff, "test$1");
        System.out.println(out);
    }
}
