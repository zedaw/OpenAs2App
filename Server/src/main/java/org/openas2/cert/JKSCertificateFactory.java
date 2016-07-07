/*
 * Copyright (c) Smals
 */
package org.openas2.cert;

import org.openas2.util.FileMonitorListener;


/**
 * @author std
 *
 */
public class JKSCertificateFactory extends BaseCertificateFactory implements AliasedCertificateFactory, KeyStoreCertificateFactory, StorableCertificateFactory, FileMonitorListener {

    /**
     * @return
     * @see org.openas2.cert.KeyStoreCertificateFactory#getKeyStoreType()
     */
    @Override
    public KeyStoreType getKeyStoreType() {
        return KeyStoreType.JKS;
    }

}
