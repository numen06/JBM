package com.jbm.util.db;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import com.google.common.collect.Lists;
import com.jbm.util.SimpleTemplateUtils;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
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


    private String sqlPath = "sqls/";

    private final static String SQL_EXT = ".sql";

    public DSTemplate(String group) {
        this.db = Db.use(group);
    }

    public DSTemplate(DataSource ds) {
        this.db = Db.use(ds);
    }

    public DSTemplate(String group, String sqlPath) {
        this.db = Db.use(group);
        this.sqlPath = sqlPath;
    }


    private String loadSql(String sqlname) {
        String sqlFile = StrUtil.concat(true, sqlPath, sqlname, SQL_EXT);
        ClassPathResource resource = new ClassPathResource(sqlFile);
        return resource.readUtf8Str();
    }

    private String renderSql(String sqlname, Object... params) {
        String inSql = this.loadSql(sqlname);
        String outSql = inSql;
        if (params.length==  1) {
           Object ctxPojo = params[0];
               try {
                   outSql =  SimpleTemplateUtils.renderStringTemplate(inSql, ctxPojo);
               } catch (IOException e) {
                   throw new RuntimeException(e);
               }
        }
        return outSql;
    }


    public <T> List<T> queryEntitys(String sqlname, Class<T> entityClass, Object... params) {
        List<T> entities = Lists.newArrayList();
        //查询
        try {
            String sql = this.renderSql(sqlname,params);
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
            String sql = this.renderSql(sqlname,params);
            log.info("execute sql:{}", sql);
            return db.execute(sqlname, params);
        } catch (SQLException e) {
            log.error("执行sql错误", e);
            return 0;
        }
    }

}
