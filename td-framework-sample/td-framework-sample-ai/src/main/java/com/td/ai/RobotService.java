package com.td.ai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.turing.util.PostServer;

@Service
public class RobotService {

	@PostConstruct
	public void init() {

		try {
			while (true) {
				BufferedReader strin = new BufferedReader(new InputStreamReader(System.in));
				System.out.print("请输入一个字符串：");
				String str = strin.readLine();
				RobotRequestBean req = new RobotRequestBean();
				req.setInfo(str);
				String result = PostServer.SendPost(req.encrypt(), "http://www.tuling123.com/openapi/api");
				System.out.println("回答：" + result);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
