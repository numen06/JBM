package com.jbm.cluster.api.form.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyUser {
    private String provider;
    private String clientId;
    private String subjectId;
    private String openId;
    private String username;
    private String mobile;
    private String email;
    private String nickname;
    private String avatar;
    private String gender;
    private String token;
}