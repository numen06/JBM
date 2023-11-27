package com.jbm.test.doc;


import cn.hutool.core.util.ReUtil;
import com.jbm.util.PatternMatchUtils;

public class ReTest {

    public static void main(String[] args) {
        String string = "/get/64e967af97ac478f2a919891.jpg";

        // 正则表达式
        String pattern = "/get/{filePath}";

        String replacedString = ReUtil.replaceAll(pattern, "\\{\\w+\\}", (match) -> {
            String filePath = match.group(0);
            return "*";
        });
        System.out.println(replacedString);


        // 进行匹配
        System.out.println("匹配到的文件路径为: " + PatternMatchUtils.simpleMatch(replacedString, string));
    }
}

