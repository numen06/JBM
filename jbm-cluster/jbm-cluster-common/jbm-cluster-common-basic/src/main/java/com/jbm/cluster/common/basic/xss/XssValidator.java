package com.jbm.cluster.common.basic.xss;

import cn.hutool.core.util.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义xss校验注解实现
 *
 * @author wesley.zhang
 */
public class XssValidator implements ConstraintValidator<Xss, String> {
    // ✅ 定义富文本白名单策略（按需调整）
    private static final Safelist SAFE_LIST = Safelist.relaxed() // 允许常见排版标签
            .addTags("p", "br", "hr", "strong", "em", "u", "s", "sub", "sup", "span", "ol", "ul", "li", "blockquote")
            .addAttributes(":all", "class", "style") // 允许 class/style（注意：style 需进一步限制，见下文）
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https");

    // 🔒 进阶：限制 style 属性值（防 CSS 表达式/XSS），推荐禁用 style 或用 CSS 白名单
    // 若必须支持内联样式，建议额外用正则过滤 style 值（如只允许 color/font-size 等）

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isBlank(value)) {
            return true;
        }

        try {
            // ✅ 1. 使用 JSoup 消毒（核心：白名单过滤）
            String cleanHtml = Jsoup.clean(value, SAFE_LIST);

            // ✅ 2. 可选：校验消毒后是否与原始内容“语义一致”（防纯文本被误转义）
            // 例如：原始是纯文本 "hello" → 消毒后仍是 "hello"；若原始含恶意代码，cleanHtml 会删掉它

            // ✅ 3. 可选：检查是否“过度净化”（比如用户本意发富文本，结果被清空）
            // 这里简单判断：消毒后是否为空或只剩空白
            if (StrUtil.isBlank(cleanHtml)) {
                // 可设置错误信息
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("{xss.rejected}")
                        .addConstraintViolation();
                return false;
            }

            // ✅ 4. 将消毒后的内容存入上下文（供后续 Controller 使用）
            // ⚠️ 注意：ConstraintValidator 不适合修改 value，建议在 Service 层调用 Jsoup.clean()
            // 所以这里仅校验，不修改。真正存储前再 clean 一次！

            return true;

        } catch (Exception e) {
            // 解析异常（如严重畸形 HTML）视为非法
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{xss.parse.error}")
                    .addConstraintViolation();
            return false;
        }
    }
}