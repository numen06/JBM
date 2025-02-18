package com.jbm.test;

import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSON;
import com.jbm.framework.metadata.bean.ResultBody;
import org.junit.jupiter.api.Test;

public class ResultBodyTest {

    @Test
    public void testCallback() {
        ResultBody.callback(() -> {
            throw new Exception("测试");
        });
    }

    @Test
    public void testSuccess() {
        ResultBody<Object> rb = ResultBody.success();
        Console.log(JSON.toJSONString(rb));
    }

    @Test
    public void testOk() {
        ResultBody<Object> rb = ResultBody.ok();
        Console.log(JSON.toJSONString(rb));
    }
    @Test
    public void testOkMsg() {
        ResultBody<Object> rb = ResultBody.ok("测试成功");
        Console.log(JSON.toJSONString(rb));
    }


    @Test
    public void testError() {
        ResultBody<Object> rb = ResultBody.error();
        Console.log(JSON.toJSONString(rb));
    }

    @Test
    public void testFail(){
        ResultBody<Object> rb = ResultBody.failed();
        Console.log(JSON.toJSONString(rb));
    }

    @Test
    public void testErrorMsg() {
        ResultBody<Object> rb = ResultBody.error("测试");
        Console.log(JSON.toJSONString(rb));
    }

    @Test
    public void testException() {
        ResultBody<String> rb = new ResultBody<>();
        rb.exception(new NullPointerException("测试异常"));
        Console.log(JSON.toJSONString(rb));
    }

}
