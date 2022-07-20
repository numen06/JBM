package jbm.framework.boot.autoconfigure.baidu.model;

import lombok.Data;

import java.util.Date;
import java.util.function.Consumer;

@Data
public class BaiduResult<R> {

    private R result;
    private String log_id;
    private String error_msg;
    private String cached;
    private String error_code;
    private Date timestamp;


    public void action(Consumer<R> function) {
        function.accept(this.result);
    }

    public void successAction(Consumer<R> function) {
        if (!"0".equals(error_code)) {
            return;
        }
        this.action(function);
    }


}

