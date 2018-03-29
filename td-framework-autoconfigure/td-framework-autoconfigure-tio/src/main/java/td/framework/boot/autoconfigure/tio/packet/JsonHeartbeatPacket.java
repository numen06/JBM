package td.framework.boot.autoconfigure.tio.packet;

public class JsonHeartbeatPacket extends JsonPacket {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JsonHeartbeatPacket() {
		super();
		this.jsonForcer = new JsonForcer(null, 0);
	}

}
