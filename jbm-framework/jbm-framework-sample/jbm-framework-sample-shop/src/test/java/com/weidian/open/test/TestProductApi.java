package com.weidian.open.test;

import com.weidian.open.sdk.entity.Cate;
import com.weidian.open.sdk.entity.Item;
import com.weidian.open.sdk.entity.Sku;
import com.weidian.open.sdk.exception.OpenException;
import com.weidian.open.sdk.request.product.*;
import com.weidian.open.sdk.response.product.VdianItemListGetResponse;
import org.junit.Test;

public class TestProductApi extends BaseTest {

  @Test
  public void testVdianItemGet() {
    this.testCommon(new VdianItemGetRequest(token, "1652903415"));
  }

  @Test
  public void testVdianItemListGet() {
    try {
      VdianItemListGetRequest request = new VdianItemListGetRequest(token);
      request.setOrderby(1);
      request.setPageNum(1);
      request.setPageSize(10);
      VdianItemListGetResponse response = client.executePost(request);
      VdianItemListGetResponse.VdianItemListGetResult result = response.getResult();
      VdianItemListGetResponse.ListItem[] items = result.getItems();
      LOGGER.debug("response:{}\n", items);
    } catch (OpenException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testVdianShopCateGet() {
    this.testCommon(new VdianShopCateGetRequest(token));
  }

  @Test
  public void testVdianItemAdd() {
    Item item = new Item();
    item.setItemName("测试商品");
    item.setImgs(new String[] {"http://wd.geilicdn.com/vshop163187074-1415608235762-1176009.png?w=480&h=0"});
    item.setPrice("1.00");
    item.setStock(100);

    Sku sku = new Sku();
    sku.setTitle("测试型号1");
    sku.setPrice("1.00");
    sku.setStock(100);
    item.setSkus(new Sku[] {sku});

    this.testCommon(new VdianItemAddRequest(token, item));
  }

  @Test
  public void testVdianItemDelete() {
    this.testCommon(new VdianItemDeleteRequest(token, "1603953293"));
  }

  @Test
  public void testVdianItemUpdate() {
    Item item = new Item();
    item.setItemId("1603959719");
    item.setItemName("测试商品1");
    item.setPrice("2.00");
    item.setStock(200);
    this.testCommon(new VdianItemUpdateRequest(token, item));
  }

  @Test
  public void testVdianItemImageAdd() {
    this.testCommon(new VdianItemImageAddRequest(token, "1603959719",
        new String[] {"http://wd.geilicdn.com/vshop543208-1396345174-1.jpg?w=110&h=110&cp=1"}));
  }

  @Test
  public void testVdianItemImageDelete() {
    this.testCommon(new VdianItemImageDeleteRequest(token, "1603959719",
        new String[] {"http://wd.geilicdn.com/vshop543208-1396345174-1.jpg?w=110&h=110&cp=1"}));
  }

  @Test
  public void testVdianItemSkuAdd() {
    Sku sku = new Sku();
    sku.setTitle("测试型号2");
    sku.setPrice("2.00");
    sku.setStock(150);
    this.testCommon(new VdianItemSkuAddRequest(token, "1603959719", new Sku[] {sku}));
  }

  @Test
  public void testVdianItemSkuUpdate() {
    Sku sku = new Sku();
    sku.setId("4205189041");
    sku.setTitle("测试型号3");
    sku.setPrice("3.00");
    sku.setStock(300);
    this.testCommon(new VdianItemSkuUpdateRequest(token, "1603959719", new Sku[] {sku}));
  }

  @Test
  public void testVdianItemSkuDelete() {
    this.testCommon(new VdianItemSkuDeleteRequest(token, "1603959719", new String[] {"4205189041"}));
  }

  @Test
  public void testVdianShopCateAdd() {
    Cate cate = new Cate();
    cate.setCateName("测试分组1");
    cate.setSortNum(1);
    this.testCommon(new VdianShopCateAddRequest(token, new Cate[] {cate}));
  }

  @Test
  public void testVdianShopCateUpdate() {
    Cate cate = new Cate();
    cate.setCateId("58855782");
    cate.setCateName("测试分组2");
    cate.setSortNum(2);
    this.testCommon(new VdianShopCateUpdateRequest(token, new Cate[] {cate}));
  }

  @Test
  public void testVdianShopCateDelete() {
    this.testCommon(new VdianShopCateDeleteRequest(token, "58855782"));
  }

  @Test
  public void testVdianItemCateSet() {
    this.testCommon(new VdianItemCateSetRequest(token, new String[] {"48950776"}, new String[] {"7949538"}));
  }

  @Test
  public void testVdianItemCateCancel() {
    this.testCommon(new VdianItemCateCancelRequest(token, "48950776", new String[] {"7949538"}));
  }

  @Test
  public void testWeidianItemOnSale() {
    this.testCommon(new WeidianItemOnSaleRequest(token, "48950776", 1));
  }

  @Test
  public void testMediaUpload() {
    MediaUploadRequest request =
        new MediaUploadRequest(token,
            super.getBytes("/Users/zhanghuijie/Downloads/vshop1445591685859-14174960.png.jpeg"));
    this.testMultipart(request);
  }

}
