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
package uk.ac.ngs.domain;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import uk.ac.ngs.service.CertUtil;
import uk.ac.ngs.validation.PKCS10Parser;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

/**
 * A class to wrap attributes and invariants for a certificate signing request.
 * Use the provided {@link Builder} to create an instance.
 * <p/>
 * Required parameters depend on the type of request - this is enforced by the
 * <tt>build()</tt> method that checks values have been provided.
 * Note, this class does not perform validation of values (e.g. checking
 * validity of emails, CSR PEM strings etc), rather this class should be used as
 * an input for subsequent validation.
 *
 * @author David Meredith
 */
public class PKCS10_RequestWrapper {

    // required (enforced by builder) 
    private final CSR_Flags.Csr_Types csr_type;
    private final CSR_Flags.Profile profile;
    private final String csrPemString;
    private final String email;

    // optional (builder.setClientData()) 
    private final String clientEmail;
    private final String clientDN;
    private final long clientSerial;

    // Available after construction 
    private final String p10CN;
    private final String p10Ou;
    private final String p10Loc;
    private final PublicKey p10PubKey;
    private final PKCS10CertificationRequest p10Req;

    private static final PKCS10Parser p10Parser = new PKCS10Parser();

    public static class Builder {

        private final CSR_Flags.Csr_Types csr_type;
        private final CSR_Flags.Profile profile;
        private final String csrPemString;
        private final String email;

        private String clientEmail;
        private String clientDN;
        private long clientSerial;

        /**
         * Create an instance of Builder and call <tt>build()</tt> to create
         * an instance of {@link PKCS10_RequestWrapper}
         *
         * @param csr_type     The type of this CSR
         * @param profile      The profile that should be used for this CSR
         * @param csrPemString A valid PKCS#10 string
         * @param email        The email that will be recorded in the <tt>request.email</tt> table
         */
        public Builder(CSR_Flags.Csr_Types csr_type,
                       CSR_Flags.Profile profile, String csrPemString, String email) {
            this.csrPemString = csrPemString;
            this.csr_type = csr_type;
            this.profile = profile;
            this.email = email;
        }

        /**
         * Create an instance and enforce parameter invariants.
         * On creation, the mandatory <code>csrPemString</code> attribute is
         * parsed and a PKCS#10 object is built.
         *
         * @return
         * @throws InvalidKeyException
         * @throws NoSuchAlgorithmException
         */
        public PKCS10_RequestWrapper build() throws InvalidKeyException, NoSuchAlgorithmException {
            PKCS10_RequestWrapper csrWrapper = new PKCS10_RequestWrapper(this);

            // Check invariants here (MUST check invariants on target object's  
            // params, not on builder fields to protect against TOCTOU attacks)    
            // See Effective Java pattern. 

            // ClientData is required For a NEW UKHOST request 
            if ((CSR_Flags.Profile.UKHOST.equals(csrWrapper.getProfile())
                    && CSR_Flags.Csr_Types.NEW.equals(csrWrapper.getCsr_type()))
                    ||
                    // ClientData is also required for a RENEW request
                    (CSR_Flags.Csr_Types.RENEW.equals(csrWrapper.getCsr_type()))
            ) {

                if (csrWrapper.getClientDN() == null
                        || csrWrapper.getClientEmail() == null
                        || !(csrWrapper.getClientSerial() > 0)) {
                    throw new IllegalStateException("Invalid Client Data for request");
                }
            }
            return csrWrapper;
        }

        /**
         * Client data is required for NEW UKHOST CSR request and for a RENEW request
         * and identifies the user / caller. It is important that the client
         * data is read from or matches the client data stored in the CA DB.
         *
         * @param clientEmail  email of client as recorded in DB.
         * @param clientDN     DN of client as recorded in DB - it is important that
         *                     this value is read from or matches the value in the CA DB.
         * @param clientSerial Certificate serial/PK used in DB.
         * @return
         */
        public Builder setClientData(String clientEmail, String clientDN, long clientSerial) {
            this.clientEmail = clientEmail;
            this.clientDN = clientDN;
            this.clientSerial = clientSerial;
            return this;
        }
    }

    /*
     * Force non-instantiation of class with private constructor
     */
    private PKCS10_RequestWrapper(Builder builder) throws InvalidKeyException, NoSuchAlgorithmException {
        this.csr_type = builder.csr_type;
        this.profile = builder.profile;
        this.csrPemString = builder.csrPemString;
        this.email = builder.email;

        this.clientEmail = builder.clientEmail;
        this.clientDN = builder.clientDN;
        this.clientSerial = builder.clientSerial;

        // Create and store PKCS#10 request 
        this.p10Req = p10Parser.parseCsrPemString(csrPemString);
        // could expose this if necessary 
        JcaPKCS10CertificationRequest jcaReq = new JcaPKCS10CertificationRequest(this.p10Req);
        this.p10PubKey = jcaReq.getPublicKey();
        this.p10CN = CertUtil.extractDnAttribute(p10Req.getSubject().toString(), CertUtil.DNAttributeType.CN);
        this.p10Loc = CertUtil.extractDnAttribute(p10Req.getSubject().toString(), CertUtil.DNAttributeType.L);
        this.p10Ou = CertUtil.extractDnAttribute(p10Req.getSubject().toString(), CertUtil.DNAttributeType.OU);
        //String ra = p10Ou+" "+p10Loc; // combined "OU L" e.g. 'CLRC DL'
    }

    /**
     * @return the p10CN
     */
    public String getP10CN() {
        return p10CN;
    }

    /**
     * @return the p10Ou
     */
    public String getP10Ou() {
        return p10Ou;
    }

    /**
     * @return the p10Loc
     */
    public String getP10Loc() {
        return p10Loc;
    }

    /**
     * @return the p10PubKey
     */
    public PublicKey getP10PubKey() {
        return p10PubKey;
    }

    /**
     * @return the p10Req
     */
    public PKCS10CertificationRequest getP10Req() {
        return p10Req;
    }

    /**
     * @return the csr_type
     */
    public CSR_Flags.Csr_Types getCsr_type() {
        return csr_type;
    }

    /**
     * @return the profile
     */
    public CSR_Flags.Profile getProfile() {
        return profile;
    }

    /**
     * @return the csrPemString
     */
    public String getCsrPemString() {
        return csrPemString;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the clientEmail
     */
    public String getClientEmail() {
        return clientEmail;
    }

    /**
     * Return clientDN which has been set by reading from the <tt>certificate.dn</tt>
     * DB table column.
     * The DN should always have an RFC2253 style value akin to:
     * <tt>emailAddress=someEmail@world.com,CN=david meredith,L=DL,OU=CLRC,O=eScience,C=UK</tt>
     * where emailAddress may/not be present.
     *
     * @return the clientDN
     */
    public String getClientDN() {
        return clientDN;
    }

    /**
     * @return the clientSerial
     */
    public long getClientSerial() {
        return clientSerial;
    }

}
