package com.weidian.open.test;

import com.weidian.open.sdk.AbstractWeidianClient;
import com.weidian.open.sdk.DefaultWeidianClient;
import com.weidian.open.sdk.exception.OpenException;
import com.weidian.open.sdk.request.AbstractRequest;
import com.weidian.open.sdk.response.AbstractResponse;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BaseTest {

  public static final Logger LOGGER = LoggerFactory.getLogger(BaseTest.class);

  protected AbstractWeidianClient client = DefaultWeidianClient.getInstance();

  protected String token = "b984ca8c1b962cd01d280721283347130001970984";

  // protected String token = "af7f88f54629a52bdd38828410fdbf5a00000001a0";

  @SuppressWarnings({"rawtypes", "unchecked"})
  protected void testCommon(AbstractRequest request) {
    try {
      // AbstractResponse response = client.executeGet(request);
      AbstractResponse response = client.executePost(request);
      LOGGER.debug("response:{}\n", response.toString());
      Assert.assertEquals(0, response.getStatus().getStatusCode());
    } catch (OpenException e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  protected void testMultipart(AbstractRequest request) {
    try {
      AbstractResponse response = client.multipart(request);
      LOGGER.debug("response:{}\n", response.toString());
      Assert.assertEquals(0, response.getStatus().getStatusCode());
    } catch (OpenException e) {
      e.printStackTrace();
    }
  }

  protected byte[] getBytes(String path) {
    byte[] data = null;
    try {
      InputStream in = new FileInputStream(path);
      data = new byte[in.available()];
      in.read(data);
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return data;
  }
}
