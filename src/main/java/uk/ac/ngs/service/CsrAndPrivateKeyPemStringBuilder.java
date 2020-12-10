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
package uk.ac.ngs.service;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.IOException;
import java.io.StringWriter;
import java.security.*;


/**
 * Dirty util/builder class to create a PKCS#10 CSR request PEM string and the
 * accompanying encrypted PKCS#8 private key PEM string.
 * <p/>
 * This really is a dirty shortcut class and it could certainly be re-written
 * with properly extracted interfaces and methods.
 * <p/>
 * On class construction, the BouncyCastle security provider is loaded with:
 * <code>Security.addProvider(new BouncyCastleProvider());</code>
 *
 * @author David Meredith
 */
//@Service
public class CsrAndPrivateKeyPemStringBuilder {

    public CsrAndPrivateKeyPemStringBuilder() {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Get the PKCS#10 PEM string and encrypted PKCS#8 PEM string.
     *
     * @param subject
     * @param email   Added as a Subject Alt Name extension if not null
     * @param pw
     * @return First element contains the PKCS#10 PEM, second element contains the private key.
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws OperatorCreationException
     * @throws PKCSException
     */
    public String[] getPkcs10_Pkcs8_AsPemStrings(X500Name subject, String email, String pw)
            throws IOException, NoSuchAlgorithmException,
            NoSuchProviderException, OperatorCreationException, PKCSException {
        // Create a PKCS10 cert signing request 
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(2048);
        KeyPair kp = kpg.genKeyPair();
        PrivateKey priKey = kp.getPrivate();

//        X500NameBuilder x500NameBld = new X500NameBuilder(BCStyle.INSTANCE);
//        x500NameBld.addRDN(BCStyle.C, csrRequestValidationConfigParams.getCountryOID());
//        x500NameBld.addRDN(BCStyle.O, csrRequestValidationConfigParams.getOrgNameOID());
//        x500NameBld.addRDN(BCStyle.OU, ou);
//        x500NameBld.addRDN(BCStyle.L, loc);
//        x500NameBld.addRDN(BCStyle.CN, cn);
//        X500Name subject = x500NameBld.build();
        PKCS10CertificationRequestBuilder requestBuilder
                = new JcaPKCS10CertificationRequestBuilder(subject, kp.getPublic());

        ExtensionsGenerator extGen = new ExtensionsGenerator();
        if (email != null) {
            extGen.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(new GeneralName(GeneralName.rfc822Name, email)));
        }

        requestBuilder.addAttribute(
                PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extGen.generate());

        String sigName = "SHA1withRSA";
        PKCS10CertificationRequest req1 = requestBuilder.build(
                new JcaContentSignerBuilder(sigName).setProvider("BC").build(kp.getPrivate()));

        if (req1.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider("BC").build(kp.getPublic()))) {
            //log.info(sigName + ": PKCS#10 request verified.");
        } else {
            //log.error(sigName + ": Failed verify check.");
            throw new RuntimeException(sigName + ": Failed verify check.");
        }

        StringWriter writer = new StringWriter();
        PEMWriter pemWrite = new PEMWriter(writer);
        pemWrite.writeObject(req1);
        pemWrite.close();
        String csr = writer.toString();

        JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder
                = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.PBE_SHA1_3DES);

        SecureRandom random = new SecureRandom();
        encryptorBuilder.setRandom(random);
        encryptorBuilder.setPasssword(pw.toCharArray());
        OutputEncryptor oe = encryptorBuilder.build();
        JcaPKCS8Generator pkcs8GeneratorEnc = new JcaPKCS8Generator(priKey, oe);

        // Output encrypted private key pkcs8 PEM string (todo use later api) 
        PemObject pkcs8PemEnc = pkcs8GeneratorEnc.generate();
        StringWriter writer2 = new StringWriter();
        PEMWriter pemWrite2 = new PEMWriter(writer2);
        pemWrite2.writeObject(pkcs8PemEnc);
        pemWrite2.close();
        String pkcs8StrEnc = writer2.toString();

        String[] pems = new String[2];
        pems[0] = csr;
        pems[1] = pkcs8StrEnc;
        return pems;
    }

}
