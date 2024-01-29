package test.db;

import com.jbm.util.db.DSTemplate;
import org.junit.Before;
import org.junit.Test;
import test.entity.SqlInitialize;

import java.util.List;


/**
 *
 * @create 2020-06-22 16:03
 *
 */
public class DSTemplateTest {


    private DSTemplate dst;

    @Before
    public void setUp() {
        dst = new DSTemplate("test");
    }

    @Test
    public void testQueryEntitys() {
        List<SqlInitialize> sqlInitializers = dst.queryEntitys("test", SqlInitialize.class);
        System.out.println(sqlInitializers);
    }
}