package com.jbm.util.template.simple;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.engine.velocity.SimpleStringResourceLoader;
import com.github.pfmiles.org.apache.velocity.app.VelocityEngine;

public class SimpleTemplateEngine implements TemplateEngine {

    private VelocityEngine engine;
    private TemplateConfig config;

    public SimpleTemplateEngine() {
    }

    public SimpleTemplateEngine(TemplateConfig config) {
        this.init(config);
    }

    public SimpleTemplateEngine(com.github.pfmiles.org.apache.velocity.app.VelocityEngine engine) {
        this.init(engine);
    }

    private static VelocityEngine createEngine(TemplateConfig config) {
        if (null == config) {
            config = new TemplateConfig();
        }

        VelocityEngine ve = new VelocityEngine();
        String charsetStr = config.getCharset().toString();
        ve.setProperty("resource.default_encoding", charsetStr);
        ve.setProperty("resource.loader.file.cache", true);
        switch (config.getResourceMode()) {
            case CLASSPATH:
                ve.setProperty("resource.loader.file.class", "com.github.pfmiles.org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
                break;
            case FILE:
                String path = config.getPath();
                if (null != path) {
                    ve.setProperty("resource.loader.file.path", path);
                }
                break;
            case STRING:
                ve.setProperty("resource.loaders", "str");
                ve.setProperty("resource.loader.str.class", SimpleStringResourceLoader.class.getName());
        }

        ve.init();
        return ve;
    }

    public TemplateEngine init(TemplateConfig config) {
        if (null == config) {
            config = TemplateConfig.DEFAULT;
        }

        this.config = config;
        this.init(createEngine(config));
        return this;
    }

    private void init(VelocityEngine engine) {
        this.engine = engine;
    }

    public VelocityEngine getRawEngine() {
        return this.engine;
    }

    public Template getTemplate(String resource) {
        if (null == this.engine) {
            this.init(TemplateConfig.DEFAULT);
        }

        String charsetStr = null;
        if (null != this.config) {
            String root = this.config.getPath();
            charsetStr = this.config.getCharsetStr();
            TemplateConfig.ResourceMode resourceMode = this.config.getResourceMode();
            if (TemplateConfig.ResourceMode.CLASSPATH == resourceMode || TemplateConfig.ResourceMode.WEB_ROOT == resourceMode) {
                resource = StrUtil.addPrefixIfNot(resource, StrUtil.addSuffixIfNot(root, "/"));
            }
        }

        return SimpleTemplate.wrap(this.engine.getTemplate(resource, charsetStr));
    }
}
