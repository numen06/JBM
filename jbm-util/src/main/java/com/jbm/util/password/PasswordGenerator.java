package com.jbm.util.password;

import cn.hutool.core.util.RandomUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @program: JBM7
 * @author: wesley.zhang
 * @create: 2020-07-08 22:27
 **/
public class PasswordGenerator {
    public static final char[] allowedSpecialCharactors = {
            '`', '~', '@', '#', '$', '%', '^', '&',
            '*', '(', ')', '-', '_', '=', '+', '[',
            '{', '}', ']', '\\', '|', ';', ':', '"',
            '\'', ',', '<', '.', '>', '/', '?'};//密码能包含的特殊字符
    private static final int letterRange = 26;
    private static final int numberRange = 10;
    private static final int spCharactorRange = allowedSpecialCharactors.length;
    private int passwordLength;//密码的长度
    private int minVariousType = 4;//密码包含字符的最少种类

    public PasswordGenerator(int passwordLength) {
        if (minVariousType > CharactorType.values().length) minVariousType = CharactorType.values().length;
        if (minVariousType > passwordLength) minVariousType = passwordLength;
        this.passwordLength = passwordLength;
        this.minVariousType = minVariousType;
    }

    public String generateRandomPassword() {
        char[] password = new char[passwordLength];
        List<Integer> pwCharsIndex = new ArrayList();
        for (int i = 0; i < password.length; i++) {
            pwCharsIndex.add(i);
        }
        List<CharactorType> takeTypes = new ArrayList(Arrays.asList(CharactorType.values()));
        List<CharactorType> fixedTypes = Arrays.asList(CharactorType.values());
        int typeCount = 0;
        while (pwCharsIndex.size() > 0) {
            int pwIndex = pwCharsIndex.remove(RandomUtil.randomInt(pwCharsIndex.size()));//随机填充一位密码
            Character c;
            if (typeCount < minVariousType) {//生成不同种类字符
                c = generateCharacter(takeTypes.remove(RandomUtil.randomInt(takeTypes.size())));
                typeCount++;
            } else {//随机生成所有种类密码
                c = generateCharacter(fixedTypes.get(RandomUtil.randomInt(fixedTypes.size())));
            }
            password[pwIndex] = c.charValue();
        }
        return String.valueOf(password);
    }

    private Character generateCharacter(CharactorType type) {
        Character c = null;
        int rand;
        switch (type) {
            case LOWERCASE://随机小写字母
                rand = RandomUtil.randomInt(letterRange);
                rand += 97;
                c = new Character((char) rand);
                break;
            case UPPERCASE://随机大写字母
                rand = RandomUtil.randomInt(letterRange);
                rand += 65;
                c = new Character((char) rand);
                break;
            case NUMBER://随机数字
                rand = RandomUtil.randomInt(numberRange);
                rand += 48;
                c = new Character((char) rand);
                break;
            case SPECIAL_CHARACTOR://随机特殊字符
                rand = RandomUtil.randomInt(spCharactorRange);
                c = new Character(allowedSpecialCharactors[rand]);
                break;
        }
        return c;
    }


}
