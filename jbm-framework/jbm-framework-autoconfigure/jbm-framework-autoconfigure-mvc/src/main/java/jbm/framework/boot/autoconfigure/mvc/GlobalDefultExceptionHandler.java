package jbm.framework.boot.autoconfigure.mvc;

import com.jbm.framework.metadata.bean.ResultForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.text.MessageFormat;

@RestControllerAdvice
public class GlobalDefultExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalDefultExceptionHandler.class);

    @ExceptionHandler(value = Exception.class)
    @ResponseBody
//    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseStatus(HttpStatus.OK)
    public ResultForm errorResult(Exception e) {
        logger.error("接口异常", e);
        return ResultForm.error(null, 500, MessageFormat.format("接口异常:{0}", e.getClass().getName()));
    }
}
