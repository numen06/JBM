package com.jbm.test.tio;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import jodd.http.HttpConnection;
import jodd.http.HttpResponse;

import java.io.InputStream;

public class ReadHttpForm extends AbstractExecutionThreadService {

    private HttpConnection httpConnection;

    public ReadHttpForm(HttpConnection httpConnection) {
        super();
        this.httpConnection = httpConnection;
    }

    @Override
    protected void run() throws Exception {
        InputStream inputStream = httpConnection.getInputStream();
        while (inputStream.read() > 0) {
            HttpResponse httpResponse = HttpResponse.readFrom(inputStream);
            System.out.println(httpResponse);
        }

    }

}
