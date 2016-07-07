package org.openas2.cert;

import org.openas2.util.FileMonitorListener;

public class PKCS12CertificateFactory extends BaseCertificateFactory implements AliasedCertificateFactory, KeyStoreCertificateFactory, StorableCertificateFactory, FileMonitorListener {

    /**
     * @return
     * @see org.openas2.cert.KeyStoreCertificateFactory#getKeyStoreType()
     */
    @Override
    public KeyStoreType getKeyStoreType() {
        return KeyStoreType.PKCS12;
    }
}