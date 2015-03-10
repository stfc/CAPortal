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

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import uk.ac.ngs.dao.JdbcRalistDao;
import uk.ac.ngs.domain.RalistRow;

/**
 * Defines valid lookup values for various PKCS10 (CSR) parameters. 
 * This class is a common dependency when validating CSRs - the values 
 * defined in this class are compared against the requested CSR parameters. 
 * 
 * @author David Meredith 
 */
public class CsrRequestValidationConfigParams {
   
    private String countryOID; 
    private String orgNameOID; 
    private int minModulus = 2048; 
    private int minExponent = 5; 
    private JdbcRalistDao ralistDao;

    /**
     * Create an instance. 
     * If calling <code>getValidLocalities()</code> or <code>getValidOrgUnits</code>
     * then <code>setRalistDao()</code> must be called. 
     * 
     * @param countryOID The supported country for the 'C' RDN in a CSR DN
     * @param orgNameOID The supported organisation name for 'O' RDN in a CSR
     */
    public CsrRequestValidationConfigParams(String countryOID, String orgNameOID){
      this.countryOID = countryOID; 
      this.orgNameOID = orgNameOID; 
    }

    /**
     * @return A list of known/valid RA Localities
     */
    public List<String> getValidLocalities(){
        if(ralistDao == null){
            throw new NullPointerException("ralistDao is null - use class setter"); 
        }
        List<RalistRow> rows = this.ralistDao.findAllByActive(true, null, null);
        List<String> locArray = new ArrayList<String>(rows.size());
        for (RalistRow row : rows) {
            locArray.add(row.getL().trim()); 
        }
        return locArray; 
    }

    /**
     * @return A list of known/valid RA Organisation Units  
     */
    public List<String> getValidOrgUnits(){
        if(ralistDao == null){
            throw new NullPointerException("ralistDao is null - use class setter"); 
        }
        List<RalistRow> rows = this.ralistDao.findAllByActive(true, null, null);
        List<String> ouArray = new ArrayList<String>(rows.size());
        for (RalistRow row : rows) {
            ouArray.add(row.getOu().trim()); 
        }
        return ouArray; 
    }
    
    /**
     * @param ralistDao the ralistDao to set
     */
    @Inject
    public void setRalistDao(JdbcRalistDao ralistDao) {
        this.ralistDao = ralistDao;
    }

    /**
     * @return the countryOID
     */
    public String getCountryOID() {
        return countryOID;
    }

    /**
     * @param countryOID the countryOID to set
     */
    public void setCountryOID(String countryOID) {
        this.countryOID = countryOID;
    }

    /**
     * @return the orgNameOID
     */
    public String getOrgNameOID() {
        return orgNameOID;
    }

    /**
     * @param orgNameOID the orgNameOID to set
     */
    public void setOrgNameOID(String orgNameOID) {
        this.orgNameOID = orgNameOID;
    }

    /**
     * @return the minModulus
     */
    public int getMinModulus() {
        return minModulus;
    }

    /**
     * @param minModulus the minModulus to set
     */
    public void setMinModulus(int minModulus) {
        this.minModulus = minModulus;
    }

    /**
     * @return the minExponent
     */
    public int getMinExponent() {
        return minExponent;
    }

    /**
     * @param minExponent the minExponent to set
     */
    public void setMinExponent(int minExponent) {
        this.minExponent = minExponent;
    }

    
    
}
