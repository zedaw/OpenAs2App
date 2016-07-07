package org.openas2.cert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openas2.BaseComponent;
import org.openas2.OpenAS2Exception;
import org.openas2.Session;
import org.openas2.WrappedException;
import org.openas2.message.Message;
import org.openas2.message.MessageMDN;
import org.openas2.params.InvalidParameterException;
import org.openas2.partner.Partnership;
import org.openas2.partner.SecurePartnership;
import org.openas2.util.AS2Util;
import org.openas2.util.FileMonitor;
import org.openas2.util.FileMonitorListener;

public abstract class BaseCertificateFactory extends BaseComponent implements AliasedCertificateFactory, KeyStoreCertificateFactory, StorableCertificateFactory, FileMonitorListener {
    
    public static final String PARAM_FILENAME = "filename";

    public static final String PARAM_PASSWORD = "password";

    public static final String PARAM_INTERVAL = "interval";

    private FileMonitor fileMonitor;

    private KeyStore keyStore;

    private Log logger = LogFactory.getLog(BaseCertificateFactory.class.getSimpleName());

    /**
     * 
     * @param partnership
     * @param partnershipType
     * @return
     * @throws OpenAS2Exception
     */
    public String getAlias(Partnership partnership, String partnershipType) throws OpenAS2Exception {
        String alias = null;

        if (partnershipType == Partnership.PTYPE_RECEIVER) {
            alias = partnership.getReceiverID(SecurePartnership.PID_X509_ALIAS);
        } else if (partnershipType == Partnership.PTYPE_SENDER) {
            alias = partnership.getSenderID(SecurePartnership.PID_X509_ALIAS);
        }

        if (alias == null) {
            throw new CertificateNotFoundException(partnershipType, null);
        }

        return alias;
    }

    /**
     * 
     * @param alias
     * @return
     * @throws OpenAS2Exception
     * @see org.openas2.cert.AliasedCertificateFactory#getCertificate(java.lang.String)
     */
    public X509Certificate getCertificate(String alias) throws OpenAS2Exception {
        try {
            KeyStore ks = getKeyStore();
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

            if (cert == null) {
                throw new CertificateNotFoundException(null, alias);
            }

            return cert;
        } catch (KeyStoreException kse) {
            throw new WrappedException(kse);
        }
    }

    /**
     * 
     * @param msg
     * @param partnershipType
     * @return
     * @throws OpenAS2Exception
     * @see org.openas2.cert.CertificateFactory#getCertificate(org.openas2.message.Message, java.lang.String)
     */
    public X509Certificate getCertificate(Message msg, String partnershipType) throws OpenAS2Exception {
        try {
            return getCertificate(getAlias(msg.getPartnership(), partnershipType));
        } catch (CertificateNotFoundException cnfe) {
            cnfe.setPartnershipType(partnershipType);
            throw cnfe;
        }
    }

    /**
     * 
     * @param mdn
     * @param partnershipType
     * @return
     * @throws OpenAS2Exception
     * @see org.openas2.cert.CertificateFactory#getCertificate(org.openas2.message.MessageMDN, java.lang.String)
     */
    public X509Certificate getCertificate(MessageMDN mdn, String partnershipType) throws OpenAS2Exception {
        try {
            return getCertificate(getAlias(mdn.getPartnership(), partnershipType));
        } catch (CertificateNotFoundException cnfe) {
            cnfe.setPartnershipType(partnershipType);
            throw cnfe;
        }
    }

    /**
     * 
     * @return
     * @throws OpenAS2Exception
     * @see org.openas2.cert.AliasedCertificateFactory#getCertificates()
     */
    public Map<String, X509Certificate> getCertificates() throws OpenAS2Exception {
        KeyStore ks = getKeyStore();

        try {
            Map<String, X509Certificate> certs = new HashMap<String, X509Certificate>();
            String certAlias;

            Enumeration<String> e = ks.aliases();

            while (e.hasMoreElements()) {
                certAlias = (String) e.nextElement();
                certs.put(certAlias, (X509Certificate) ks.getCertificate(certAlias));
            }

            return certs;
        } catch (GeneralSecurityException gse) {
            throw new WrappedException(gse);
        }
    }

    /**
     * 
     * @param fileMonitor
     */
    public void setFileMonitor(FileMonitor fileMonitor) {
        this.fileMonitor = fileMonitor;
    }

    /**
     * 
     * @return
     * @throws InvalidParameterException
     */
    public FileMonitor getFileMonitor() throws InvalidParameterException {
        boolean createMonitor = ((fileMonitor == null) && (getParameter(PARAM_INTERVAL, false) != null));

        if (!createMonitor && fileMonitor != null) {
            String filename = fileMonitor.getFilename();
            createMonitor = ((filename != null) && !filename.equals(getFilename()));
        }

        if (createMonitor) {
            if (fileMonitor != null) {
                fileMonitor.stop();
            }

            int interval = getParameterInt(PARAM_INTERVAL, true);
            File file = new File(getFilename());
            fileMonitor = new FileMonitor(file, interval);
            fileMonitor.addListener(this);
        }

        return fileMonitor;
    }

    /**
     * 
     * @param filename
     * @see org.openas2.cert.StorableCertificateFactory#setFilename(java.lang.String)
     */
    public void setFilename(String filename) {
        getParameters().put(PARAM_FILENAME, filename);
    }

    /**
     * 
     * @return
     * @throws InvalidParameterException
     * @see org.openas2.cert.StorableCertificateFactory#getFilename()
     */
    public String getFilename() throws InvalidParameterException {
        return getParameter(PARAM_FILENAME, true);
    }

    /**
     * 
     * @param keyStore
     * @see org.openas2.cert.KeyStoreCertificateFactory#setKeyStore(java.security.KeyStore)
     */
    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    /**
     * 
     * @return
     * @see org.openas2.cert.KeyStoreCertificateFactory#getKeyStore()
     */
    public KeyStore getKeyStore() {
        return keyStore;
    }

    /**
     * 
     * @param password
     * @see org.openas2.cert.StorableCertificateFactory#setPassword(char[])
     */
    public void setPassword(char[] password) {
        getParameters().put(PARAM_PASSWORD, new String(password));
    }

    /**
     * 
     * @return
     * @throws InvalidParameterException
     * @see org.openas2.cert.StorableCertificateFactory#getPassword()
     */
    public char[] getPassword() throws InvalidParameterException {
        return getParameter(PARAM_PASSWORD, true).toCharArray();
    }

    /**
     * 
     * @param cert
     * @return
     * @throws OpenAS2Exception
     */
    public PrivateKey getPrivateKey(X509Certificate cert) throws OpenAS2Exception {
        KeyStore ks = getKeyStore();
        String alias = null;

        try {
            alias = ks.getCertificateAlias(cert);

            if (alias == null) {
                throw new KeyNotFoundException(cert, null);
            }

            PrivateKey key = (PrivateKey) ks.getKey(alias, getPassword());

            if (key == null) {
                throw new KeyNotFoundException(cert, null);
            }

            return key;
        } catch (GeneralSecurityException e) {
            throw new KeyNotFoundException(cert, alias, e);
        }
    }

    /**
     * 
     * @param msg
     * @param cert
     * @return
     * @throws OpenAS2Exception
     * @see org.openas2.cert.CertificateFactory#getPrivateKey(org.openas2.message.Message, java.security.cert.X509Certificate)
     */
    public PrivateKey getPrivateKey(Message msg, X509Certificate cert) throws OpenAS2Exception {
        return getPrivateKey(cert);
    }

    /**
     * 
     * @param mdn
     * @param cert
     * @return
     * @throws OpenAS2Exception
     * @see org.openas2.cert.CertificateFactory#getPrivateKey(org.openas2.message.MessageMDN, java.security.cert.X509Certificate)
     */
    public PrivateKey getPrivateKey(MessageMDN mdn, X509Certificate cert) throws OpenAS2Exception {
        return getPrivateKey(cert);
    }

    /**
     * 
     * @param alias
     * @param cert
     * @param overwrite
     * @throws OpenAS2Exception
     * @see org.openas2.cert.AliasedCertificateFactory#addCertificate(java.lang.String, java.security.cert.X509Certificate, boolean)
     */
    public void addCertificate(String alias, X509Certificate cert, boolean overwrite) throws OpenAS2Exception {
        KeyStore ks = getKeyStore();

        try {
            if (ks.containsAlias(alias) && !overwrite) {
                throw new CertificateExistsException(alias);
            }

            ks.setCertificateEntry(alias, cert);
            save(getFilename(), getPassword());
        } catch (GeneralSecurityException gse) {
            throw new WrappedException(gse);
        }
    }

    /**
     * 
     * @param alias
     * @param key
     * @param password
     * @throws OpenAS2Exception
     * @see org.openas2.cert.AliasedCertificateFactory#addPrivateKey(java.lang.String, java.security.Key, java.lang.String)
     */
    public void addPrivateKey(String alias, Key key, String password) throws OpenAS2Exception {
        KeyStore ks = getKeyStore();

        try {
            if (!ks.containsAlias(alias)) {
                throw new CertificateNotFoundException(null, alias);
            }

            Certificate[] certChain = ks.getCertificateChain(alias);
            ks.setKeyEntry(alias, key, password.toCharArray(), certChain);

            save(getFilename(), getPassword());
        } catch (GeneralSecurityException gse) {
            throw new WrappedException(gse);
        }
    }

    /**
     * 
     * @throws OpenAS2Exception
     * @see org.openas2.cert.AliasedCertificateFactory#clearCertificates()
     */
    public void clearCertificates() throws OpenAS2Exception {
        KeyStore ks = getKeyStore();

        try {
            Enumeration<String> aliases = ks.aliases();

            while (aliases.hasMoreElements()) {
                ks.deleteEntry((String) aliases.nextElement());
            }

            save(getFilename(), getPassword());
        } catch (GeneralSecurityException gse) {
            throw new WrappedException(gse);
        }
    }

    /**
     * 
     * @param monitor
     * @param file
     * @param eventID
     * @see org.openas2.util.FileMonitorListener#handle(org.openas2.util.FileMonitor, java.io.File, int)
     */
    public void handle(FileMonitor monitor, File file, int eventID) {
        switch (eventID) {
            case FileMonitorListener.EVENT_MODIFIED:

                try {
                    load();
                    logger.info("- Certificates Reloaded -");
                } catch (OpenAS2Exception oae) {
                    oae.terminate();
                }

                break;
        }
    }

    /**
     * 
     * @param session
     * @param options
     * @throws OpenAS2Exception
     * @see org.openas2.BaseComponent#init(org.openas2.Session, java.util.Map)
     */
    public void init(Session session, Map<String, String> options) throws OpenAS2Exception {
        super.init(session, options);

        // Override the password if it was passed as a system property
        String pwd = System.getProperty("org.openas2.cert.Password");
        if (pwd != null) {
            setPassword(pwd.toCharArray());
        }

        try {
            this.keyStore = AS2Util.getCryptoHelper().getKeyStore(this.getKeyStoreType());
        } catch (Exception e) {
            throw new WrappedException(e);
        }


        load(getFilename(), getPassword());
    }

    /**
     * 
     * @param filename
     * @param password
     * @throws OpenAS2Exception
     * @see org.openas2.cert.StorableCertificateFactory#load(java.lang.String, char[])
     */
    public void load(String filename, char[] password) throws OpenAS2Exception {
        logger.info("Filename loaded : " + filename);
        System.out.println("Filename loaded : " + filename);
        try {
            FileInputStream fIn = new FileInputStream(filename);

            load(fIn, password);

            fIn.close();
        } catch (IOException ioe) {
            throw new WrappedException(ioe);
        }
    }

    /**
     * 
     * @param in
     * @param password
     * @throws OpenAS2Exception
     * @see org.openas2.cert.StorableCertificateFactory#load(java.io.InputStream, char[])
     */
    public void load(InputStream in, char[] password) throws OpenAS2Exception {
        try {
            KeyStore ks = getKeyStore();

            synchronized (ks) {
                ks.load(in, password);
            }

            getFileMonitor();
        } catch (IOException ioe) {
            throw new WrappedException(ioe);
        } catch (GeneralSecurityException gse) {
            throw new WrappedException(gse);
        }
    }

    /**
     * 
     * @throws OpenAS2Exception
     * @see org.openas2.cert.StorableCertificateFactory#load()
     */
    public void load() throws OpenAS2Exception {
        load(getFilename(), getPassword());
    }

    /**
     * 
     * @param cert
     * @throws OpenAS2Exception
     * @see org.openas2.cert.AliasedCertificateFactory#removeCertificate(java.security.cert.X509Certificate)
     */
    public void removeCertificate(X509Certificate cert) throws OpenAS2Exception {
        KeyStore ks = getKeyStore();

        try {
            String alias = ks.getCertificateAlias(cert);

            if (alias == null) {
                throw new CertificateNotFoundException(cert);
            }

            removeCertificate(alias);
        } catch (GeneralSecurityException gse) {
            throw new WrappedException(gse);
        }
    }

    /**
     * 
     * @param alias
     * @throws OpenAS2Exception
     * @see org.openas2.cert.AliasedCertificateFactory#removeCertificate(java.lang.String)
     */
    public void removeCertificate(String alias) throws OpenAS2Exception {
        KeyStore ks = getKeyStore();

        try {
            if (ks.getCertificate(alias) == null) {
                throw new CertificateNotFoundException(null, alias);
            }

            ks.deleteEntry(alias);
            save(getFilename(), getPassword());
        } catch (GeneralSecurityException gse) {
            throw new WrappedException(gse);
        }
    }

    /**
     * 
     * @throws OpenAS2Exception
     * @see org.openas2.cert.StorableCertificateFactory#save()
     */
    public void save() throws OpenAS2Exception {
        save(getFilename(), getPassword());
    }

    /**
     * 
     * @param filename
     * @param password
     * @throws OpenAS2Exception
     * @see org.openas2.cert.StorableCertificateFactory#save(java.lang.String, char[])
     */
    public void save(String filename, char[] password) throws OpenAS2Exception {
        try {
            FileOutputStream fOut = new FileOutputStream(filename, false);

            save(fOut, password);

            fOut.close();
        } catch (IOException ioe) {
            throw new WrappedException(ioe);
        }
    }

    /**
     * 
     * @param out
     * @param password
     * @throws OpenAS2Exception
     * @see org.openas2.cert.StorableCertificateFactory#save(java.io.OutputStream, char[])
     */
    public void save(OutputStream out, char[] password) throws OpenAS2Exception {
        try {
            getKeyStore().store(out, password);
        } catch (IOException ioe) {
            throw new WrappedException(ioe);
        } catch (GeneralSecurityException gse) {
            throw new WrappedException(gse);
        }
    }
    
}