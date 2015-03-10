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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author David Meredith
 */
public class TestBC_x509 {

    public TestBC_x509() {
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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void testParseSampleX509_Via_bcprov_jdk15on_1_50() throws Exception {
         String x509Pem = this.getSampleX509(); 
         //PemReader pemReader = new PemReader(new StringReader(x509Pem));
         //PemObject obj = pemReader.readPemObject();
         //byte[] pembytes = obj.getContent();
         
         CertificateFactory cf = CertificateFactory.getInstance("X.509");
         InputStream is = new ByteArrayInputStream(x509Pem.getBytes("UTF-8"));
         X509Certificate certObj = (X509Certificate) cf.generateCertificate(is);
         //System.out.println(certObj.getSubjectDN().getName()); 
         assertEquals("CN=david ra meredith, L=DL, OU=CLRC, O=eScienceDev, C=UK", certObj.getSubjectDN().getName()); 
    }

    private String getSampleX509() {
        String x509 = "-----BEGIN CERTIFICATE-----\n"
                + "MIIFXjCCBEagAwIBAgICDgUwDQYJKoZIhvcNAQEFBQAwSTELMAkGA1UEBhMCVUsx\n"
                + "FDASBgNVBAoTC2VTY2llbmNlRGV2MQwwCgYDVQQLEwNOR1MxFjAUBgNVBAMTDURl\n"
                + "dmVsb3BtZW50Q0EwHhcNMTMxMTE5MTExNTEzWhcNMTQwNTE4MTExNTEzWjBbMQsw\n"
                + "CQYDVQQGEwJVSzEUMBIGA1UEChMLZVNjaWVuY2VEZXYxDTALBgNVBAsTBENMUkMx\n"
                + "CzAJBgNVBAcTAkRMMRowGAYDVQQDExFkYXZpZCByYSBtZXJlZGl0aDCCASIwDQYJ\n"
                + "KoZIhvcNAQEBBQADggEPADCCAQoCggEBAIYdo73HY5wlXb75/tNYeftHegJTa64l\n"
                + "fki2ecKiroAC6tvAjuFEm/y4EJwH0OVUHcpfiJBcqlrNVD6xUvOM1tRJEARfmzjt\n"
                + "+63eLeBN0PSFU8VLOBD1V12ip9mvkytlP501kRLEtDqH813uF2DKnhLvHxPQAzjP\n"
                + "bg313YAZQ0zrzu47eEALBiifFijZYwutsmUQFoKZH8pzNl2hfhZ8bhrjzDbfDA7O\n"
                + "augHz8KWNKo6l8wp5WOmVZZy4eS1sFyqTWHv0FwDEm6XB2lJIXGlxsxy3iqtpYXl\n"
                + "YE5Sv9xUp95gdUv+FPTXLjBXfwtKIR0z5ywQx0q1u84Fi1PxpKXF3gsCAwEAAaOC\n"
                + "AjwwggI4MAwGA1UdEwEB/wQCMAAwEQYJYIZIAYb4QgEBBAQDAgWgMA4GA1UdDwEB\n"
                + "/wQEAwID6DAsBglghkgBhvhCAQ0EHxYdVUsgZS1TY2llbmNlIFVzZXIgQ2VydGlm\n"
                + "aWNhdGUwHQYDVR0OBBYEFCqf/aZRL4PtrQqtez84rVJqcvk2MHEGA1UdIwRqMGiA\n"
                + "FAXvzO2cwMpQVWOamhSGFvfibOMCoU2kSzBJMQswCQYDVQQGEwJVSzEUMBIGA1UE\n"
                + "ChMLZVNjaWVuY2VEZXYxDDAKBgNVBAsTA05HUzEWMBQGA1UEAxMNRGV2ZWxvcG1l\n"
                + "bnRDQYIBATAkBgNVHREEHTAbgRlkYXZpZC5tZXJlZGl0aEBzdGZjLmFjLnVrMCUG\n"
                + "A1UdEgQeMByBGnN1cHBvcnRAZ3JpZC1zdXBwb3J0LmFjLnVrMBkGA1UdIAQSMBAw\n"
                + "DgYMKwYBBAHZLwEBAQEHMEkGCWCGSAGG+EIBBAQ8FjpodHRwOi8vY2EuZ3JpZC1z\n"
                + "dXBwb3J0LmFjLnVrL3B1Yi9jcmwvZXNjaWVuY2Utcm9vdC1jcmwuY3JsMEcGCWCG\n"
                + "SAGG+EIBAwQ6FjhodHRwOi8vY2EuZ3JpZC1zdXBwb3J0LmFjLnVrL3B1Yi9jcmwv\n"
                + "ZXNjaWVuY2UtY2EtY3JsLmNybDBJBgNVHR8EQjBAMD6gPKA6hjhodHRwOi8vY2Eu\n"
                + "Z3JpZC1zdXBwb3J0LmFjLnVrL3B1Yi9jcmwvZXNjaWVuY2UtY2EtY3JsLmNybDAN\n"
                + "BgkqhkiG9w0BAQUFAAOCAQEABJ6Nc4/B3sDl0BbzKf+/YLP/vV76pDq5GI8++3AX\n"
                + "gnMWTPeXadapF6WycvLARudvX64f9xdPpr5Mo5G7BBbpB7CpVrvRS+8DVYfN4MDB\n"
                + "Vfn0kjxC6LHSXGnXUvZsMmxQEUxXZC3R19ulmTJGxSTBXODzp0VrRyb3aiKnZMT9\n"
                + "0z4weeyr7Z7BlzQ9jiTC/SWbgxVrqFOpnA3tusGc9H1nkNIdj3nD1km3O97nwvc8\n"
                + "BDFR+LKEWtkvft5+2gFV6qxg8NKZTOaGGtSrPQktYRCxyq78FnhPDy1beI2Cjmgt\n"
                + "zIth6lLxAqyxewwxPKutJZDIGxWR1HdWeU8GFaF4PmDB4g==\n"
                + "-----END CERTIFICATE-----";
        return x509;
    }
}
