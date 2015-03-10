/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.service;

import java.util.HashMap;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import uk.ac.ngs.domain.PKCS10_RequestWrapper;

/**
 * Immutable transfer object that defines the result (success or fail) of a 
 * service layer Certificate Signing Request (CSR) operation.
 *
 * @author David Meredith
 */
public class ProcessCsrResult {

    private final boolean success;
    private final Errors errors;
    private final Long req_key;

    private final PKCS10_RequestWrapper csrWrapper;
    private final String pkcs8PrivateKey;

    /**
     * Construct an instance to signify a <b>fail</b>.  
     * @param errors 
     * @param csrWrapper 
     */
    public ProcessCsrResult(final Errors errors, final PKCS10_RequestWrapper csrWrapper) {
        if(errors == null){
            throw new IllegalArgumentException("errors is null"); 
        }
        if(csrWrapper == null){
            throw new IllegalArgumentException("csrWrapper is null"); 
        }
        this.success = false;
        this.errors = errors;
        this.req_key = null;
        this.csrWrapper = csrWrapper;
        this.pkcs8PrivateKey = ""; 
    }

    /**
     * Construct an instance to signify <b>success</b>. 
     * @param req_key
     * @param csrWrapper
     * @param pkcs8PrivateKey Optional, the PKCS#8 PEM string or null  
     */
    public ProcessCsrResult(final Long req_key, final PKCS10_RequestWrapper csrWrapper, 
            final String pkcs8PrivateKey) {
        if(req_key == null){
            throw new IllegalArgumentException("req_key is null");  
        }
        if(csrWrapper == null){
            throw new IllegalArgumentException("csrWrapper is null"); 
        }
        this.success = true;
        this.errors = new MapBindingResult(new HashMap<String, String>(), "csrWrapper");
        this.csrWrapper = csrWrapper;
        this.pkcs8PrivateKey = pkcs8PrivateKey;
        this.req_key = req_key;
    }


    /**
     * @return the success 
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return the errors (empty on successful request)
     */
    public Errors getErrors() {
        return errors;
    }

    /**
     * @return the pkcs8PrivateKey or null if not known (will be null if the 
     * client provided the CSR and never provided the private key). 
     */
    public String getPkcs8PrivateKey() {
        return pkcs8PrivateKey;
    }

    /**
     * @return the req_key or null if success is false 
     */
    public Long getReq_key() {
        return req_key;
    }

    /**
     * @return the csrWrapper
     */
    public PKCS10_RequestWrapper getCsrWrapper() {
        return csrWrapper;
    }

}
