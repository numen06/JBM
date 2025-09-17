package jbm.framework.boot.autoconfigure.emqx.model;


import lombok.Data;

import java.util.Date;


/**
 * {
 * "awaiting_rel_max" : 100,
 * "send_msg.dropped.too_large" : 0,
 * "inflight_max" : 32,
 * "mqueue_max" : 1000,
 * "mailbox_len" : 1,
 * "recv_msg.dropped" : 0,
 * "proto_name" : "MQTT",
 * "clean_start" : true,
 * "clientid" : "wW3mMNEQjbh22Wtb",
 * "connected_at" : "2025-09-12T18:41:30.411+08:00",
 * "recv_msg.dropped.await_pubrel_timeout" : 0,
 * "send_msg.qos0" : 0,
 * "send_msg.qos2" : 0,
 * "recv_msg.qos2" : 0,
 * "username" : "admin",
 * "recv_pkt" : 108,
 * "ip_address" : "10.100.10.123",
 * "recv_msg" : 0,
 * "send_pkt" : 108,
 * "proto_ver" : 5,
 * "is_persistent" : false,
 * "auth_expire_at" : null,
 * "send_msg" : 0,
 * "recv_cnt" : 108,
 * "send_oct" : 257,
 * "heap_size" : 610,
 * "reductions" : 109219,
 * "is_expired" : false,
 * "created_at" : "2025-09-12T18:41:30.411+08:00",
 * "peersni" : null,
 * "client_attrs" : { },
 * "expiry_interval" : 0,
 * "mqueue_dropped" : 0,
 * "is_bridge" : false,
 * "node" : "emqx@10.0.2.2",
 * "send_msg.dropped.queue_full" : 0,
 * "mqueue_len" : 0,
 * "mountpoint" : null,
 * "send_cnt" : 108,
 * "recv_msg.qos0" : 0,
 * "recv_msg.qos1" : 0,
 * "port" : 41544,
 * "subscriptions_max" : "infinity",
 * "enable_authn" : true,
 * "recv_oct" : 246,
 * "listener" : "tcp:default",
 * "inflight_cnt" : 0,
 * "send_msg.dropped.expired" : 0,
 * "connected" : true,
 * "subscriptions_cnt" : 0,
 * "send_msg.qos1" : 0,
 * "send_msg.dropped" : 0,
 * "durable" : false,
 * "keepalive" : 60
 * }
 *
 * @author wesley
 */
@Data
public class EmqxClient {
//    {
//        "ipaddress" : "10.100.10.123",
//            "expiry_interval" : 0,
//            "clean_start" : true,
//            "sockport" : 1883,
//            "proto_name" : "MQTT",
//            "connected_at" : 1757695239100,
//            "proto_ver" : 5,
//            "clientid" : "hivemq_IPrintService_078d5b3d9ed24e0ebfbe26715ff0f537",
//            "username" : "undefined",
//            "ts" : 1757695239100,
//            "protocol" : "mqtt",
//            "keepalive" : 60
//    }

    private String clientId;
    private String username;
    private String ipAddress;
    private String node;
    private Date connectedAt;
    private Date createdAt;
    private Boolean connected;
    private Integer subscriptionsCnt;
    private String protocol;
    private Integer keepalive;


}