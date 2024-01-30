package com.jbm.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.Resource;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pfmiles.minvelocity.ImplHelper;
import com.github.pfmiles.minvelocity.ParseUtil;
import com.github.pfmiles.org.apache.commons.collections.ExtendedProperties;
import com.github.pfmiles.org.apache.velocity.Template;
import com.github.pfmiles.org.apache.velocity.VelocityContext;
import com.github.pfmiles.org.apache.velocity.app.VelocityEngine;
import com.github.pfmiles.org.apache.velocity.context.Context;
import com.github.pfmiles.org.apache.velocity.runtime.RuntimeConstants;
import org.apache.commons.io.IOUtils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 模板工具类
 *
 * @author wesley
 */
public class SimpleTemplateUtils   {

    protected static ExtendedProperties props = new ExtendedProperties();
    protected static final VelocityEngine tempEngine = new VelocityEngine();
    static {
        try {
            InputStream propStream = ImplHelper.getCurrentClsLoader().getResourceAsStream("min-velocity.properties");
            if (propStream != null) {
                tempEngine.getLog().debug("'min-velocity.properties' at classpath root found, loading...");
                props.load(propStream, "UTF-8");
            }
            tempEngine.init(props);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 传入模板路径(classpath中的路径)，以及任意pojo或map形式的context，返回merge好的String结果
     *
     * @param tempPath
     *            模板路径, 以classpath根为起始, 模板需以utf-8编码
     * @param ctxPojo
     *            任意自定义pojo context，需提供符合javaBean规范的getter方法, 或map
     * @return merge好的String结果
     */
    public static <T> String render(String tempPath, T ctxPojo) {
        Context ctx = new VelocityContext();
        prepareCtx(ctxPojo, ctx);
        StringWriter writer = new StringWriter();
        try {
            tempEngine.mergeTemplate(tempPath, StandardCharsets.UTF_8.name(), ctx, writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    /**
     * 传入String类型的内存模板，以及任意形式的pojo或map context, 将merge好的结果写入out中;
     * 该方法每次都会解析传入的模板内容；若希望节省模板解析成本，请考虑结合使用'parseStringTemplate' +
     * 'renderTemplate' api
     *
     * @param templateString
     *            模板，以内存String的形式
     * @param ctxPojo
     *            任意自定义pojo context，需提供符合javaBean规范的getter方法, 或map
     * @param out
     *            渲染结果输出writer
     */
    public static <T> void renderString(String templateString, T ctxPojo, Writer out) {
        Context ctx = new VelocityContext();
        prepareCtx(ctxPojo, ctx);
        try {
            tempEngine.mergeLiteralTemplate(templateString, ctx, out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将传入的String类型的模板内容，解析为Template对象，可供缓存起来，重复多次渲染使用，以节省模板解析成本
     *
     * @param templateString
     *            模板内容
     * @return 解析好的template对象
     */
    public static Template parseStringTemplate(String templateString) {
        return tempEngine.parseStringTemplate(templateString);
    }

    /**
     * 根据tempPath找到对应的模板，解析后返回
     *
     * @param tempPath
     *            模板路径
     * @return 解析好的template对象
     */
    public static Template getTemplate(String tempPath) {
        return tempEngine.getTemplate(tempPath, "UTF-8");
    }

    /**
     * 渲染模板
     *
     * @param temp
     *            传入的模板对象
     * @param ctxPojo
     *            模板上下文对象，可为pojo也可为map
     * @param out
     *            用于渲染结果输出的writer
     */
    public static <T> void renderTemplate(Template temp, T ctxPojo, Writer out) {
        Context ctx = new VelocityContext();
        prepareCtx(ctxPojo, ctx);
        tempEngine.mergeTemplate(temp, ctx, out);
    }

    /**
     * 渲染模板，直接返回渲染好的string
     */
    public static <T> String renderTemplate(Template temp, T ctxPojo) {
        StringWriter w = new StringWriter();
        renderTemplate(temp, ctxPojo, w);
        return w.toString();
    }

    private static <T> void prepareCtx(T ctxPojo, Context ctx) {
        List<Class<?>> defaultStaticUtils =CollUtil.newArrayList(ParseUtil.class,StrUtil.class);
        for (Class<?> c : defaultStaticUtils) {
            ctx.put(ClassUtil.getClassName(c,true),c);
        }
        putAllDefaultStaticUtils(ctx);
        putAllPojoVals(ctxPojo, ctx);
    }

    @SuppressWarnings("unchecked")
    private static void putAllDefaultStaticUtils(Context ctx) {
        List<String> ms = null;
        Object prop = tempEngine.getProperty(RuntimeConstants.DEFAULT_STATIC_UTIL_MAPPINGS);
        if (prop instanceof String) {
            ms = Arrays.asList((String) prop);
        } else {
            ms = (List<String>) prop;
        }
        if (ms != null)
            for (String m : ms) {
                String[] kv = m.split(":");
                try {
                    ctx.put(kv[0].trim(), ImplHelper.getCurrentClsLoader().loadClass(kv[1].trim()));
                } catch (ClassNotFoundException e) {
                    tempEngine.getLog().error("Could not load static util class: " + kv[0], e);
                }
            }
    }

    private static void putAllPojoVals(Object ctxPojo, Context ctx) {
        ctx.put("ParseUtil", ParseUtil.class);
        if (ctxPojo == null)
            return;
        if (ctxPojo instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) ctxPojo).entrySet()) {
                ctx.put(e.getKey().toString(), e.getValue());
            }
        } else {
            BeanInfo bi;
            try {
                bi = Introspector.getBeanInfo(ctxPojo.getClass());
                for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
                    if ("class".equals(pd.getName()))
                        continue;
                    Method rm = pd.getReadMethod();
                    if (rm != null)
                        ctx.put(pd.getName(), rm.invoke(ctxPojo));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    /**
     * 通过文件生成执行模板
     *
     * @param tempFile 模板文件
     * @param ctxPojo  模板上下文对象，可为pojo也可为map
     * @return 渲染模板，直接返回渲染好的string
     * @throws IOException
     */
    public static <T> String renderTemplate(File tempFile, T ctxPojo) throws IOException {
        String templateString = FileUtils.readFileToString(tempFile, StandardCharsets.UTF_8);
        Template temp = parseStringTemplate(templateString);
        return renderTemplate(temp, ctxPojo);
    }

    public static <T> String renderTemplate(InputStream inputStream, T ctxPojo) throws IOException {
        String templateString = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        IOUtils.closeQuietly(inputStream);
        Template temp = parseStringTemplate(templateString);
        return renderTemplate(temp, ctxPojo);
    }

    public static <T> String renderResourceTemplate(Resource resource, T ctxPojo) {
        String templateString = IoUtil.readUtf8(resource.getStream());
        Template temp =parseStringTemplate(templateString);
        return renderTemplate(temp, ctxPojo);
    }

    public static <T> String renderStringTemplate(String templateString, T ctxPojo) throws IOException {
        Template temp = parseStringTemplate(templateString);
        return renderTemplate(temp, ctxPojo);
    }


}
