package com.jbm.framework.cloud.auth.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jbm.framework.cloud.auth.controller.UserDetailsFeignService;
import com.jbm.framework.cloud.auth.model.JbmAuthUser;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

//@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired(required = false)
    public UserDetailsFeignService userDetailsFeignService;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String url = "http://192.168.20.222:8095/hubao-platform-system/user/loadUserByUsername";
        HttpResponse httpResponse = HttpRequest.post(url).query("username", username).send();
        JSONObject body = JSON.parseObject(httpResponse.body());
//        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
//        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority("ROLE_" + "ADMIN");
//        grantedAuthorities.add(grantedAuthority);
        JbmAuthUser userDetails = body.getObject("result", JbmAuthUser.class);
//        userDetails.build();
//        User user = new User(username, userDetails.getPassword(), true, true, true, true, grantedAuthorities);
        return userDetails;
    }


//    @Override
//	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
////		Result<UserVo> userResult = userService.findByUsername(username);
////        if (userResult.getCode() == 100) {
////            throw new UsernameNotFoundException("用户:" + username + ",不存在!");
////        }
////        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
////        boolean enabled = true; // 可用性 :true:可用 false:不可用
////        boolean accountNonExpired = true; // 过期性 :true:没过期 false:过期
////        boolean credentialsNonExpired = true; // 有效性 :true:凭证有效 false:凭证无效
////        boolean accountNonLocked = true; // 锁定性 :true:未锁定 false:已锁定
////        UserVo userVo = new UserVo();
////        BeanUtils.copyProperties(userResult.getData(),userVo);
////        Result<List<RoleVo>> roleResult = roleService.getRoleByUserId(userVo.getId());
////        if (roleResult.getCode() != 100){
////            List<RoleVo> roleVoList = roleResult.getData();
////            for (RoleVo role:roleVoList){
////                //角色必须是ROLE_开头，可以在数据库中设置
////                GrantedAuthority grantedAuthority = new SimpleGrantedAuthority("ROLE_"+role.getValue());
////                grantedAuthorities.add(grantedAuthority);
////                //获取权限
////                Result<List<MenuVo>> perResult  = permissionService.getRolePermission(role.getId());
////                if (perResult.getCode() != 100){
////                    List<MenuVo> permissionList = perResult.getData();
////                    for (MenuVo menu:permissionList
////                            ) {
////                        GrantedAuthority authority = new SimpleGrantedAuthority(menu.getCode());
////                        grantedAuthorities.add(authority);
////                    }
////                }
////            }
////        }
//		Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
//		GrantedAuthority grantedAuthority = new SimpleGrantedAuthority("ROLE_" + "ADMIN");
//		grantedAuthorities.add(grantedAuthority);
//
//		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//		// 加密
//		String encodedPassword = passwordEncoder.encode("123456".trim());
//		User user = new User(username, encodedPassword, true, true, true, true, grantedAuthorities);
//		return user;
//	}

}