package com.lenin.tradingplatform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderMain {

	public static void main(String[] args) {
			
		String configFile = "META-INF/order-context.xml";
		
		if(args.length > 0) {
			if(args[0].equals("debug")) {
				configFile = "META-INF/order-context-debug.xml";
			}
		}
		
		ApplicationContext context = 
				new ClassPathXmlApplicationContext(configFile);
		
		OrderMain main = context.getBean(OrderMain.class);
		main.init();
		//System.out.println("Staring update manually...");
		//main.update();
		
	}

	
	@Autowired
	private OrderProcess btceOrderProcess;
	
	@Autowired
	private OrderProcess mtgoxOrderProcess;
	
	@Autowired
	private OrderProcess testOrderProcess;
	
	
	private void init() {
		btceOrderProcess.init();
		mtgoxOrderProcess.init();
		testOrderProcess.init();
	}
	
	@Scheduled(fixedDelay = 15000)
	public void update() {
		
		System.out.println("Start scheduled trades and orders update");
		
		btceOrderProcess.update();
		mtgoxOrderProcess.update();
		testOrderProcess.update();
		
		System.out.println("Finished scheduled trades and orders update");
		
		
	}
	
}
