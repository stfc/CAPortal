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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ngs.domain.CertificateRow;


/**
 * Authentication related DAO operations. 
 * @author David Meredith 
 *
 */
@Repository
public class JdbcCaUserAuthDao {

    private NamedParameterJdbcTemplate jdbcTemplate;
    
    public JdbcCaUserAuthDao(){
       
    }
    
    /**
     * Set the JDBC dataSource. 
     * @param dataSource
     */
    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    
    private static final class CertificateRowMapper implements RowMapper<CertificateRow> {
        public CertificateRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            CertificateRow cr = new CertificateRow();
            cr.setCert_key(rs.getLong("cert_key")); 
            cr.setData(rs.getString("data"));  
            cr.setEmail(rs.getString("email"));  
            cr.setStatus(rs.getString("status")); 
            cr.setRole(rs.getString("role")); 
            cr.setDn(rs.getString("dn"));
            BigDecimal notafterBigD = rs.getBigDecimal("notafter");
            if(notafterBigD != null){
                String notafterStr = notafterBigD.toString();
                try {
                    DateFormat utcTimeStampFormatter = new SimpleDateFormat("yyyyMMddHHmmss"); 
                    utcTimeStampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));  
                    Date date = utcTimeStampFormatter.parse(notafterStr);
                    cr.setNotAfter(date);
                } catch (ParseException ex) {
                    throw new SQLException(ex);
                }
            }
            return cr;
        }
    }
    
    /**
     * For the specified dn, return a list of CertificateRow instances or an empty list.  
     * @param dn DN string of the form <pre>CN=david meredith, L=DL, OU=CLRC, O=eScience, C=UK</pre>
     * @return A CertificateRow or null if not found
     */
    public List<CertificateRow> getCaUserEntry(String dn){
        DateFormat utcTimeStampFormatter = new SimpleDateFormat("yyyyMMddHHmmss"); 
        utcTimeStampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));  
        String currentTime = utcTimeStampFormatter.format(new Date()); 
        dn = dn.replace("EMAILADDRESS", "emailAddress"); 
        String sql = "select cert_key, data, dn, email, status, role, notafter from certificate " +
        		"where status = 'VALID' and dn = :dn and notafter > :current_time";
        //Map<String, String> namedParameters = Collections.singletonMap("dn", dn);
        Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("dn", dn); 
        namedParameters.put("current_time", Long.parseLong(currentTime)); 
        if(jdbcTemplate == null){
            throw new NullPointerException("jdbcTemplate is null - call setDataSource()"); 
        }
        return this.jdbcTemplate.query(sql, namedParameters, new CertificateRowMapper());  
    }
    
    public int getCertificateRowCount(){
        if(jdbcTemplate == null){
            throw new NullPointerException("jdbcTemplate is null - call setDataSource()"); 
        }
        return this.jdbcTemplate.queryForObject("select count(*) from certificate", new HashMap<String, String>(0), Integer.class);
    }
    
}
