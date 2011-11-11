package com.binhunt.workflow.serv;

import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.binhunt.workflow.impl.BHProcessContext;
import com.binhunt.workflow.impl.BaseActivity;
import com.binhunt.workflow.itf.ErrorHandler;
import com.binhunt.workflow.itf.ProcessContext;
import com.binhunt.workflow.itf.SenderDelegate;
import com.binhunt.workflow.test.MailTest.Email_Autherticator;

import javax.mail.Address;
import javax.mail.Session;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import javax.mail.Transport;

public class Notification extends BaseActivity
{
	private Log log = LogFactory.getLog(Notification.class);

	private String mailhost;
	private String username;
	private String password;
	private String mailfrom;
	private String mailsubject;
	private String personalName;
	private String body;
	
	public void setMailhost(String mailhost) {
		this.mailhost = mailhost;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setMailfrom(String mailfrom) {
		this.mailfrom = mailfrom;
	}

	public void setMailsubject(String mailsubject) {
		this.mailsubject = mailsubject;
	}

	public void setPersonalName(String personalName) {
		this.personalName = personalName;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public ProcessContext execute(ProcessContext context) throws Exception
	{
		log.debug("--- Entering execution of Notification ---");
		boolean bok = sendMail((BHProcessContext)context);
//		boolean bok = true;
		if(!bok)
		{
			log.debug("--- Send Mail Failed! ---");
			((BHProcessContext)context).setStopEntireProcess(true);
		}
		return context;
	}
	
	private boolean sendMail(BHProcessContext context)
	{
		try
		{
			String strHeadName = "Hi ";
			String strHeadValue = context.getUseremail();
			int posAt = strHeadValue.indexOf('@');
			strHeadValue = strHeadValue.substring(0, posAt);
			
			Properties props = new Properties();
			Authenticator auth = new Email_Autherticator();
			props.put("mail.smtp.host", mailhost);
			
			//props.put("mail.smtp.socketFactory.port", "465");
			//props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			//props.put("mail.smtp.port", "465");
			
			props.put("mail.smtp.auth", "true");
			Session session = Session.getDefaultInstance(props, auth);
			
			MimeMessage message = new MimeMessage(session);
			message.setSubject(mailsubject);
			String tBody = "Hi " + strHeadValue + ",\n\r\t" + body;
			message.setText(tBody);
			message.setHeader(strHeadName, strHeadValue);
			message.setSentDate(new Date());
			Address address = new InternetAddress(mailfrom, personalName);
			message.setFrom(address);
			Address toAddress = new InternetAddress(context.getUseremail());
			message.addRecipient(Message.RecipientType.TO, toAddress);
			Transport.send(message);
			log.debug("Sending notification email ok!");
		}
		catch (Exception ex)
		{
			log.debug(ex.getMessage());
			return false;
		}
		return true;
	}
	
	class Email_Autherticator extends Authenticator
	{
		public Email_Autherticator()
		{
			super();
		}

		public Email_Autherticator(String user, String pwd)
		{
			super();
			username = user;
			password = pwd;
		}
		
		public PasswordAuthentication getPasswordAuthentication()
		{
			return new PasswordAuthentication(username, password);
		}
	}
}
