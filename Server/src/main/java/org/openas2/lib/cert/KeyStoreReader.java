package org.openas2.lib.cert;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import org.openas2.cert.KeyStoreType;
import org.openas2.lib.helper.ICryptoHelper;

public class KeyStoreReader {

    public static void read(KeyStoreType keyStoreType, KeyStore keyStore, InputStream in, char[] password, ICryptoHelper cryptoHelper)
            throws Exception {
        java.security.KeyStore ks = cryptoHelper.loadKeyStore(keyStoreType, in, password);
        keyStore.setKeyStore(ks);
    }

    public static KeyStore read(KeyStoreType keyStoreType, InputStream in, char[] password, ICryptoHelper cryptoHelper) throws Exception {
        KeyStore keyStore = new KeyStore(null);
        read(keyStoreType, keyStore, in, password, cryptoHelper);
        return keyStore;
    }

    public static void read(KeyStoreType keyStoreType, KeyStore keyStore, URL url, char[] password, ICryptoHelper cryptoHelper) throws Exception {
        InputStream in = url.openStream();
        try {
            read(keyStoreType, keyStore, in, password, cryptoHelper);
        } finally {
            in.close();
        }
    }

    public static KeyStore read(KeyStoreType keyStoreType, URL url, char[] password, ICryptoHelper cryptoHelper) throws Exception {
        KeyStore keyStore = new KeyStore(null);
        read(keyStoreType, keyStore, url, password, cryptoHelper);
        return keyStore;
    }

    public static void read(KeyStoreType keyStoreType, KeyStore keyStore, String filename, char[] password, ICryptoHelper cryptoHelper)
            throws Exception {
        FileInputStream in = new FileInputStream(filename);
        try {
            read(keyStoreType, keyStore, in, password, cryptoHelper);
        } finally {
            in.close();
        }
    }

    public static KeyStore read(KeyStoreType keyStoreType, String filename, char[] password, ICryptoHelper cryptoHelper) throws Exception {
        KeyStore keyStore = new KeyStore(null);
        read(keyStoreType, keyStore, filename, password, cryptoHelper);
        return keyStore;
    }

}