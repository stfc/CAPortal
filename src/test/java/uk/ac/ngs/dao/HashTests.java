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
package uk.ac.ngs.dao;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.apache.commons.codec.digest.DigestUtils; 
/**
 *
 * @author David Meredith  
 */
public class HashTests {
    
    public HashTests() {
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
    
    @Test
    public void testSha1HexHash() {
        String pin = "1234567890";
        String expectedHash = "01B307ACBA4F54F55AAFC33BB06BBBF6CA803E9A"; 

        // test commons codec. Note we have to re-upper case the returned hash.   
        String calculatedHash = DigestUtils.sha1Hex(pin);
        assertEquals(expectedHash, calculatedHash.toUpperCase()); 
        // Alternatively, just ignore the case
        assertTrue(expectedHash.equalsIgnoreCase(calculatedHash)); 

        // Now lets try with XDW's method 
        calculatedHash = this.getHash(pin); 
        assertEquals(expectedHash, calculatedHash); 
    }


    @Test
    public void testOpenCASha1HashAlgorithm(){
        // OpenCA appends a null char followed by 'exit' and a newline so append
        // those to our pin.  
        String pin = "1234567890"+"\u0000exit\n";
        // expectedOpenCAHash is the result OpenCA gives us when hashing via sha1Hex 
        String expectedOpenCAHash = "25c1cd954c5f9d83bd7b46ecb71c2db0145bc3a8"; 
        String calculatedHash = DigestUtils.sha1Hex(pin);  
        //System.out.println(calculatedHash); 
        assertEquals(expectedOpenCAHash, calculatedHash); 
    }


     private String getHash(String originalValue) {
        try {
            java.security.MessageDigest d = null;
            d = java.security.MessageDigest.getInstance("SHA-1");
            //d = java.security.MessageDigest.getInstance("MD5");
            d.reset();
            d.update(originalValue.getBytes("UTF-8"));  //originally was: d.update(originalValue.getBytes());
            byte[] b = d.digest();

            StringBuffer sb = new StringBuffer(b.length * 2);
            for (int i = 0; i < b.length; i++) {
                int v = b[i] & 0xff;
                if (v < 16) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(v));
            }
            return sb.toString().toUpperCase();
        } catch (Exception ep) {
            ep.printStackTrace();
            return null;
        }
    }
}
