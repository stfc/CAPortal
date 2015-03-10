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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;

/**
 * Utility class for base64 encoding and decoding of PublicKeys. The class also
 * provides methods for formatting the given PublicKeys into the CA database
 * proprietary format.
 *
 * @author xw75
 * @author David Meredith (some fixes only, much still to fix)
 */
public class ConvertUtil {

    private static final Logger log = Logger.getLogger(ConvertUtil.class.getName());

    //public static boolean areStringsRFC2253ComparableEquivalents(String s1, String s2){     
    //    return false; 
    //}
    public static String getHash(String originalValue) {
        try {
            java.security.MessageDigest d = java.security.MessageDigest.getInstance("SHA-1");
            d.reset();
            d.update(originalValue.getBytes("UTF-8"));
            byte[] b = d.digest();

            StringBuilder sb = new StringBuilder(b.length * 2);
            for (int i = 0; i < b.length; i++) {
                int v = b[i] & 0xff;
                if (v < 16) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(v));
            }
            return sb.toString().toUpperCase();
        } catch (UnsupportedEncodingException ex) {
            log.log(Level.INFO, ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            log.log(Level.INFO, ex.getMessage());
        }
        return null;
    }

    /**
     * Base64 encodes the given PublicKey.
     *
     * @param publicKey Public Key
     * @return Base64 encoded Public Key string.
     */
    public static String getBase64EncodedPublicKey(PublicKey publicKey) {
        // http://www.rgagnon.com/javadetails/java-0598.html 
        //return new String(Base64.encode(publicKey.getEncoded()));
        return new String(Base64.encodeBase64(publicKey.getEncoded()));
    }

    /**
     * Get an RSA PublicKey from the given encoded Public Key String. Important:
     * We may need to support different pub key algorithms in future (e.g. DER)
     * and so will need to lookup the pub key alg.
     *
     * @param base64EncodedPubKey Base64 encoded Public Key
     * @return Public Key or null if RSA pub key could not be generated (e.g.
     * the given pub key was an encoded DER pubkey not RSA).
     */
    public static PublicKey getPublicKey(String base64EncodedPubKey) {
        try {
            // lets not use the BC Base64.decode 
            //byte[] decodedPubKey = Base64.decode(encodedPublicKeyString);
            byte[] decodedPubKey = Base64.decodeBase64(base64EncodedPubKey.getBytes());
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(decodedPubKey);

            // DM: hmm, hardwired the provider here (i don't think we want to do this) 
            //Provider provider = new BouncyCastleProvider();
            //KeyFactory keyFactory = KeyFactory.getInstance("RSA", provider); 
            // Rather, lets rely on the default: 
            // KeyFactory.getInstance("RSA") traverses the list of registered security Providers, 
            // starting with the most preferred Provider. A new KeyFactory object 
            // encapsulating the KeyFactorySpi implementation from the first 
            // Provider that supports the specified algorithm is returned.
            // Note that the list of registered providers may be retrieved via 
            // the Security.getProviders() method. 
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);

            return pubKey;
        } catch (InvalidKeySpecException ex) {
            log.log(Level.INFO, ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            log.log(Level.INFO, ex.getMessage());
        }
        return null;
    }

    /**
     * Create an RSA PublicKey String that is formatted according to the
     * proprietary CA database format (this is actually the openssl -text format
     * of the pubkey).
     * <p>
     * Important: We only support the RSA pubkey algorithm, therefore this
     * method can barf
     * <p>
     * Importantly, the Modulus part of the returned public key string can be
     * padded with leading '00:' if and only if the most significant byte of the
     * given keyIdAsHexString is >= 128 (0x80). The format of the returned
     * Public Key String is:
     * <pre>
     *     Modulus (1024 bit):
     *         00:dc:5d:a0:84:bc:74:4e:87:0f:3d:07:ea:72:63:
     *         d8:ea:dc:8b:d8:e5:40:cf:93:bb:60:48:c8:4b:e1:
     *         8d:89:b0:06:7c:09:6c:72:63:e0:6d:55:36:ab:fc:
     *         e9:5c:35:61:8d:02:ce:28:ee:ef:31:e4:5b:6a:c5:
     *         b5:a3:7d:0b:77:
     *     Exponent: 65537 (0x10001)
     * </pre>
     *
     * @param publicKey Public Key
     * @return Public Key formated for CA DB
     */
    public static String getDBFormattedRSAPublicKey(PublicKey publicKey) {

        // 
        //if(!"RSA".equals(publicKey.getAlgorithm())){
        //    return null; 
        //}
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
     * Create a PublicKey String that is formatted to the proprietary CA
     * database format.
     * <p>
     * The format of the given keyIdAsHexString is:
     * 'hex_Formatted_Public_Key_Modulus.hex_Formatted_Exponent' For example:
     * '00dc5da084bc74.....b5a37d0b77.10001'
     * <p>
     * Importantly, the Modulus part of the returned public key string can be
     * padded with leading '00:' if and only if the most significant byte of the
     * given keyIdAsHexString is >= 128 (0x80).
     * <p>
     * For an example of the returned format of KeyId, see      {@link #getDBFormattedPublicKey(java.security.PublicKey)
     *
     * @param keyIdAsHexString KeyId
     * @return Public Key formated for CA DB
     */
    public static String getDBFormattedPublicKey(String keyIdAsHexString) {
        int index = keyIdAsHexString.indexOf(".");
        String modulusHexString = keyIdAsHexString.substring(0, index).trim();
        String exponentAsHex = keyIdAsHexString.substring(index + 1);
        String exponentAsDecimal = hexToDecimalString(exponentAsHex);

        modulusHexString = leadPad(modulusHexString);

        BigInteger bigInteger = hex2BigInteger(modulusHexString);
        int modulusBitLength = bigInteger.bitLength(); // e.g. 1024 or 2048 

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

    /**
     * Get the hex encoded version of the db_pubkey, which MUST be given in
     * proprietary CA database format). Important: Because db_pubkey is in
     * proprietary CA DB format, the returned encoded public key may have a
     * leading '00' padding. For an example of the required format of db_pubKey,
     * see {@link #getDBFormattedPublicKey(java.security.PublicKey)
     *
     * @param db_pubKey DB formatted public key string.
     * @return hex encoded public key (with optional padding)
     */
    public static String getHexEncodedKeyIdFromDBPublicKey(String db_pubKey) {
        int index = db_pubKey.indexOf(":");
        String subString = db_pubKey.substring(index + 1);
        index = subString.indexOf("Exponent");
        String modulusString = subString.substring(0, index);
        modulusString = modulusString.replaceAll(":", "");
        modulusString = modulusString.replaceAll("\n", "").trim();
        modulusString = modulusString.replaceAll("\\s", "");

        String exponentString = subString.substring(index);
        index = exponentString.indexOf("0x") + 2;
        exponentString = exponentString.substring(index);
        index = exponentString.indexOf(")");
        exponentString = exponentString.substring(0, index).trim();

        return modulusString + "." + exponentString;// keyid 
    }

    /**
     * Get publicKey keyID string, which is defined as the concatenation of:
     * <ul> <li>the (lower case) hex formatted public key modulus.</li> <li>the
     * character '.' (dot)</li> <li>the hex formatted exponent.</li> </ul> For
     * example: 'dc5da084bc74.....b5a37d0b77.10001'
     *
     * @param publicKey
     * @return The key Id As a Hex String
     */
    public static String getHexEncodedPublicKeyId(PublicKey publicKey) {
        RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
        String modulusString = rsaPublicKey.getModulus().toString(16);
        String exponentString = rsaPublicKey.getPublicExponent().toString(16);
        return modulusString + "." + exponentString;
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

    private static String hexToDecimalString(String hex) {
        long result = 0;
        int length = hex.length();
        for (int i = 0; i < length; i++) {
            String s = hex.substring(length - i - 1, length - i);
            if (!s.equals("0")) {
                long _result = 1;
                for (int j = length; j > length - i; j--) {
                    _result = _result * 16;
                }
                result = result + _result;
            }
        }
        return Long.toString(result);
    }

    private static BigInteger hex2BigInteger(String hex) {
        String digits = "0123456789ABCDEF";
        hex = hex.toUpperCase();
        BigInteger b = new BigInteger("0");
        BigInteger my_b = new BigInteger(Integer.toString(16));
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            int d = digits.indexOf(c);
            BigInteger _b = new BigInteger(new Integer(d).toString());
            b = b.multiply(my_b);
            b = b.add(_b);
        }

        return b;
    }

    /*
     * public static String getDBFormattedPublicKey(PublicKey publicKey) {
     *
     * String keyString = null; String result = null; keyString =
     * publicKey.toString(); // no no no no - can't rely on this !      *
     * //returns the index of the first character of the first given substring
     * int index = keyString.indexOf("modulus:"); index = index + 9; String
     * _keyString = keyString.substring(index); int _index =
     * _keyString.indexOf("\n"); String modulusString = _keyString.substring(0,
     * _index).trim(); //System.out.println("modStr: ["+modulusString+"]");
     *
     *
     * // modu, expHex and expDec are the same regardless of provider:
     * RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey; int modulusBit =
     * rsaPublicKey.getModulus().bitLength(); //e.g. 2048 String
     * exponentHexString = rsaPublicKey.getPublicExponent().toString(16); //e.g.
     * 10001 String exponentDecString =
     * rsaPublicKey.getPublicExponent().toString();//e.g. 65537
     *
     * //the (lower case) hex formatted modulus (with a leading 00 if and only
     * if the most significant byte is >= 128 (0x80)). String firstTwo =
     * modulusString.substring(0, 2); firstTwo = "0x" + firstTwo; int _int =
     * Integer.decode(firstTwo).intValue(); if (_int >= 128) { modulusString =
     * "00" + modulusString; }
     *
     * int length = modulusString.length(); String keyFormatString = "";
     * keyFormatString = "Modulus (" + modulusBit + " bit):\n" + " ";
     *
     * for (int i = 0, j = 1; i < length; i = i + 2, j++) { if (j == 15) { j =
     * 0; keyFormatString = keyFormatString + modulusString.substring(i, i + 2)
     * + ":\n" + " "; } else { if (i == (length - 2)) { keyFormatString =
     * keyFormatString + modulusString.substring(i, i + 2); } else {
     * keyFormatString = keyFormatString + modulusString.substring(i, i + 2) +
     * ":"; } } } keyFormatString = keyFormatString + "\n" + "Exponent: " +
     * exponentDecString + " (0x" + exponentHexString + ")" + "\n"; result =
     * keyFormatString; return result;
     *
     * }
     */
}
