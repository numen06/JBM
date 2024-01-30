package com.jbm.util.db;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.lang.Filter;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import com.google.common.collect.Lists;
import com.jbm.util.db.load.FileLoader;
import com.jbm.util.db.load.SqlLoader;
import com.jbm.util.db.load.XmlLoader;
import com.jbm.util.db.sqltemplate.Configuration;
import com.jbm.util.db.sqltemplate.SqlMeta;
import com.jbm.util.db.sqltemplate.SqlTemplate;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.net.URL;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据库模板
 * 默认sql文件目录为：sqls/
 * 默认配置文件为：/config/db.properties
 *
 * @author wesley
 */
@Slf4j
public class DSTemplate {


    private final Db db;


    private final static String DEFAULT_PATH = "sqls/";

    private List<FileLoader> fileLoaderList;


    public DSTemplate(String group) {
        this(group, DEFAULT_PATH, DEFAULT_PATH);
    }

    public DSTemplate(DataSource ds) {
        this(ds, DEFAULT_PATH, DEFAULT_PATH);
    }

    public DSTemplate(String group, String xmlPath, String sqlPath) {
        this.db = Db.use(group);
        fileLoaderList = CollUtil.newArrayList(new SqlLoader(xmlPath), new XmlLoader(sqlPath));
    }

    public DSTemplate(DataSource ds, String xmlPath, String sqlPath) {
        this.db = Db.use(ds);
        fileLoaderList = CollUtil.newArrayList(new SqlLoader(xmlPath), new XmlLoader(sqlPath));
    }

    private String getSql(String sqlname, Object... params) {
        for (FileLoader fileLoader : fileLoaderList) {
            if (fileLoader.canRead(sqlname)) {
                String content = fileLoader.load(sqlname, params);
                if (StrUtil.isBlank(content)) {
                    log.error("sql文件内容为空");
                    continue;
                }
                return content;
            }
        }
        return null;
    }


    public <T> List<T> queryEntitys(String sqlname, Class<T> entityClass, Object... params) {
        List<T> entities = Lists.newArrayList();
        //查询
        try {
            String sql = this.getSql(sqlname, params);
            log.info("execute sql:{}", sql);
            List<Entity> result = db.query(sql, params);
            result.forEach(new Consumer<Entity>() {
                @Override
                public void accept(Entity entity) {
                    T t = ReflectUtil.newInstance(entityClass);
                    entity.toBean(t, true);
                    entities.add(t);
                }
            });
        } catch (SQLException e) {
            log.error("查询数据库错误", e);
//            throw new RuntimeException(e);
        }
        return entities;
    }

    public int execute(String sqlname, Object... params) {
        try {
            String sql = this.getSql(sqlname, params);
            log.info("execute sql:{}", sql);
            return db.execute(sqlname, params);
        } catch (SQLException e) {
            log.error("执行sql错误", e);
            return 0;
        }
    }

}
