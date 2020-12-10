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
package uk.ac.ngs.validation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.DomainValidator;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ngs.common.MutableConfigParams;
import uk.ac.ngs.domain.CSR_Flags;
import uk.ac.ngs.domain.CSR_Flags.Profile;
import uk.ac.ngs.domain.PKCS10_RequestWrapper;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Validate the encoded Subject DN of the CSR (PKCS#10) PEM string provided
 * by {@link PKCS10_RequestWrapper}. Different DN attributes and value formats
 * are valid according to the type of {@link PKCS10_RequestWrapper}.
 * <p>
 * The DN must be RFC2253 with the OIDs in REVERSE structure order, i.e. starting
 * with C and ending with CN with following order (C, O, OU, L, CN):
 * <tt>C=UK,O=eScience,OU=CLRC,L=DL,CN=some valid body</tt>
 * This OID structure order is required by the UK CA.
 *
 * @author David Meredith
 */
public class PKCS10SubjectDNValidator implements Validator {
    private static final Log log = LogFactory.getLog(PKCS10SubjectDNValidator.class);
    // For list of RDN names and corresponding OID see: 
    // http://technet.microsoft.com/en-us/library/cc772812%28WS.10%29.aspx
    private final ASN1ObjectIdentifier cn = new ASN1ObjectIdentifier("2.5.4.3");
    private final ASN1ObjectIdentifier c = new ASN1ObjectIdentifier("2.5.4.6");
    private final ASN1ObjectIdentifier loc = new ASN1ObjectIdentifier("2.5.4.7");
    private final ASN1ObjectIdentifier orgname = new ASN1ObjectIdentifier("2.5.4.10");
    private final ASN1ObjectIdentifier ou = new ASN1ObjectIdentifier("2.5.4.11");
    private final ASN1ObjectIdentifier email = new ASN1ObjectIdentifier("1.2.840.113549.1.9.1");

    //private final Pattern userCN_Pattern = Pattern.compile("[\\-\\(\\)a-zA-Z0-9\\s]+");
    private final Pattern negatedUserCN_Pattern = Pattern.compile("[^A-Za-z0-9\\-\\(\\) ]");
    private final Pattern negatedHostServiceCN_Pattern = Pattern.compile("[^a-z\\-]");
    private final PKCS10Parser csrParser = new PKCS10Parser();
    private final CsrRequestValidationConfigParams validationParams;
    private final EmailValidator emailValidator = new EmailValidator();
    private MutableConfigParams mutableConfigParams;

    /**
     * Construct a new instance.
     *
     * @param validationParams Checks are made against the valid values stored in this object.
     */
    public PKCS10SubjectDNValidator(CsrRequestValidationConfigParams validationParams) {
        this.validationParams = validationParams;
    }

    /**
     * Supported object type: {@link PKCS10_RequestWrapper}.
     *
     * @param type
     * @return
     */
    @Override
    public boolean supports(Class<?> type) {
        return PKCS10_RequestWrapper.class.equals(type);
    }

    /**
     * Validate the given {@link PKCS10_RequestWrapper} object.
     * Important: the error messages added to {@code Errors} may reflect
     * the provided (untrusted) values in the message. In a web context, the
     * calling/client code MUST ensure these error messages are correctly
     * escaped/encoded as per:
     *
     * @param o      Must be a {@link PKCS10_RequestWrapper} instance
     * @param errors
     * @see https://www.owasp.org/index.php/XSS_%28Cross_Site_Scripting%29_Prevention_Cheat_Sheet
     */
    @Override
    public void validate(Object o, Errors errors) {
        PKCS10_RequestWrapper requestWrapper = (PKCS10_RequestWrapper) o;
        String csrPemString = requestWrapper.getCsrPemString();
        CSR_Flags.Profile profile = requestWrapper.getProfile();
        CSR_Flags.Csr_Types csrType = requestWrapper.getCsr_type();

        // parse the pemString
        PKCS10CertificationRequest req = csrParser.parseCsrPemString(csrPemString);
        if (req == null) {
            errors.reject("pkcs10.validation.ioexception", "Invalid PKCS10 - unable to read pem string");
            return;
        }
        // Get the RDNs in structured order  
        X500Name x500name = req.getSubject();
        RDN[] rdn = x500name.getRDNs();

        // validate expected RDN length 
        if (rdn.length != 5 && rdn.length != 6) {
            errors.reject("pkcs10.validation.invalid.rdnlength",
                    "Invalid number of OIDs, expected (C, O, OU, L, CN, [E])");
            return;
        }

        // Validate the RDNs and their ordering. The encoded DN needs to have 
        // OIDs in the following (reverse) order for our CA: (C, O, OU, L, CN) 
        //e.g.  C=UK,O=eScience,OU=CLRC,L=DL,CN=some valid body[,E=dave@world.com]
        if (!c.equals(rdn[0].getFirst().getType())
                || !orgname.equals(rdn[1].getFirst().getType())
                || !ou.equals(rdn[2].getFirst().getType())
                || !loc.equals(rdn[3].getFirst().getType())
                || !cn.equals(rdn[4].getFirst().getType())) {
            errors.reject("pkcs10.validation.invalid.rdnsequence",
                    "Invalid DN sequence order - expected OID structure order (C, O, OU, L, CN)");
            return;
        }
        if (rdn.length == 6) {
            if (!email.equals(rdn[5].getFirst().getType())) {
                errors.reject("pkcs10.validation.invalid.rdnsequence", "Invalid RDN, expected email");
                return;
            }
            // For HOST RENEW, we support PKCS10 requests with/without an 
            // email address encoded in its DN. 
            if (!Profile.UKHOST.equals(profile) && !CSR_Flags.Csr_Types.RENEW.equals(csrType)) {
                errors.reject("pkcs10.validation.invalid.", "Invalid RDN, Email OID is only supported for HOST requets");
                return;
            }
        }

        // Validate the RDN Values
        // Email (optional) 
        if (rdn.length == 6) {
            String emailVal = rdn[5].getFirst().getValue().toString();
            if (!emailValidator.validate(emailVal)) {
                errors.reject("pkcs10.validation.invalid.email", "Invalid email value [" + emailVal + "]");
                return;
            }
        }
        // Country
        String countryVal = rdn[0].getFirst().getValue().toString();
        if (!this.validationParams.getCountryOID().equals(countryVal)) {
            errors.reject("pkcs10.validation.invalid.orgname", "Invalid Country OID value [" + countryVal + "]");
            return;
        }
        // OrgName
        String orgNameVal = rdn[1].getFirst().getValue().toString();
        if (!this.validationParams.getOrgNameOID().equals(orgNameVal)) {
            errors.reject("pkcs10.validation.invalid.country", "Invalid OrgName OID value [" + orgNameVal + "]");
            return;
        }
        // OU
        String ouVal = rdn[2].getFirst().getValue().toString();
        boolean isValidOu = validationParams.getValidOrgUnits().contains(ouVal);
        if (!isValidOu) {
            errors.reject("pkcs10.validation.invalid.orgname", "Invalid OrgUnit OID value [" + ouVal + "]");
            return;
        }
        // L  
        String locVal = rdn[3].getFirst().getValue().toString();
        boolean isValidLoc = validationParams.getValidLocalities().contains(locVal);
        if (!isValidLoc) {
            errors.reject("pkcs10.validation.invalid.loc", "Invalid LocalityName OID value [" + locVal + "]");
            return;
        }
        // CN
        String cnVal = rdn[4].getFirst().getValue().toString();
        isValidCN(cnVal, profile, errors);
        if (errors.hasErrors()) {
            return;
        }

    }

    private void isValidCN(String testCN, Profile profile, Errors errors) {
        if (testCN == null || testCN.trim().length() == 0) {
            errors.reject("pkcs10.validation.invalid.cn", "Invalid CN value [" + testCN + "]");
            return;
        }
        // check CN does not have leading/trailing whitespace 
        if (!testCN.trim().equals(testCN)) {
            errors.reject("pkcs10.validation.invalid.cn",
                    "Invalid CN value [" + testCN + "]");
            return;
        }
        // check CN does not have whitespace other than single spaces 
        // (e.g. tabs, multiple consecutive spaces are illegal)
        if (testCN.contains("  ") || testCN.contains("\t")) {
            errors.reject("pkcs10.validation.invalid.cn",
                    "Invalid CN value contains illegal whitespace (tabs, multiple consecutive whitespace)");
            return;
        }
        // newline and carriage return are illegal (doubt this would ever happen) 
        if (testCN.contains("\n") || testCN.contains("\r")) {
            errors.reject("pkcs10.validation.invalid.cn",
                    "Invalid CN value contains illegal newline/carriage return");
            return;
        }


        // check if CN has a CAPS letter (not allowed in CN)
        for (int i = 0; i < testCN.length(); ++i) {
            char c = testCN.charAt(i);
            if (Character.isLetter(c) && Character.isUpperCase(c)) {
                errors.reject("pkcs10.validation.invalid.cn", "Invalid CN value contains CAPS");
                return;
            }
        }

        if (Profile.UKPERSON.equals(profile)) {
            if (negatedUserCN_Pattern.matcher(testCN).find()) {
                errors.reject("pkcs10.validation.invalid.cn", "Invalid CN value - found illegal chars");
                return;
            }
            String[] names = testCN.split("\\s");
            // Must be at least TWO names 
            if (names.length < 2) {
                errors.reject("pkcs10.validation.invalid.cn", "Invalid CN value - need at least two names ");
                return;
            }
            // At least TWO of these names must have length TWO OR MORE
            int ii = 0;
            for (String name : names) {
                if (name.length() >= 2) {
                    ++ii;
                }
            }
            //return ii >= 2;
            if (!(ii >= 2)) {
                errors.reject("pkcs10.validation.invalid.cn", "Invalid CN value - At least two names need two or more chars");
            }

        } else if (Profile.UKHOST.equals(profile)) {
            // Cater for host CNs with a service, e.g. 'service/host.domain.ac.uk'
            if (testCN.contains("/")) {
                String[] serviceHostName = testCN.split("/");
                if (serviceHostName.length != 2) {
                    errors.reject("pkcs10.validation.invalid.cn", "Invalid CN value, required 'service/valid.dns.name' [" + testCN + "]");
                }
                if (!DomainValidator.getInstance().isValid(serviceHostName[1])) {
                    errors.reject("pkcs10.validation.invalid.cn", "Invalid CN value, invaild domain [" + serviceHostName[1] + "]");
                }
                // if mutableConfigParams has been set, then validate 
                // against the list of valid service values (if any) 
                if (this.mutableConfigParams != null) {
                    String[] services = new String[0];
                    String servicesParam;
                    try {
                        servicesParam = this.mutableConfigParams.getProperty("hostname.service.values");
                        if (servicesParam != null) {
                            services = servicesParam.split(",");
                        }
                    } catch (IOException ex) {
                        throw new IllegalArgumentException(ex);
                    }
                    if (services.length > 0) {
                        if (!Arrays.asList(services).contains(serviceHostName[0])) {
                            errors.reject("pkcs10.validation.invalid.cn",
                                    "Invalid CN value - Unsupported Service. Allowed values [" + servicesParam + "]");
                        }
                    }
                } else {
                    // if not, then validate against a pattern as a fallback 
                    log.debug("mutableConfigParams is null, fallback to validate CN with regex");
                    if (negatedHostServiceCN_Pattern.matcher(serviceHostName[0]).find()) {
                        errors.reject("pkcs10.validation.invalid.cn", "Invalid CN service value [" + testCN + "]");
                    }
                }
            } else {
                if (!DomainValidator.getInstance().isValid(testCN)) {
                    errors.reject("pkcs10.validation.invalid.cn", "Invalid CN domain value [" + testCN + "]");
                }
            }
        }
    }

    /**
     * Optional: specifies an optional set of service values for service/host certificates.
     *
     * @param mutableConfigParams
     */
    @Inject
    public void setMutableConfigParams(MutableConfigParams mutableConfigParams) {
        this.mutableConfigParams = mutableConfigParams;
    }

}
