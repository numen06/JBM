package jbm.framework.boot.autoconfigure.tio.packet;

import org.tio.core.intf.Packet;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.util.IOUtils;

/**
 * 
 * json传输封装类
 * 
 * @author wesley.zhang
 * @date 2017年11月7日
 * @version 1.0
 *
 */
public class JsonPacket extends Packet {
	private static final long serialVersionUID = 1L;
	public static final int HEADER_LENGHT = 4;// 消息头的长度

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
