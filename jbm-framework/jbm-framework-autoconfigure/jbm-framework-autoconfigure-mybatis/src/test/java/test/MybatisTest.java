package test;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.baomidou.mybatisplus.core.MybatisConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MybatisConfiguration.class)
public class MybatisTest {


    @Test
    public void exampleTest() throws IOException {
    }

}
