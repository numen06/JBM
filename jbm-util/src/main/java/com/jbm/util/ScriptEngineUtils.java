package com.jbm.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Map;

/**
 * 对JS脚本进行封装的工具类
 *
 * @author Wesley
 */
@Slf4j
public class ScriptEngineUtils {

    private static final Logger logger = LoggerFactory.getLogger(ScriptEngineUtils.class);

    /**
     * 执行资源文件下面的脚本
     *
     * @param classPathFile 脚本路径
     * @param function      执行方法名称
     * @param params        全局参数
     * @param args          方法参数
     * @return
     */
    public static Object callScript(String classPathFile, String function, Map<? extends String, ? extends Object> params, Object... args) {
        final String[] paths = StringUtils.splitToEmpty(classPathFile);
        Enumeration<InputStream> inputStreams = new Enumeration<InputStream>() {
            private int index = -1;

            @Override
            public InputStream nextElement() {
                try {
                    File file = FileUtils.getFile(paths[index]);
                    InputStream inputStream = new FileInputStream(file);
                    return inputStream;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public boolean hasMoreElements() {
                index++;
                return paths.length > index;
            }
        };
        SequenceInputStream in = new SequenceInputStream(inputStreams);
        return callScript(in, function, params, args);
    }

    public static Object callScript(File file, String function, Map<? extends String, ? extends Object> params, Object... args) throws IOException {
        return callScript(FileUtils.openInputStream(file), function, params, args);
    }

    /**
     * @param inputStream 输入流
     * @param function    执行方法名称
     * @param params      全局参数
     * @param args        方法参数
     * @return
     */
    public static Object callScript(InputStream inputStream, String function, Map<? extends String, ? extends Object> params, Object... args) {
        ScriptEngineManager maneger = new ScriptEngineManager();
        ScriptEngine engine = maneger.getEngineByName("js");
        Reader scriptReader = new InputStreamReader(inputStream);
        if (engine != null) {
            try {
                if (MapUtils.isNotEmpty(params)) {
                    // 建立上下文变量
                    Bindings bind = engine.createBindings();
                    bind.putAll(params);
                    // 绑定上下文，作用域为当前引擎范围
                    engine.setBindings(bind, ScriptContext.ENGINE_SCOPE);
                }
                // JS引擎解析文件
                engine.eval(scriptReader);
                if (engine instanceof Invocable) {
                    Invocable invocable = (Invocable) engine;
                    // JS引擎调用方法
                    Object result = invocable.invokeFunction(function, args);
                    return result;
                }
            } catch (ScriptException e) {
                log.error("Script Error", e);
            } catch (NoSuchMethodException e) {
                log.error(MessageFormat.format("Script nosuch function:{0}", function), e);
            } finally {
                try {
                    scriptReader.close();
                } catch (IOException e) {
                    log.error("Close Script Error", e);
                }
            }
        } else {
            log.error("ScriptEngine create Error!");
        }
        return null;
    }
}
