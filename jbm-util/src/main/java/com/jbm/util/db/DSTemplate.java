package com.jbm.util.db;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import com.google.common.collect.Lists;
import com.jbm.util.db.load.FileLoader;
import com.jbm.util.db.load.SqlLoader;
import com.jbm.util.db.load.XmlLoader;
import com.jbm.util.db.sqltemplate.SqlMeta;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

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

    private SqlMeta getSql(String sqlName, Object... params) {
        for (FileLoader fileLoader : fileLoaderList) {
            if (fileLoader.canRead(sqlName)) {
                SqlMeta content = fileLoader.load(sqlName, params);
                if (ObjectUtil.isNull(content)) {
                    log.error("sql文件内容为空");
                    continue;
                }
                return content;
            }
        }
        return null;
    }

    public <T> T queryEntity(String sqlName, Class<T> entityClass, Object... params) {
        return CollUtil.getFirst(this.queryEntitys(sqlName, entityClass, params));
    }

    public <T> List<T> queryEntitys(String sqlName, Class<T> entityClass, Object... params) {
        List<T> entities = Lists.newArrayList();
        //查询
        try {
            SqlMeta sqlMeta = this.getSql(sqlName, params);
            if (sqlMeta != null) {
                log.info("execute sql:{}", sqlMeta.getSql());
            }else{
                log.error("sql执行失败:{}", sqlName);
                return entities;
            }
            List<Entity> result = db.query(sqlMeta.getSql(), sqlMeta.getParameter().toArray());
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

    public int execute(String sqlName, Object... params) {
        try {
            SqlMeta sqlMeta = this.getSql(sqlName, params);
            if (sqlMeta != null) {
                log.info("execute sql:{}", sqlMeta.getSql());
            }else{
                log.error("sql执行失败:{}", sqlName);
                return 0;
            }
            return db.execute(sqlName, params);
        } catch (SQLException e) {
            log.error("执行sql错误", e);
            return 0;
        }
    }

}
