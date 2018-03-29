package com.weidian.open.test;

import com.weidian.open.sdk.exception.OpenException;
import com.weidian.open.sdk.request.cps.VdianCpsItemGetRequest;
import com.weidian.open.sdk.request.cps.VdianCpsItemSearchRequest;
import com.weidian.open.sdk.request.cps.VdianItemGetPublicRequest;
import com.weidian.open.sdk.response.cps.VdianCpsItemGetResponse;
import org.junit.Test;

public class TestCpsApi extends BaseTest {

  @Test
  public void testVdianCpsItemGet() {
    try {
      VdianCpsItemGetResponse response = client.executePost(new VdianCpsItemGetRequest(token, "1603959719"));
      VdianCpsItemGetResponse.CpsItemResult result = response.getResult();
      LOGGER.debug("response:{}\n", result);
    } catch (OpenException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testVdianItemGetPublic() {
    this.testCommon(new VdianItemGetPublicRequest(token, "1603959719"));
  }

  @Test
  public void testVdianCpsItemSearch() {
    VdianCpsItemSearchRequest request = new VdianCpsItemSearchRequest(token);
    request.setKeyword("面膜");
    request.setPage(1);
    request.setPageSize(3);
    this.testCommon(request);
  }

}
