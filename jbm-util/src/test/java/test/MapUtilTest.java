package test;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.jbm.util.BeanUtils;
import com.jbm.util.MapUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import test.entity.StudentDto;

import java.lang.reflect.InvocationTargetException;

@Slf4j
public class MapUtilTest {

    @Test
    public void testBeanToMap() {
        StudentDto studentDto = new StudentDto();
        studentDto.setName("John");
        log.info("{}", BeanUtil.beanToMap(studentDto, false, true));
    }


    @Test
    public void testBeanToJson() {
        StudentDto studentDto = new StudentDto();
        studentDto.setName("John");
        log.info("{}", JSONUtil.parseObj(studentDto));
    }

    @Test
    public void testBeanGetToMap() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        StudentDto studentDto = new StudentDto();
        studentDto.setName("John");
        log.info("{}", MapUtils.toMap(studentDto));
    }
}
