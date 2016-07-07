package org.openas2.cert;

import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.Map;

import org.openas2.OpenAS2Exception;

/**
 * 
 *
 */
public interface AliasedCertificateFactory extends CertificateFactory {
    /**
     * 
     * @param alias
     * @return
     * @throws OpenAS2Exception
     */
    public X509Certificate getCertificate(String alias) throws OpenAS2Exception;

    /**
     * 
     * @return
     * @throws OpenAS2Exception
     */
    public Map<String,X509Certificate> getCertificates() throws OpenAS2Exception;

    /**
     * 
     * @param alias
     * @param cert
     * @param overwrite
     * @throws OpenAS2Exception
     */
    public void addCertificate(String alias, X509Certificate cert, boolean overwrite) throws OpenAS2Exception;

    /**
     * 
     * @param alias
     * @param key
     * @param password
     * @throws OpenAS2Exception
     */
    public void addPrivateKey(String alias, Key key, String password) throws OpenAS2Exception;

    /**
     * 
     * @throws OpenAS2Exception
     */
    public void clearCertificates() throws OpenAS2Exception;

    /**
     * 
     * @param cert
     * @throws OpenAS2Exception
     */
    public void removeCertificate(X509Certificate cert) throws OpenAS2Exception;

    /**
     * 
     * @param alias
     * @throws OpenAS2Exception
     */
    public void removeCertificate(String alias) throws OpenAS2Exception;
}
