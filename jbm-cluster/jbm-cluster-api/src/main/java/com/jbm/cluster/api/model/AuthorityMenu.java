package com.jbm.cluster.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jbm.cluster.api.model.entity.BaseMenu;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 菜单权限
 *
 * @author wesley.zhang
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AuthorityMenu extends BaseMenu implements Serializable {

    private static final long serialVersionUID = 3474271304324863160L;


    public Long getMenuId() {
        return this.getId();
    }

    public void setMenuId(Long id) {
        this.setId(id);
    }

    /**
     * 权限ID
     */
    private Long authorityId;

    /**
     * 权限标识
     */
    private String authority;


    private List<AuthorityAction> actionList;

    public Long getAuthorityId() {
        return authorityId;
    }

    public void setAuthorityId(Long authorityId) {
        this.authorityId = authorityId;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }


    public List<AuthorityAction> getActionList() {
        return actionList;
    }

    public void setActionList(List<AuthorityAction> actionList) {
        this.actionList = actionList;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AuthorityMenu)) {
            return false;
        }
        AuthorityMenu a = (AuthorityMenu) obj;
        return this.authorityId.equals(a.getAuthorityId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorityId);
    }
}
