package test;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MybatisConfiguration.class)
public class MybatisTest {


    @Test
    public void exampleTest() throws IOException {
    }

}
