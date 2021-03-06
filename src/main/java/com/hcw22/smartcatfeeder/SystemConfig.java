/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hcw22.smartcatfeeder;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author Tom Wong
 */
public class SystemConfig {
    public final static String DEFAULT_THING_NAME = "SmartCatFeeder";
    public final static String CLIENT_END_POINT = "a23lr1f9nedqrs.iot.us-west-2.amazonaws.com";
    public final static String CLIENT_ID = "SmartCatFeeder";
    public final static String CERTIFICATE_FILE_PATH = "cf78374c03-certificate.pem.crt";
    public final static String PRIVATE_KEY_FILE_PATH = "cf78374c03-private.pem.key";
    public final static String DATE_PATTERN = "yyyy-MM-dd";
    public final static String CAT_FEEDER_DELTA_TOPIC = "$aws/things/SmartCatFeeder/shadow/update/delta";
    public final static String CAT_FEEDER_UPDATE_TOPIC = "$aws/things/SmartCatFeeder/shadow/update";
    
    public static class KeyStorePasswordPair {
        public KeyStore keyStore;
        public String keyPassword;

        public KeyStorePasswordPair(KeyStore keyStore, String keyPassword) {
            this.keyStore = keyStore;
            this.keyPassword = keyPassword;
        }
    }
    
    
    public static Certificate loadCertificateFromFile(String filename) {
        Certificate certificate = null;

        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Certificate file not found: " + filename);
            return null;
        }
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            certificate = certFactory.generateCertificate(stream);
        } catch (IOException | CertificateException e) {
            System.out.println("Failed to load certificate file " + filename);
        }

        return certificate;
    }
    
    private static PrivateKey loadPrivateKeyFromFile(String filename) {
        PrivateKey privateKey = null;

        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Private key file not found: " + filename);
            return null;
        }
        try (DataInputStream stream = new DataInputStream(new FileInputStream(file))) {
            privateKey = PrivateKeyReader.getPrivateKey(stream, null);
        } catch (IOException | GeneralSecurityException e) {
            System.out.println("Failed to load private key from file " + filename);
        }

        return privateKey;
    }
    
    public static KeyStorePasswordPair getKeyStorePasswordPair(String certificateFile, String privateKeyFile,
            String keyAlgorithm) {
        if (certificateFile == null || privateKeyFile == null) {
            System.out.println("Certificate or private key file missing");
            return null;
        }

        Certificate certificate = loadCertificateFromFile(certificateFile);
        PrivateKey privateKey = loadPrivateKeyFromFile(privateKeyFile);
        if (certificate == null || privateKey == null) {
            return null;
        }

        return getKeyStorePasswordPair(certificate, privateKey);
    }
    
    public static KeyStorePasswordPair getKeyStorePasswordPair(Certificate certificate, PrivateKey privateKey) {
        KeyStore keyStore = null;
        String keyPassword = null;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            keyStore.setCertificateEntry("alias", certificate);

            // randomly generated key password for the key in the KeyStore
            keyPassword = new BigInteger(128, new SecureRandom()).toString(32);
            keyStore.setKeyEntry("alias", privateKey, keyPassword.toCharArray(), new Certificate[] { certificate });
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
            System.out.println("Failed to create key store");
            return null;
        }

        return new KeyStorePasswordPair(keyStore, keyPassword);
    }
}
