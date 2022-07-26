package jbm.framework.boot.autoconfigure.tio.packet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.util.IOUtils;
import org.tio.core.intf.Packet;

/**
 * json传输封装类
 *
 * @author wesley.zhang
 * @version 1.0
 * @date 2017年11月7日
 */
public class JsonPacket extends Packet {
    public static final int HEADER_LENGHT = 4;// 消息头的长度
    private static final long serialVersionUID = 1L;
    protected JsonForcer jsonForcer = new JsonForcer(null);

    // private byte[] body;
    // private String jsonBody;
    //
    // public String getJsonBody() {
    // return jsonBody;
    // }

    // public byte[] getBody() {
    // return body;
    // }
    //
    // public void setBody(byte[] body) {
    // this.body = body;
    // }

    public void pack(Object obj) {
        // this.jsonBody = JSON.toJSONString(jsonForcer,
        // SerializerFeature.DisableCircularReferenceDetect);
        jsonForcer = new JsonForcer(obj);
    }

    public JsonForcer getJsonForcer() {
        return jsonForcer;
    }

    public void unPack(byte[] bytes) {
        try {
            if (bytes == null || bytes.length <= 0)
                return;
            this.jsonForcer = JSON.parseObject(bytes, JsonForcer.class);
        } catch (Exception e) {
            System.err.println(new String(bytes, IOUtils.UTF8));
        }
    }

}
