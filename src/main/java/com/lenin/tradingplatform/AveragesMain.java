package com.lenin.tradingplatform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AveragesMain {

	public static void main(String[] args) {
		
		String configFile = "META-INF/averages-context.xml";
		
		if(args.length > 0) {
			if(args[0].equals("debug")) {
				configFile = "META-INF/averages-context-debug.xml";
			}
		}
		
		ApplicationContext context = 
				new ClassPathXmlApplicationContext(configFile);
		
		AveragesMain main = context.getBean(AveragesMain.class);
		System.out.println("Staring update manually...");
		main.updateMovingAverages();
		
	}
	
	@Autowired
	private MovingAverageProcess movingAverageProcess;
	
	//@Scheduled(fixedRate = 60000)
	public void updateMovingAverages() {
		
		System.out.println("Start scheduled moving averages update");	
		movingAverageProcess.update();
		System.out.println("Finished scheduled moving averages update");
		
	}
	
	
}
