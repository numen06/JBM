package jbm.framework.boot.autoconfigure.emqx.model;

import lombok.Data;

/**
 {
 "data": [
 {
 "is_superuser": false,
 "user_id": "test"
 }
 ],
 "meta": {
 "count": 1,
 "limit": 50,
 "page": 1,
 "hasnext": false
 }
 }
 * @author wesley
 */
@Data
public class AuthUser {
    private String userId;
    private Boolean isSuperuser;
    private String username;
    private String password;
    private String clientId;
    private String ipAddress;
    private String node;
    private String createdAt;


}
