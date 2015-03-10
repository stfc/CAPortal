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

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.security.auth.x500.X500Principal;
// ************ Use these imports if using 
// bcprov-jdk15-1.45.jar ************** 
/*import java.io.StringReader;
 import java.security.Provider;
 import java.security.PublicKey;
 import java.security.interfaces.RSAPublicKey;
 import org.bouncycastle.jce.PKCS10CertificationRequest;
 import org.bouncycastle.openssl.PEMReader;
 import org.junit.After;
 import org.junit.AfterClass;
 import org.junit.Before;
 import org.junit.BeforeClass;
 import org.junit.Test;
 import static org.junit.Assert.*;*/
// ****************************************************************************
// ************ Use these imports if using 
// bcprov-jdk15on-1.50.jar and bcpkix-jdk15on-1.50.jar ************** 

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.SignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
// ****************************************************************************

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import uk.ac.ngs.domain.CSR_Flags;
import uk.ac.ngs.domain.PKCS10_RequestWrapper;
import uk.ac.ngs.validation.CsrRequestValidationConfigParams;
import uk.ac.ngs.validation.PKCS10Parser;
import uk.ac.ngs.validation.PKCS10SubjectDNValidator;
import uk.ac.ngs.validation.PKCS10Validator;
import static org.mockito.Mockito.*;
import uk.ac.ngs.dao.JdbcRalistDao;
import uk.ac.ngs.domain.RalistRow;

/**
 * Test the BC API to parse a PKCS#10 CSR pem request and get attributes.
 *
 * There are two versions of the main test method. Please uncomment the version
 * for your implementation of BC (also uncomment the imports accordingly).
 * <ul>
 * <li>testCreateCsrFromPemVia_bcprov_jdk15on_1_50() (for:
 * bcprov-jdk15on-1.50.jar and bcpkix-jdk15on-1.50.jar)</li>
 * <li>testReCreateCsrFromPemVia_bcprov_jdk15_145() (for:
 * bcprov-jdk15-1.45.jar)</li>
 * </ul>
 *
 * @author David Meredith
 */
public class TestBC_PKCS10 {

    private final ASN1ObjectIdentifier cn = new ASN1ObjectIdentifier("2.5.4.3");
    private final ASN1ObjectIdentifier c = new ASN1ObjectIdentifier("2.5.4.6");
    private final ASN1ObjectIdentifier loc = new ASN1ObjectIdentifier("2.5.4.7");
    private final ASN1ObjectIdentifier orgname = new ASN1ObjectIdentifier("2.5.4.10");
    private final ASN1ObjectIdentifier ou = new ASN1ObjectIdentifier("2.5.4.11");
    private final ASN1ObjectIdentifier email = new ASN1ObjectIdentifier("1.2.840.113549.1.9.1");
    private final PKCS10Parser csrParser = new PKCS10Parser();

    public TestBC_PKCS10() {
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

    private JdbcRalistDao createMockJdbcRalistDao(String l, String ou) {
        // Mock the RalistDao object 
        List<RalistRow> raListRow = new ArrayList<RalistRow>();
        RalistRow row = new RalistRow();
        row.setActive(Boolean.TRUE);
        row.setL(l);
        row.setOu(ou);
        raListRow.add(row);
        JdbcRalistDao daoMock = mock(JdbcRalistDao.class);
        when(daoMock.findAllByActive(true, null, null)).thenReturn(raListRow);
        return daoMock;
    }


    /*@Test
    public void createCSR() throws Exception {
        KeyPair pair = generateKeyPair();
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                new X500Principal("CN=Requested Test Certificate"), pair.getPublic());
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256withRSA");
        ContentSigner signer = csBuilder.build(pair.getPrivate());
        PKCS10CertificationRequest csr = p10Builder.build(signer);
    }*/

    @Test
    public void expectedX500ToString() {
        X500Principal xname = new X500Principal("CN=some valid body,L=DL,OU=CLRC,O=eScience,C=UK");
        assertEquals("CN=some valid body, L=DL, OU=CLRC, O=eScience, C=UK", xname.toString()); 
    }

    @Test
    public void testCsrWithValidRdnStructureOrder()throws Exception {
        String csrPemStr = this.getCsrWithReverseDnStructureOrder();
        PKCS10CertificationRequest req = csrParser.parseCsrPemString(csrPemStr);
        X500Name xname = req.getSubject();
        //System.out.println("" + xname.toString());
        assertEquals("C=UK,O=eScience,OU=CLRC,L=DL,CN=some valid body", xname.toString());
        RDN[] rdn = req.getSubject().getRDNs(); // return an array of RDNs in structure order.
        /*for (int i = 0; i < rdn.length; i++) {
         //System.out.println("ASN1: "+rdn[i].toASN1Primitive());
         AttributeTypeAndValue tv = rdn[i].getFirst();
         System.out.println("t: " + tv.getType().toString() + " v: " + tv.getValue().toString());
         }*/
        // Note the order of the RDNs reflects the X500Name
        assertEquals(rdn[0].getFirst().getType(), c);
        assertEquals(rdn[1].getFirst().getType(), orgname);
        assertEquals(rdn[2].getFirst().getType(), ou);
        assertEquals(rdn[3].getFirst().getType(), loc);
        assertEquals(rdn[4].getFirst().getType(), cn);

         // Validate 
        CsrRequestValidationConfigParams params = new CsrRequestValidationConfigParams("UK", "eScience");
        params.setRalistDao(this.createMockJdbcRalistDao("DL", "CLRC"));
        PKCS10SubjectDNValidator validator = new PKCS10SubjectDNValidator(params);
        Errors errors = new MapBindingResult(new HashMap<String, String>(), "csrPemStr");
        PKCS10_RequestWrapper.Builder builder = new PKCS10_RequestWrapper.Builder(CSR_Flags.Csr_Types.NEW, CSR_Flags.Profile.UKPERSON, csrPemStr, "somebody@world.com"); 
        PKCS10_RequestWrapper csrWrapper = builder.build();
        validator.validate(csrWrapper, errors);
        assertTrue(!errors.hasErrors());
    }



    @Test
    public void testCsrWithInvalidRdnStructureOrder() throws Exception {
        // Create the validator 
        CsrRequestValidationConfigParams params = new CsrRequestValidationConfigParams("UK", "eScience");
        params.setRalistDao(this.createMockJdbcRalistDao("DL", "CLRC"));
        PKCS10SubjectDNValidator validator = new PKCS10SubjectDNValidator(params);
        Errors errors = new MapBindingResult(new HashMap<String, String>(), "csrPemStr");
        // CSR DN has following order (this is invalid for our DB): 
        // CN=some valid body,L=DL,OU=CLRC,O=eScience,C=UK 
        String csrPemStr = this.getCsrRegularDnStructureOrder();
        PKCS10_RequestWrapper.Builder builder = new PKCS10_RequestWrapper.Builder(CSR_Flags.Csr_Types.NEW, CSR_Flags.Profile.UKPERSON, csrPemStr, "somebody@world.com"); 
        PKCS10_RequestWrapper csrWrapper = builder.build();
        validator.validate(csrWrapper, errors);
        assertTrue(errors.hasErrors());
        //System.out.println("D: " + errors.getAllErrors().get(0).getDefaultMessage());
    }

    @Test
    public void testInvalidCountryOID()throws Exception {
        CsrRequestValidationConfigParams params = new CsrRequestValidationConfigParams("SomeOtherCountry", "eScience");
        params.setRalistDao(this.createMockJdbcRalistDao("DL", "CLRC"));
        PKCS10SubjectDNValidator validator = new PKCS10SubjectDNValidator(params);
        Errors errors = new MapBindingResult(new HashMap<String, String>(), "csrPemStr");
        // CN=some valid body,L=DL,OU=CLRC,O=eScience,C=UK 
        String csrPemStr = this.getCsrRegularDnStructureOrder();
        PKCS10_RequestWrapper.Builder builder = new PKCS10_RequestWrapper.Builder(CSR_Flags.Csr_Types.NEW, CSR_Flags.Profile.UKPERSON, csrPemStr, "somebody@world.com"); 
        PKCS10_RequestWrapper csrWrapper = builder.build();
        validator.validate(csrWrapper, errors);
        assertTrue(errors.hasErrors());
    }

    @Test
    public void testInvalidRFC2253_RDN_Order()throws Exception  {
        CsrRequestValidationConfigParams params = new CsrRequestValidationConfigParams("UK", "eScience");
        params.setRalistDao(this.createMockJdbcRalistDao("DL", "CLRC"));
        PKCS10SubjectDNValidator validator = new PKCS10SubjectDNValidator(params);
        Errors errors = new MapBindingResult(new HashMap<String, String>(), "csrPemStr");
        // CSR DN has an invalid RFC2253 order: 
        // CN=some body,C=UK,L=DL,O=eScience,OU=CLRC
        // Needs to be: (CN=val,L=val,OU=val,O=val,C=val) 
        String csrPemStr = this.getCsrIrregularOrderDN();
        PKCS10_RequestWrapper.Builder builder = new PKCS10_RequestWrapper.Builder(CSR_Flags.Csr_Types.NEW, CSR_Flags.Profile.UKPERSON, csrPemStr, "somebody@world.com"); 
        PKCS10_RequestWrapper csrWrapper = builder.build();
        validator.validate(csrWrapper, errors);
        assertTrue(errors.hasErrors());
        //System.out.println("" + errors.getAllErrors().get(0).getDefaultMessage());
    }

    @Test
    public void testInvalidRFC2253_RDN_Count()throws Exception {
        CsrRequestValidationConfigParams params = new CsrRequestValidationConfigParams("UK", "eScienceDev");
        params.setRalistDao(this.createMockJdbcRalistDao("MC", "Manchester"));
        PKCS10SubjectDNValidator validator = new PKCS10SubjectDNValidator(params);
        Errors errors = new MapBindingResult(new HashMap<String, String>(), "csrPemStr");
        // CSR has invalid RFC2253 (E is not supported) 
        // C=UK,O=eScienceDev,OU=Manchester,L=MC,CN=grid course eight,E=leith_d@hotmail.com 
        String csrPemStr = this.getCsrWithReverseDnStructureOrder_AndEmail();
        PKCS10_RequestWrapper.Builder builder = new PKCS10_RequestWrapper.Builder(CSR_Flags.Csr_Types.NEW, CSR_Flags.Profile.UKPERSON, csrPemStr, "somebody@world.com"); 
        PKCS10_RequestWrapper csrWrapper = builder.build();
        validator.validate(csrWrapper, errors);
        assertTrue(errors.hasErrors());
        //System.out.println("" + errors.getAllErrors().get(0).getDefaultMessage());
    }

    @Test
    public void testParseCsrRdnWithEMail() {
        String csrPemStr = this.getCsrWithReverseDnStructureOrder_AndEmail();
        PKCS10CertificationRequest req = csrParser.parseCsrPemString(csrPemStr);
        X500Name xname = req.getSubject();

        System.out.println("" + xname.toString());
        assertEquals("C=UK,O=eScienceDev,OU=Manchester,L=MC,CN=grid course eight,E=leith_d@hotmail.com", xname.toString());
        RDN[] rdn = req.getSubject().getRDNs(); // return an array of RDNs in structure order.
        assertEquals(rdn[5].getFirst().getType(), email);
        /*for (int i = 0; i < rdn.length; i++) {
         //System.out.println("ASN1: "+rdn[i].toASN1Primitive());
         AttributeTypeAndValue tv = rdn[i].getFirst();
         System.out.println("t: " + tv.getType().toString() + " v: " + tv.getValue().toString());
         }*/
        // Note the order of the RDNs reflects the X500Name
        assertEquals(rdn[0].getFirst().getType(), c);
        assertEquals(rdn[1].getFirst().getType(), orgname);
        assertEquals(rdn[2].getFirst().getType(), ou);
        assertEquals(rdn[3].getFirst().getType(), loc);
        assertEquals(rdn[4].getFirst().getType(), cn);
        assertEquals(rdn[5].getFirst().getType(), email);
    }

    @Test
    public void testParseCsrRdn() {
        String csrPemStr = this.getCsrIrregularOrderDN();
        PKCS10CertificationRequest req = csrParser.parseCsrPemString(csrPemStr);
        X500Name xname = req.getSubject();

        System.out.println("" + req.getSubject().toString());
        assertEquals("CN=some body,C=UK,L=DL,O=eScience,OU=CLRC", xname.toString());
        RDN[] rdn = req.getSubject().getRDNs(); // return an array of RDNs in structure order.
        /*for (int i = 0; i < rdn.length; i++) {
         AttributeTypeAndValue tv = rdn[i].getFirst();
         System.out.println("t: " + tv.getType().toString() + " v: " + tv.getValue().toString());
         //AttributeTypeAndValue[] tvs = rdn[i].getTypesAndValues(); 
         //for(int ii=0; ii<tvs.length; ii++){
         //    System.out.println("t: "+tvs[ii].getType().toString()+" v: "+tvs[ii].getValue().toString());
         //}
         }*/
        // Note the order of the RDNs reflects the X500Name
        assertEquals(rdn[0].getFirst().getType(), cn);
        assertEquals(rdn[1].getFirst().getType(), c);
        assertEquals(rdn[2].getFirst().getType(), loc);
        assertEquals(rdn[3].getFirst().getType(), orgname);
        assertEquals(rdn[4].getFirst().getType(), ou);
//       ASN1ObjectIdentifier[] ids = xname.getAttributeTypes();
//       for(int i=0; i<ids.length; i++){
//           System.out.println(""+ids[i].getId());
//       }
    }

    @Test
    public void testPKCS10Validator() throws Exception {
        String csrPemStr = this.getCsrIrregularOrderDN();
        CsrRequestValidationConfigParams params = new CsrRequestValidationConfigParams("UK", "eScienceDev");

        PKCS10Validator validator = new PKCS10Validator(params);
        assertTrue(validator.supports(csrPemStr.getClass()));
        Errors errors = new MapBindingResult(new HashMap<String, String>(), "csrPemStr");
        validator.validate(csrPemStr, errors);
        assertTrue(errors.hasErrors() == false);

        validator.validate("invalid csr string", errors);
        assertTrue(errors.hasErrors());
        //System.out.println(errors.getAllErrors().get(0)); 

    }

    /**
     * Read the pkcs#10 CSR pem text and create a BC CSR object for subsequent
     * verifying and manipulation. This test uses bcprov-jdk15on-1.50.jar and
     * bcpkix-jdk15on-1.50.jar
     *
     * @throws Exception
     */
    @Test
    public void testCreateCsrFromPemV2_bcprov_jdk15on_1_50() throws Exception {
        SignatureAlgorithmIdentifierFinder algFinder = new DefaultSignatureAlgorithmIdentifierFinder();
        String csrPemStr = this.getCsrIrregularOrderDN();

        PemReader pemReader = new PemReader(new StringReader(csrPemStr));
        PemObject obj = pemReader.readPemObject();
        pemReader.close();
        byte[] pembytes = obj.getContent();
        PKCS10CertificationRequest req = new PKCS10CertificationRequest(pembytes);

        assertEquals("CN=some body,C=UK,L=DL,O=eScience,OU=CLRC", req.getSubject().toString());
        //System.out.println(req.getSubject().toString());
        SubjectPublicKeyInfo pkInfo = req.getSubjectPublicKeyInfo();

        // get the algorithm of the pubkey RSA  
        AlgorithmIdentifier pubKeyAlgId = pkInfo.getAlgorithm();
        //System.out.println(pubKeyAlgId.getAlgorithm().getId());   // 1.2.840.113549.1.1.1
        //System.out.println(pubKeyAlgId.getAlgorithm().toString());// 1.2.840.113549.1.1.1
        assertEquals("1.2.840.113549.1.1.1", pubKeyAlgId.getAlgorithm().getId());

        // get the algorithm of the request (we expect SHA1 with RSA)  
        AlgorithmIdentifier reqSigAlgId = req.getSignatureAlgorithm();
        AlgorithmIdentifier algIdExpected = algFinder.find("SHA1WITHRSAENCRYPTION"); // or "SHA1withRSA", "SHA1withRSAEncryption" 
        assertEquals(reqSigAlgId.getAlgorithm().getId(), algIdExpected.getAlgorithm().getId());
        assertEquals("1.2.840.113549.1.1.5", algIdExpected.getAlgorithm().getId());

        // Get java.security.PublicKey so we can validate and get the DB formatted pub key  
        // Looks like we can get the public key in two ways: 
        //
        // 1) 
        //http://stackoverflow.com/questions/11028932/how-to-get-publickey-from-pkcs10certificationrequest-using-new-bouncy-castle-lib
        RSAKeyParameters rsa = (RSAKeyParameters) PublicKeyFactory.createKey(pkInfo);
        assertEquals(2048, rsa.getModulus().bitLength());
        RSAPublicKeySpec rsaSpec = new RSAPublicKeySpec(rsa.getModulus(), rsa.getExponent());
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pubKey1 = kf.generatePublic(rsaSpec);
        String pubkey1DBFormat = getDBFormattedRSAPublicKey(pubKey1);
        boolean valid = req.isSignatureValid((new JcaContentVerifierProviderBuilder()).build(pubKey1));
        assertTrue(valid);
        //
        // 2) 
        JcaPKCS10CertificationRequest jcaReq = new JcaPKCS10CertificationRequest(req);
        PublicKey pubKey2 = jcaReq.getPublicKey();
        RSAPublicKey rsa2 = (RSAPublicKey) pubKey2;
        assertEquals(2048, rsa2.getModulus().bitLength());

        String pubkey2DBFormat = getDBFormattedRSAPublicKey(pubKey2);
        assertTrue(jcaReq.isSignatureValid((new JcaContentVerifierProviderBuilder()).build(pubKey2)));

        // Test that the two DB formated pubkeys are the same 
        assertEquals(pubkey1DBFormat, pubkey2DBFormat);

    }

    /**
     * Read the pkcs#10 CSR pem text (created with Forge) and re-create the CSR
     * as a BC CSR object for subsequent verifying and manipulation. This test
     * uses the (now deprecated) bcprov-jdk15-1.45.jar.
     *
     * @throws Exception
     */
    /*@Test
     public void testReCreateCsrFromPemVia_bcprov_jdk15_145() throws Exception {
     Provider prov = new org.bouncycastle.jce.provider.BouncyCastleProvider();
     java.security.Security.insertProviderAt(prov, 1);

     String csrPemStr = this.getForgeCreatedCsrPem2048();
     PEMReader pemReader = new PEMReader(new StringReader(csrPemStr));
     Object obj = pemReader.readObject();
     PKCS10CertificationRequest req = (PKCS10CertificationRequest) obj;
     req.verify(); 
     PublicKey pkey = req.getPublicKey();

     String dn = req.getCertificationRequestInfo().getSubject().toString();
     assertEquals("CN=some body,C=UK,L=DL,O=eScience,OU=CLRC", dn);
     assertTrue("RSA".equals(pkey.getAlgorithm()));

     RSAPublicKey rsaPkey = (RSAPublicKey) pkey;
     assertEquals(2048, rsaPkey.getModulus().bitLength());
     //System.out.println(rsaPkey.getModulus().bitLength());
     //System.out.println(rsaPkey.getPublicExponent().intValue());  
     System.out.println(getDBFormattedRSAPublicKey(pkey)); 
     }*/
    public static String getDBFormattedRSAPublicKey(PublicKey publicKey) {
        // modu, expHex and expDec are the same regardless of security provider: 
        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;

        //  get the (lower case) hex formatted modulus (16 is the radix for hex) 
        String modulusHexString = rsaPublicKey.getModulus().toString(16);
        int modulusBitLength = rsaPublicKey.getModulus().bitLength(); //e.g. 2048
        String exponentAsHex = rsaPublicKey.getPublicExponent().toString(16); //e.g. 10001
        String exponentAsDecimal = rsaPublicKey.getPublicExponent().toString();//e.g. 65537

        modulusHexString = leadPad(modulusHexString);

        return convertToCA_DB_Format(modulusHexString, exponentAsHex, exponentAsDecimal, modulusBitLength);
    }

    /**
     * Pad the given hex formatted modulus with a leading 00 if and only if the
     * most significant byte is >= 128 (0x80).
     *
     * @param modulusHexString
     * @return modulus hex string with leading padding.
     */
    private static String leadPad(String modulusHexString) {
        String firstByte = "0x" + modulusHexString.substring(0, 2);
        int _int = Integer.decode(firstByte).intValue();
        if (_int >= 128) {
            modulusHexString = "00" + modulusHexString;
        }
        return modulusHexString;
    }

    private static String convertToCA_DB_Format(String modulusHexString,
            String exponentAsHex, String exponentAsDecimal, int modulusBitLength) {

        int length = modulusHexString.length();
        String keyFormatString = "Modulus (" + modulusBitLength + " bit):\n" + "    ";

        for (int i = 0, j = 1; i < length; i = i + 2, j++) {
            if (j == 15) {
                j = 0;
                keyFormatString = keyFormatString + modulusHexString.substring(i, i + 2) + ":\n" + "    ";
            } else {
                if (i == (length - 2)) {
                    keyFormatString = keyFormatString + modulusHexString.substring(i, i + 2);
                } else {
                    keyFormatString = keyFormatString + modulusHexString.substring(i, i + 2) + ":";
                }
            }
        }
        return keyFormatString + "\n" + "Exponent: " + exponentAsDecimal + " (0x" + exponentAsHex + ")" + "\n";

    }


    /**
     * Pem has following DN: <tt>C=UK,O=eScience,OU=CLRC,L=DL,CN=some valid body</tt>. 
     * Note the OID structure order of the DN is in reverse (starting with a C ending in CN) 
     * @return PEM string.
     */
    private String getCsrWithReverseDnStructureOrder() {
        return "-----BEGIN CERTIFICATE REQUEST-----\n"
                + "MIICzzCCAbcCAQAwVjELMAkGA1UEBhMCVUsxETAPBgNVBAoTCGVTY2llbmNlMQ0w\n"
                + "CwYDVQQLEwRDTFJDMQswCQYDVQQHEwJETDEYMBYGA1UEAxMPc29tZSB2YWxpZCBi\n"
                + "b2R5MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn8sx6eHl2ZD8JpbX\n"
                + "Ch/ej49uYrjrqDfJamZ+eQHA+O+337BRVIOU+ra2g5uxWaSF7nXZ5arYjhAJmhhL\n"
                + "raDpXjVIOmJv/6+++khKyaYZpr5j83ue0o3Mw8Xl2pC5XsNtRWHDWlPWcAoI3lcJ\n"
                + "azPpNjCzX+ZDxY7jNpz30KKuaIKPFTn9eyIqSQwC/xxmtSr+G2a+/DJiMk1vbsTp\n"
                + "SPo9dghKvgjAQeWEtvccgVH6rif/dEYatOo9lcPEG8P+S35zte2vuYZ//UEN804T\n"
                + "A66vJxfjk0M9JyeujFknhSOxlqQ/l5rad1Hj6NzZ0/hmm/vN74Hjz2C05QJyokTG\n"
                + "LaocUQIDAQABoDQwFwYJKoZIhvcNAQkHMQoMCHBhc3N3b3JkMBkGCSqGSIb3DQEJ\n"
                + "AjEMDApNeSBjb21wYW55MA0GCSqGSIb3DQEBBQUAA4IBAQBlBzQJiK8vGanOw31k\n"
                + "zEEhodNvuaOoelWPI8ySuTEzqDFakGmRZ7bsRqRgODn5BHgoS7xTrbCGTazqRkzy\n"
                + "hDazSTmxJQlu9jyI/oXEA5GJnim/tPfMvYB4F/y7Fvr7BAymwZSf5miOa/rshcbd\n"
                + "7+4F70zU4YpsySCn2bp4GYWVI5CmCEBoGbFnX3+3va7oQKoCmv1nuf+3yFkAxYsX\n"
                + "ARr0zpCVs9SYkXciup/gBU4NFD8BPow5cN+lR8936HtQTvhK6T0HeJMPMWHuRvd2\n"
                + "yr/H1bltazW8pw8Kz9doNbrZ30szHR4MciQzAwDzLVCgH48DA5eHA2AhNf2Uw8eH\n"
                + "EfQ7\n"
                + "-----END CERTIFICATE REQUEST-----";
    }
        
    /**
     * Pem has following DN: <tt>C=UK,O=eScienceDev,OU=Manchester,L=MC,CN=grid course eight,E=leith_d@hotmail.com</tt>. 
     * Note the OID structure order of the DN is in reverse (starting with a C ending in CN) 
     * @return PEM string.
     */
    private String getCsrWithReverseDnStructureOrder_AndEmail() {
        return "-----BEGIN CERTIFICATE REQUEST-----\n"
                + "MIIBxjCCAS8CAQAwgYUxCzAJBgNVBAYTAlVLMRQwEgYDVQQKEwtlU2NpZW5jZURl\n"
                + "djETMBEGA1UECxMKTWFuY2hlc3RlcjELMAkGA1UEBxMCTUMxGjAYBgNVBAMTEWdy\n"
                + "aWQgY291cnNlIGVpZ2h0MSIwIAYJKoZIhvcNAQkBFhNsZWl0aF9kQGhvdG1haWwu\n"
                + "Y29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC0aetX77NN141n0QzFk5q/\n"
                + "IBzaVVdIBCIQL/xRiaxrN4QNoD75nOzasjc/VzupfXqgtmIhTU5lzq+osyo74QeZ\n"
                + "bIWFhjIFqfiGlePpQuF0ZQiEOKOcILlBSm+PF1wKYkfkpOY6lI+06rpHPYLMkrHk\n"
                + "LzGdhgTcTQxBTtlDiVsdFQIDAQABoAAwDQYJKoZIhvcNAQEEBQADgYEAb3tKX+sg\n"
                + "J7tlQmfpXU/IGeUPliEaoCkFc2e3f+t+k7fEwKhXvSHbQwqpy981wGuIVGg9s+UA\n"
                + "sHATIcoiocfP0XsB+VPp5+HTSn05mz45zHrsaE8CDGlzuFzeyTWFbhe/6GbV3U+Z\n"
                + "G0QYlPVSPx1DmH+zMhwsiopOm6fwlQEfkIg=\n"
                + "-----END CERTIFICATE REQUEST-----";
    }

    /**
     * Pem has following DN: <tt>CN=some body,C=UK,L=DL,O=eScience,OU=CLRC</tt>. 
     * Note the CSR DN OID structure order is irregular.
     * @return PEM string.
     */
    private String getCsrIrregularOrderDN() {
        String csrPem = "-----BEGIN CERTIFICATE REQUEST-----\n"
                + "MIICyTCCAbECAQAwUDESMBAGA1UEAxMJc29tZSBib2R5MQswCQYDVQQGEwJVSzEL\n"
                + "MAkGA1UEBxMCREwxETAPBgNVBAoTCGVTY2llbmNlMQ0wCwYDVQQLEwRDTFJDMIIB\n"
                + "IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgagib0oTFFTNUBtJA3eAcF8F\n"
                + "WubqGBdLHVfluf9iNJgKLLfrzUNkes3MM/embCGJ8jcHSI9r0b/8EnRAwOv6p7zu\n"
                + "4NuyEJnQfv/FLJIoP5XNRV0JEFe5ZwDiXHLMdj0zT/SxOkkbu+IIJ3oiXNhow2PS\n"
                + "I1Dd11/qYmiVQxpaYxMUVrQQo4eSRXqLUIJPG4K6+AppV+83pMSFuTC7Wd9A9qdX\n"
                + "1bP7hLTW8sQRfy2C6sBQiKsX4t5tYvT2x8Cn+HiJp/34/sZgHTmDZ/Lc/9z8ziTo\n"
                + "7ekRbNaNZs9U5o8FmH5caI7wYo80bT+y1pYs6QrWZWo2BKLB3QtrQf4IF6T0gQID\n"
                + "AQABoDQwFwYJKoZIhvcNAQkHMQoMCHBhc3N3b3JkMBkGCSqGSIb3DQEJAjEMDApN\n"
                + "eSBjb21wYW55MA0GCSqGSIb3DQEBBQUAA4IBAQADTa8jgxNfJ0chisIZqJwg6XnA\n"
                + "boEol2d+dGY5pZkORGnGRgSFsPrDtB0sHRyitnPUkqNwORMBCxF1CYWSDCS5oN79\n"
                + "H79PVbvJjnULNkWEm29efG2IIjq/CX/ILcKeEj8GHGaJ0oVodW8s3QdB/kuyNOYT\n"
                + "HS/BqxflCPOtWp6mc1oWBZh2Pv9FiAva+zdTgDWjtSOOzPuJlOps4cYB6O6t15tM\n"
                + "4vGn40sv0XBStGfAotgYukY6YrKVLWa1WCnL5fZAsVIvMaOvRmHi6JkCQaWru0xD\n"
                + "yUXyX2E0GXhN2Obq09d7hZ4Kl2E5ggslUM3ARyYTJpeGcAYrw/o1mK+Aaw0a\n"
                + "-----END CERTIFICATE REQUEST-----";
        return csrPem;
    }

    /**
     * Pem has following dn: <tt></tt> and has 1024 bit strength. 
     * 
     * @return PEM string
     */
    /*private String getForgeCreatedCsrPem1024() {
        String csrPem = "-----BEGIN CERTIFICATE REQUEST-----\n"
                + "MIIBxDCCAS0CAQAwUDESMBAGA1UEAxMJc29tZSBib2R5MQswCQYDVQQGEwJVSzEL\n"
                + "MAkGA1UEBxMCREwxETAPBgNVBAoTCGVTY2llbmNlMQ0wCwYDVQQLEwRDTFJDMIGf\n"
                + "MA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDKki1TBC35MjO43dULTOvhslneGllT\n"
                + "CJGLJMyanC4RiEymKCRBGjrh9WNzA8pawH8ImeHWmkW1iuE2VmJh2RLtUTGqPAQJ\n"
                + "yMcAK95gXzGHI/XRKaQYUgJAOrIYjnGvTelF2igWHrTuqA9QjcghnuV5Teztp6UT\n"
                + "XB4/DEjrxpQVAwIDAQABoDQwFwYJKoZIhvcNAQkHMQoMCHBhc3N3b3JkMBkGCSqG\n"
                + "SIb3DQEJAjEMDApNeSBjb21wYW55MA0GCSqGSIb3DQEBBQUAA4GBAHrz27fDtfyN\n"
                + "H2Ceuik2BkwcU+bGjAenjHegiWX0+Kh0Y2UDKwXrd7L/raGxNqmDntS586SL9yG5\n"
                + "O053OAqXnxe0YxhQbbFhpZzPbhwV/W1m/SPoxRuxUbUTluHtfHyux9dA45prKv/7\n"
                + "hhe2MhlKyk/x4Rr7ruWUUXLH7XnA+aHk\n"
                + "-----END CERTIFICATE REQUEST-----";
        return csrPem;
    }*/

    /**
     * Pem has following dn: <tt>CN=some valid body,L=DL,OU=CLRC,O=eScience,C=UK</tt>. 
     * Note the OID structure order of the DN is NOT in reverse (starting with a CN ending in C) 
     * @return PEM string.
     */
    private String getCsrRegularDnStructureOrder() {
        String csrPem = "-----BEGIN CERTIFICATE REQUEST-----\n"
                + "MIICzzCCAbcCAQAwVjEYMBYGA1UEAxMPc29tZSB2YWxpZCBib2R5MQswCQYDVQQH\n"
                + "EwJETDENMAsGA1UECxMEQ0xSQzERMA8GA1UEChMIZVNjaWVuY2UxCzAJBgNVBAYT\n"
                + "AlVLMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmQOTU35MAMJk3VTQ\n"
                + "bUvHtXRoYKT08CeuJ2vZkkO/yVyFtNSGBL+rjIttT6SqgFJu0SKzG7tPvd2hMFXp\n"
                + "28F2vPleOG1/9KE6Hk/u2r6EbWQmzG5UprMNON2avo9Gb44e5t/+JfwxkDmLV/Ru\n"
                + "h/ESMvmumnu3i+yJr33O8oiBRua/VM3XlbQs32EuY0hjuKpHi/lLVW/QIjGClVTE\n"
                + "4nJu4pkIPJYiTBxJesxzeAf15h+TMPIjp61p/vmXjO/R0KL20Isq4/iQA9KBChuJ\n"
                + "v7YfZrlC4NOo5VyyRAMipNgg9SGGCSJgKiz1KlZVeZ4yVzT58nfreY6GzL4+ncZv\n"
                + "lI17GQIDAQABoDQwFwYJKoZIhvcNAQkHMQoMCHBhc3N3b3JkMBkGCSqGSIb3DQEJ\n"
                + "AjEMDApNeSBjb21wYW55MA0GCSqGSIb3DQEBBQUAA4IBAQBjB5zb2j2TFIOPKZBB\n"
                + "7rsPxM6moJrmCaRh2DQJ8dwKv9qK716cUP6BZsHNIqHrFavR9NnxztzUI9LxR9H0\n"
                + "SEvQE4lm1bXATUEjYfjl4u2k9eK9U5+1t+XUhBPzQLmhkPxgiUXXf6uIP6JM5jcA\n"
                + "WbowMwNTksHxKEITMJc1pP9bCXH1bfqwuc09UjtIe5xFeJKJ8iVKr9Rdw9CVzjF+\n"
                + "M1iBVWcC0ETlOTn5lYRJ373D+R0k1IDt2+EjCBmwMGX+soGN4KLqUFVJpQuVw29B\n"
                + "a/mSAnBUAljwJ1lm7bYe5ZliQztgcl9Nw5Ez9d9WTqKoeK5BMLEUogD9tccHdlsm\n"
                + "prfG\n"
                + "-----END CERTIFICATE REQUEST-----";
        return csrPem;
    }
}
