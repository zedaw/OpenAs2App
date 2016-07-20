package org.openas2.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.WrappedException;
import org.openas2.message.AS2Message;
import org.openas2.message.Message;
import org.openas2.message.MessageMDN;
import org.openas2.params.CompositeParameters;
import org.openas2.params.ExceptionParameters;
import org.openas2.params.InvalidParameterException;
import org.openas2.params.MessageMDNParameters;
import org.openas2.params.MessageParameters;
import org.openas2.params.ParameterParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;


public class EmailLogger extends BaseLogger {
    private static final Logger logger = LoggerFactory.getLogger(EmailLogger.class);
    public static final String PARAM_FROM_DISPLAY = "from_display";
    public static final String PARAM_FROM = "from";
    public static final String PARAM_TO = "to";
    public static final String PARAM_SMTPSERVER = "smtpserver";
    public static final String PARAM_SMTPPORT = "smtpport";
    public static final String PARAM_SMTPPROTOCOL = "smtpprotocol";
    public static final String PARAM_SUBJECT = "subject";
    public static final String PARAM_BODY = "body";
    public static final String PARAM_BODYTEMPLATE = "bodytemplate";

    private Properties props = new Properties();
    private boolean isDebugOn = false;

    public void init(Session session, Map<String, String> parameters) throws OpenAS2Exception {
        super.init(session, parameters);
        getParameter(PARAM_FROM, true);
        getParameter(PARAM_TO, true);
        getParameter(PARAM_SMTPSERVER, true);
        // copy system properties then allow override by javax.mail.properties.file 
        props.putAll(System.getProperties());
        String filename = getParameter("javax.mail.properties.file", false);
        if (filename != null)
        {
            ParameterParser parser = createParser();
            filename = ParameterParser.parse(filename, parser);
            FileInputStream in = null;
            try
			{
            	in = new FileInputStream(filename);
				props.load(in);
			} catch (FileNotFoundException e)
			{
	            logger.error("File not found for attribute javax.mail.properties.file: " + filename);
	            e.printStackTrace();
			} catch (IOException e)
			{
	            logger.error("File for attribute javax.mail.properties.file cannot be accessed: " + filename);
	            e.printStackTrace();
			}
            finally
            {
            	if (in != null)
					try
					{
						in.close();
					} catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            }
        }
        
    }

	public void doLog(Level level, String msgText, Message as2Msg) {
    	
    	if (level != Level.ERROR)
    	{
    		return;
    	}
    	isDebugOn = "true".equalsIgnoreCase(System.getProperty("maillogger.debug.enabled", "false"));
        try {
            String subject = getSubject(level, msgText);
            sendMessage(subject, getFormatter().format(level, msgText + (as2Msg == null?"":as2Msg.getLogMsgID())));
        } catch (Exception e) {
            logger.error("Failed to send email: ", e);
        }
    }

    protected void doLog(OpenAS2Exception exception, boolean terminated) {
    	isDebugOn = "true".equalsIgnoreCase(System.getProperty("maillogger.debug.enabled", "false"));
        try {
            String subject = getParameter(PARAM_SUBJECT, false);

            if (subject == null) {
                subject = getSubject(exception);
            }

            subject = parseText(exception, terminated, subject);

            StringBuffer body = new StringBuffer();

            if (getParameter(PARAM_BODY, false) != null) {
                body.append(parseText(exception, terminated, getParameter(PARAM_BODY, false)));
            }

            body.append("\r\n");

            if (getParameter(PARAM_BODYTEMPLATE, false) != null) {
                body.append(parseText(exception, terminated, getTemplateText()));
            } else {
                body.append(getFormatter().format(exception, terminated));
            }

            sendMessage(subject, body.toString());
        } catch (Exception e) {
            logger.error("Failed to send email: ", e);
        }
    }

    protected String getShowDefaults() {
        return VALUE_SHOW_TERMINATED;
    }

    protected String getSubject(Level level, String msg) {
        StringBuffer subject = new StringBuffer("OpenAS2 Log (" + level.name() + "): " + msg.substring(0, msg.indexOf("\n")));

        return subject.toString();
    }

    protected String getSubject(OpenAS2Exception e) {
        StringBuffer subject = new StringBuffer("OpenAS2 Exception: ");

        if (e instanceof WrappedException) {
            subject.append(((WrappedException) e).getSource().getClass().getName());
        } else {
            subject.append(e.getClass().getName());
        }

        subject.append(": ").append(e.getMessage());

        return subject.toString();
    }

    protected String getTemplateText() throws InvalidParameterException {
        try {
            File file = new File(getParameter(PARAM_BODYTEMPLATE, true));

            if (file.exists() && file.isFile()) {
                FileInputStream fIn = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fIn.read(data);
                fIn.close();

                return new String(data);
            }

            return "";
        } catch (IOException ioe) {
            throw new InvalidParameterException("Error reading or parsing template file " +
                getParameter(PARAM_BODYTEMPLATE, true) + ":" + ioe.getMessage(), this, null, null);
        }
    }

    protected ParameterParser createParser() {
        CompositeParameters compParams = new CompositeParameters(false);
        return compParams;
    }

    protected CompositeParameters createParser(AS2Message msg, OpenAS2Exception exception,
        boolean terminated) {
        CompositeParameters params = new CompositeParameters(true);

        if (msg != null) {
            params.add("message", new MessageParameters(msg));
        } else {
            params.add("message", null);
        }

        if (msg != null && (Object) msg instanceof MessageMDN) {
            params.add("mdn", new MessageMDNParameters((MessageMDN) (Object) msg));
        } else {
            params.add("mdn", null);
        }

        if (exception != null) {
            params.add("exception", new ExceptionParameters(exception, terminated));
        } else {
            params.add("exception", null);
        }

        return params;
    }

    protected String parseText(OpenAS2Exception exception, boolean terminated, String text)
        throws InvalidParameterException {
        AS2Message msg = (AS2Message) exception.getSource(OpenAS2Exception.SOURCE_MESSAGE);
        
        CompositeParameters parser = createParser(msg, exception, terminated);
        
        return ParameterParser.parse(text, parser);
        
    }

    protected String parseText(AS2Message msg, String text)
        throws InvalidParameterException {
        CompositeParameters parser = createParser(msg, null, false);

        return ParameterParser.parse(text, parser);
    }

    protected void sendMessage(String subject, String text) throws OpenAS2Exception {
        String protocol = getParameter("smtpprotocol", false);
        if (protocol == null || protocol.length() < 1)
        {
        	protocol = (String) props.get("mail.transport.protocol");
            if (protocol == null || protocol.length() < 1) protocol = "smtp";
        }

        String userName = null;
        String password = null;
        boolean isAuth = false;
        javax.mail.Session jmSession = null;
		try
		{
			isAuth = "true".equalsIgnoreCase(getParameter("smtpauth", "false"));
		} catch (InvalidParameterException e)
		{
		}
        if (isAuth)
        {
	        props.put("mail." + protocol + ".auth", "true");
        	try
			{
				userName = getParameter("smtpuser", false);
				if (userName == null)
				{
					userName = (String) props.get("mail."+protocol+".user");
					if (userName == null)
					{
						userName = (String) props.get("mail.user");
					}
					if (userName == null) getParameter("smtpuser", true);
				}
	        	password = getParameter("smtppwd", true);
			} catch (InvalidParameterException e)
			{
				throw new OpenAS2Exception("Failed to find email logger parameter: ", e);
			}
        }
        final String uid = userName;
        final String pwd = password;
        if (isAuth)
        {
	        props.put("mail." + protocol + ".user", uid);
			jmSession = javax.mail.Session.getDefaultInstance(props, new javax.mail.Authenticator()
			{
				protected PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication(uid, pwd);
				}
			});
        }
        else
			jmSession = javax.mail.Session.getDefaultInstance(props);

        if (isDebugOn)
        {
        	jmSession.setDebug(true);
        	jmSession.setDebugOut(System.out);
        }
        try {
            MimeMessage m = new MimeMessage(jmSession);
            String from = getParameter(PARAM_FROM, true);
            String fromDisplay = getParameter(PARAM_FROM_DISPLAY, false);
            if (fromDisplay == null || fromDisplay.length() < 1) fromDisplay = from;
            try
			{
				m.setFrom(new InternetAddress(from, fromDisplay));
			} catch (UnsupportedEncodingException e)
			{
				logger.error("Check the text in the \"" + PARAM_FROM_DISPLAY + "\" parameter for encoding issues.");
				e.printStackTrace();
				m.setFrom(new InternetAddress(from));
			}
            m.setSender(new InternetAddress(getParameter(PARAM_FROM, true)));
            m.setRecipient(javax.mail.Message.RecipientType.TO,
                new InternetAddress(getParameter(PARAM_TO, true)));

            m.setSentDate(new Date());
            m.setSubject(subject);
            m.setText(text);            
            String mailServer = getParameter(PARAM_SMTPSERVER, false);
            if (mailServer == null || mailServer.length() < 1)
            {
            	mailServer = (String) props.get("mail."+protocol+".host");
                if (mailServer == null || mailServer.length() < 1) mailServer = (String) props.get("mail.host");
                if (mailServer == null || mailServer.length() < 1) getParameter(PARAM_SMTPSERVER, true);
            }

            if (mailServer != null) props.put("mail."+protocol+".host", mailServer);
            String serverPort = getParameter(PARAM_SMTPPORT, false);
            if (serverPort == null || serverPort.length() < 1)
            {
            	serverPort = (String) props.get("mail."+protocol+".port");
                if (serverPort == null || serverPort.length() < 1) serverPort = (String) props.get("mail.port");
                if (serverPort == null || serverPort.length() < 1) getParameter(PARAM_SMTPPORT, true);
            }

            if (serverPort != null) props.put("mail."+protocol+".port", serverPort);

            if (isDebugOn)
            {
            	logger.debug("Mail Logger Config:::");
            	logger.debug("\tSMTP server: " + props.get("mail.smtp.host"));
            	logger.debug("\tSMTP port: " + props.get("mail.smtp.port"));
            	logger.debug("\tSMTP protocol: " + protocol);
            	logger.debug("\tSMTP authentication: " + isAuth);
            	logger.debug("\tSMTP user name: " + uid);
            	logger.debug("\tSMTP password: " + pwd);
            }

            if (isAuth)
            {
                Transport t = jmSession.getTransport(protocol);
                try {
                    t.connect(uid, pwd);
                    t.sendMessage(m, m.getAllRecipients());
                } finally {
                    t.close();
                }

            }
            else
            	Transport.send(m);
        } catch (MessagingException me) {
            me.printStackTrace();
        } catch (InvalidParameterException ipe) {
            ipe.printStackTrace();
        } finally {
            if (isDebugOn)
            {
            	logger.debug("Mail Logger EXIT...:::");
            }
        }
    }
}
