package com.td.sample.wechat.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;
import org.sword.wechat4j.event.EventType;
import org.sword.wechat4j.exception.WeChatException;
import org.sword.wechat4j.menu.Menu;
import org.sword.wechat4j.menu.MenuButton;
import org.sword.wechat4j.menu.MenuManager;

@Service
public class MenuService {
	private MenuManager manager;

	@PostConstruct
	public void init() throws WeChatException {
		manager = new MenuManager();
		manager.delete();
//		getMenu();
		//createMenu();
	}

	public void getMenu() {
		try {
			Menu menu = manager.getMenu();
			System.out.println(menu.getButton().size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createMenu() {
		List<MenuButton> subButList1 = new ArrayList<MenuButton>();
		MenuButton subBut1 = new MenuButton();
		subBut1.setName("资产价值进化中心");
		subBut1.setType(EventType.view);
		subBut1.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=2");
		MenuButton subBut2 = new MenuButton();
		subBut2.setName("智慧能源创新中心");
		subBut2.setType(EventType.view);
		subBut2.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=3");
		MenuButton subBut3 = new MenuButton();
		subBut3.setName("先进城市运载工具研究所");
		subBut3.setType(EventType.view);
		subBut3.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=4");
		MenuButton subBut4 = new MenuButton();
		subBut4.setName("智慧人居与建筑研究中心");
		subBut4.setType(EventType.view);
		subBut4.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=5");
		MenuButton subBut5 = new MenuButton();
		subBut5.setName("可持续环境评估中心");
		subBut5.setType(EventType.view);
		subBut5.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=6");
		MenuButton subBut6 = new MenuButton();
		subBut6.setName("产业互联网平台研发中心");
		subBut6.setType(EventType.view);
		subBut6.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=7");

		subButList1.add(subBut1);
		subButList1.add(subBut2);
		subButList1.add(subBut3);
		subButList1.add(subBut4);
		subButList1.add(subBut5);
//		subButList1.add(subBut6);

		// 单击按钮
		MenuButton btnClick1 = new MenuButton();
		btnClick1.setName("研究中心");
		btnClick1.setType(EventType.click);
		btnClick1.setKey("研究中心");
		btnClick1.setSubButton(subButList1);

		List<MenuButton> subButList2 = new ArrayList<MenuButton>();
		MenuButton subBut21 = new MenuButton();
		subBut21.setName("能源互联网");
		subBut21.setType(EventType.view);
		subBut21.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=8");
		MenuButton subBut22 = new MenuButton();
		subBut22.setName("工业4.0");
		subBut22.setType(EventType.view);
		subBut22.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=9");
		MenuButton subBut23 = new MenuButton();
		subBut23.setName("深度学习");
		subBut23.setType(EventType.view);
		subBut23.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=10");
		MenuButton subBut24 = new MenuButton();
		subBut24.setName("智慧交通");
		subBut24.setType(EventType.view);
		subBut24.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=11");
		MenuButton subBut25 = new MenuButton();
		subBut25.setName("环境与可持续发展");
		subBut25.setType(EventType.view);
		subBut25.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=12");
		MenuButton subBut26 = new MenuButton();
		subBut25.setName("区块链");
		subBut25.setType(EventType.view);
		subBut25.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=13");

		subButList2.add(subBut21);
		subButList2.add(subBut22);
		subButList2.add(subBut23);
		subButList2.add(subBut24);
		subButList2.add(subBut25);
//		subButList2.add(subBut26);

		// 单击按钮
		MenuButton btnClick2 = new MenuButton();
		btnClick2.setName("行业前沿");
		btnClick2.setType(EventType.click);
		btnClick2.setKey("行业前沿");
		btnClick2.setSubButton(subButList2);

		MenuButton btnClick3 = new MenuButton();
		btnClick3.setName("关于我们");
		btnClick3.setType(EventType.view);
		btnClick3.setUrl("http://www.gi2t.com/index.php?m=page&a=index&id=15");

		List<MenuButton> button = new ArrayList<MenuButton>();
		button.add(btnClick1);
		button.add(btnClick2);
		button.add(btnClick3);
		Menu menu = new Menu();
		menu.setButton(button);
		
		try {
			manager.create(menu);
			System.out.println("菜单创建成功");
		} catch (WeChatException e) {
			System.out.println("菜单创建失败");
			e.printStackTrace();
		}
		
	}

}
