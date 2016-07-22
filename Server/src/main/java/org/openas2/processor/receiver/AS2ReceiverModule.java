package org.openas2.processor.receiver;

import org.openas2.message.NetAttribute;
import org.openas2.params.MessageParameters;
import org.openas2.partner.AS2Partnership;


public class AS2ReceiverModule extends NetModule {

    private static final String DOLLAR = "$";

    private static final String DOT = ".";

    // Macros for responses
    public static final String MSG_SENDER = new StringBuilder(DOLLAR).append(MessageParameters.KEY_SENDER).append(DOT).append(AS2Partnership.PID_AS2).append(DOLLAR).toString();

    public static final String MSG_RECEIVER = new StringBuilder(DOLLAR).append(MessageParameters.KEY_RECEIVER).append(DOT).append(AS2Partnership.PID_AS2).append(DOLLAR).toString();

    public static final String MSG_DATE = new StringBuilder(DOLLAR).append(MessageParameters.KEY_HEADERS).append(DOT).append("date").append(DOLLAR).toString();

    public static final String MSG_SUBJECT = new StringBuilder(DOLLAR).append(MessageParameters.KEY_HEADERS).append(DOT).append("subject").append(DOLLAR).toString();

    public static final String MSG_SOURCE_ADDRESS = new StringBuilder(DOLLAR).append(MessageParameters.KEY_ATTRIBUTES).append(DOT).append(NetAttribute.MA_SOURCE_IP).append(DOLLAR).toString();

    public static final String DP_HEADER = new StringBuilder("The message sent to Recipient ").append(MSG_RECEIVER).append(" on ").append(MSG_DATE).append(" with Subject ").append(MSG_SUBJECT).append(" has been received, ").toString();

    public static final String DP_DECRYPTED = new StringBuilder(DP_HEADER).append("the EDI Interchange was successfully decrypted and it's integrity was verified. ").toString();

    public static final String DP_VERIFIED = new StringBuilder(DP_DECRYPTED).append("In addition, the sender of the message, Sender ").append(MSG_SENDER).append(" at Location ").append(MSG_SOURCE_ADDRESS).append(" was authenticated as the originator of the message. ").toString();

    // Response texts
    public static final String DISP_PARTNERSHIP_NOT_FOUND = new StringBuilder(DP_HEADER).append("but the Sender ").append(MSG_SENDER).append(" and/or Recipient ").append(MSG_RECEIVER).append(" are unknown.").toString();

    public static final String DISP_PARSING_MIME_FAILED = new StringBuilder(DP_HEADER).append("but an error occured while parsing the MIME content.").toString();

    public static final String DISP_DECRYPTION_ERROR = new StringBuilder(DP_HEADER).append("but an error occured decrypting the content.").toString();

    public static final String DISP_DECOMPRESSION_ERROR = new StringBuilder(DP_HEADER).append("but an error occured decompressing the content.").toString();

    public static final String DISP_VERIFY_SIGNATURE_FAILED = new StringBuilder(DP_DECRYPTED).append("Authentication of the originator of the message failed.").toString();

    public static final String DISP_CALC_MIC_FAILED = new StringBuilder(DP_DECRYPTED).append("Calculation of the MIC for the message failed.").toString();

    public static final String DISP_STORAGE_FAILED = new StringBuilder(DP_VERIFIED).append(" An error occured while storing the data to the file system.").toString();

    public static final String DISP_SUCCESS = new StringBuilder(DP_VERIFIED).append("There is no guarantee however that the EDI Interchange was syntactically correct, or was received by the EDI application/translator.").toString();

    protected NetModuleHandler getHandler() {
        return new AS2ReceiverHandler(this);
    }


}
