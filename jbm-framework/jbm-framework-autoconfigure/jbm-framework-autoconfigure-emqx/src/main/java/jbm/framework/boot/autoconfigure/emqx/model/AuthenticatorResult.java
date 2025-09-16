package jbm.framework.boot.autoconfigure.emqx.model;

import lombok.Data;

/**
 {
 "id": "password_based:built_in_database",
 "backend": "built_in_database",
 "bootstrap_file": "${EMQX_ETC_DIR}/auth-built-in-db-bootstrap.csv",
 "bootstrap_type": "plain",
 "enable": true,
 "mechanism": "password_based",
 "password_hash_algorithm": {
 "name": "sha256",
 "salt_position": "suffix"
 },
 "user_id_type": "clientid"
 }
 * @author wesley
 */
@Data
public class AuthenticatorResult {
    private String id;
    private String backend;
    private String bootstrapFile;
    private String bootstrapType;
    private Boolean enable;
    private String mechanism;
    private PasswordHashAlgorithm passwordHashAlgorithm;
    private String userIdType;
    @Data
    public static class PasswordHashAlgorithm {
        private String name;
        private String saltPosition;
    }

}
