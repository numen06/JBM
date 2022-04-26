package com.jbm.job.test;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.jbm.cluster.api.entitys.job.SysJob;
import com.jbm.framework.usage.paging.DataPaging;
import jbm.framework.boot.autoconfigure.fastjson.FastJsonConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class JobJsonTest {
    public static void main(String[] args) throws IOException {
        DataPaging<SysJob> date = new DataPaging<>();
        SysJob sysJob = new SysJob();
        sysJob.setJobId(1121l);
        date.setContents(Lists.newArrayList(sysJob));
        OutputStream outputStream = new ByteArrayOutputStream();
        HttpOutputMessage httpOutputMessage = new HttpOutputMessage() {
            @Override
            public OutputStream getBody() throws IOException {
                return outputStream;
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
        FastJsonConfiguration.getFastJsonHttpMessageConverter().write(date, MediaType.APPLICATION_JSON, httpOutputMessage);
        System.out.println(StrUtil.toString(httpOutputMessage.getBody()));
    }
}
