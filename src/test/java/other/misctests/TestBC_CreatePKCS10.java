/*
 * Copyright (C) 2015 STFC
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package other.misctests;

import java.io.StringReader;
import java.io.StringWriter;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Meredith
 */
public class TestBC_CreatePKCS10 {

    public TestBC_CreatePKCS10() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test simulates the following sequence which effectively shows how 
     * CSRs can be requested via the portal and the encrypted private 
     * key downloaded by the user as a text file. 
     * 
     * Specify CSR attributes (which would be POSTed to portal) 
     * Create a new PKCS#10 CSR (created server side) 
     * Output the CSR as a PEM string  
     * Output the un-encrypted PKCS8 private key as a string  
     * Encrypt the PKCS8 private key
     * Output the encrypted PKCS#8 private key as a string (for subsequent download by user) 
     * Decrypt the encrypted private key file String  
     * Assert that the encrypted and decrypted private keys are same. 
     * 
     * @throws Exception 
     */
    @Test
    public void createPKCS10_EncryptPKCS8_withBC() throws Exception {
        
        // Create a PKCS10 cert signing request 
        Security.addProvider(new BouncyCastleProvider());
       
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(2048);
        KeyPair kp = kpg.genKeyPair();
        PrivateKey priKey = kp.getPrivate();

        X500NameBuilder x500NameBld = new X500NameBuilder(BCStyle.INSTANCE);
        // Notice we have to create the CSR with a DN that has the  
        // following RDN structure order (C, O, OU, L, CN) 
        x500NameBld.addRDN(BCStyle.C, "UK");
        x500NameBld.addRDN(BCStyle.O, "eScienceDev");
        x500NameBld.addRDN(BCStyle.OU, "CLRC");
        x500NameBld.addRDN(BCStyle.L, "DL");
        x500NameBld.addRDN(BCStyle.CN, "david meredith");

        X500Name subject = x500NameBld.build();

        PKCS10CertificationRequestBuilder requestBuilder
                = new JcaPKCS10CertificationRequestBuilder(subject, kp.getPublic());

        ExtensionsGenerator extGen = new ExtensionsGenerator();
        extGen.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(new GeneralName(GeneralName.rfc822Name, "feedback-crypto@bouncycastle.org")));

        requestBuilder.addAttribute(
                PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());
        
        String sigName = "SHA1withRSA";
        PKCS10CertificationRequest req1 = requestBuilder.build(
                new JcaContentSignerBuilder(sigName).setProvider("BC").build(kp.getPrivate()));

        if (req1.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider("BC").build(kp.getPublic()))) {
            System.out.println(sigName + ": PKCS#10 request verified.");
            System.out.println(""+req1.getSubject().toString()); //CN=david meredith,L=DL,OU=CLRC,O=eScienceDev,C=UK
        } else {
            System.out.println(sigName + ": Failed verify check.");
            fail("fail");
        }

        // output the CSR pem string 
        StringWriter writer = new StringWriter();
        PEMWriter pemWrite = new PEMWriter(writer);
        pemWrite.writeObject(req1);
        pemWrite.close();
        System.out.println(writer);

        // Output un-encrypted private key pkcs8 Pem string
        JcaPKCS8Generator pkcs8GeneratorNoEnc = new JcaPKCS8Generator(priKey, null);
        PemObject pkcs8PemNoEnc = pkcs8GeneratorNoEnc.generate();
        StringWriter writer3 = new StringWriter();
        PEMWriter pemWrite3 = new PEMWriter(writer3);
        pemWrite3.writeObject(pkcs8PemNoEnc);
        pemWrite3.close();
        String pkcs8StrNoEnc = writer3.toString();
        System.out.println(pkcs8StrNoEnc);

        // Encrypt the private key file 
        //http://stackoverflow.com/questions/14597371/encrypt-a-private-key-with-password-using-bouncycastle
        JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder
                = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.PBE_SHA1_3DES);
        String passwd = "12345";
        // From https://www.openssl.org/docs/apps/pkcs8.html : 
        // '-v2 alg' option for the pkcs8 openssl conversion tool
        // This option enables the use of PKCS#5 v2.0 algorithms. 
        // Normally PKCS#8 private keys are encrypted with the password based 
        // encryption algorithm called pbeWithMD5AndDES-CBC this uses 56 bit DES 
        // encryption but it was the strongest encryption algorithm supported in 
        // PKCS#5 v1.5. Using the -v2 option PKCS#5 v2.0 algorithms are used 
        // which can use any encryption algorithm such as 168 bit triple DES 
        // or 128 bit RC2 however not many implementations support PKCS#5 v2.0 yet. 
        // If you are just using private keys with OpenSSL then this doesn't matter.
        // 
        //The 'alg' argument is the encryption algorithm to use, valid values 
        // include des, des3 and rc2. It is recommended that des3 is used

        ASN1ObjectIdentifier asn1 = PKCS8Generator.PBE_SHA1_3DES;
        assertEquals(asn1.getId(), "1.2.840.113549.1.12.1.3"); // 3 key triple DES with SHA-1 (des3)
        SecureRandom random = new SecureRandom();
        encryptorBuilder.setRandom(random);
        encryptorBuilder.setPasssword(passwd.toCharArray());
        OutputEncryptor oe = encryptorBuilder.build();
        JcaPKCS8Generator pkcs8GeneratorEnc = new JcaPKCS8Generator(priKey, oe);

        // Output encrypted private key pkcs8 PEM string (todo use later api) 
        PemObject pkcs8PemEnc = pkcs8GeneratorEnc.generate();
        StringWriter writer2 = new StringWriter();
        PEMWriter pemWrite2 = new PEMWriter(writer2);
        pemWrite2.writeObject(pkcs8PemEnc);
        pemWrite2.close();
        String pkcs8StrEnc = writer2.toString();
        System.out.println(pkcs8StrEnc);

        // Can now check the private key with openssl (remember to use correct password):     
        // openssl rsa -in privatekey.pem -check
        // openssl pkcs8 -in privatekey.pem

        
        // Decrypt the private key file from String
        // http://stackoverflow.com/questions/2654949/how-to-read-a-password-encrypted-key-with-java?rq=1
        PEMParser keyPemParser = new PEMParser(new StringReader(pkcs8StrEnc));
        PemObject keyObj = keyPemParser.readPemObject();
        byte[] keyBytes = keyObj.getContent();

        EncryptedPrivateKeyInfo encryptPKInfo = new EncryptedPrivateKeyInfo(keyBytes);
        Cipher cipher = Cipher.getInstance(encryptPKInfo.getAlgName());
        PBEKeySpec pbeKeySpec = new PBEKeySpec(passwd.toCharArray());
        SecretKeyFactory secFac = SecretKeyFactory.getInstance(encryptPKInfo.getAlgName());
        Key pbeKey = secFac.generateSecret(pbeKeySpec);
        AlgorithmParameters algParams = encryptPKInfo.getAlgParameters();
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams);
        KeySpec pkcs8KeySpec = encryptPKInfo.getKeySpec(cipher);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey priKeyDecrypted = kf.generatePrivate(pkcs8KeySpec);

        // Could use below by specifying the provider, 
        // Assert that the two private keys are same
        assertEquals("RSA", priKey.getAlgorithm());
        assertEquals(priKey.getAlgorithm(), priKeyDecrypted.getAlgorithm());

        assertEquals("PKCS#8", priKey.getFormat());
        assertEquals(priKey.getFormat(), priKeyDecrypted.getFormat());
        Assert.assertArrayEquals(priKey.getEncoded(), priKeyDecrypted.getEncoded());

        //ParserTest.java
        //https://github.com/bcgit/bc-java/blob/53d17ef99e30c6bd49e6eec9235e3eefca6a222d/pkix/src/test/java/org/bouncycastle/openssl/test/ParserTest.java
        // Below is from: 
        //C:\Users\djm76\Documents\work\programming\java\javadocs\bcprov-jdk15on-150\org\bouncycastle\jce\provider\test\EncryptedPrivateKeyInfoTest.java 

    }

    /**
     * Test to show that tripple des PKCS8 private key pem created with ForgeJS
     * can be decrypted with BC. 
     * 
     * @throws Exception 
     */
    @Test
    public void decryptForgePkcs8PrivateKeyPem_PBEWithSHA1AndDESede() throws Exception {
        // http://bouncy-castle.1462172.n4.nabble.com/Help-with-EncryptedPrivateKeyInfo-td1468363.html
        // https://community.oracle.com/thread/1530354?start=0&tstart=0
        Security.addProvider(new BouncyCastleProvider());

        PEMParser keyPemParser = new PEMParser(new StringReader(getPkcs8ForgePriKeyPem_EncryptedWithPBEWithSHA1AndDESede()));
        String passwd = "password";
        PemObject keyObj = keyPemParser.readPemObject();
        byte[] keyBytes = keyObj.getContent();

        EncryptedPrivateKeyInfo encryptPKInfo = new EncryptedPrivateKeyInfo(keyBytes);
        // 1.2.840.113549.1.5.13 == PBEWithMD5AndDES
        // 1.2.840.113549.1.12.1.3 == PBEWithSHA1AndDESede
        String algName = encryptPKInfo.getAlgName();
        String algId = encryptPKInfo.getAlgParameters().getAlgorithm();
        assertEquals("PBEWithSHA1AndDESede", algName);
        assertEquals("1.2.840.113549.1.12.1.3", algId);
        assertEquals("1.2.840.113549.1.12.1.3", PKCS8Generator.PBE_SHA1_3DES.getId());

        // Decrypt private key 
        Cipher cipher = Cipher.getInstance(algName);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(passwd.toCharArray());
        SecretKeyFactory secFac = SecretKeyFactory.getInstance(algName);
        Key pbeKey = secFac.generateSecret(pbeKeySpec);
        AlgorithmParameters algParams = encryptPKInfo.getAlgParameters();
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, algParams);
        KeySpec pkcs8KeySpec = encryptPKInfo.getKeySpec(cipher);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey priKeyDecryptedBC = kf.generatePrivate(pkcs8KeySpec);

        // Compare decrypted private key with a version that was decrypted using 
        // openssl and assert that they are the same. 
        JcaPKCS8Generator pkcs8GeneratorNoEnc = new JcaPKCS8Generator(priKeyDecryptedBC, null);
        PemObject pkcs8PemDecryptedBC = pkcs8GeneratorNoEnc.generate();
        StringWriter writer3 = new StringWriter();
        PEMWriter pemWrite3 = new PEMWriter(writer3);
        pemWrite3.writeObject(pkcs8PemDecryptedBC);
        pemWrite3.close();
        String pkcs8StrDecryptedBC = writer3.toString().trim().replaceAll("\\r\\n", "\n");
        String pkcs8StrDecryptedOpenSSL = getPkcs8ForgePriKeyPem_DecryptedWithOpenSSL().trim().replaceAll("\\r\\n", "\n");
        assertTrue(pkcs8StrDecryptedBC.equals(pkcs8StrDecryptedOpenSSL));
    }



    /**
     * Return an un-encrypted private key for the encrypted private key returned by
     * <code>getPkcs8ForgePriKeyPem_EncryptedWithPBEWithSHA1AndDESede()</code>. 
     * The private key was decrypted using: 'openssl pkcs8 -in forge3desPkcs8.pem'
     * (providing the password of 'password') 
     *
     * @return
     */
    private String getPkcs8ForgePriKeyPem_DecryptedWithOpenSSL() {
        return "-----BEGIN PRIVATE KEY-----\n"
                + "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCTmt6800o+Yoee\n"
                + "nxTO1fsRMrn4TxtqxdD+f8IZ/3CfLnszKKyrNddVk/IkOJWMr8m3oRKpBew3yPVO\n"
                + "M7CBvxPJCwYgqEb1Dgqi4J2qjBRZZhhEyOLla5nXlNQtkbUc1F5SsQe7yOagDUkl\n"
                + "pqEmnMzW5C2rf9VvDinVv9YmZnu60m8itCF1bfdewGt+0K3k9q4lOrl+znNp4DlC\n"
                + "hDS0hOIk/CfXIhme43cKEC+uqMw5usMk4UePMWX7sosDamqR3mNLJCmls2WvN3Fj\n"
                + "7KlcJXkQfVIAy9HPms3MzbuW/yqvvXZoFoOo3WLjr8c9egMDmbrHkefvxF62a7hI\n"
                + "uTXzJuCdAgMBAAECggEBAIJwzn4YSvguoqMu+nh+6U0dgvNJFXWaM0N1K9dFwgUq\n"
                + "Un23aEA1aIKcJ9SSnLajcqPwmEj+ju4NjZr6WvWOIrH8KAIcM/jD2+psjWj2OdV3\n"
                + "P+MplDoiiOXXBFrw9RjxJBn3kFoHBKhUlVvMkYVJ9EvDggiZA8kMvzPCQEHXN2i0\n"
                + "zvM/A+st5dr/8s9bmx1C1ZJExLmYTmOwMe5BbDbXYamK01IW+lM2B4G2U9SC94Uo\n"
                + "ypmu+YeDfCdPPmP8Y6QzNSWQWGwnmzPOX7hmS+aC8JnoJOt2E4g3Yh4XqPo4XGbH\n"
                + "CpBSW+Pc3cJo4A0AJ9Xu84tZ1hHrJvgqudCR5dgMcIECgYEAyS8cvqLJ30Zp/Vx0\n"
                + "292aWvoJSi9+R8dkMFDJbmMGiK/Y+bhJXMuZ4Sq6DhQSDouw84dQoDPCqpg2rBiT\n"
                + "cCe8UPGiIix4YGg+tpea2blOrMII50aVyYy8+QrQqMjXvyrN0yWSyUBR07DSa3gY\n"
                + "33hVW88a4NjL8WIbpKuO5rTyDfkCgYEAu9KJi7DqbHa6MeGVUzfnSIUf+HwKfZXL\n"
                + "xKHwaEFjXckH0v1d48Q0RKFf+P8J5U1JqEhRraP46KOY15DHfdDGbR6NuZ90zeLN\n"
                + "kq+UUyjq4yn52+LYjvX+y6DCLtIt5bKAfYyubSIwozv1aUDO3LAZ+858E/9ukOx7\n"
                + "2HCBQMD7IMUCgYEAwNGinyOuj4wRMX9XkRKHSgKyvKNgSLNV1ujW5jGKpZ7EOjLi\n"
                + "PUn2JdSplayu0boY0o8yOxjgzlgsriyvwqKS3pF4b4Bnrx66XI5ZH6t3OCSQG/mO\n"
                + "vilhRN+UtPApt1LzChfM44393wJt6gqk7CmMxf1tKWsfrC33iI/U2lE6XSECgYAk\n"
                + "urdLKf2t2EnxFzwsWfJQrDfkT919UZ3XhhONT7wuyvFMwV9q+yN9iFFMUBOPU93j\n"
                + "msDeRAKY++UXwqhAYmNrU15DvnsJCCFLXiqTWJ0Wb079QQ84ZcK972IJ5fAzywR1\n"
                + "iN1TWixIv6DuRE4vugBazbZV8s2caaKOYPHlx9dUQQKBgEO+nEoVOyLdj5YdBP6G\n"
                + "2eXAazugMiWPOLDIdAdD+UM1H9b13boUqCI0te1nacMf1pbG+iYKQzBHcQOLcoio\n"
                + "imnetENXKDlj/ezn2SMbpTxnxdHT5Ab+/qxkOzSBTrxYhLSwdbM6J9yavnSvNKqD\n"
                + "d8nKIAcBs3sSZT+MIyCKOk6+\n"
                + "-----END PRIVATE KEY-----";
    }

    /**
     * Get a pkcs8 encrypted private key string created using Forge JS. The
     * encryption password is 'password' and the encryption algorithm for BC is
     * PBEWithSHA1AndDESede ( 1.2.840.113549.1.12.1.3 ). 
     * This is equivalent of '3des' algName for forge.
     *
     * @return
     */
    private String getPkcs8ForgePriKeyPem_EncryptedWithPBEWithSHA1AndDESede() {
        return "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
                + "MIIE6jAcBgoqhkiG9w0BDAEDMA4ECDwfrAeqlVNsAgIIAASCBMh5vXVo0nvI0DBk\n"
                + "LaavnYI59Yx1aD5ErWfDlut7etAq+NX74yMKVx1PJ0XMVRbcUxDm3JklF6YEoRnu\n"
                + "HNQJHEcGgcvWMj7xsssQTpT/277PhW8IVXv2dL5IFQTj17lSFVfxhz47WPKCH+MP\n"
                + "AxACyEb5FDWvRyt315VgRymKfPJBI/LP9ER6RRits1BD3W4oj4lTNxIiZif+3bVp\n"
                + "K5ai2ObKQK+iPehMgiBzo8A5uVounWt68upcxHEqJVyPb4pqJoqCDk6WuzS+26uq\n"
                + "GFJzViV/41fgYSNYuHUJXCjsvuO4XDs9SLMoI7JG1qV6txFMKtlG7O5Q9jnrHUwD\n"
                + "+W/qFdNAY85wdGkT5D9jFgVZwRmXCNBrMBGWM/QNGM34002Gp5K+66BTmHG+1r3A\n"
                + "aTjx3uPHQaXKAndJWVbh+Yy2LL8TO50qX8OX5+XaR5mDcIriUF0emcjyoIjXOuY3\n"
                + "flTvNT3B0eVPgQ80NERzXLNWdvL+DAcsiDOY6d2PV5Q1/6udXJLujLWOgrCMhtfF\n"
                + "+NawonSEbdIAh+JLJkB7oRBkjUORsQyigw6QLDIewjHvtC+oYJ9PPEDw1kb/27ii\n"
                + "8MDOkpS8+dagHDTVsQUe7AhT/ndk8EwZ81xzDAfatgVtTi7iJ5wgW5bqnvIMZLWO\n"
                + "FqFs7/dm+9lXDK42Be1OjHNbuJnyiD/P54LbGrqLUMnvn4SDCQvsDOMxMOUqOLAr\n"
                + "E81twn4SP5o3w1rz1muUgw0R+Cb3UNCfWc3wE7XEQQyL+77ml74LWwUOzMwkmR6M\n"
                + "Oqd+R9Q/XQvL7mkyoPGqTuikj0M3yLQWbjPwpYTNU4EevG5w2yAfcpEa51mN5Ss3\n"
                + "Ts+h6G4o/KZYW8QNPzVQy843OIUiQJeRs1v0XJqjfnuJ6co2jP37keDFbqmpPaot\n"
                + "7K8oEUnZ8kZY1kl0G0owWnAReVd8xUMPJZDtDILzZNo+2cL3dhoX+EPA+tC9wtTG\n"
                + "gRsr5tTyY+F2VgsE8E5TIfsNuq1OItTeYJiNO8RJ1I9+BadTuKK/LB8k4XfMovDz\n"
                + "rUkvRdEx2h6NwLhf98t4k61S1t2dlqfF3/ZXExphh0Kb9qkV1oX6vw3gnTpKgW13\n"
                + "wpciQ865AUThzYSpgp9OFdnDW5HYYJGb5/CU/LHhINI1/J+EE7vbMOYzQ0qk6wuv\n"
                + "cOKarNevL/WeEMH0HGS3WXbTuWwp9OHbN+/0bpuGyfDYH7ouGBa+zGeopU9Mi67c\n"
                + "vwnaZJKbsP/Ot/NyuPLfNzZxRrpFC9Mx5F9QKxuELuV9M9NhawN1xzYu1ge4Oyw3\n"
                + "zzV7Rk5qFdbWM55ofRCd2ijatSe0PLHG4HKePN8yujHNLNU8wzYh1EGXeOJaInsv\n"
                + "HIKac5+FNacXn/2kXhqIPDLA20i7KF/47ZYc5erZgaxoxb/ulmdky1OrEZL7jLQP\n"
                + "DxRMWcqjHEYNm5wDzZkEdrajQjoDIFQJ6McAcIMhrY6YQP2RDZU+bLBfPfp/y4bg\n"
                + "psWWZrp999qI02I0aAzpA9+UJFJLvqXVp7+RBemZvdlbn8XREc4I0qQtxr4u1VS2\n"
                + "H5hL9ipPCSp2i/VX92UNPRIA88lxh69AuXUyQShfbjEMNZHNLq5GIfACrhxiK2wo\n"
                + "hYV+FivJvK33L5QFCt4=\n"
                + "-----END ENCRYPTED PRIVATE KEY-----";
    }

    /**
     * Get a pkcs8 encrypted private key string created using Forge JS. The
     * encryption password is '1234567890' and the encryption algorithm is
     * PBEWithMD5AndDES (1.2.840.113549.1.5.13);
     *
     * @return
     */
    private String getPkcs8ForgePriKeyPem_PBEWithMD5AndDES() {
        String pkcs8ForgePriKeyPem = "-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
                + "MIIFHzBJBgkqhkiG9w0BBQ0wPDAbBgkqhkiG9w0BBQwwDgQIWHtms/BsyOsCAggA\n"
                + "MB0GCWCGSAFlAwQBAgQQsV28xXE59/8zQtrWHfoXvASCBNBfzhJoZhLpVwgKXfP9\n"
                + "p9by9XXFtOITzfZzAQY6IScNO0VCRkdygsh3iryfTkH6Tr0pJpG9Bf3ixEsZ08nC\n"
                + "9QGbo9Udkayq2K6dBKcI1LhHeckCwy10Ba1kINPRNmKxUGwFtS9/ORRwkyTFpJOL\n"
                + "I4+jnClmpo/antYywLbu+qr8+mGJk64D67VyhKllM11NgLV3mqeDbi1oSHNhQ4GK\n"
                + "iT2xNJxIJaY52s99fBPL0cbaLhxAlcIAmiXv54VFqHD8ukVHJosGP0PW3QmCgILb\n"
                + "hP/k6p/XU3DsvZpEPE2hHI3oAkqGp0lHTfZgiiGPz40/B4dt6poLw0NyFIK9SbAH\n"
                + "ejqHFOdFqNghm03DlwFNSQYCz/kEApdFyKY3/QG0nJ38QhVSfmG83kdUT3aAzdcc\n"
                + "T6YlrROjmaG0yJGEhxOZOq7MMPvT0mfl4ikwd0M00udOnx9yHyv/qCvUuWUJBdaj\n"
                + "zhgzI3GkYuZXEhVk38nUZXIKOfeT2Ck+ZNHHlp5hfmg56jLPGjBijOpvW7wVpKZ/\n"
                + "dfLDDFC89Ob6/hX8uMeHlBlByE8b7JAqhhaUCZI3GWh7DKehcPzUl8HIpZ8mjt7X\n"
                + "+3RwGYRyTkkRn8u67xI5B1xzkRtmB+GlDeDikMJE1+7QrbCiiLl4DLo0ZW0FFeYy\n"
                + "0d7JeCZFI7vU+x4whgqpidj5hShI2jcxjjnmv/r4scVxbooDf2utEdf/owXS0Swn\n"
                + "rockTUNvGqrFbWQQZHFj4XGmc8CKl4c6hcgion74LSz5oNJh3ZokABWOzwAovPwD\n"
                + "IIMaBaoS0boASaU2A1diC7oGd8wFVxnH57ebmSL3S3Fa3QO3xRbWx8zC6CIJk05p\n"
                + "qrC6JJ87Ph+5EuhxnqyGWA2yYcvoze1QIDPbeoebIsCiNTehwcxACpQgEZ9nVcGT\n"
                + "IDTUGjRq4RsM4NwNgIYBUslZcIyEajaSNIOv3bZAgX+LB78XoQwzTVa8g9LmpjnW\n"
                + "C+chuecZIG+lhVLOisPejVyfgLuU68Qe3sgZ6T4InJzEXpOGQILg0+4ka3Is0xxV\n"
                + "fPL8yL5fnPnbQ/oUcW13WNW0MNDQlfjQP7ia471K2mBAzrmRPk1SX/V410NJm6PY\n"
                + "m7kUBgc805Fl7+MUoP94kygQ6eQIUd5/vCUB6GJ19scQ/WSdaHgYtsfi9cmJTvR7\n"
                + "CIHbFZrJWBbcYQ0vN5hE87annz/JkVcxvn5SGZV1CfhLw+PSdK54UN3gW+137Pvy\n"
                + "F9XYQZmX8oNmwFwL7OLM18Wdu1xPTS6sbr2MUULW8bjdj00yHABeg3EgqG163QRv\n"
                + "lI3hT6Fojb2Jj8lXR9PXOVneSjgl9U2u53NavJ5t1xFViHsOxcKNRi5uM5sD/mkV\n"
                + "5w18o8VbmkZwKrijvtnRb9HBVJwt2ftBj98iK9B0g3nZAGltcMlkhvHtJFWXvM8y\n"
                + "3R+9F9ukQtFsPETIkDZ0tE1qNf/+mX9Xm3zRfK9C5u4RDrV9baP++Ayq1MfjKXhq\n"
                + "8LLn03FxTuy8tbO7UE/2maoxZ7hnDVsTYd34YUBaP7fXu1SXkeV5mmbSD0+vt4Fn\n"
                + "nYVBLPEhGnGa8ePfFpYC+XxapQxUYw5QYVQFagMJ5GWIZNCL9m8TVmGNn69X+gyr\n"
                + "7m7c/+X56OSVKeLs+RpDM/ARuA==\n"
                + "-----END ENCRYPTED PRIVATE KEY-----";
        return pkcs8ForgePriKeyPem;
    }

}
