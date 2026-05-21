package com.jbm.cluster.center.integration;

import com.alibaba.fastjson.JSON;
import com.jbm.cluster.api.entitys.basic.BaseMenu;
import com.jbm.cluster.api.entitys.basic.BaseRole;
import com.jbm.cluster.api.entitys.basic.BaseUser;
import com.jbm.cluster.api.form.BaseAuthorityRoleForm;
import com.jbm.cluster.api.form.BaseMenuForm;
import com.jbm.cluster.api.form.BaseRoleForm;
import com.jbm.cluster.api.form.BaseUserForm;
import com.jbm.cluster.api.model.auth.OpenAuthority;
import com.jbm.cluster.api.model.auth.UserAccount;
import com.jbm.cluster.center.business.BaseUserBusiness;
import com.jbm.cluster.center.controller.BaseAuthorityController;
import com.jbm.cluster.center.controller.BaseMenuController;
import com.jbm.cluster.center.controller.BaseRoleController;
import com.jbm.cluster.center.controller.BaseUserController;
import com.jbm.cluster.center.integration.support.CenterH2ApiTestSupport;
import com.jbm.cluster.core.constant.JbmConstants;
import com.jbm.framework.metadata.bean.ResultBody;
import com.jbm.framework.usage.paging.DataPaging;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CenterOpenApiH2IT extends CenterH2ApiTestSupport {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private BaseUserController baseUserController;

    @Autowired
    private BaseMenuController baseMenuController;

    @Autowired
    private BaseRoleController baseRoleController;

    @Autowired
    private BaseAuthorityController baseAuthorityController;

    @Autowired
    private BaseUserBusiness baseUserBusiness;

    private String adminToken;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Order(1)
    @DisplayName("Flow 1: Admin login and get token")
    void testFlow1_adminLogin() throws Exception {
        UserAccount account = baseUserBusiness.login(JbmConstants.ROOT_USER_NAME);
        assertThat(account).isNotNull();
        assertThat(account.getToken()).isNotBlank();
        adminToken = account.getToken();

        MvcResult result = mockMvc.perform(post("/user/sessions")
                .param("username", JbmConstants.ROOT_USER_NAME)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> body = JSON.parseObject(response, ResultBody.class);
        assertThat(body.getSuccess()).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("Flow 2: Query user list with pagination")
    void testFlow2_userListAndPage() throws Exception {
        MvcResult result = mockMvc.perform(get("/user")
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> body = JSON.parseObject(response, ResultBody.class);
        assertThat(body.getSuccess()).isTrue();
        assertThat(body.getResult()).isNotNull();

        MvcResult pageResult = mockMvc.perform(get("/user")
                .param("pageForm.pageNo", "1")
                .param("pageForm.pageSize", "10")
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.total").isNumber())
                .andReturn();

        String pageResponse = pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> pageBody = JSON.parseObject(pageResponse, ResultBody.class);
        assertThat(pageBody.getSuccess()).isTrue();
        DataPaging<?> paging = JSON.parseObject(JSON.toJSONString(pageBody.getResult()), DataPaging.class);
        assertThat(paging.getContents()).isNotEmpty();
        assertThat(paging.getTotal()).isGreaterThan(0);
    }

    @Test
    @Order(3)
    @DisplayName("Flow 3: Query user detail")
    void testFlow3_userDetail() throws Exception {
        MvcResult result = mockMvc.perform(get("/user/{userId}", JbmConstants.ROOT_USER_ID)
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.userName").value(JbmConstants.ROOT_USER_NAME))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> body = JSON.parseObject(response, ResultBody.class);
        assertThat(body.getSuccess()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("Flow 4: User CRUD - Create, Read, Update, Delete")
    void testFlow4_userCrud() throws Exception {
        String userName = "test_user_" + System.currentTimeMillis();
        String password = "Test123456!";

        BaseUserForm createForm = new BaseUserForm();
        createForm.setUserName(userName);
        createForm.setPassword(password);
        createForm.setNickName("Test User");
        createForm.setStatus(1);

        MvcResult createResult = mockMvc.perform(post("/user")
                .header("satoken", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(createForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        MvcResult queryResult = mockMvc.perform(get("/user")
                .param("userName", userName)
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String queryResponse = queryResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> queryBody = JSON.parseObject(queryResponse, ResultBody.class);
        assertThat(queryBody.getSuccess()).isTrue();

        DataPaging<?> paging = JSON.parseObject(JSON.toJSONString(queryBody.getResult()), DataPaging.class);
        assertThat(paging.getContents()).isNotEmpty();
        Long userId = ((BaseUser) paging.getContents().get(0)).getUserId();
        assertThat(userId).isNotNull();

        BaseUserForm updateForm = new BaseUserForm();
        updateForm.setUserId(userId);
        updateForm.setUserName(userName);
        updateForm.setNickName("Test User Updated");
        updateForm.setStatus(1);

        MvcResult updateResult = mockMvc.perform(put("/user/{userId}", userId)
                .header("satoken", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(updateForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.nickName").value("Test User Updated"))
                .andReturn();

        MvcResult verifyResult = mockMvc.perform(get("/user/{userId}", userId)
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.nickName").value("Test User Updated"))
                .andReturn();

        MvcResult deleteResult = mockMvc.perform(delete("/user/{userId}", userId)
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
    }

    @Test
    @Order(5)
    @DisplayName("Flow 5: Query all menus")
    void testFlow5_menuList() throws Exception {
        MvcResult result = mockMvc.perform(get("/menu")
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> body = JSON.parseObject(response, ResultBody.class);
        assertThat(body.getSuccess()).isTrue();
        assertThat(body.getResult()).isNotNull();
    }

    @Test
    @Order(6)
    @DisplayName("Flow 6: Menu CRUD - Create, Read, Update, Delete")
    void testFlow6_menuCrud() throws Exception {
        String menuCode = "test_menu_" + System.currentTimeMillis();

        BaseMenuForm createForm = new BaseMenuForm();
        createForm.setMenuCode(menuCode);
        createForm.setMenuName("Test Menu");
        createForm.setStatus(1);
        createForm.setMenuType(1);
        createForm.setParentId(0L);

        MvcResult createResult = mockMvc.perform(post("/menu")
                .header("satoken", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(createForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        MvcResult listResult = mockMvc.perform(get("/menu")
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String listResponse = listResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> listBody = JSON.parseObject(listResponse, ResultBody.class);

        List<BaseMenu> menuList = (List<BaseMenu>) listBody.getResult();
        BaseMenu createdMenu = menuList.stream()
                .filter(m -> menuCode.equals(m.getMenuCode()))
                .findFirst()
                .orElse(null);
        assertThat(createdMenu).isNotNull();
        Long menuId = createdMenu.getMenuId();

        MvcResult detailResult = mockMvc.perform(get("/menu/{menuId}", menuId)
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.menuCode").value(menuCode))
                .andReturn();

        BaseMenuForm updateForm = new BaseMenuForm();
        updateForm.setMenuId(menuId);
        updateForm.setMenuCode(menuCode);
        updateForm.setMenuName("Test Menu Updated");
        updateForm.setStatus(1);
        updateForm.setMenuType(1);
        updateForm.setParentId(0L);

        MvcResult updateResult = mockMvc.perform(put("/menu/{menuId}", menuId)
                .header("satoken", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(updateForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        MvcResult deleteResult = mockMvc.perform(delete("/menu/{menuId}", menuId)
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
    }

    @Test
    @Order(7)
    @DisplayName("Flow 7: Query all roles")
    void testFlow7_roleList() throws Exception {
        MvcResult result = mockMvc.perform(get("/role/all")
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> body = JSON.parseObject(response, ResultBody.class);
        assertThat(body.getSuccess()).isTrue();

        List<BaseRole> roleList = (List<BaseRole>) body.getResult();
        assertThat(roleList).isNotEmpty();
        assertThat(roleList.stream().anyMatch(r -> "super_admin".equals(r.getRoleCode()))).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("Flow 8: Role CRUD with authority grant")
    void testFlow8_roleCrudAndGrant() throws Exception {
        String roleCode = "test_role_" + System.currentTimeMillis();

        BaseRoleForm createForm = new BaseRoleForm();
        createForm.setRoleCode(roleCode);
        createForm.setRoleName("Test Role");
        createForm.setStatus(1);
        createForm.setRoleDesc("OpenAPI integration test role");

        MvcResult createResult = mockMvc.perform(post("/role")
                .header("satoken", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(createForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        MvcResult listResult = mockMvc.perform(get("/role/all")
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String listResponse = listResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> listBody = JSON.parseObject(listResponse, ResultBody.class);

        List<BaseRole> roleList = (List<BaseRole>) listBody.getResult();
        BaseRole createdRole = roleList.stream()
                .filter(r -> roleCode.equals(r.getRoleCode()))
                .findFirst()
                .orElse(null);
        assertThat(createdRole).isNotNull();
        Long roleId = createdRole.getRoleId();

        MvcResult detailResult = mockMvc.perform(get("/role/{roleId}", roleId)
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.result.roleCode").value(roleCode))
                .andReturn();

        BaseRoleForm updateForm = new BaseRoleForm();
        updateForm.setRoleId(roleId);
        updateForm.setRoleCode(roleCode);
        updateForm.setRoleName("Test Role Updated");
        updateForm.setStatus(1);

        MvcResult updateResult = mockMvc.perform(put("/role/{roleId}", roleId)
                .header("satoken", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(updateForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        ResultBody<List<OpenAuthority>> adminAuthResult = baseAuthorityController.getUserAuthorities(JbmConstants.ROOT_USER_ID);
        assertThat(adminAuthResult.getSuccess()).isTrue();
        String authorityId = adminAuthResult.getResult().get(0).getAuthorityId();

        BaseAuthorityRoleForm grantForm = new BaseAuthorityRoleForm();
        grantForm.setAuthorityIds(new String[]{authorityId});

        MvcResult grantResult = mockMvc.perform(put("/authority/roles/{roleId}", roleId)
                .header("satoken", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJSONString(grantForm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        MvcResult authResult = mockMvc.perform(get("/authority/roles/{roleId}/authorities", roleId)
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String authResponse = authResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> authBody = JSON.parseObject(authResponse, ResultBody.class);
        assertThat(authBody.getSuccess()).isTrue();

        MvcResult deleteResult = mockMvc.perform(delete("/role/{roleId}", roleId)
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
    }

    @Test
    @Order(9)
    @DisplayName("Flow 9: Query user authorities")
    void testFlow9_userAuthorities() throws Exception {
        MvcResult result = mockMvc.perform(get("/authority/users/{userId}/authorities", JbmConstants.ROOT_USER_ID)
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> body = JSON.parseObject(response, ResultBody.class);
        assertThat(body.getSuccess()).isTrue();

        List<OpenAuthority> authorities = (List<OpenAuthority>) body.getResult();
        assertThat(authorities).isNotEmpty();
    }

    @Test
    @Order(10)
    @DisplayName("Flow 10: Query menu tree")
    void testFlow10_menuTree() throws Exception {
        MvcResult result = mockMvc.perform(get("/authority/menus")
                .header("satoken", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultBody<?> body = JSON.parseObject(response, ResultBody.class);
        assertThat(body.getSuccess()).isTrue();
        assertThat(body.getResult()).isNotNull();
    }
}
