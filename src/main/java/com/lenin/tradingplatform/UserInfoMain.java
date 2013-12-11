package com.lenin.tradingplatform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class UserInfoMain {

	public static void main(String[] args) {
			
		String configFile = "META-INF/userinfo-context.xml";
		
		if(args.length > 0) {
			if(args[0].equals("debug")) {
				configFile = "META-INF/userinfo-context-debug.xml";
			}
		}
		
		ApplicationContext context = 
				new ClassPathXmlApplicationContext(configFile);
		
		UserInfoMain main = context.getBean(UserInfoMain.class);
		main.init();
		
		
	}

	
	@Autowired
	private UserInfoProcess btceUserInfoProcess;
	
	@Autowired
	private UserInfoProcess mtgoxUserInfoProcess;
	
	private void init() {
		btceUserInfoProcess.init();
		mtgoxUserInfoProcess.init();
	}
	
	@Scheduled(fixedDelay = 15000)
	public void update() {
		
		System.out.println("Start scheduled user info update");
		
		btceUserInfoProcess.update();
		mtgoxUserInfoProcess.update();
		
		System.out.println("Finished scheduled user info update");
		
		
	}
	
}
