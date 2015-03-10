/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package uk.ac.ngs.validation;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

/**
 * Class to parse a string to PKCS10 certification request. 
 * Stateless and thread safe. 
 * 
 * @author David Meredith 
 */
public class PKCS10Parser {
   

    /**
     * Parse the given PKCS10 pem string. 
     * @param csrPemStr Must be a valid pem string. 
     * @return PKCS10 object or null if can't bind the given csrPem String 
     */
	public PKCS10CertificationRequest parseCsrPemString(String csrPemStr) {
		PemReader pemReader = null;
		try {
			pemReader = new PemReader(new StringReader((String) csrPemStr));
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
		} finally {
			try { pemReader.close(); } catch (IOException e) { /* have tried to be good citizen*/ }
		}
		return null;
	}
}
