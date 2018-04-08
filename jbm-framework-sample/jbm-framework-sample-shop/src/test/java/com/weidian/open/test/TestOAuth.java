package com.weidian.open.test;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.weidian.open.sdk.exception.OpenException;
import com.weidian.open.sdk.oauth.OAuth;
import com.weidian.open.sdk.response.oauth.OAuthResponse;

public class TestOAuth {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestOAuth.class);

  private OAuth oauth = OAuth.getInstance();

  @Test
  public void testGetPersonalToken() {
    try {
      OAuthResponse response = oauth.getPersonalToken();
      LOGGER.debug("response:{}\n", response.toString());
      Assert.assertEquals(0, response.getStatus().getStatusCode());
    } catch (OpenException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testOAuth2AuthorizeUrl() {
    try {
      String response = oauth.getOAuth2AuthorizeUrl("http://www.abc.com", "a");
      LOGGER.debug("response:{}\n", response);
      Assert.assertNotNull(response);
    } catch (OpenException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testOAuth2Token() {
    try {
      OAuthResponse response = oauth.getOAuth2Token("0c8369516b99e063df1d1f3feda7c91b");
      LOGGER.debug("response:{}\n", response.toString());
      Assert.assertEquals(0, response.getStatus().getStatusCode());
    } catch (OpenException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testRefreshOAuth2Token() {
    try {
      OAuthResponse response = oauth.refreshOAuth2Token("fa34a3eff1361aa816a28f38cb3eae1f0001970984");
      LOGGER.debug("response:{}\n", response.toString());
      Assert.assertEquals(0, response.getStatus().getStatusCode());
    } catch (OpenException e) {
      e.printStackTrace();
    }
  }
}
