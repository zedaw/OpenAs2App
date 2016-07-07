/*
 * Copyright (c) Smals
 */
package org.openas2.cert;


/**
 * @author std
 *
 */
public enum KeyStoreType {

    PKCS12("p12", "pfx"),
    JKS("jks");
    
    private KeyStoreType(String... strings) {
        if (strings != null) {
            this.extensions = strings;
//            extensions = new String[strings.length];
//            for (int count = (strings.length - 1); count < strings.length; count++) {
//                extensions[count] = strings[count];
//            }
            
        }
    }
    
    private String[] extensions;
    
    public String[] getExtensions() {
        return this.extensions;
    }
}
