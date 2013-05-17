package com.lenin.tradingplatform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Main {

	public static void main(String[] args) {
			
		ApplicationContext context = 
				new ClassPathXmlApplicationContext("META-INF/application-context.xml");
		
		Main main = context.getBean(Main.class);
		main.init();
		
	}

	
	@Autowired
	private TradingProcess tradingProcess;
	
	private void init() {
		
		System.out.println("Started: "+tradingProcess);
		
	}
	
	@Scheduled(fixedDelay = 15000)
	public void update() {
		
		tradingProcess.update();
		
		
	}
	
}
