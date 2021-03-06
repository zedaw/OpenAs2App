package org.openas2.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;

import org.openas2.ComponentNotFoundException;
import org.openas2.DispositionException;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.cert.CertificateFactory;
import org.openas2.cert.CertificateNotFoundException;
import org.openas2.cert.KeyNotFoundException;
import org.openas2.lib.helper.BCCryptoHelper;
import org.openas2.lib.helper.ICryptoHelper;
import org.openas2.lib.util.MimeUtil;
import org.openas2.message.AS2Message;
import org.openas2.message.AS2MessageMDN;
import org.openas2.message.FileAttribute;
import org.openas2.message.Message;
import org.openas2.message.MessageMDN;
import org.openas2.message.NetAttribute;
import org.openas2.params.MessageParameters;
import org.openas2.params.ParameterParser;
import org.openas2.partner.AS2Partnership;
import org.openas2.partner.ASXPartnership;
import org.openas2.partner.Partnership;
import org.openas2.processor.Processor;
import org.openas2.processor.resender.ResenderModule;
import org.openas2.processor.sender.SenderModule;
import org.openas2.processor.storage.StorageModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AS2Util {

    private static final Logger logger = LoggerFactory.getLogger(AS2Util.class);

    private static ICryptoHelper ch;

    public static ICryptoHelper getCryptoHelper() throws Exception {
        if (ch == null) {
            ch = new BCCryptoHelper();
            ch.initialize();
        }

        return ch;
    }

    public static MessageMDN createMDN(Session session, AS2Message msg, String mic, DispositionType disposition, String text) throws Exception {
        AS2MessageMDN mdn = new AS2MessageMDN(msg);
        mdn.setHeader("AS2-Version", "1.1");
        // RFC2822 format: Wed, 04 Mar 2009 10:59:17 +0100
        mdn.setHeader("Date", DateUtil.formatDate("EEE, dd MMM yyyy HH:mm:ss Z"));
        mdn.setHeader("Server", Session.TITLE);
        mdn.setHeader("Mime-Version", "1.0");
        mdn.setHeader("AS2-To", msg.getPartnership().getSenderID(AS2Partnership.PID_AS2));
        mdn.setHeader("AS2-From", msg.getPartnership().getReceiverID(AS2Partnership.PID_AS2));

        // get the MDN partnership info
        mdn.getPartnership().setSenderID(AS2Partnership.PID_AS2, mdn.getHeader("AS2-From"));
        mdn.getPartnership().setReceiverID(AS2Partnership.PID_AS2, mdn.getHeader("AS2-To"));
        session.getPartnershipFactory().updatePartnership(mdn, true);

        mdn.setHeader("From", msg.getPartnership().getReceiverID(Partnership.PID_EMAIL));
        String subject = mdn.getPartnership().getAttribute(ASXPartnership.PA_MDN_SUBJECT);

        if (subject != null) {
            mdn.setHeader("Subject", ParameterParser.parse(subject, new MessageParameters(msg)));
        } else {
            mdn.setHeader("Subject", "Your Requested MDN Response");
        }
        mdn.setText(ParameterParser.parse(text, new MessageParameters(msg)));
        mdn.setAttribute(AS2MessageMDN.MDNA_REPORTING_UA, Session.TITLE + "@" + msg.getAttribute(NetAttribute.MA_DESTINATION_IP) + ":" + msg.getAttribute(NetAttribute.MA_DESTINATION_PORT));
        mdn.setAttribute(AS2MessageMDN.MDNA_ORIG_RECIPIENT, "rfc822; " + msg.getHeader("AS2-To"));
        mdn.setAttribute(AS2MessageMDN.MDNA_FINAL_RECIPIENT, "rfc822; " + msg.getPartnership().getReceiverID(AS2Partnership.PID_AS2));
        mdn.setAttribute(AS2MessageMDN.MDNA_ORIG_MESSAGEID, msg.getHeader("Message-ID"));
        mdn.setAttribute(AS2MessageMDN.MDNA_DISPOSITION, disposition.toString());

        DispositionOptions dispOptions = new DispositionOptions(msg.getHeader("Disposition-Notification-Options"));

        mdn.setAttribute(AS2MessageMDN.MDNA_MIC, mic);
        createMDNData(session, mdn, dispOptions.getMicalg(), dispOptions.getProtocol());

        mdn.updateMessageID();

        // store MDN into msg in case AsynchMDN is sent fails, needs to be resent by send module
        msg.setMDN(mdn);

        return mdn;
    }

    public static void createMDNData(Session session, MessageMDN mdn, String micAlg, String signatureProtocol) throws Exception {
        // Create the report and sub-body parts
        MimeMultipart reportParts = new MimeMultipart();

        // Create the text part
        MimeBodyPart textPart = new MimeBodyPart();
        String text = mdn.getText() + "\r\n";
        textPart.setContent(text, "text/plain");
        textPart.setHeader("Content-Type", "text/plain");
        reportParts.addBodyPart(textPart);

        // Create the report part
        MimeBodyPart reportPart = new MimeBodyPart();
        InternetHeaders reportValues = new InternetHeaders();
        reportValues.setHeader("Reporting-UA", mdn.getAttribute(AS2MessageMDN.MDNA_REPORTING_UA));
        reportValues.setHeader("Original-Recipient", mdn.getAttribute(AS2MessageMDN.MDNA_ORIG_RECIPIENT));
        reportValues.setHeader("Final-Recipient", mdn.getAttribute(AS2MessageMDN.MDNA_FINAL_RECIPIENT));
        reportValues.setHeader("Original-Message-ID", mdn.getAttribute(AS2MessageMDN.MDNA_ORIG_MESSAGEID));
        reportValues.setHeader("Disposition", mdn.getAttribute(AS2MessageMDN.MDNA_DISPOSITION));
        reportValues.setHeader("Received-Content-MIC", mdn.getAttribute(AS2MessageMDN.MDNA_MIC));

        @SuppressWarnings("unchecked")
        Enumeration<String> reportEn = reportValues.getAllHeaderLines();
        StringBuffer reportData = new StringBuffer();

        while (reportEn.hasMoreElements()) {
            reportData.append((String) reportEn.nextElement()).append("\r\n");
        }

        reportData.append("\r\n");

        String reportText = reportData.toString();
        reportPart.setContent(reportText, "message/disposition-notification");
        reportPart.setHeader("Content-Type", "message/disposition-notification");
        reportParts.addBodyPart(reportPart);

        // Convert report parts to MimeBodyPart
        MimeBodyPart report = new MimeBodyPart();
        reportParts.setSubType("report; report-type=disposition-notification");
        report.setContent(reportParts);
        report.setHeader("Content-Type", reportParts.getContentType());

        // Sign the data if needed
        if (signatureProtocol != null) {
            CertificateFactory certFx = session.getCertificateFactory();

            try {
                X509Certificate senderCert = certFx.getCertificate(mdn, Partnership.PTYPE_SENDER);
                PrivateKey senderKey = certFx.getPrivateKey(mdn, senderCert);
                Partnership p = mdn.getMessage().getPartnership();
                String contentTxfrEncoding = p.getAttribute(Partnership.PA_CONTENT_TRANSFER_ENCODING);
                boolean isRemoveCmsAlgorithmProtectionAttr = "true".equalsIgnoreCase(p.getAttribute(Partnership.PA_REMOVE_PROTECTION_ATTRIB));
                if (contentTxfrEncoding == null)
                    contentTxfrEncoding = Session.DEFAULT_CONTENT_TRANSFER_ENCODING;
                // sign the data using CryptoHelper
                MimeBodyPart signedReport = getCryptoHelper().sign(report, senderCert, senderKey, micAlg, contentTxfrEncoding, false, isRemoveCmsAlgorithmProtectionAttr);
                mdn.setData(signedReport);
            } catch (CertificateNotFoundException cnfe) {
                cnfe.terminate();
                mdn.setData(report);
            } catch (KeyNotFoundException knfe) {
                knfe.terminate();
                mdn.setData(report);
            }
        } else {
            mdn.setData(report);
        }

        // Update the MDN headers with content information
        MimeBodyPart data = mdn.getData();
        mdn.setHeader("Content-Type", data.getContentType());

        // int size = getSize(data);
        // mdn.setHeader("Content-Length", Integer.toString(size));
    }

    public static void parseMDN(Message msg, X509Certificate receiver) throws OpenAS2Exception {
        MessageMDN mdn = msg.getMDN();
        MimeBodyPart mainPart = mdn.getData();
        try {
            ICryptoHelper ch = getCryptoHelper();

            if (ch.isSigned(mainPart)) {
                mainPart = getCryptoHelper().verifySignature(mainPart, receiver);
            }
        } catch (Exception e1) {
            logger.error("Error parsing MDN: ", e1);
            throw new OpenAS2Exception("Failed to verify signature of received MDN.");

        }

        try {
            MimeMultipart reportParts = new MimeMultipart(mainPart.getDataHandler().getDataSource());

            if (logger.isTraceEnabled() && "true".equalsIgnoreCase(System.getProperty("logRxdMdnMimeBodyParts", "false"))) {
                logger.trace("Received MimeBodyPart for inbound MDN: " + msg.getLogMsgID() + "\n" + MimeUtil.toString(mainPart, true));
            }

            if (reportParts != null) {
                ContentType reportType = new ContentType(reportParts.getContentType());

                if (reportType.getBaseType().equalsIgnoreCase("multipart/report")) {
                    int reportCount = reportParts.getCount();
                    MimeBodyPart reportPart;

                    for (int j = 0; j < reportCount; j++) {
                        reportPart = (MimeBodyPart) reportParts.getBodyPart(j);
                        if (logger.isTraceEnabled() && "true".equalsIgnoreCase(System.getProperty("logRxdMdnMimeBodyParts", "false"))) {
                            logger.trace("Report MimeBodyPart from Multipart for inbound MDN: " + msg.getLogMsgID() + "\n" + MimeUtil.toString(reportPart, true));
                        }

                        if (reportPart.isMimeType("text/plain")) {
                            mdn.setText(reportPart.getContent().toString());
                        } else if (reportPart.isMimeType("message/disposition-notification")) {
                            InternetHeaders disposition = new InternetHeaders(reportPart.getInputStream());
                            mdn.setAttribute(AS2MessageMDN.MDNA_REPORTING_UA, disposition.getHeader("Reporting-UA", ", "));
                            mdn.setAttribute(AS2MessageMDN.MDNA_ORIG_RECIPIENT, disposition.getHeader("Original-Recipient", ", "));
                            mdn.setAttribute(AS2MessageMDN.MDNA_FINAL_RECIPIENT, disposition.getHeader("Final-Recipient", ", "));
                            mdn.setAttribute(AS2MessageMDN.MDNA_ORIG_MESSAGEID, disposition.getHeader("Original-Message-ID", ", "));
                            mdn.setAttribute(AS2MessageMDN.MDNA_DISPOSITION, disposition.getHeader("Disposition", ", "));
                            mdn.setAttribute(AS2MessageMDN.MDNA_MIC, disposition.getHeader("Received-Content-MIC", ", "));
                        }
                    }
                }
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MessagingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @description Verify disposition sytus is "processed" then check MIC is matched
     * @param msg - the original message sent to the partner that the MDN relates to
     * @return true if mdn processed
     */
    public static boolean checkMDN(AS2Message msg) throws DispositionException, OpenAS2Exception {
        /*
         * The sender may return an error in the disposition and not set the MIC so check disposition first
         */
        String disposition = msg.getMDN().getAttribute(AS2MessageMDN.MDNA_DISPOSITION);
        if (disposition != null && logger.isInfoEnabled())
            logger.info("received MDN [" + disposition + "]" + msg.getLogMsgID());
        boolean dispositionHasWarning = false;
        try {
            new DispositionType(disposition).validate();
        } catch (DispositionException de) {
            // Something wrong detected so flag it for later use
            dispositionHasWarning = true;
            de.setText(msg.getMDN().getText());

            if ((de.getDisposition() != null) && de.getDisposition().isWarning()) {
                // Do not throw error in this case ... just log it
                de.addSource(OpenAS2Exception.SOURCE_MESSAGE, msg);
                de.terminate();
            } else {
                throw de;
            }
        } catch (OpenAS2Exception e) {
            logger.error("Processing error occurred: ", e);
            throw new OpenAS2Exception(e);
        }

        if (logger.isTraceEnabled())
            logger.trace("MIC processing start... ");
        // get the returned mic from mdn object

        String returnMIC = msg.getMDN().getAttribute(AS2MessageMDN.MDNA_MIC);
        if (returnMIC == null || returnMIC.length() < 1) {
            if (dispositionHasWarning) {
                // TODO: Think this should pribably throw error if MIC should have been returned but for now...
                msg.setLogMsg("Returned MIC not found but disposition has warning so might be normal.");
                logger.warn(msg.toString());
            } else {
                msg.setLogMsg("Returned MIC not found so cannot validate returned message.");
                logger.error(msg.toString());
            }
            return false;
        }
        String calcMIC = msg.getCalculatedMIC();
        if (calcMIC == null) {
            throw new OpenAS2Exception("The claculated MIC was not retrieved from the message object.");
        }
        if (logger.isTraceEnabled())
            logger.trace("MIC check on calculated MIC: " + calcMIC + msg.getLogMsgID());

        /*
         * Returned-Content-MIC header and rfc822 headers can contain spaces all over the place. (not to mention comments!). Simple fix -
         * delete all spaces. Since the partner could return the algorithm in different case to what was sent, remove the algorithm before
         * compare The Algorithm is appended as a part of the MIC by adding a comma then optionally a space followed by the algorithm
         */
        String regex = "^\\s*(\\S+)\\s*,\\s*(\\S+)\\s*$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(returnMIC);
        if (!m.find()) {
            msg.setLogMsg("Invalid MIC format in returned MIC: " + returnMIC);
            logger.error(msg.toString());
            throw new OpenAS2Exception("Invalid MIC string received. Forcing Resend");
        }
        String rMic = m.group(1);
        String rMicAlg = m.group(2);
        m = p.matcher(calcMIC);
        if (!m.find()) {
            msg.setLogMsg("Invalid MIC format in calculated MIC: " + calcMIC);
            logger.error(msg.toString());
            throw new OpenAS2Exception("Invalid MIC string retrieved from calculated MIC. Forcing Resend");
        }
        String cMic = m.group(1);
        String cMicAlg = m.group(2);

        if (!cMicAlg.equalsIgnoreCase(rMicAlg)) {
            // Appears not to match.... make sure dash is not the issue as in SHA-1 compared to SHA1
            if (!cMicAlg.replaceAll("-", "").equalsIgnoreCase(rMicAlg.replaceAll("-", ""))) {
                /*
                 * RFC 6362 specifies that the sent attachments should be considered invalid and retransmitted
                 */
                String errmsg = "MIC algorithm returned by partner is not the same as the algorithm requested, original MIC alg: " + cMicAlg + " ::: returned MIC alg: " + rMicAlg + "\n\t\tPartner probably not implemented AS2 spec correctly or does not support the requested algorithm. Check that the \"as2_mdn_options\" attribute for the partner uses the same algorithm as the \"sign\" attribute.";
                throw new OpenAS2Exception(errmsg + " Forcing Resend");
            }
        }
        if (!cMic.equals(rMic)) {
            /*
             * RFC 6362 specifies that the sent attachments should be considered invalid and retransmitted
             */
            msg.setLogMsg("MIC not matched, original MIC: " + calcMIC + " return MIC: " + returnMIC);
            logger.error(msg.toString());
            throw new OpenAS2Exception("MIC not matched. Forcing Resend");
        }
        if (logger.isTraceEnabled())
            logger.trace("MIC is matched, received MIC: " + returnMIC + msg.getLogMsgID());
        return true;
    }

    // How many times should this message be sent?
    public static String retries(Map<Object, Object> options, String fallbackRetries) {
        String left;
        if (options == null || (left = (String) options.get(SenderModule.SOPT_RETRIES)) == null) {
            left = fallbackRetries;
        }

        if (left == null)
            left = SenderModule.DEFAULT_RETRIES;
        // Verify it is a valid integer
        try {
            Integer.parseInt(left);
        } catch (Exception e) {
            return SenderModule.DEFAULT_RETRIES;
        }
        return left;
    }

    /*
     * @description Attempts to check if a resend should go ahead and if o decrements the resend count and stores the decremented retry
     * count in the options map. If the passed in retry count is null or invalid it will fall back to a system default
     */
    public static boolean resend(Processor processor, Object sourceClass, String how, Message msg, OpenAS2Exception cause, String tries, boolean useOriginalMsgObject) throws OpenAS2Exception {
        if (logger.isDebugEnabled())
            logger.debug("RESEND requested.... retries to go: " + tries + "\n        Message file from passed in object: " + msg.getAttribute(FileAttribute.MA_PENDINGFILE) + msg.getLogMsgID());

        int retries = -1;
        if (tries == null)
            tries = SenderModule.DEFAULT_RETRIES;
        try {
            retries = Integer.parseInt(tries);
        } catch (Exception e) {
            logger.error("The retry count is not a valid integer value: " + tries, e);
        }
        if (retries >= 0 && retries-- <= 0) {
            msg.setLogMsg("Message abandoned after retry limit reached.");
            logger.error(msg.toString());
            throw new OpenAS2Exception("Message abandoned after retry limit reached." + msg.getLogMsgID());
        }

        if (useOriginalMsgObject) {
            String pendingMsgObjFileName = msg.getAttribute(FileAttribute.MA_PENDINGFILE) + ".object";

            if (logger.isDebugEnabled())
                logger.debug("Pending msg object file to retrieve data from in MDN receiver: " + pendingMsgObjFileName);
            ObjectInputStream pifois = null;
            Message originalMsg;
            try {
                try {
                    pifois = new ObjectInputStream(new FileInputStream(new File(pendingMsgObjFileName)));
                } catch (FileNotFoundException e) {
                    throw new OpenAS2Exception("Could not retrieve pending info file: ", e);
                } catch (IOException e) {
                    throw new OpenAS2Exception("Could not open pending info file: ", e);
                }
                try {
                    originalMsg = (Message) pifois.readObject();
                } catch (Exception e) {
                    throw new OpenAS2Exception("Cannot retrieve original message object for resend: ", e);
                }
            } finally {
                try {
                    if (pifois != null)
                        pifois.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            // Update original with latest message-id and pendinginfo file so it
            // is kept up to date
            originalMsg.setAttribute(FileAttribute.MA_PENDINGINFO, msg.getAttribute(FileAttribute.MA_PENDINGINFO));
            originalMsg.setMessageID(msg.getMessageID());
            originalMsg.setOption(ResenderModule.OPTION_RETRIES, tries);
            if (logger.isTraceEnabled())
                logger.trace("Message file extracted from passed in object: " + msg.getAttribute(FileAttribute.MA_PENDINGFILE) + "\n        Message file extracted from original object: " + originalMsg.getAttribute(FileAttribute.MA_PENDINGFILE) + msg.getLogMsgID());
            msg = originalMsg;
        }

        // Resend requires a new Message-Id and we need to update the pendinginfo file name to match....
        // The actual file that is pending can remain the same name since it is pointed to by line in pendinginfo file
        String oldMsgId = msg.getMessageID();
        String oldPendingInfoFileName = msg.getAttribute(FileAttribute.MA_PENDINGINFO);
        String newMsgId = ((AS2Message) msg).generateMessageID();
        // Set new Id in Message object so we can generate new file name
        msg.setMessageID(newMsgId);
        String newPendingInfoFileName = buildPendingFileName(msg, processor, "pendingmdninfo");
        if (logger.isDebugEnabled())
            logger.debug("" + "\n        Old Msg Id: " + oldMsgId + "\n        Old Info File: " + oldPendingInfoFileName + "\n        New Info File: " + newPendingInfoFileName + msg.getLogMsgID());
        // Update the pending file to new name
        File oldPendInfFile = new File(oldPendingInfoFileName);
        File newPendInfFile = new File(newPendingInfoFileName);
        if (logger.isTraceEnabled())
            logger.trace("Attempting to rename pending info file : " + oldPendInfFile.getName() + " :::: New name: " + newPendInfFile.getName() + msg.getLogMsgID());
        try {
            newPendInfFile = IOUtilOld.moveFile(oldPendInfFile, newPendInfFile, false, true);
            // Update the name of the file in the message object
            msg.setAttribute(FileAttribute.MA_PENDINGINFO, newPendingInfoFileName);
            if (logger.isInfoEnabled())
                logger.info("Renamed pending info file : " + oldPendInfFile.getName() + " :::: New name: " + newPendInfFile.getName() + msg.getLogMsgID());

        } catch (IOException iose) {
            logger.error("Error renaming file: ", iose);
        }

        Map<Object, Object> options = new HashMap<Object, Object>();
        options.put(ResenderModule.OPTION_CAUSE, cause);
        options.put(ResenderModule.OPTION_INITIAL_SENDER, sourceClass);
        options.put(ResenderModule.OPTION_RESEND_METHOD, how);
        options.put(ResenderModule.OPTION_RETRIES, "" + retries);
        processor.handle(ResenderModule.DO_RESEND, msg, options);
        return true;
    }

    /**
     * method for receiving & processing Async MDN sent from receiver.
     */
    public static void processMDN(AS2Message msg, byte[] data, OutputStream out, boolean isAsyncMDN, Session session, Object sourceClass) throws OpenAS2Exception, IOException {

        // Create a MessageMDN and copy HTTP headers
        MessageMDN mdn = msg.getMDN();
        MimeBodyPart part;
        try {
            part = new MimeBodyPart(mdn.getHeaders(), data);
            msg.getMDN().setData(part);
        } catch (MessagingException e1) {
            logger.error("Failed to create mimebodypart from received MDN data: ", e1);
            if (isAsyncMDN)
                HTTPUtil.sendHTTPResponse(out, HttpURLConnection.HTTP_BAD_REQUEST, false);
            throw new OpenAS2Exception("Error receiving MDN. Processing stopped.");
        }

        // get the MDN partnership info
        mdn.getPartnership().setSenderID(AS2Partnership.PID_AS2, mdn.getHeader("AS2-From"));
        mdn.getPartnership().setReceiverID(AS2Partnership.PID_AS2, mdn.getHeader("AS2-To"));
        session.getPartnershipFactory().updatePartnership(mdn, false);

        CertificateFactory cFx = session.getCertificateFactory();
        X509Certificate senderCert = cFx.getCertificate(mdn, Partnership.PTYPE_SENDER);

        msg.setStatus(Message.MSG_STATUS_MDN_PARSE);
        AS2Util.parseMDN(msg, senderCert);

        if (isAsyncMDN) {
            getMetaData(msg, session);
        }

        String retries = (String) msg.getOption(ResenderModule.OPTION_RETRIES);

        msg.setStatus(Message.MSG_STATUS_MDN_VERIFY);
        try {
            AS2Util.checkMDN(msg);
            /*
             * If the MDN was successfully received send correct HTTP response irrespective of possible error conditions due to disposition
             * errors or MIC mismatch
             */
            if (isAsyncMDN)
                HTTPUtil.sendHTTPResponse(out, HttpURLConnection.HTTP_OK, false);

        } catch (DispositionException de) {
            /*
             * Issue with disposition but still sent OK at HTTP level to indicate message received
             */
            if (isAsyncMDN)
                HTTPUtil.sendHTTPResponse(out, HttpURLConnection.HTTP_OK, false);
            // If a disposition exception occurs then there must have been an
            // error response in the disposition
            // Hmmmm... Error may require manual intervention but keep
            // trying.... possibly change retry count to 1 or just fail????
            AS2Util.resend(session.getProcessor(), sourceClass, SenderModule.DO_SEND, msg, de, retries, true);
            return;
        } catch (OpenAS2Exception oae) {
            // Possibly MIC mismatch so resend
            if (isAsyncMDN)
                HTTPUtil.sendHTTPResponse(out, HttpURLConnection.HTTP_OK, false);
            OpenAS2Exception oae2 = new OpenAS2Exception("Message was sent but an error occured while receiving the MDN: ", oae);
            oae2.initCause(oae);
            oae2.addSource(OpenAS2Exception.SOURCE_MESSAGE, msg);
            oae2.terminate();
            AS2Util.resend(session.getProcessor(), sourceClass, SenderModule.DO_SEND, msg, oae2, retries, true);
            return;
        }

        session.getProcessor().handle(StorageModule.DO_STOREMDN, msg, null);
        msg.setStatus(Message.MSG_STATUS_MSG_CLEANUP);
        // To support extended reporting via logging log info passing Message object
        msg.setLogMsg("Message sent and MDN received successfully.");
        logger.info(msg.toString());

        cleanupFiles(msg, false);

    }

    /*
     * @description This method buiold the name of the pending info file
     * 
     * @param msg - the Message object containing enough information to build the pending info file name
     */
    public static String buildPendingFileName(Message msg, Processor processor, String directoryIdentifier) throws OpenAS2Exception {
        String msgId = msg.getMessageID(); // this includes enclosing angled brackets <>
        return ((String) processor.getParameters().get(directoryIdentifier) + "/" + msgId.substring(1, msgId.length() - 1));
    }

    /*
     * @description This method retrieves the information from the pending information file written by the sender module
     * 
     * @param msg - the Message object containing enough information to build the pending info file name
     */
    public static void getMetaData(AS2Message msg, Session session) throws OpenAS2Exception {
        MessageMDN mdn = msg.getMDN();
        // in order to name & save the mdn with the original AS2-From + AS2-To + Message id.,
        // the 3 msg attributes have to be reset before calling MDNFileModule
        msg.getPartnership().setReceiverID(AS2Partnership.PID_AS2, mdn.getHeader("AS2-From"));
        msg.getPartnership().setSenderID(AS2Partnership.PID_AS2, mdn.getHeader("AS2-To"));
        try {
            session.getPartnershipFactory().updatePartnership(msg, false);
        } catch (ComponentNotFoundException e) {
            throw new OpenAS2Exception("Error updating partnership: ", e);
        }
        // use original message ID to open the pending information file from pendinginfo folder.
        String originalMsgId = msg.getMDN().getAttribute(AS2MessageMDN.MDNA_ORIG_MESSAGEID);
        // TODO: CB: Think we are supposed to verify the MDN received msg Id with what we sent
        msg.setMessageID(originalMsgId);
        String pendinginfofile = buildPendingFileName(msg, session.getProcessor(), "pendingmdninfo");

        if (logger.isDebugEnabled())
            logger.debug("Pending info file to retrieve data from in MDN receiver: " + pendinginfofile);
        ObjectInputStream pifois;
        try {
            pifois = new ObjectInputStream(new FileInputStream(new File(pendinginfofile)));
        } catch (FileNotFoundException e) {
            throw new OpenAS2Exception("Could not retrieve pending info file: ", e);
        } catch (IOException e) {
            throw new OpenAS2Exception("Could not open pending info file: ", e);
        }

        try {
            // Get the original MIC from the first line of pending information file
            msg.setCalculatedMIC((String) pifois.readObject());
            // Get the retry count for number of resends to go from the second line of pending information file
            String retries = (String) pifois.readObject();
            if (logger.isTraceEnabled())
                logger.trace("RETRY COUNT from pending info file: " + retries);
            msg.setOption(ResenderModule.OPTION_RETRIES, retries);
            // Get the original source file name from the 3rd line of pending information file
            msg.setAttribute(FileAttribute.MA_FILENAME, (String) pifois.readObject());
            // Get the original pending file from the 4th line of pending information file
            msg.setAttribute(FileAttribute.MA_PENDINGFILE, (String) pifois.readObject());
            msg.setAttribute(FileAttribute.MA_ERROR_DIR, (String) pifois.readObject());
            msg.setAttribute(FileAttribute.MA_SENT_DIR, (String) pifois.readObject());

            msg.setAttribute(FileAttribute.MA_PENDINGINFO, pendinginfofile);
            if (logger.isTraceEnabled())
                logger.trace("Data retrieved from Pending info file:" + "\n        Original MIC: " + msg.getCalculatedMIC() + "\n        Retry Count: " + retries + "\n        Original file name : " + msg.getAttribute(FileAttribute.MA_FILENAME) + "\n        Pending message file : " + msg.getAttribute(FileAttribute.MA_PENDINGFILE) + "\n        Error directory: " + msg.getAttribute(FileAttribute.MA_ERROR_DIR) + "\n        Sent directory: " + msg.getAttribute(FileAttribute.MA_SENT_DIR) + msg.getLogMsgID());
        } catch (IOException e) {
            throw new OpenAS2Exception("Failed to retrieve the pending MDN information from file: ", e);
        } catch (ClassNotFoundException e) {
            throw new OpenAS2Exception("Failed to rebuild an object from the pending MDN information from file: ", e);
        } finally {
            if (pifois != null)
                try {
                    pifois.close();
                } catch (IOException e) {
                }
        }

    }

    public static void cleanupFiles(Message msg, boolean isError) {
        String pendingInfoFileName = msg.getAttribute(FileAttribute.MA_PENDINGINFO);
        File fPendingInfoFile = new File(pendingInfoFileName);
        if (logger.isTraceEnabled())
            logger.trace("Deleting pendinginfo file : " + fPendingInfoFile.getAbsolutePath() + msg.getLogMsgID());

        try {
            IOUtilOld.deleteFile(fPendingInfoFile);
            if (logger.isTraceEnabled())
                logger.trace("deleted " + pendingInfoFileName + msg.getLogMsgID());
        } catch (Exception e) {
            logger.warn("File was successfully sent but info file not deleted: " + pendingInfoFileName, e);
        }

        String pendingFileName = msg.getAttribute(FileAttribute.MA_PENDINGFILE);
        File fPendingFile = new File(pendingFileName);
        try {
            IOUtilOld.deleteFile(new File(pendingFileName + ".object"));
            if (logger.isTraceEnabled())
                logger.trace("deleted " + pendingFileName + ".object" + msg.getLogMsgID());
        } catch (Exception e) {
            logger.warn("File was successfully sent but message object file not deleted: ", e);
        }
        if (logger.isTraceEnabled())
            logger.trace("Cleaning up pending file : " + fPendingFile.getName() + " from pending folder : " + fPendingFile.getParent() + msg.getLogMsgID());
        try {
            boolean isMoved = false;
            String tgtDir = null;
            if (isError) {
                tgtDir = msg.getAttribute(FileAttribute.MA_ERROR_DIR);
            } else {
                // If the Sent Directory option is set, move the transmitted file to the sent directory
                tgtDir = msg.getAttribute(FileAttribute.MA_SENT_DIR);
            }
            if (tgtDir != null && tgtDir.length() > 0) {
                File tgtFile = null;

                try {
                    tgtFile = new File(tgtDir + "/" + fPendingFile.getName());
                    tgtFile = IOUtilOld.moveFile(fPendingFile, tgtFile, false, true);
                    isMoved = true;

                    if (logger.isInfoEnabled())
                        logger.info("moved " + fPendingFile.getAbsolutePath() + " to " + tgtFile.getAbsolutePath() + msg.getLogMsgID());

                } catch (IOException iose) {
                    logger.error("Error moving file to sent folder: " + iose.getMessage() + msg.getLogMsgID(), iose);
                }
            }

            if (!isMoved) {
                IOUtilOld.deleteFile(fPendingFile);
                if (logger.isInfoEnabled())
                    logger.info("deleted " + fPendingFile.getAbsolutePath() + msg.getLogMsgID());
            }
        } catch (Exception e) {
            logger.error("File was successfully sent but not deleted: " + fPendingFile.getAbsolutePath(), e);
        }
    }

}
