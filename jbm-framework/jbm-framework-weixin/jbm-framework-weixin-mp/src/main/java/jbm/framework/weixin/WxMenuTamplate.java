package jbm.framework.weixin;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.menu.WxMenu;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.menu.WxMpMenu;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@NoArgsConstructor
public class WxMenuTamplate {

    private Map<String, WxMenu> wxMenuMap = new ConcurrentHashMap<>();

    @Autowired
    private WxMpService wxService;

    private WxMenuProperties wxMenuProperties;

    public WxMenuTamplate(WxMenuProperties wxMenuProperties) {
        this.wxMenuProperties = wxMenuProperties;
    }

    @PostConstruct
    private void init() throws WxErrorException {
        if (ObjectUtils.isNotEmpty(wxMenuProperties)) {
            WxMpMenu mpMenu = this.wxService.getMenuService().menuGet();
            if (ObjectUtils.isNotEmpty(mpMenu)) {
                log.info("现有菜单:{}", mpMenu.toJson());
                if (wxMenuProperties.getClean()) {
                    mpMenu.getMenu().getButtons().forEach(wxMenuButton -> {
                        try {
                            this.wxService.getMenuService().menuDelete(wxMenuButton.getKey());
                        } catch (WxErrorException e) {
                            throw new RuntimeException(e);
                        }
                    });

                } else {
                    return;
                }
            }
            this.create(wxMenuProperties);
        }
    }

    public void create(WxMenu wxMenu) {
        try {
            WxMenu menu = new WxMenu();
            menu.getButtons().addAll(wxMenu.getButtons());
//            log.info(wxMenu.toJson());
            log.info(menu.toJson());
            this.wxService.switchover(wxService.getWxMpConfigStorage().getAppId());
            this.wxService.getMenuService().menuCreate(menu);
        } catch (Exception e) {
            log.error("生成菜单错误", e);
        }
    }
}
