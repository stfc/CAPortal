/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.domain;

/**
 * Defines the different types of PKCS10 request. 
 * @author David Meredith 
 */
public interface CSR_Flags {
    
    public static enum Profile {
        UKPERSON,
        UKHOST,
    }
    
    public static enum Csr_Types { 
        NEW, 
        RENEW,
    }
    
}
