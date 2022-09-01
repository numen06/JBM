package jbm.framework.weixin;

import lombok.Data;
import me.chanjar.weixin.common.bean.menu.WxMenu;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wx.mp.menu")
public class WxMenuProperties extends WxMenu {

    private Boolean clean = false;

}
