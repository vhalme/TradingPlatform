package com.lenin.tradingplatform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TradingMain {

	public static void main(String[] args) {
			
		String configFile = "META-INF/trading-context.xml";
		
		if(args.length > 0) {
			if(args[0].equals("debug")) {
				configFile = "META-INF/trading-context-debug.xml";
			}
		}
		
		ApplicationContext context = 
				new ClassPathXmlApplicationContext(configFile);
		
		TradingMain main = context.getBean(TradingMain.class);
		main.init();
		
	}

	
	@Autowired
	private TradingProcess tradingProcess;
	
	private void init() {
		tradingProcess.init();
	}
	
	@Scheduled(fixedDelay = 15000)
	public void update() {
		
		System.out.println("Start scheduled trading update v7");
		
		tradingProcess.update();
		
		System.out.println("Finished scheduled trading update v7");
		
		
	}
	
}
