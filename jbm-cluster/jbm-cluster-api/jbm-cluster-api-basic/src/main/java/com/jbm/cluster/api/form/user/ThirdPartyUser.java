package com.jbm.cluster.api.form.user;

import lombok.Builder;
import lombok.Data;

/***
 * 第三方用户信息
 * ThirdPartyUser.builder()
 *                 .provider("github")
 *                 .subjectId(userResp.get("id").toString())
 *                 .email((String) userResp.get("email"))
 *                 .nickname((String) userResp.get("login"))
 *                 .build();
 * @author wesley
 */
@Data
@Builder
public class ThirdPartyUser {
    private String provider;
    private String subjectId;
    private String email;
    private String nickname;
    private String avatar;
    private String gender;
}