package com.iped.ipcam.mail;

import java.security.Security;
import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.iped.ipcam.gui.R;
import com.iped.ipcam.utils.DateUtil;

public class SendEmail {

	private Context context;
	
	private SharedPreferences settings = null;
	
	public SendEmail(Context context) throws Exception {
		this.context = context;
		settings = context.getSharedPreferences("PERSONAL_SET", 0);
	}
	
	public void sendEmail(String content) throws Exception {
		final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory"; 
		Security.addProvider(new JSSEProvider());  
		final String userName = settings.getString("send_email_username", "skyxcu@126.com");//发件地址
		final String password = settings.getString("send_email_password", "123456");//发件人密码
		String receiver = settings.getString("receive_email_username", "sky.xctc@163.com");//收件人地址
		Properties props = System.getProperties();  
		props.put("mail.smtp.connectiontimeout", "20000"); 
		props.put("mail.smtp.timeout", "50000");
		props.setProperty("mail.smtp.host", settings.getString("smtp_server", "smtp.126.com")); //smtp.126.com  smtp.gmail.com
		props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);  
		props.setProperty("mail.smtp.socketFactory.fallback", "false");  
		props.setProperty("mail.smtp.port", settings.getString("smtp_port", "465"));  //
		props.setProperty("mail.smtp.socketFactory.port", settings.getString("smtp_port", "465"));  //465
		props.put("mail.smtp.auth", "true");  
		Session session = Session.getDefaultInstance(props, new Authenticator(){  
			protected PasswordAuthentication getPasswordAuthentication() { 
					return new PasswordAuthentication(userName, password);  
				}
		});  
	    MimeMessage msg = new MimeMessage(session);
	    msg.setFrom(new InternetAddress(userName));
	    if(receiver == null || "".equals(receiver)) {
	    	receiver = userName;
	    }
	    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiver,false));  
	    msg.setSubject(context.getResources().getString(R.string.sms_to_email_title_str) + DateUtil.formatTimeToDate5(System.currentTimeMillis()));  
	    msg.setText(content);  
	    msg.setSentDate(new Date());  
	    Transport.send(msg); 
	    Log.d("SendEmailThread", "SEND EMAIL REPORT SUCCESS!");
	}
}
