package jbm.framework.boot.autoconfigure;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.metadata.bean.ResultBody;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @Created wesley.zhang
 * @Date 2022/5/18 23:43
 * @Description TODO
 */
@Slf4j
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class NotFoundErrorController extends BasicErrorController {

    public NotFoundErrorController(ServerProperties serverProperties) {
        super(new DefaultErrorAttributes(), serverProperties.getError());
    }

    /**
     * 覆盖默认的Json响应
     */
    @Override
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        // 获取原始的错误信息
        HttpStatus status = getStatus(request);
        if (status == HttpStatus.NO_CONTENT) {
            return new ResponseEntity<>(status);
        }
        Map<String, Object> errorMap = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL));
        String error = StrUtil.toString(errorMap.getOrDefault("error", ""));
        Map<String, Object> body = ResultBody.failed().httpStatus(status.value()).code(status.value()).msg(error).toMap();

        return new ResponseEntity<>(body, status);
    }


}
