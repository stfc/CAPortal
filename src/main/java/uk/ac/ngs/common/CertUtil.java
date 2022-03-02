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
package uk.ac.ngs.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import uk.ac.ngs.domain.RequestRow;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Utility class for certificate related functions. Stateless and thread safe.
 *
 * @author David Meredith (some modifications, javadoc)
 */
public class CertUtil {
    private static final Log log = LogFactory.getLog(CertUtil.class);

    public enum DNAttributeType {
        CN, L, OU, O, C, E, EMAILADDRESS
    }

    /**
     * Get the value of the specified DN attribute. The given dn must use
     * the comma char to separate attributes, ala RFC2253 or RFC1179.
     *
     * @param dn
     * @param attType
     * @return The value of the attribute, null if the attribute does not exist
     * or has an empty string value.
     */
    public static String extractDnAttribute(String dn, DNAttributeType attType) {
        //dn = dn.replace('/', ','); // consider host requests where CN=globusservice/host.dl.ac.uk
        String attribute = attType.name() + "=";

        int index = dn.indexOf(attribute);
        if (index == -1) {
            return null;
        }

        index = index + attribute.length();
        String result = dn.substring(index);
        int _index = result.indexOf(",");
        if (_index != -1) {
            result = result.substring(0, _index);
        }
        result = result.trim();
        if ("".equals(result)) {
            return null;
        }
        return result;
    }


    /**
     * Reverse the given DN and use the / char as the attribute separator.
     * The given DN must use the comma ',' as the attribute separator char.
     * For example, given: "CN=david meredith ral,L=RAL,OU=CLRC,O=eScienceDev,C=UK"
     * the returned DN is:  "/C=UK/O=eScienceDev/OU=CLRC/L=RAL/CN=david meredith ral"
     *
     * @param dnrfc2253 DN that uses comma chars as the attribute separator.
     * @return Formatted DN string.
     */
    public static String getReverseSlashSeparatedDN(String dnrfc2253) {
        StringBuilder buff = new StringBuilder("/");
        String[] oids = dnrfc2253.split(",");
        for (int i = oids.length - 1; i >= 0; --i) {
            buff.append(oids[i].trim()).append("/");
        }
        buff.delete(buff.length() - 1, buff.length());  //remove trailing /
        return buff.toString();
    }

    /**
     * Reverse the given DN and use the ',' char as the attribute separator
     * with no interleaving spaces.
     * The given DN must the the comma as the attribute separator char. The
     * returned DN will remove any spaces occurring after the comma separator.
     * For example, given:
     * "C=UK, O=eScienceDev, OU=CLRC, L=RAL, CN=david meredith ral"  or
     * "C=UK,O=eScienceDev,OU=CLRC,L=RAL,CN=david meredith ral"
     * The returned DN is:
     * "CN=david meredith ral,L=RAL,OU=CLRC,O=eScienceDev,C=UK"
     *
     * @param dnCommaSeparated
     * @return
     */
    public static String getReversedCommaSeparatedDN(String dnCommaSeparated) {
        StringBuilder buff = new StringBuilder();
        String[] oids = dnCommaSeparated.split(",");
        for (int i = oids.length - 1; i >= 0; --i) {
            buff.append(oids[i].trim()).append(",");
        }
        String dn = buff.toString().trim();
        while (dn.endsWith(",")) {
            dn = dn.substring(0, dn.length() - 1);
        }
        return dn;
    }


    /**
     * Get the 'canonical' DN expected by the DN column in the DB 'request' table.
     * The returned DN string will have the following OID structure order:
     * <tt>CN, L, OU, O, C</tt>
     * OID attributes are returned in that order and are separated by commas (no
     * emailAddress or other attributes). No spaces exist between attributes.
     * Example:
     * <tt>CN=serv01.foo.esc.rl.ac.uk,L=DL,OU=CLRC,O=eScience,C=UK</tt>
     *
     * @param dn
     * @return Canonical DN
     */
    public static String getDbCanonicalDN(String dn) {
        // gets the DN from the CSR 
        String CN = extractDnAttribute(dn, DNAttributeType.CN);
        String L = extractDnAttribute(dn, DNAttributeType.L);
        String OU = extractDnAttribute(dn, DNAttributeType.OU);
        String O = extractDnAttribute(dn, DNAttributeType.O);
        String C = extractDnAttribute(dn, DNAttributeType.C); //getPKCS10_C();
        //String email = this.getEmail(); 
        return "CN=" + CN + ",L=" + L + ",OU=" + OU + ",O=" + O + ",C=" + C;
    }

    public static String getPrefix(int tagNumber) {
        String prefix = "";
        switch (tagNumber) {
            case 0:
                prefix = "otherName";
                break;
            case 1:
                prefix = "rfc822Name";
                break;
            case 2:
                prefix = "DNSName";
                break;
            case 3:
                prefix = "x400Address";
                break;
            case 4:
                prefix = "directoryName";
                break;
            case 5:
                prefix = "ediPartyName";
                break;
            case 6:
                prefix = "uniformResourceIdentifier";
                break;
            case 7:
                prefix = "IPAddress";
                break;
            case 8:
                prefix = "registeredID";
                break;
            default:
                prefix = "UNKNOWN";
                break;
        }
        return prefix;
    }

    public static ArrayList<String> getSansCSR(Matcher certmatcher, RequestRow row) {
        if (certmatcher.find()) {
            String pemString = certmatcher.group();
            try {
                ArrayList<String> sans = new ArrayList<>();
                PEMParser pemParser = new PEMParser(new StringReader(pemString));
                PKCS10CertificationRequest certObj = (PKCS10CertificationRequest) pemParser.readObject();
                /*
                 * Extensions and SANs follow the below ASN1 format, which is why the code below is _complicated_:
                 *
                 * TAGGED [0]:
                 *   SEQUENCE
                 *   {
                 *       OBJECT IDENTIFIER=ExtensionRequest (1.2.840.113549.1.9.14)
                 *       SET
                 *       {
                 *           SEQUENCE
                 *           {
                 *               SEQUENCE
                 *               {
                 *                   OBJECT IDENTIFIER=SubjectAltName (2.5.29.17)
                 *                   OCTET STRING, encapsulates:
                 *                       SEQUENCE
                 *                       {
                 *                           TAGGED [2] IMPLICIT :
                 *                               OCTET STRING=
                 *                                   67 72 69 64 2D 73 75 70   grid-sup
                 *                                   70 6F 72 74 2E 61 63 2E   port.ac.
                 *                                   75 6B                     uk
                 *                           TAGGED [2] IMPLICIT :
                 *                               OCTET STRING=
                 *                                   77 77 77 2E 63 61 2E 67   www.ca.g
                 *                                   72 69 64 2D 73 75 70 70   rid-supp
                 *                                   6F 72 74 2E 61 63 2E 75   ort.ac.u
                 *                                   6B                        k
                 *                           TAGGED [2] IMPLICIT :
                 *                               OCTET STRING=
                 *                                   77 77 77 2E 67 72 69 64   www.grid
                 *                                   2D 73 75 70 70 6F 72 74   -support
                 *                                   2E 61 63 2E 75 6B         .ac.uk
                 *                       }
                 *
                 *               }
                 *           }
                 *       }
                 */

                Attribute[] erAttributes = certObj.getAttributes(new ASN1ObjectIdentifier("1.2.840.113549.1.9.14")); // Get the ExtensionRequest attributes
                for(Attribute a : erAttributes) {
                    ASN1Set set = a.getAttrValues();
                    if(set != null) {
                        ASN1Encodable asn1Encodable = set.getObjectAt(0); // Assume first object we get from this is the extension requests
                        Extensions extensions = Extensions.getInstance(asn1Encodable);
                        if(extensions != null) {
                            Extension sansExtension = extensions.getExtension(new ASN1ObjectIdentifier("2.5.29.17")); // Get SANs extension
                            ASN1Sequence sansSequence = ASN1Sequence.getInstance(sansExtension.getExtnValue().getOctets()); // convert to a sequence (as above)
                            if(sansSequence != null) {
                                for (ASN1Encodable san : sansSequence) {
                                    // There is at least one tagged SAN in every sequence
                                    ASN1TaggedObject sanTagged = (ASN1TaggedObject) san;
                                    ASN1OctetString sanOctectString = (ASN1OctetString) sanTagged.getBaseObject();
                                    String sanString = new String(sanOctectString.getOctets(), StandardCharsets.UTF_8);
                                    if(!sanString.equals(row.getCn())) {
                                        sans.add(CertUtil.getPrefix(sanTagged.getTagNo()) + "=<span style=\"color:red;font-weight:bold\">" + sanString + "</span>");
                                    } else {
                                        sans.add(CertUtil.getPrefix(sanTagged.getTagNo()) + "=" + sanString);
                                    }
                                }
                            }
                        }
                    }
                }
                return sans;
            } catch (IOException ex) {
                log.error(ex);
            }
        }
        return null;
    }

    public static ArrayList<String> getSans(X509Certificate cert) throws CertificateParsingException {
        Collection<List<?>> sans = cert.getSubjectAlternativeNames();
        ArrayList<String> outputSans = new ArrayList<>();
        for (List<?> san: sans) {
            outputSans.add(getPrefix((int) san.get(0)) + "=" + san.get(1)); // index 0 is the identifier - DNSName etc.
        }
        return outputSans;
    }

    public static ArrayList<String> getHtmlFormattedSans(X509Certificate cert) throws CertificateParsingException {
        Collection<List<?>> sans = cert.getSubjectAlternativeNames();
        X500Principal subject = cert.getSubjectX500Principal();
        String cn = extractDnAttribute(subject.toString(), DNAttributeType.CN);
        ArrayList<String> outputSans = new ArrayList<>();
        for (List<?> san: sans) {
            String sanName = (String) san.get(1);
            if (!sanName.equals(cn)) {
                outputSans.add(getPrefix((int) san.get(0)) + "=<span style=\"color:red;font-weight:bold\">" + sanName + "</span>"); // index 0 is the identifier - DNSName etc.
            } else {
                outputSans.add(getPrefix((int) san.get(0)) + "=" + sanName);
            }
        }
        return outputSans;
    }
}
