package com.jbm.util;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.util.StrUtil;
import com.jbm.util.password.PasswordGenerator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: JBM6
 * @author: wesley.zhang
 * @create: 2020-07-03 12:54
 **/
public class PasswordUtils {

    private static final String DEFAULT_QUERY_REGEX = "[!$^&*+=|{}';'\",<>/?~！#￥%……&*——|{}【】‘；：”“'。，、？]";
    private static final String[] DEFAULT_REGEX = new String[]{"\\d{2}", "[a-z]{2}", "[A-Z]+"};

    /**
     * 验证密码重复性
     *
     * @param originPassword
     * @param currentPassword
     * @param confirmPassword
     * @throws ValidateException
     */
    public static void validatorPassword(String originPassword, String currentPassword, String confirmPassword) {
        boolean unified = StrUtil.equals(currentPassword, confirmPassword);
        if (!unified) {
            throw new ValidateException("重复密码错误");
        }
        unified = StrUtil.equals(originPassword, currentPassword);
        if (unified) {
            throw new ValidateException("密码不能和原密码一致");
        }
    }

    /**
     * 判断查询参数中是否以特殊字符开头，如果以特殊字符开头则返回true，否则返回false
     *
     * @param value
     * @return
     */
    public static boolean specialSymbols(String value) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        Pattern pattern = Pattern.compile(DEFAULT_QUERY_REGEX);
        Matcher matcher = pattern.matcher(value);
        char[] specialSymbols = DEFAULT_QUERY_REGEX.toCharArray();
        boolean isStartWithSpecialSymbol = false; // 是否以特殊字符开头
        for (int i = 0; i < specialSymbols.length; i++) {
            char c = specialSymbols[i];
            if (value.indexOf(c) == 0) {
                isStartWithSpecialSymbol = true;
                break;
            }
        }
        return matcher.find() && isStartWithSpecialSymbol;
    }

    public static int checkPassword(String passwordStr) {
        int sort = 0;
        try {
            if (StrUtil.isBlank(passwordStr)) {
                return 0;
            }
            if (passwordStr.length() < 6) {
                return 0;
            }
            for (String reg : DEFAULT_REGEX) {
                Pattern pattern = Pattern.compile(reg);
                Matcher matcher = pattern.matcher(passwordStr);
                if (matcher.find())
                    sort++;
            }
            if (passwordStr.length() > 7) {
                sort++;
            }
            if (specialSymbols(passwordStr)) {
                sort++;
            }
        } catch (Exception e) {
            return 0;
        }
        return sort;
    }

    public static String generatePassword(int passwordLength) {
        PasswordGenerator passwordGenerator = new PasswordGenerator(passwordLength);
        return passwordGenerator.generateRandomPassword();
    }

    public static String generatePassword() {
        PasswordGenerator passwordGenerator = new PasswordGenerator(10);
        return passwordGenerator.generateRandomPassword();
    }

    private enum CharactorType {
        LOWERCASE,
        UPPERCASE,
        NUMBER,
        SPECIAL_CHARACTOR
    }


}
