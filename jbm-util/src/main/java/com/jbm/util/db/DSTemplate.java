package com.jbm.util.db;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.db.Db;
import cn.hutool.db.DbUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.sql.SqlExecutor;
import com.google.common.collect.Lists;
import com.jbm.util.db.load.FileLoader;
import com.jbm.util.db.load.SqlLoader;
import com.jbm.util.db.load.XmlLoader;
import com.jbm.util.db.sqltemplate.SqlMeta;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
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


    @Getter
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
                    log.error("SQL文件内容为空");
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
        SqlMeta sqlMeta = null;
        //查询
        try {
            sqlMeta = this.getSql(sqlName, params);
            if (sqlMeta != null) {
                log.info("执行SQL:{}", sqlMeta.getSql());
            } else {
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
            log.error("查询数据库错误:{}", sqlName, e);
//            throw new RuntimeException(e);
        }
        return entities;
    }


    public <T> T findById(T bean, String pkey) {
        Entity entity = Entity.parseWithUnderlineCase(bean);
        try {
            List<Entity> result = db.findBy(entity.getTableName(), pkey, entity.get(pkey));
            if (CollUtil.isEmpty(result)) {
                return null;
            }
            return (T) CollUtil.getFirst(result).toBean(bean.getClass());
        } catch (SQLException e) {
            return null;
        }
    }

    public <T> List<T> find(T bean) {
        Entity entity = Entity.parseWithUnderlineCase(bean);
        List<T> entities = Lists.newArrayList();
        try {
            Class<T> entityClass = (Class<T>) entity.getClass();
            List<Entity> result = db.find(entity);
            result.forEach(new Consumer<Entity>() {
                @Override
                public void accept(Entity entity) {
                    T t = ReflectUtil.newInstance(entityClass);
                    entity.toBean(t, true);
                    entities.add(t);
                }
            });
        } catch (SQLException e) {
        }
        return entities;
    }

    /**
     * @param bean
     * @param <T>
     * @return
     */
    public <T> Long insertForGeneratedKey(T bean) {
        Entity entity = Entity.parseWithUnderlineCase(bean);
        try {
            return db.insertForGeneratedKey(entity);
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * @param entityList
     * @param <T>
     * @return
     */
    public <T> int insertBatch(List<T> entityList) {
        List<Entity> entities = CollUtil.newArrayList();
        entityList.forEach(bean -> {
            Entity entity = Entity.parseWithUnderlineCase(bean);
            entities.add(entity);
        });
        int[] result = new int[]{};
        try {
            result = db.insert(entities);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Arrays.stream(result).sum();
    }


    public <T> int delete(T bean, String pkey) {
        Entity entity = Entity.parseWithUnderlineCase(bean);
        Entity where = Entity.create(entity.getTableName()).set(pkey, entity.get(pkey));
        try {
            return db.del(where);
        } catch (SQLException e) {
            return 0;
        }
    }

    public <T> int update(T bean, String pkey) {
        Entity entity = Entity.parseWithUnderlineCase(bean);
        Entity where = Entity.create(entity.getTableName()).set(pkey, entity.get(pkey));
        try {
            return db.update(entity, where);
        } catch (SQLException e) {
            return 0;
        }
    }

    public int execute(String sqlName, Object... params) {
        Connection conn = null;
        try {
            SqlMeta sqlMeta = this.getSql(sqlName, params);
            if (sqlMeta != null) {
                log.info("执行SQL:");
                log.info("{}", sqlMeta.getSql());
            } else {
                return 0;
            }
            conn = db.getConnection();
            int count = SqlExecutor.execute(db.getConnection(), sqlMeta.getSql());
            log.info("影响行数：{}", count);
            return count;
        } catch (SQLException e) {
            log.error("执行SQL错误", e);
            return 0;
        } finally {
            DbUtil.close(conn);
        }
    }

}
