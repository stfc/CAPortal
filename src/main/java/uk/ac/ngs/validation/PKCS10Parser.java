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

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to parse a string to PKCS10 certification request.
 * Stateless and thread safe.
 *
 * @author David Meredith
 */
public class PKCS10Parser {


    /**
     * Parse the given PKCS10 pem string.
     *
     * @param csrPemStr Must be a valid pem string.
     * @return PKCS10 object or null if can't bind the given csrPem String
     */
    public PKCS10CertificationRequest parseCsrPemString(String csrPemStr) {
        try (PemReader pemReader = new PemReader(new StringReader(csrPemStr))) {
            PemObject obj = pemReader.readPemObject();
            if (obj == null) {
                return null;
            }
            byte[] pembytes = obj.getContent();
            // Create the PKCS10
            return new PKCS10CertificationRequest(pembytes);
        } catch (IOException ex) {
            Logger.getLogger(PKCS10Parser.class.getName()).log(Level.WARNING,
                    null, ex);
        }
        /* have tried to be good citizen*/
        return null;
    }
}
