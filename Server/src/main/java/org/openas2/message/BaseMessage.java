package org.openas2.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;

import org.openas2.OpenAS2Exception;
import org.openas2.WrappedException;
import org.openas2.lib.helper.ICryptoHelper;
import org.openas2.params.InvalidParameterException;
import org.openas2.partner.Partnership;


public abstract class BaseMessage implements Message {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DataHistory history;
    private InternetHeaders headers;
    private Map<String,String> attributes;
    private MessageMDN MDN;
    private MimeBodyPart data;
    private Partnership partnership;
	private String compressionType = ICryptoHelper.COMPRESSION_NONE;
	private boolean rxdMsgWasSigned = false;
	private boolean rxdMsgWasEncrypted = false;
	private Map<Object, Object> options = new HashMap<Object, Object>();
	private String calculatedMIC = null;
	private String logMsg = null;
    private String status = MSG_STATUS_MSG_INIT;
	private Map<String, String> customOuterMimeHeaders = new HashMap<String, String>();
    

	public BaseMessage() {
        super();
    }


	public Map<Object, Object> getOptions() {
        if (options == null) {
            options = new HashMap<Object, Object>();
        }
		return options;
	}
	
    public String getStatus()
	{
		return status;
	}

	public void setStatus(String status)
	{
		this.status = status;
	}

	public Map<String, String> getCustomOuterMimeHeaders()
	{
		return customOuterMimeHeaders;
	}


	public void setCustomOuterMimeHeaders(Map<String, String> customOuterMimeHeaders)
	{
		this.customOuterMimeHeaders = customOuterMimeHeaders;
	}

	public void addCustomOuterMimeHeader(String key, String value)
	{
		this.customOuterMimeHeaders.put(key, value);
	}

	public void setOption(Object key, Object value) {
		getOptions().put(key, value);
	}

    public Object getOption(Object key) {
        return getOptions().get(key);
    }

    public void setAttribute(String key, String value) {
        getAttributes().put(key, value);
    }

    public String getAttribute(String key) {
        return (String) getAttributes().get(key);
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<String, String>();
        }

        return attributes;
    }

    public void setContentType(String contentType) {
        setHeader("Content-Type", contentType);
    }

    public String getContentType() {
        return getHeader("Content-Type");
    }

    public String getCompressionType() {
        return (compressionType);
    }

    public void setCompressionType(String myCompressionType) {
        compressionType = myCompressionType;
    }

    /**
     * @since 2007-06-01
     * @param contentDisposition
     */
    public void setContentDisposition(String contentDisposition) {
        setHeader("Content-Disposition", contentDisposition);
    }

    /**
     * @since 2007-06-01
     * @return
     */
    public String getContentDisposition() {
        return getHeader("Content-Disposition");
    }
    
    public void setData(MimeBodyPart data, DataHistoryItem historyItem) {
        this.data = data;

        if (data != null) {
            try {
                setContentType(data.getContentType());
            } catch (MessagingException e) {
                setContentType(null);
            }
            try { 
            	setContentDisposition(data.getHeader("Content-Disposition", null)); 
            }
            catch (MessagingException e) { 
            	setContentDisposition(null); // TODO: why ignore?????
            } 
        }

        if (historyItem != null) {
            getHistory().getItems().add(historyItem);
        }
    }

    public DataHistoryItem setData(MimeBodyPart data) throws OpenAS2Exception {
        try {
            DataHistoryItem historyItem = new DataHistoryItem(data.getContentType());
            setData(data, historyItem);

            return historyItem;
        } catch (Exception e) {
            throw new WrappedException(e);
        }
    }

    public MimeBodyPart getData() {
        return data;
    }

    public void setHeader(String key, String value) {
        getHeaders().setHeader(key, value);
    }

    public String getHeader(String key) {
        return getHeader(key, ", ");
    }

    public String getHeader(String key, String delimiter) {
        return getHeaders().getHeader(key, delimiter);
    }

    public void setHeaders(InternetHeaders headers) {
        this.headers = headers;
    }

    public InternetHeaders getHeaders() {
        if (headers == null) {
            headers = new InternetHeaders();
        }

        return headers;
    }

    public void setHistory(DataHistory history) {
        this.history = history;
    }

    public DataHistory getHistory() {
        if (history == null) {
            history = new DataHistory();
        }

        return history;
    }

    public void setMDN(MessageMDN mdn) {
        this.MDN = mdn;
    }

    public MessageMDN getMDN() {
        return MDN;
    }

    public void setMessageID(String messageID) {
        setHeader("Message-ID", messageID);
    }

    public String getMessageID() {
        return getHeader("Message-ID");
    }

    public void setPartnership(Partnership partnership) {
        this.partnership = partnership;
    }

    public Partnership getPartnership() {
        if (partnership == null) {
            partnership = new Partnership();
        }

        return partnership;
    }

    public abstract String generateMessageID() throws InvalidParameterException;

    public void setSubject(String subject) {
        setHeader("Subject", subject);
    }

    public String getSubject() {
        return getHeader("Subject");
    }

    public boolean isRxdMsgWasSigned()
	{
		return rxdMsgWasSigned;
	}

	public void setRxdMsgWasSigned(boolean rxdMsgWasSigned)
	{
		this.rxdMsgWasSigned = rxdMsgWasSigned;
	}

	public boolean isRxdMsgWasEncrypted()
	{
		return rxdMsgWasEncrypted;
	}

	public void setRxdMsgWasEncrypted(boolean rxdMsgWasEncrypted)
	{
		this.rxdMsgWasEncrypted = rxdMsgWasEncrypted;
	}

    public void addHeader(String key, String value) {
        getHeaders().addHeader(key, value);
    }

//    public String toString() {
//        StringBuffer buf = new StringBuffer();
//        buf.append("Message From:").append(getPartnership().getSenderIDs());
//        buf.append("To:").append(getPartnership().getReceiverIDs());
//
//        Enumeration<Header> headerEn = getHeaders().getAllHeaders();
//        buf.append("\r\nHeaders:{");
//
//        while (headerEn.hasMoreElements()) {
//            Header header = headerEn.nextElement();
//            buf.append(header.getName()).append("=").append(header.getValue());
//
//            if (headerEn.hasMoreElements()) {
//                buf.append(", ");
//            }
//        }
//
//        buf.append("}");
//        buf.append("\r\nAttributes:").append(getAttributes());
//
//        MessageMDN mdn = getMDN();
//
//        if (mdn != null) {
//            buf.append("\r\nMDN:");
//            buf.append(mdn.toString());
//        }
//
//        return buf.toString();
//    }

    
    
    public void updateMessageID() throws InvalidParameterException {
        setMessageID(generateMessageID());
    }

    /**
     * @return
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BaseMessage [history=");
        builder.append(history);
        builder.append(", headers=");
        builder.append(headers);
        builder.append(", attributes=");
        builder.append(attributes);
        builder.append(", MDN=");
        builder.append(MDN);
        builder.append(", data=");
        builder.append(data);
        builder.append(", partnership=");
        builder.append(partnership);
        builder.append(", compressionType=");
        builder.append(compressionType);
        builder.append(", rxdMsgWasSigned=");
        builder.append(rxdMsgWasSigned);
        builder.append(", rxdMsgWasEncrypted=");
        builder.append(rxdMsgWasEncrypted);
        builder.append(", options=");
        builder.append(options);
        builder.append(", calculatedMIC=");
        builder.append(calculatedMIC);
        builder.append(", logMsg=");
        builder.append(logMsg);
        builder.append(", status=");
        builder.append(status);
        builder.append(", customOuterMimeHeaders=");
        builder.append(customOuterMimeHeaders);
        builder.append("]");
        return builder.toString();
    }


    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        // read in partnership
        partnership = (Partnership) in.readObject();

        // read in attributes
        attributes = (Map<String, String>) in.readObject();
		
		// read in data history
		history = (DataHistory) in.readObject();
		
        try {
            // read in message headers
            headers = new InternetHeaders(in);

            // read in mime body 
            if (in.read() == 1) {
                data = new MimeBodyPart(in);
            }
        } catch (MessagingException me) {
            throw new IOException("Messaging exception: " + me.getMessage());
        }

        // read in MDN
        MDN = (MessageMDN) in.readObject();

        if (MDN != null) {
            MDN.setMessage(this);
        }
    }

    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException {
        // write partnership info
        out.writeObject(partnership);

        // write attributes
        out.writeObject(attributes);
		
		// write data history
		out.writeObject(history);
		
        // write message headers
        Enumeration<String> en = headers.getAllHeaderLines();

        while (en.hasMoreElements()) {
            out.writeBytes(en.nextElement().toString() + "\r\n");
        }

        out.writeBytes(new String("\r\n"));

        // write the mime body
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            if (data != null) {
                baos.write(1);
                data.writeTo(baos);
            } else {
                baos.write(0);
            }
        } catch (MessagingException e) {
            throw new IOException("Messaging exception: " + e.getMessage());
        }

        out.write(baos.toByteArray());
        baos.close();

        // write the message's MDN
        out.writeObject(MDN);
    }
    
    public String getLogMsgID() {
    	return " [" + getMessageID() + "]";
    }
    
	public void setLogMsg(String msg) {
		logMsg = msg;
	}

    public String getLogMsg() {
        return logMsg;
    }

    public String getCalculatedMIC()
	{
		return calculatedMIC;
	}

	public void setCalculatedMIC(String calculatedMIC)
	{
		this.calculatedMIC = calculatedMIC;
	}

}
