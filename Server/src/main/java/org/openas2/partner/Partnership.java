package org.openas2.partner;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;


public class Partnership implements Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = -8365608387462470629L;
	public static final String PTYPE_SENDER = "sender"; // Sender partner type
    public static final String PTYPE_RECEIVER = "receiver"; // Receiver partner type
    public static final String PID_EMAIL = "email"; // Email address
    public static final String PA_PROTOCOL = "protocol"; // AS1 or AS2
    public static final String PA_SUBJECT = "subject"; // Subject sent in messages    
    public static final String PA_CONTENT_TRANSFER_ENCODING = "content_transfer_encoding"; // optional content transer enc value
    public static final String PA_REMOVE_PROTECTION_ATTRIB = "remove_cms_algorithm_protection_attrib"; // Some AS2 systems do not support the attribute
   
 
    private Map<String,String> attributes;
    private Map<String,Object> receiverIDs;
    private Map<String,Object> senderIDs;
    private String name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setAttribute(String id, String value) {
        getAttributes().put(id, value);
    }

    public String getAttribute(String id) {
        return (String) getAttributes().get(id);
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

    public void setReceiverID(String id, String value) {
        getReceiverIDs().put(id, value);
    }

    public String getReceiverID(String id) {
        return (String) getReceiverIDs().get(id);
    }

    public void setReceiverIDs(Map<String, Object> receiverIDs) {
        this.receiverIDs = receiverIDs;
    }

    public Map<String, Object> getReceiverIDs() {
        if (receiverIDs == null) {
            receiverIDs = new HashMap<String, Object>();
        }

        return receiverIDs;
    }

    public void setSenderID(String id, String value) {
        getSenderIDs().put(id, value);
    }

    public String getSenderID(String id) {
        return (String) getSenderIDs().get(id);
    }

    public void setSenderIDs(Map<String,Object> senderIDs) {
        this.senderIDs = senderIDs;
    }

    public Map<String,Object> getSenderIDs() {
        if (senderIDs == null) {
            senderIDs = new HashMap<String,Object>();
        }

        return senderIDs;
    }

    public boolean matches(Partnership partnership) {
        Map<String,Object> senderIDs = partnership.getSenderIDs();
        Map<String,Object> receiverIDs = partnership.getReceiverIDs();

        if (compareIDs(senderIDs, getSenderIDs())) {
            return true;
        } else if (compareIDs(receiverIDs, getReceiverIDs())) {
            return true;
        }

        return false;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Partnership " + getName());
        buf.append(" Sender IDs = ").append(getSenderIDs());
        buf.append(" Receiver IDs = ").append(getReceiverIDs());
        buf.append(" Attributes = ").append(getAttributes());

        return buf.toString();
    }

    protected boolean compareIDs(Map<String,Object> ids, Map<String,Object> compareTo) {
        Set<Entry<String, Object>> idSet = ids.entrySet();
        Iterator<Entry<String, Object>> it = idSet.iterator();

        if (!it.hasNext()) {
            return false;
        }

        Map.Entry<String,Object> currentId;
        Object currentValue;
        Object compareValue;

        while (it.hasNext()) {
            currentId = (Entry<String, Object>) it.next();
            currentValue = currentId.getValue();
            compareValue = compareTo.get(currentId.getKey());

            if ((currentValue != null) && (compareValue == null)) {
                return false;
            } else if ((currentValue == null) && (compareValue != null)) {
                return false;
            } else if (!currentValue.equals(compareValue)) {
                return false;
            }
        }

        return true;
    }

    public void copy(Partnership partnership) {
        if (partnership.getName() != null) {
            setName(partnership.getName());
        }        
        getSenderIDs().putAll(partnership.getSenderIDs());
        getReceiverIDs().putAll(partnership.getReceiverIDs());
        getAttributes().putAll(partnership.getAttributes());
    }
    
    public boolean isAsyncMDN()
    {
    	String receiptOptions = getAttribute(AS2Partnership.PA_AS2_RECEIPT_OPTION);
    	return (receiptOptions != null && receiptOptions.length() > 0);
    }

    public boolean isSetTransferEncodingOnInitialBodyPart()
    {
    	// The default must be true and allow it to be disabled by explicit config
		String setTxfrEncoding = getAttribute("set_transfer_encoding_on_inital_body_part");
        return (setTxfrEncoding == null || "true".equals(setTxfrEncoding));
    }

    public boolean isPreventCanonicalization()
    {
		String preventCanonicalization = getAttribute("prevent_canonicalization_for_mic");
        return (preventCanonicalization != null && "true".equals(preventCanonicalization));
    }

    public boolean isRenameDigestToOldName()
    {
		String removeDash = getAttribute("rename_digest_to_old_name");
        return (removeDash != null && "true".equals(removeDash));
    }
    
    public boolean isRemoveCmsAlgorithmProtectionAttr()
    {
    	return "true".equalsIgnoreCase(getAttribute(Partnership.PA_REMOVE_PROTECTION_ATTRIB));
    }
}
