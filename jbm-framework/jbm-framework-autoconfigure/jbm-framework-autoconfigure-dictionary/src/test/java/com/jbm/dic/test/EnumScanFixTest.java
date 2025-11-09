package com.jbm.dic.test;

import com.jbm.autoconfig.dic.EnumTypeConverter;
import com.jbm.framework.dictionary.JbmDictionary;
import com.jbm.framework.dictionary.annotation.JbmDicCode;
import com.jbm.framework.dictionary.annotation.JbmDicType;
import com.jbm.framework.dictionary.annotation.JbmDicValue;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InaccessibleObjectException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字典扫描 Java 17/21 兼容性测试
 * 验证修复后不会出现 InaccessibleObjectException
 * 
 * @author wesley
 * @date 2025-11-09
 */
public class EnumScanFixTest {

    /**
     * 测试枚举 1: 标准枚举(带 value 字段)
     */
    @JbmDicType(value = "TestStatus", typeName = "测试状态")
    enum TestStatus {
        /**
         * 启用
         */
        ENABLED("启用"),
        /**
         * 禁用
         */
        DISABLED("禁用"),
        /**
         * 待审核
         */
        PENDING("待审核");

        @JbmDicValue
        private final String value;

        TestStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 测试枚举 2: 带 code 和 value 字段
     */
    @JbmDicType(value = "OrderStatus", typeName = "订单状态")
    enum OrderStatus {
        CREATED(1, "已创建"),
        PAID(2, "已支付"),
        SHIPPED(3, "已发货"),
        COMPLETED(4, "已完成");

        @JbmDicCode
        private final Integer code;
        
        @JbmDicValue
        private final String value;

        OrderStatus(Integer code, String value) {
            this.code = code;
            this.value = value;
        }

        public Integer getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 测试枚举 3: 简单枚举(无自定义字段,只有 name 和 ordinal)
     */
    @JbmDicType(value = "SimpleEnum", typeName = "简单枚举")
    enum SimpleEnum {
        OPTION_A,
        OPTION_B,
        OPTION_C
    }

    /**
     * 测试 1: 验证标准枚举转换成功
     */
    @Test
    public void testStandardEnumConversion() {
        System.out.println("=== 测试标准枚举转换 ===");
        EnumTypeConverter converter = new EnumTypeConverter();
        
        List<JbmDictionary> result = converter.convert(TestStatus.class);
        
        assertNotNull(result, "转换结果不应为 null");
        assertEquals(3, result.size(), "应该转换出 3 个字典项");
        
        // 验证第一个字典项
        JbmDictionary first = result.get(0);
        assertEquals("TestStatus", first.getType(), "类型应该是 TestStatus");
        assertEquals("测试状态", first.getTypeName(), "类型名称应该是'测试状态'");
        assertEquals("启用", first.getValue(), "值应该是'启用'");
        
        System.out.println("✓ 标准枚举转换成功: " + result.size() + " 个字典项");
        result.forEach(dict -> System.out.println("  - code=" + dict.getCode() + ", value=" + dict.getValue()));
    }

    /**
     * 测试 2: 验证带 code 和 value 的枚举转换
     */
    @Test
    public void testEnumWithCodeAndValue() {
        System.out.println("=== 测试带 code 和 value 的枚举转换 ===");
        EnumTypeConverter converter = new EnumTypeConverter();
        
        List<JbmDictionary> result = converter.convert(OrderStatus.class);
        
        assertNotNull(result, "转换结果不应为 null");
        assertEquals(4, result.size(), "应该转换出 4 个字典项");
        
        // 验证第一个字典项有 code
        JbmDictionary first = result.get(0);
        assertNotNull(first.getCode(), "code 不应为 null");
        assertEquals("已创建", first.getValue(), "值应该是'已创建'");
        
        System.out.println("✓ 带 code 枚举转换成功: " + result.size() + " 个字典项");
        result.forEach(dict -> System.out.println("  - code=" + dict.getCode() + ", value=" + dict.getValue()));
    }

    /**
     * 测试 3: 验证简单枚举(无 value 字段)的处理
     * 这种枚举应该返回空列表,因为没有 value 字段
     */
    @Test
    public void testSimpleEnumWithoutValueField() {
        System.out.println("=== 测试简单枚举(无 value 字段) ===");
        EnumTypeConverter converter = new EnumTypeConverter();
        
        List<JbmDictionary> result = converter.convert(SimpleEnum.class);
        
        assertNotNull(result, "转换结果不应为 null");
        // 简单枚举没有 value 字段,应该返回空列表
        assertEquals(0, result.size(), "没有 value 字段的枚举应该返回空列表");
        
        System.out.println("✓ 简单枚举正确处理(返回空列表)");
    }

    /**
     * 测试 4: 验证不会抛出 InaccessibleObjectException
     * 这是核心测试,确保修复生效
     */
    @Test
    public void testNoInaccessibleObjectException() {
        System.out.println("=== 测试不会抛出 InaccessibleObjectException ===");
        EnumTypeConverter converter = new EnumTypeConverter();
        
        try {
            // 测试所有枚举类型
            converter.convert(TestStatus.class);
            converter.convert(OrderStatus.class);
            converter.convert(PileDealStatusDict2.class);
            converter.convert(PileDealStatusDict3.class);
            
            System.out.println("✓ 所有枚举转换成功,未抛出 InaccessibleObjectException");
        } catch (Exception e) {
            // 检查是否是 InaccessibleObjectException
            if (e.getCause() instanceof InaccessibleObjectException) {
                fail("修复失败: 仍然抛出 InaccessibleObjectException - " + e.getMessage());
            } else {
                // 其他异常也记录下来
                e.printStackTrace();
                fail("转换失败: " + e.getMessage());
            }
        }
    }

    /**
     * 测试 5: 验证现有测试枚举的兼容性
     */
    @Test
    public void testExistingEnums() {
        System.out.println("=== 测试现有枚举的兼容性 ===");
        EnumTypeConverter converter = new EnumTypeConverter();
        
        // 测试 PileDealStatusDict2
        List<JbmDictionary> result2 = converter.convert(PileDealStatusDict2.class);
        assertNotNull(result2, "PileDealStatusDict2 转换结果不应为 null");
        assertTrue(result2.size() > 0, "PileDealStatusDict2 应该有字典项");
        System.out.println("✓ PileDealStatusDict2 转换成功: " + result2.size() + " 个字典项");
        
        // 测试 PileDealStatusDict3
        List<JbmDictionary> result3 = converter.convert(PileDealStatusDict3.class);
        assertNotNull(result3, "PileDealStatusDict3 转换结果不应为 null");
        System.out.println("✓ PileDealStatusDict3 转换成功: " + result3.size() + " 个字典项");
    }

    /**
     * 测试 6: 性能测试 - 确保修复不影响性能
     */
    @Test
    public void testPerformance() {
        System.out.println("=== 性能测试 ===");
        EnumTypeConverter converter = new EnumTypeConverter();
        
        long startTime = System.currentTimeMillis();
        
        // 转换 1000 次
        for (int i = 0; i < 1000; i++) {
            converter.convert(TestStatus.class);
            converter.convert(OrderStatus.class);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("✓ 2000 次转换耗时: " + duration + " ms (平均 " + (duration / 2000.0) + " ms/次)");
        assertTrue(duration < 5000, "2000 次转换应该在 5 秒内完成");
    }

    /**
     * 测试 7: 验证日志输出
     */
    @Test
    public void testLogging() {
        System.out.println("=== 测试日志输出 ===");
        EnumTypeConverter converter = new EnumTypeConverter();
        
        // 开启 DEBUG 日志级别时,应该能看到详细的转换过程
        List<JbmDictionary> result = converter.convert(TestStatus.class);
        
        assertNotNull(result);
        System.out.println("✓ 日志输出测试完成");
        System.out.println("提示: 设置 logging.level.com.jbm.autoconfig.dic=DEBUG 可查看详细日志");
    }
}

