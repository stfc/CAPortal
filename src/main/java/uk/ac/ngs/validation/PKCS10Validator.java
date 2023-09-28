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

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.SignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validate a PKCS#10 certification request (CSR) PEM <code>String</code>.
 * <p>
 * Typical usage outside of the Spring context requires you create an
 * {@link org.springframework.validation.Errors} implementation. You can use the
 * Spring MapBindingResult as shown below:
 * <code>
 * PKCS10Validator validator = new PKCS10Validator(validationConfigParams);
 * Errors errors = new MapBindingResult(new HashMap<String, String>(), "csrPemStrRequest");
 * validator.validate(csrPemStr, errors);
 * if(errors.hasErrors()) {  }
 * </code>
 *
 * @author David Meredith
 */
public class PKCS10Validator implements Validator {

    // In future we could inject the required parameters 
    private final SignatureAlgorithmIdentifierFinder algFinder = new DefaultSignatureAlgorithmIdentifierFinder();
    private final AlgorithmIdentifier algIdExpected = algFinder.find("SHA256WITHRSAENCRYPTION"); // or "SHA1withRSA", "SHA1withRSAEncryption"
    private final PKCS10Parser csrParser = new PKCS10Parser();
    private final CsrRequestValidationConfigParams validationConfigParams;

    // http://stackoverflow.com/questions/12146298/spring-mvc-how-to-perform-validation
    // http://www.dzone.com/tutorials/java/spring/spring-form-validation-1.html
    // http://stackoverflow.com/questions/9607491/using-spring-validator-outside-of-the-context-of-spring-mvc


    /**
     * Construct a new instance and specify allowed values in the given {@link CsrRequestValidationConfigParams}
     *
     * @param validationConfigParams
     */
    public PKCS10Validator(CsrRequestValidationConfigParams validationConfigParams) {
        this.validationConfigParams = validationConfigParams;
    }

    @Override
    public boolean supports(Class<?> type) {
        return String.class.equals(type);
    }

    @Override
    public void validate(Object csrPemStr, Errors e) {
        try {
            //http://stackoverflow.com/questions/11028932/how-to-get-publickey-from-pkcs10certificationrequest-using-new-bouncy-castle-lib
            //http://comments.gmane.org/gmane.comp.encryption.bouncy-castle.devel/10582

            // Convert from pem string to bytes 
            PKCS10CertificationRequest req = csrParser.parseCsrPemString((String) csrPemStr);
            if (req == null) {
                e.reject("pkcs10.validation.ioexception", "Invalid PKCS10 - unable to read pem string");
                return;
            }

            JcaPKCS10CertificationRequest jcaReq = new JcaPKCS10CertificationRequest(req);

            // Test the algorithm of the request (we expect SHA1 with RSA)  
            AlgorithmIdentifier reqSigAlgId = req.getSignatureAlgorithm();
            if (!reqSigAlgId.getAlgorithm().getId().equals(algIdExpected.getAlgorithm().getId())) {
                e.reject("pkcs10.validation.failed", "Invalid PKCS10 signature - Required SHA256withRSA");
                return;
            }

            // Test the RSA modulus strength for minimum level 
            //SubjectPublicKeyInfo pkInfo = req.getSubjectPublicKeyInfo();
            PublicKey rsaPub = jcaReq.getPublicKey();
            RSAPublicKey rsaKey = (RSAPublicKey) rsaPub;
            if (rsaKey.getModulus().bitLength() < validationConfigParams.getMinModulus()) {
                e.reject("pkcs10.validation.failed", "Invalid PKCS10 - Key modulus bit strength too low");
                return;
            }

            if (rsaKey.getPublicExponent().intValue() < validationConfigParams.getMinExponent()) {
                e.reject("pkcs10.validation.failed", "Invalid PKCS10 - PublicExponent too small");
                return;
            }

            //RSAPublicKeySpec rsaSpec = new RSAPublicKeySpec(rsa.getModulus(), rsa.getExponent());
            // Verify the signature
            boolean valid = req.isSignatureValid((new JcaContentVerifierProviderBuilder()).build(rsaPub));
            if (!valid) {
                e.reject("pkcs10.validation.failed", "Invalid PKCS10 - Could not validate signature");
                return;
            }

        } catch (InvalidKeyException ex) {
            Logger.getLogger(PKCS10Validator.class.getName()).log(Level.WARNING, null, ex);
            e.reject("pkcs10.validation.invalidkeyspecexception", "Invalid PKCS10 key");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(PKCS10Validator.class.getName()).log(Level.WARNING, null, ex);
            e.reject("pkcs10.validation.nosuchalgorithmException", "Invalid PKCS10 No Such Algorithm");
        } catch (OperatorCreationException ex) {
            Logger.getLogger(PKCS10Validator.class.getName()).log(Level.WARNING, null, ex);
            e.reject("pkcs10.validation.operatorcreationexception", "Invalid PKCS10");
        } catch (PKCSException ex) {
            Logger.getLogger(PKCS10Validator.class.getName()).log(Level.WARNING, null, ex);
            e.reject("pkcs10.validation.pkcsexception", "Invalid PKCS10");
        }
    }

}
