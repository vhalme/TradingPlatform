package com.lenin.tradingplatform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RatesMain {

	public static void main(String[] args) {
			
		String configFile = "META-INF/rates-context.xml";
		
		if(args.length > 0) {
			if(args[0].equals("debug")) {
				configFile = "META-INF/rates-context-debug.xml";
			}
		}
		
		ApplicationContext context = 
				new ClassPathXmlApplicationContext(configFile);
		
		RatesMain main = context.getBean(RatesMain.class);
		
	}

	
	@Autowired
	private RatesUpdateProcess ratesUpdateProcess;
	
	
	@Scheduled(fixedRate = 15500)
	public void update() {
		
		System.out.println("Start scheduled rates update v7");
		
		ratesUpdateProcess.update();
		
		System.out.println("Finished scheduled rates update v7");
		
		
	}
	
}
