/*
 * Copyright (c) Smals
 */
package org.openas2.specs;


/**
 * Although all MIME content types SHOULD be supported, the following
 * MIME content types MUST be supported:
 * <p/>
 * <ul>
 * <li>Content-type: <a href="http://tools.ietf.org/html/rfc1847#section-2.1">Multipart/Signed</a></li>
 * <li>Content-type: <a href="https://tools.ietf.org/html/rfc3462#section-1">Multipart/Report</a></li>
 * <li>Content-type: <a href="https://tools.ietf.org/html/rfc3798#section-3.1">Message/Disposition-Notification</a></li>
 * <li>Content-type: <a href="https://tools.ietf.org/html/rfc2633#section-3.4.3.1">Application/PKCS7-Signature</a></li>
 * <li>Content-type: <a href="https://tools.ietf.org/html/rfc2633#section-3.2">Application/PKCS7-Mime</a></li>
 * <li>Content-Type: <a href="https://tools.ietf.org/html/rfc1767#page-3">Application/EDI-X12</a></li>
 * <li>Content-type: <a href="https://tools.ietf.org/html/rfc1767#page-2">Application/EDIFACT</a></li>
 * <li>Content-type: <a href="https://tools.ietf.org/html/rfc1767#page-4">Application/edi-consent</a></li>
 * <li>Content-type: <a href="https://tools.ietf.org/html/rfc7303">Application/XML</a></li>
 * </ul>
 * 
 * @author std
 * @see <a href="https://tools.ietf.org/html/rfc4130#section-4.2">AS2 RFC</a> 
 */
public enum MustBeSupportedMIMEContentType {

    /**  Content-type: <a href="http://tools.ietf.org/html/rfc1847#section-2.1">Multipart/Signed</a> */
    SIGNED("multipart/signed"),
    
    /**  Content-type: <a href="https://tools.ietf.org/html/rfc3462#section-1">Multipart/Report</a> */
    REPORT("multipart/report"),
    
    /**  Content-type: <a href="https://tools.ietf.org/html/rfc3798#section-3.1">Message/Disposition-Notification</a> */
    MDN("message/disposition-notification"),
    
    /**  Content-type: <a href="https://tools.ietf.org/html/rfc2633#section-3.4.3.1">Application/PKCS7-Signature</a> */
    P7S("application/PKCS7-signature"),
    
    /**  Content-type: <a href="https://tools.ietf.org/html/rfc2633#section-3.2">Application/PKCS7-Mime</a> */
    P7M("application/PKCS7-mime"),
    
    /** Content-Type: <a href="https://tools.ietf.org/html/rfc1767#page-3">Application/EDI-X12</a>*/
    EDI_X12("application/EDI-X12"),
    
    /**  Content-type: <a href="https://tools.ietf.org/html/rfc1767#page-2">Application/EDIFACT</a> */
    EDIFACT("application/EDIFACT"),
    
    /**  Content-type: <a href="https://tools.ietf.org/html/rfc1767#page-4">Application/edi-consent</a> */
    EDI_CONSENT("application/edi-consent"),
    
    /**  Content-type: <a href="https://tools.ietf.org/html/rfc7303">Application/XML</a> */
    XML("application/XML")
    ;
    
    /**
     * @param contentType
     */
    private MustBeSupportedMIMEContentType(String contentType) {
        this.contentType = contentType;
    }

    private String contentType;

    /**
     * @return the contentType
     */
    public String getContentType() {
        return contentType;
    }
    
}
