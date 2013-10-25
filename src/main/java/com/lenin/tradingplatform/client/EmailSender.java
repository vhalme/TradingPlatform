package com.lenin.tradingplatform.client;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

	public static String hostName = "http://btcmachin.es";
	//public static String hostName = "http://localhost/Flux";
	
	public static Boolean send(String emailAddress, String subject, String text) {
		
		Boolean sendOk = false;
		
		System.out.println("Settin up e-mail properties");
		
		Properties props = new Properties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");

		try {
			
			System.out.println("Settin up e-mail session");
			
			Session session = Session.getDefaultInstance(props,
					new javax.mail.Authenticator() {
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication("btcmachines@gmail.com",
									"bTcmGoogleThuyu5eP");
						}
					});
			
			System.out.println("Creating message");
			
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress("btcmachines@gmail.com"));
			message.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(emailAddress));
			message.setSubject(subject);
			message.setText(text, "UTF-8", "html");

			System.out.println("Sending message");
			Transport.send(message);
			
			sendOk = true;
			
			System.out.println("Done");

		} catch (MessagingException e) {
			e.printStackTrace();
		}
		
		return sendOk;
		
	}
	
}