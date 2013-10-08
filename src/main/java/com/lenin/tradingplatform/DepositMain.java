package com.lenin.tradingplatform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DepositMain {
	
	private static String currency = null;
	
	public static void main(String[] args) {
		
		String configFile = "META-INF/deposit-context.xml";
		
		if(args.length > 0) {
			if(!args[0].equals("all")) {
				currency = args[0];
			}
		}
		
		if(args.length > 1) {
			if(args[1].equals("debug")) {
				configFile = "META-INF/deposit-context-debug.xml";
			}
		}
		
		ApplicationContext context = 
				new ClassPathXmlApplicationContext(configFile);
		
		DepositMain main = context.getBean(DepositMain.class);
		
	}
	
	@Autowired
	private DepositMonitor depositMonitor;
	
	@Scheduled(fixedDelay = 15000)
	public void update() {
		
		System.out.println("Start scheduled deposits monitor");
		
		if(currency == null) {
			depositMonitor.update();
		} else {
			
			depositMonitor.update(currency);
			
			if(currency.equals("usd")) {
				depositMonitor.sendEmails();
			}
			
		}
		
		System.out.println("Finished scheduled deposits monitor");
		
		
	}
	
}
