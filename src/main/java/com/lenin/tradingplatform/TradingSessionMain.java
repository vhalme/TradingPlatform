package com.lenin.tradingplatform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TradingSessionMain {

	public static void main(String[] args) {
			
		String configFile = "META-INF/tradingsession-context.xml";
		
		if(args.length > 0) {
			if(args[0].equals("debug")) {
				configFile = "META-INF/tradingsession-context-debug.xml";
			}
		}
		
		ApplicationContext context = 
				new ClassPathXmlApplicationContext(configFile);
		
		TradingSessionMain main = context.getBean(TradingSessionMain.class);
		main.init();
		
	}

	
	@Autowired
	private TradingSessionProcess tradingSessionProcess;
	
	private void init() {
		tradingSessionProcess.init();
	}
	
	@Scheduled(fixedDelay = 15000)
	public void update() {
		
		System.out.println("Start scheduled trading session update");
		
		tradingSessionProcess.update();
		
		System.out.println("Finished scheduled trading session update");
		
		
	}
	
}
