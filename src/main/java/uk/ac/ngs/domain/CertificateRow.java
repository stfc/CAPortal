package uk.ac.ngs.domain;

import java.io.Serializable;
import java.util.Date;


/**
 * Domain object that maps to the <code>certificate</code> DB table.  
 * @author David Meredith
 */
public class CertificateRow implements Serializable{

    private static final long serialVersionUID = -300138607282104550L;
    
    private long cert_key; 
    private String data; 
    private String dn; 
    private String cn; 
    private String email; 
    private String status; 
    private String role; 
    private Date notAfter;
    
    public String getCn() {
        return cn;
    }
    public void setCn(String cn) {
        this.cn = cn;
    }
    
    public long getCert_key() {
        return cert_key;
    }
    public void setCert_key(long cert_key) {
        this.cert_key = cert_key;
    }
    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }
    public String getDn() {
        return dn;
    }
    public Date getNotAfter(){
        return this.notAfter; 
    }
    public void setNotAfter(Date notAfter){
        this.notAfter = notAfter; 
    }
    public void setDn(String dn) {
        this.dn = dn;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
   
    
    
}
