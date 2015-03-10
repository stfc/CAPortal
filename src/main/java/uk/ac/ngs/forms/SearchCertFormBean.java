package uk.ac.ngs.forms;

import java.io.Serializable;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

//import org.hibernate.validator.constraints.NotEmpty;

/**
 * Search certificate form bean. 
 * @author David Meredith
 *
 */
public class SearchCertFormBean implements Serializable {

    @Pattern(message="Invalid chars \" ' ; `", regexp="^[^\"';`]*$") 
    private String ra;
    
    @Pattern(message="Invalid chars \" ' ; `", regexp="^[^\"';`]*$")
    private String name; 
    
    @Pattern(message="Invalid chars \" ' ; `", regexp="^[^\"';`]*$")
    private String emailAddress; 
    
    private Boolean searchNullEmailAddress = false; 
    
    //@Pattern(message="Invalid chars, valid: a-zA-Z0-9_ .-,@=", regexp="[a-zA-Z0-9_\\-,@.%\\s=/\\\\]*")
    @Pattern(message="Invalid chars \" ' ; `", regexp="^[^\"';`]*$")
    private String dn; 
    
    @Pattern(message="Invalid chars \" ' ; `", regexp="^[^\"';`]*$")
    private String role;
    
    @Pattern(message="Invalid chars \" ' ; `", regexp="^[^\"';`]*$")
    private String data;        
    
    @Min(value=0, message="0 is minimum")
    private Integer serial; 
    
    @Pattern(message="Invalid chars \" ' ; `", regexp="^[^\"';`]*$")
    private String status; 
    
    @Min(value=0, message="0 is minimum")
    private Integer showRowCount = 10; 
   
    @Min(value=0, message="0 is minimum")
    private Integer startRow = 0; 
     
    private Boolean notExpired = true; 
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmailAddress() {
        return emailAddress;
    }
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    public String getDn() {
        return dn;
    }
    public void setDn(String dn) {
        this.dn = dn;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public Integer getSerial() {
        return serial;
    }
    public void setSerial(Integer serial) {
        this.serial = serial;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the showRowCount
     */
    public Integer getShowRowCount() {
        return showRowCount;
    }

    /**
     * @param showRowCount the showRowCount to set
     */
    public void setShowRowCount(Integer showRowCount) {
        this.showRowCount = showRowCount;
    }

    /**
     * @return the notExpired
     */
    public Boolean getNotExpired() {
        return notExpired;
    }

    /**
     * @param notExpired the notExpired to set
     */
    public void setNotExpired(Boolean notExpired) {
        this.notExpired = notExpired;
    }

    /**
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * @return the searchNullEmailAddress
     */
    public Boolean getSearchNullEmailAddress() {
        return searchNullEmailAddress;
    }

    /**
     * @param searchNullEmailAddress the searchNullEmailAddress to set
     */
    public void setSearchNullEmailAddress(Boolean searchNullEmailAddress) {
        this.searchNullEmailAddress = searchNullEmailAddress;
    }

    /**
     * @return the startRow
     */
    public Integer getStartRow() {
        return startRow;
    }

    /**
     * @param startRow the startRow to set
     */
    public void setStartRow(Integer startRow) {
        this.startRow = startRow;
    }

    /**
     * @return the ra
     */
    public String getRa() {
        return ra;
    }

    /**
     * @param ra the ra to set
     */
    public void setRa(String ra) {
        this.ra = ra;
    }
    
}
