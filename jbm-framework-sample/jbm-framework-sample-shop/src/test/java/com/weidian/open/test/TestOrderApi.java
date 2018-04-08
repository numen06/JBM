package com.weidian.open.test;

import com.weidian.open.sdk.exception.OpenException;
import com.weidian.open.sdk.request.order.*;
import com.weidian.open.sdk.response.order.VdianOrderGetResponse;
import com.weidian.open.sdk.response.order.VdianOrderListGetResponse;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestOrderApi extends BaseTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestOrderApi.class);

  @Test
  public void testVdianOrderGet() {
    try {
      VdianOrderGetResponse response = client.executePost(new VdianOrderGetRequest(token, "1289567908"));
      VdianOrderGetResponse.VdianOrderGetResult result = response.getResult();
      LOGGER.debug("response:{}\n", result);
    } catch (OpenException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testVdianOrderListGet() {
    try {
      VdianOrderListGetResponse response = client.executePost(new VdianOrderListGetRequest(token));
      VdianOrderListGetResponse.VdianOrderListGetResult result = response.getResult();
      VdianOrderListGetResponse.ListOrder[] orders = result.getOrders();
      LOGGER.debug("response:{}\n", orders);
    } catch (OpenException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testVdianOrderModify() {
    VdianOrderModifyRequest request = new VdianOrderModifyRequest(token);
    request.setOrderId("774080820162204");
    request.setTotalItemsPrice("1");
    request.setExpressPrice("0");
    this.testCommon(request);
  }

  @Test
  public void testVdianOrderDeliver() {
    VdianOrderDeliverRequest request = new VdianOrderDeliverRequest(token);
    request.setOrderId("774080820162204");
    request.setExpressType("1");
    request.setExpressNo("111");
    this.testCommon(request);
  }

  @Test
  public void testVdianOrderExpressModify() {
    VdianOrderExpressModifyRequest request = new VdianOrderExpressModifyRequest(token);
    request.setOrderId("774080820162204");
    request.setExpressType("2");
    request.setExpressNo("222");
    this.testCommon(request);
  }

  @Test
  public void testVdianOrderRefundAccept() {
    this.testCommon(new VdianOrderRefundAcceptRequest(token, "774080820162204", "1"));
  }

}
