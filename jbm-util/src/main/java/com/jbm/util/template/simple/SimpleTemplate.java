package com.jbm.util.template.simple;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.AbstractTemplate;
import com.github.pfmiles.org.apache.velocity.Template;
import com.github.pfmiles.org.apache.velocity.VelocityContext;

import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.Map;

/**
 * Velocity模板包装
 *
 * @author looly
 */
public class SimpleTemplate extends AbstractTemplate implements Serializable {
    private static final long serialVersionUID = -132774960373894911L;

    private final Template rawTemplate;
    private String charset;

    /**
     * 构造
     *
     * @param rawTemplate Velocity模板对象
     */
    public SimpleTemplate(com.github.pfmiles.org.apache.velocity.Template rawTemplate) {
        this.rawTemplate = rawTemplate;
    }

    /**
     * 包装Velocity模板
     *
     * @param template Velocity的模板对象 {@link com.github.pfmiles.org.apache.velocity.Template}
     * @return VelocityTemplate
     */
    public static SimpleTemplate wrap(com.github.pfmiles.org.apache.velocity.Template template) {
        return (null == template) ? null : new SimpleTemplate(template);
    }

    @Override
    public void render(Map<?, ?> bindingMap, Writer writer) {
        rawTemplate.merge(toContext(bindingMap), writer);
        IoUtil.flush(writer);
    }

    @Override
    public void render(Map<?, ?> bindingMap, OutputStream out) {
        if (null == charset) {
            loadEncoding();
        }
        render(bindingMap, IoUtil.getWriter(out, CharsetUtil.charset(this.charset)));
    }

    /**
     * 将Map转为VelocityContext
     *
     * @param bindingMap 参数绑定的Map
     * @return {@link VelocityContext}
     */
    private VelocityContext toContext(Map<?, ?> bindingMap) {
        final Map<String, Object> map = Convert.convert(new TypeReference<Map<String, Object>>() {
        }, bindingMap);
        return new VelocityContext(map);
    }

    /**
     * 加载可用的Velocity中预定义的编码
     */
    private void loadEncoding() {
        this.charset = StrUtil.isEmpty(charset) ? CharsetUtil.UTF_8 : charset;
    }
}
