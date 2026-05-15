package com.jbm.micro.mysql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = MicroMysqlApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class MicroMysqlApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() throws Exception {
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM md_sample")) {
                assertTrue(rs.next());
                assertTrue(rs.getLong(1) >= 0);
            }
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM md_form_row")) {
                assertTrue(rs.next());
                assertTrue(rs.getLong(1) >= 0);
            }
        }
    }
}
