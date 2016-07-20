package org.openas2.processor.receiver;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;

import javax.activation.DataHandler;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;

import org.openas2.OpenAS2Exception;
import org.openas2.message.AS2Message;
import org.openas2.message.AS2MessageMDN;
import org.openas2.message.Message;
import org.openas2.message.MessageMDN;
import org.openas2.util.AS2Util;
import org.openas2.util.ByteArrayDataSource;
import org.openas2.util.HTTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AS2MDNReceiverHandler implements NetModuleHandler {
    private AS2MDNReceiverModule module;

    private static final Logger logger = LoggerFactory.getLogger(AS2MDNReceiverHandler.class);

    
    public AS2MDNReceiverHandler(AS2MDNReceiverModule module) {
        super();
        this.module = module;
    }

    public String getClientInfo(Socket s) {
        return " " + s.getInetAddress().getHostAddress() + " " + Integer.toString(s.getPort());
    }

    public AS2MDNReceiverModule getModule() {
        return module;
    }

	public void handle(NetModule owner, Socket s)
	{

		if (logger.isInfoEnabled())
			logger.info("incoming connection" + " [" + getClientInfo(s) + "]");

		AS2Message msg = new AS2Message();

		byte[] data = null;

		// Read in the message request, headers, and data
		try
		{
			data = HTTPUtil.readData(s.getInputStream(), s.getOutputStream(), msg);
			// check if the requested URL is defined in attribute
			// "as2_receipt_option"
			// in one of partnerships, if yes, then process incoming AsyncMDN
			if (logger.isInfoEnabled())
				logger.info("incoming connection for receiving AsyncMDN" + " [" + getClientInfo(s) + "]"
						+ msg.getLogMsgID());
			ContentType receivedContentType;

			MimeBodyPart receivedPart = new MimeBodyPart(msg.getHeaders(), data);
			msg.setData(receivedPart);
			receivedContentType = new ContentType(receivedPart.getContentType());

			// MimeBodyPart receivedPart = new MimeBodyPart();
			receivedPart.setDataHandler(new DataHandler(new ByteArrayDataSource(data, receivedContentType.toString(),
					null)));
			receivedPart.setHeader("Content-Type", receivedContentType.toString());

			msg.setData(receivedPart);
			// Create a MessageMDN and copy HTTP headers
			MessageMDN mdn = new AS2MessageMDN(msg);
			// copy headers from msg to MDN from msg
			mdn.setHeaders(msg.getHeaders());
			AS2Util.processMDN(msg, data, s.getOutputStream(), true, getModule().getSession(), this);

		} catch (Exception e)
		{
			if (Message.MSG_STATUS_MDN_PROCESS_INIT.equals(msg.getStatus())
					|| Message.MSG_STATUS_MDN_PARSE.equals(msg.getStatus())
					|| !(e instanceof OpenAS2Exception))
			{
				/*
				 * Cannot identify the target if in init or parse state so not sure what the
				 * best course of action is apart from do nothing
				 */
				try
				{
					HTTPUtil.sendHTTPResponse(s.getOutputStream(), HttpURLConnection.HTTP_BAD_REQUEST, false);
				} catch (IOException e1)
				{
				}
				msg.setLogMsg("Unhandled error condition receiving asynchronous MDN. Message and asociated files cleanup will be attempted but may be in an unknown state.");
				logger.error(msg.toString(), e);
			}
			/*
			 * Most likely a resend abort of max resend reached if
			 * OpenAS2Exception so do not log as should have been logged
			 * upstream ... just clean up the mess
			 */
			else
			{
				// Must have received MDN successfully so must respond with OK
				try
				{
					HTTPUtil.sendHTTPResponse(s.getOutputStream(), HttpURLConnection.HTTP_OK, false);
				} catch (IOException e1)
				{ // What to do .... 
				}
				msg.setLogMsg("Exception receiving asynchronous MDN. Message and asociated files cleanup will be attempted but may be in an unknown state.");
				logger.error(msg.toString(), e);

			}
			AS2Util.cleanupFiles(msg, true);
		}

	}
}