/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.security;

import java.io.*;
import java.util.*;

import java.security.*;

import javax.mail.internet.*;

import org.bouncycastle.mail.smime.*;

/* the following 3 imports are needed if support for both jdk 1.3 and jdk 1.4+ is needed */
import java.security.cert.X509Certificate;      // needed for jdk 1.3
import java.security.cert.CertificateException; // needed for jdk 1.3
import org.bouncycastle.jce.*;                  // needed for jdk 1.3
import org.bouncycastle.jce.cert.*;             // needed for jdk 1.3
/* the following import should be used instead of the 4 above if no support for jdk 1.3 is needed, but only for jdk 1.4+ */
//import java.security.cert.*;

import org.bouncycastle.mail.smime.*;

/**
 * <p>Loads a {@link java.security.KeyStore} in memory and keeps it ready for the
 * cryptographic activity.</p>
 * <p>It has the role of being a simpler intermediate to the crypto libraries.
 * Uses specifically the <a href="http://www.bouncycastle.org/">Legion of the Bouncy Castle</a>
 * libraries, particularly for the SMIME activity.</p>
 * @version CVS $Revision$ $Date$
 * @since 2.2.1
 */
public class KeyHolder {
    
    /**
     * Returns the default keystore type as specified in the Java security properties file,
     * or the string "jks" (acronym for "Java keystore") if no such property exists.
     * @return The defaultType, issuing a <CODE>KeyStore.getDefaultType()</CODE>.
     */
    public static String getDefaultType() {
        return KeyStore.getDefaultType();
    }
    
    /**
     * Security Providers initialization class.
     * The first call of the init method will have the class loader do the job.
     * This technique ensures proper initialization without the need of maintaining
     * the <i>${java_home}/lib/security/java.security</i> file, that would otherwise need
     * the addition of the following line:
     * <code>security.provider.<i>n</i>=org.bouncycastle.jce.provider.BouncyCastleProvider</code>.
     **/
    public static class InitJCE {
        private static java.security.Provider bouncyCastleProvider = new org.bouncycastle.jce.provider.BouncyCastleProvider();
        static {
            java.security.Security.addProvider(bouncyCastleProvider);
        }
        /**
         * Dummy static method to call in order to trigger the initialization by the class loader.
         */        
        public static void init() {
            // NOP
        }
    }
    
    /**
     * Holds value of property privateKey.
     */
    private PrivateKey privateKey;
    
    /**
     * Holds value of property certificate.
     */
    private X509Certificate certificate;
    
    /**
     * Holds value of property certStore.
     */
    private CertStore certStore;
    
    /** Creates a new instance of KeyHolder */
    private KeyHolder() {
    }
    
    /**
     * Creates a new instance of <CODE>KeyHolder</CODE> using {@link java.security.KeyStore} related parameters.
     * @param keyStoreFileName The (absolute) file name of the .keystore file to load the keystore from.
     * @param keyStorePassword The (optional) password used to check the integrity of the keystore.
     *      If given, it is used to check the integrity of the keystore data,
     *      otherwise, if null, the integrity of the keystore is not checked.
     * @param keyAlias The alias name of the key.
     *      If missing (is null) and if there is only one key in the keystore, will default to it.
     * @param keyAliasPassword The password of the alias for recovering the key.
     *      If missing (is null) will default to <I>keyStorePassword</I>. At least one of the passwords must be provided.
     * @param keyStoreType The type of keystore.
     *      If missing (is null) will default to the keystore type as specified in the Java security properties file,
     *      or the string "jks" (acronym for "Java keystore") if no such property exists.
     * @throws java.security.KeyStoreException Thrown when the <I>keyAlias</I> is specified and not found,
     *      or is not specified and either no alias is found or more than one is found.
     * @see java.security.KeyStore#getDefaultType
     * @see java.security.KeyStore#getInstance(String)
     * @see java.security.KeyStore#load
     * @see java.security.KeyStore#getKey
     * @see java.security.KeyStore#getCertificate
     */
    public KeyHolder(String keyStoreFileName, String keyStorePassword, String keyAlias, String keyAliasPassword, String keyStoreType)
    throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
    CertificateException, UnrecoverableKeyException, NoSuchProviderException {
        
        InitJCE.init();

        if (keyStoreType == null) {
            keyStoreType = KeyStore.getDefaultType();
        }
        
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(new BufferedInputStream(new FileInputStream(keyStoreFileName)), keyStorePassword.toCharArray());
        
        Enumeration aliases = keyStore.aliases();
        if (keyAlias == null) {
            if (aliases.hasMoreElements()) {
                keyAlias = (String) aliases.nextElement();
            } else {
                throw new KeyStoreException("No alias was found in keystore.");
            }
            if (aliases.hasMoreElements()) {
                throw new KeyStoreException("No <keyAlias> was given and more than one alias was found in keystore.");
            }
        }
        
        if (keyAliasPassword == null) {
            keyAliasPassword = keyStorePassword;
        }
        
        this.privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyAliasPassword.toCharArray());
        if (this.privateKey == null) {
            throw new KeyStoreException("The \"" + keyAlias + "\" PrivateKey alias was not found in keystore.");
        }
        this.certificate = (X509Certificate) keyStore.getCertificate(keyAlias);
        if (this.certificate == null) {
            throw new KeyStoreException("The \"" + keyAlias + "\" X509Certificate alias was not found in keystore.");
        }
        java.security.cert.Certificate[] certificateChain = keyStore.getCertificateChain(keyAlias);
        ArrayList certList = new ArrayList();
        if (certificateChain == null) {
            certList.add(this.certificate);
        } else {
            for (int i = 0; i < certificateChain.length; i++) {
                certList.add(certificateChain[i]);
            }
        }
        
        // create a CertStore containing the certificates we want carried
        // in the signature
        this.certStore = CertStore.getInstance("Collection",
        new CollectionCertStoreParameters(certList), "BC");
        
    }
    
    /**
     * Getter for property privateKey.
     * @return Value of property privateKey.
     */
    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }
    
    /**
     * Getter for property certificate.
     * @return Value of property certificate.
     */
    public X509Certificate getCertificate() {
        return this.certificate;
    }
    
    /**
     * Getter for property certStore.
     * @return Value of property certStore.
     */
    public CertStore getCertStore() {
        return this.certStore;
    }
    
    /**
     * Creates an <CODE>SMIMESignedGenerator</CODE>. Includes a signer private key and certificate,
     * and a pool of certs and cerls (if any) to go with the signature.
     * @return The generated SMIMESignedGenerator.
     */    
    public SMIMESignedGenerator createGenerator() throws CertStoreException, SMIMEException {
        
        // create the generator for creating an smime/signed message
        SMIMESignedGenerator generator = new SMIMESignedGenerator();
        
        // add a signer to the generator - this specifies we are using SHA1
        // the encryption algorithm used is taken from the key
        generator.addSigner(this.privateKey, this.certificate, SMIMESignedGenerator.DIGEST_SHA1);
        
        // add our pool of certs and cerls (if any) to go with the signature
        generator.addCertificatesAndCRLs(this.certStore);
        
        return generator;
        
    }
    
    /**
     * Generates a signed MimeMultipart from a MimeMessage.
     * @param message The message to sign.
     * @return The signed <CODE>MimeMultipart</CODE>.
     */    
    public MimeMultipart generate(MimeMessage message) throws CertStoreException,
    NoSuchAlgorithmException, NoSuchProviderException, SMIMEException {
        
        // create the generator for creating an smime/signed MimeMultipart
        SMIMESignedGenerator generator = createGenerator();
        
        // do it
        return generator.generate(message, "BC");
        
    }
    
    /**
     * Generates a signed MimeMultipart from a MimeBodyPart.
     * @param content The content to sign.
     * @return The signed <CODE>MimeMultipart</CODE>.
     */    
    public MimeMultipart generate(MimeBodyPart content) throws CertStoreException,
    NoSuchAlgorithmException, NoSuchProviderException, SMIMEException {
        
        // create the generator for creating an smime/signed MimeMultipart
        SMIMESignedGenerator generator = createGenerator();
        
        // do it
        return generator.generate(content, "BC");
        
    }

    /**
     * Extracts the signer <I>distinguished name</I> (DN) from an <CODE>X509Certificate</CODE>.
     * @param certificate The certificate to extract the information from.
     * @return The requested information.
     */    
    public static String getSignerDistinguishedName(X509Certificate certificate) {
        
        return certificate.getSubjectDN().toString();
        
    }
    
    /**
     * Extracts the signer <I>common name</I> (CN=) from an <CODE>X509Certificate</CODE> <I>distinguished name</I>.
     * @param certificate The certificate to extract the information from.
     * @return The requested information.
     * @see getSignerDistinguishedName(X509Certificate)
     */    
    public static String getSignerCN(X509Certificate certificate) {
        
        return extractAttribute(certificate.getSubjectDN().toString(), "CN=");
        
    }
    
    /**
     * Extracts the signer <I>email address</I> (EMAILADDRESS=) from an <CODE>X509Certificate</CODE> <I>distinguished name</I>.
     * @param certificate The certificate to extract the information from.
     * @return The requested information.
     * @see getSignerDistinguishedName(X509Certificate)
     */    
    public static String getSignerAddress(X509Certificate certificate) {
        
        return extractAttribute(certificate.getSubjectDN().toString(), "EMAILADDRESS=");
        
    }
    
    /**
     * Getter for property signerDistinguishedName.
     * @return Value of property signerDistinguishedName.
     * @see getSignerDistinguishedName(X509Certificate)
     */
    public String getSignerDistinguishedName() {
        return getSignerDistinguishedName(getCertificate());
    }
    
    /**
     * Getter for property signerCN.
     * @return Value of property signerCN.
     * @see getSignerCN(X509Certificate)
     */
    public String getSignerCN() {
        return getSignerCN(getCertificate());
    }
    
     /**
     * Getter for property signerAddress.
     * @return Value of property signerMailAddress.
     * @see getSignerAddress(X509Certificate)
     */
    public String getSignerAddress() {
        return getSignerAddress(getCertificate());
    }
    
   private static String extractAttribute(String DistinguishedName, String attributeName) {
        
        int i = DistinguishedName.indexOf(attributeName);
        
        if (i < 0) {
            return null;
        }
        
        i += attributeName.length();
        int j = DistinguishedName.indexOf(",", i);
        
        if (j - 1 <= 0) {
            return null;
        }
        
        return DistinguishedName.substring(i, j).trim();
        
    }
    
}
