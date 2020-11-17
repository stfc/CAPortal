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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ngs.common.Pair;
import uk.ac.ngs.domain.CertificateRow;

/**
 * DAO for the CA DB <code>certificate</code> table.
 *
 * @author David Meredith
 *
 */
@Repository
public class JdbcCertificateDao {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private static final Log log = LogFactory.getLog(JdbcCertificateDao.class);
    /*
     * Define SQL query fragments used to build the DAOs queries. 
     */
    public static final String SELECT_PROJECT = "select cert_key, data, dn, cn, email, status, role, notafter from certificate ";
    public static final String SELECT_COUNT = "select count(*) from certificate ";
    public static final String UPDATE_BY_REQ_KEY = "update certificate set "
            + "data=:data, dn=:dn, cn=:cn, email=:email, status=:status, role=:role, notafter=:notafter "
            + "where cert_key=:cert_key";
    
    private static final String LIMIT_ROWS = "LIMIT :limit OFFSET :offset";
    //private static final String ORDER_BY = "ORDER BY cert_key";
    private static final String WHERE_BY_ID = "where cert_key = :cert_key ";
    private static final String SQL_SELECT_BY_ID = SELECT_PROJECT + WHERE_BY_ID;
    private static final String SQL_SELECT_ALL = SELECT_PROJECT + LIMIT_ROWS;

    private final static Pattern DATA_CERT_PATTERN = Pattern.compile("-----BEGIN CERTIFICATE-----(.+?)-----END CERTIFICATE-----", Pattern.DOTALL);

    private final static Pattern DATA_LAD_PATTERN = Pattern.compile("LAST_ACTION_DATE\\s?=\\s?([^\\n]+)$", Pattern.MULTILINE);
    private final static Pattern DATA_RAOP_PATTERN = Pattern.compile("RAOP\\s?=\\s?([0-9]+)\\s*$", Pattern.MULTILINE);
    // Date will be of the form:  Tue Apr 23 13:47:13 2013 UTC 
    private final DateFormat utcDateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy zzz");


    
    /**
     * Keys for defining 'where' parameters and values for building queries.  
     * <p>
     * If the key ends with '_LIKE' then the where clause is appended with a SQL
     * 'like' clause (e.g. <code>key like 'value'</code>). 
     * If the key ends with '_EQ' then the where clause is appended  with a SQL 
     * '=' clause (e.g. <code>key = 'value'</code>).
     * <p>
     * _EQ takes precedence over _LIKE so if both EMAIL_EQ and EMAIL_LIKE are given, then 
     * only EMAIL_EQ is used to create the query. 
     */
    public static enum WHERE_PARAMS {

        DN_HAS_RA_LIKE, CN_LIKE, EMAIL_LIKE, EMAIL_EQ, DN_LIKE, ROLE_LIKE, STATUS_LIKE, DATA_LIKE, NOTAFTER_GREATERTHAN_CURRENTTIME
    };

    public JdbcCertificateDao() {

    }

    private static DateFormat getDateFormat(){
        DateFormat utcTimeStampFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
        utcTimeStampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcTimeStampFormatter; 
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
            long lValcert_key = rs.getLong("cert_key"); 
            if(!rs.wasNull()){
                cr.setCert_key(lValcert_key); 
            }
            cr.setData(rs.getString("data"));
            cr.setEmail(rs.getString("email"));
            cr.setStatus(rs.getString("status"));
            cr.setRole(rs.getString("role"));
            cr.setDn(rs.getString("dn"));
            cr.setCn(rs.getString("cn")); 
            BigDecimal notafterBigD = rs.getBigDecimal("notafter");
            if(notafterBigD != null){
                String notafterStr = notafterBigD.toString();
                try {
                    Date date = getDateFormat().parse(notafterStr);
                    cr.setNotAfter(date);
                } catch (ParseException ex) {
                    throw new SQLException(ex);
                }
            }
            return cr;
        }
    }


    /**
     * Count the number of <tt>certificate</tt> rows where <tt>status</tt> 
     * is <tt>VALID</tt> and <tt>dn</tt> is like the given DN and 
     * <tt>notafter</tt> is greater than the current time after the given 
     * notafterTolerance is applied. 
     * <p>
     * The notafterTolerance is applied in days, e.g. to subtract 5 days from 
     * the current time specify -5, to add 5 days from the current time specify 5. 
     * 
     * @param rfc2253DN Compare this argument to the value of the <tt>dn</tt> column 
     *   using a case in-sensitive 'LIKE' comparison. 
     * @param notafterTolerance Tolerance period defined in days. 
     * @return Number of matching rows. 
     */
    public int countByStatusIsVALIDNotExpired(String rfc2253DN, int notafterTolerance) {
        // If the status of the request is "REVOKED" or "DELETED", then we 
        // think the certificate doesn't exist.
        // In addition, the certificate may also be expired. In this case, 
        // the certificate CAN still be marked as "VALID" and NOT as "EXPIRED", 
        // we thus need the manual timestamp check to test for expired certs. 
        // 
        // See the ***IMPORTANT NOTE on canonical DN checking*** 

        Calendar calendarLocal = Calendar.getInstance(); // current time in default locale 
        // Get time in past 'notafterTolerance' number of days ago - used to define how many 
        // days a certificate can be expired but still eligible for renewal.  
        // E.g. to subtract 5 days from the current time of the calendar, specify -5
        calendarLocal.add(Calendar.DAY_OF_MONTH, notafterTolerance);

        String currentTimeUTC = getDateFormat().format(calendarLocal.getTime());
        Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("currentTime", new Long(currentTimeUTC));
        namedParameters.put("dn", rfc2253DN);
        //log.debug("notafter: ["+namedParameters.get("currentTime")+"]"); 
        String query = "select count(*) from certificate where dn ILIKE :dn and status='VALID' and notafter > :currentTime";
        return this.jdbcTemplate.queryForObject(query, namedParameters, Integer.class);
    }


    
    /**
     * Return all the certificates in the <code>certificate</code> table. 
     * @param limit
     * @param offset
     * @return 
     */
    public List<CertificateRow> findAll(Integer limit, Integer offset) {
        Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("limit", limit);
        namedParameters.put("offset", offset);
        return this.jdbcTemplate.query(SQL_SELECT_ALL, namedParameters, new CertificateRowMapper());
    }

    /**
     * Count all the certificates in the <code>certificate</code> table. 
     * @return 
     */
    public int countAll() {
        return this.jdbcTemplate.queryForObject("select count(*) from certificate", new HashMap<String, String>(0), Integer.class);
    }

    /**
     * Find the specified certificate in the <code>certificate</code> table with 
     * the given <code>cert_key</code>. 
     * @param cert_key
     * @return row or null if no row is found  
     */
    public CertificateRow findById(long cert_key) {
        Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("cert_key", cert_key);
        try { 
            return this.jdbcTemplate.queryForObject(SQL_SELECT_BY_ID, namedParameters, new CertificateRowMapper());
        } catch (EmptyResultDataAccessException ex) {
            log.warn("No certificate row found with [" + cert_key + "]");
            return null;
        }
    }

    /**
     * Search for certificates using the search criteria specified in the given 
     * where-by parameter map. 
     * Multiple whereByParams are appended together using 'and' statements. 
     * 
     * @param whereByParams Search-by parameters used in where clause of SQL query. 
     * @param limit Limit the returned row count to this many rows or null not to specify a limit.   
     * @param offset Return rows from this row number or null not to specify an offset.  
     * @return 
     */
    public List<CertificateRow> findBy(Map<WHERE_PARAMS, String> whereByParams, Integer limit, Integer offset) {
        Pair<String, Map<String, Object>> p = this.buildQuery(SELECT_PROJECT, whereByParams, limit, offset, true);
        return this.jdbcTemplate.query(p.first, p.second, new CertificateRowMapper());
    }
    /**
     * Count the total number of rows that are selected by the given where-by search criteria. 
     * @see #findBy(java.util.Map, java.lang.Integer, java.lang.Integer)  
     * @param whereByParams
     * @return number of matching rows.  
     */
    public int countBy(Map<WHERE_PARAMS, String> whereByParams) {
        Pair<String, Map<String, Object>> p = this.buildQuery(SELECT_COUNT, whereByParams, null, null, false);
        return this.jdbcTemplate.queryForObject(p.first, p.second, Integer.class);
    }

    /**
     * Find all rows with role 'RA Operator' or 'CA Operator' with a 
     * status of 'VALID' and a 'notafter' time that is in the future with the specified 
     * loc (L=) and ou (OU=) values in the dn.  
     * @param loc Locality value (optional, use null to prevent filtering by loc) 
     * @param ou OrgUnit value (optional, use null to prevent filtering by ou) 
     * @return 
     */
    public List<CertificateRow> findActiveRAsBy(String loc, String ou){
        String currentTime = getDateFormat().format(new Date());  
        Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("current_time",  Long.parseLong(currentTime));  

        // Build the RA filter 
        StringBuilder raVal = new StringBuilder("%"); 
        boolean restrictByRa = false; 
        if(loc != null){
           raVal.append("L=").append(loc).append(","); 
           restrictByRa = true; 
        }
        if(ou != null){
           raVal.append("OU=").append(ou); 
           restrictByRa = true; 
        }
        raVal.append("%"); 
        if(restrictByRa){
            //namedParameters.put("ra", "%L="+loc+",OU="+ou+"%"); 
            namedParameters.put("ra", raVal.toString()); 
        }
        
        StringBuilder query = new StringBuilder(SELECT_PROJECT);  
        query.append("where (role='RA Operator' or role='CA Operator') ");
        if (restrictByRa) {
            query.append("and dn like :ra ");
        }
        query.append("and status='VALID' and notafter > :current_time");
        return this.jdbcTemplate.query(query.toString(), namedParameters, new CertificateRowMapper());    
    }

 
    /**
     * Update the 'certificate' table with the values from the given CertificateRow. 
     * The given CertificateRow must have a populated <code>cert_key</code> value to 
     * identify the correct row in the DB. 
     * 
     * @param certRow Extracts values from this certificate to update the db. 
     * @return Number of rows updated (should always be 1) 
     */
    public int updateCertificateRow(CertificateRow certRow){
        if(certRow.getCert_key() <=0 ){
             throw new IllegalArgumentException("Invalid certificateRow, cert_key is zero or negative"); 
        }
        Map<String, Object> namedParameters = new HashMap<String, Object>(); 
        namedParameters.put("data", certRow.getData()); 
        namedParameters.put("dn", certRow.getDn()); 
        namedParameters.put("cn", certRow.getCn()); 
        namedParameters.put("email", certRow.getEmail()); 
        namedParameters.put("status", certRow.getStatus()); 
        namedParameters.put("role", certRow.getRole()); 
        namedParameters.put("notafter", new Long(getDateFormat().format(certRow.getNotAfter()))); 
        namedParameters.put("cert_key", certRow.getCert_key()); 
        /*"update certificate set data=:data, dn=:dn, cn=:cn, email=:email, 
         * status=:status, role=:role, notafter=:notafter where cert_key=:cert_key";*/
        return this.jdbcTemplate.update(UPDATE_BY_REQ_KEY, namedParameters);     
    }


    /**
     * Extract the PEM string from the given string (if any). 
     * The returned string excludes the '-----BEGIN CERTIFICATE-----' and 
     * '-----END CERTIFICATE-----' header and footer. 
     * @param cert
     * @return PEM String 
     */
    public String getPemStringFromData(String data) {
        Matcher certmatcher = DATA_CERT_PATTERN.matcher(data);
        if (certmatcher.find()) {
            return certmatcher.group();
        }
        return null;
    }

        
    public X509Certificate getX509CertificateFromData(CertificateRow certRow) 
            throws CertificateException, UnsupportedEncodingException {
        
        String pemString = this.getPemStringFromData(certRow.getData());
        X509Certificate certObj = null;
        if (pemString != null) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream is = new ByteArrayInputStream(pemString.getBytes("UTF-8"));
            certObj = (X509Certificate) cf.generateCertificate(is);
            return certObj;
        }
        return certObj;
    }


    /**
     * Update the LAST_ACTION_DATE and RAOP values in the given data field.  
     * The LAST_ACTION_DATE key-value pair is updated/replaced if present or added if not. 
     * A new RAOP key-value pair is appended after the last RAOP entry. 
    * 
     * @param data String to update. If null, then an empty string is created/modified and returned. 
     * @param raopId the RA operator id used to update the RAOP key value or -1 to exclude RAOP update. 
     * @return updated string (never null) 
     */
    public String updateDataCol_LastActionDateRaop(String data, long raopId) {
        if(data == null) {
            data = "-----END HEADER-----"; 
        }
        // Update/add RAOP 
        if(raopId != -1){
            Matcher raopMatcher = DATA_RAOP_PATTERN.matcher(data);
            if (raopMatcher.find()) {
                // append a new 'RAOP = raopId' line after the last found RAOP line
                int lastIndex = raopMatcher.end(); 
                while (raopMatcher.find()) {
                    lastIndex = raopMatcher.end();
                }
                String dataStart = data.substring(0, lastIndex); 
                String dataMiddle = "\nRAOP = "+raopId; 
                String dataEnd = data.substring(lastIndex, data.length()); 
                data = dataStart+dataMiddle+dataEnd; 
                // We don't replace all as we want to maintain a history  
                //data = raopMatcher.replaceAll("RAOP = " + raopId);
            } else {
                data = data.replaceAll("-----END HEADER-----", "RAOP = " + raopId + "\n-----END HEADER-----");
            }
        }
        // Update or add LAST_ACTION_DATE (add if it can't be found)
        Matcher ladmatcher = DATA_LAD_PATTERN.matcher(data);
        String dateString = utcDateFormat.format(new Date());
        if (ladmatcher.find()) {
            data = ladmatcher.replaceAll("LAST_ACTION_DATE = " + dateString);
        } else {
            data = data.replaceAll("-----END HEADER-----", "LAST_ACTION_DATE = " + dateString + "\n-----END HEADER-----");
        }
        return data;
    }

    
    
    /**
     * Build up the query using the given where by parameters in the map
     * and return the query and the named parameter map for subsequent parameter-binding/execution.  
     */
    protected Pair<String, Map<String, Object>> buildQuery(String selectStatement,
            Map<WHERE_PARAMS, String> whereByParams, Integer limit, Integer offset, boolean orderby) {

        String whereClause = "";
        Map<String, Object> namedParameters = new HashMap<String, Object>();
        if (whereByParams != null && !whereByParams.isEmpty()) {
            StringBuilder whereBuilder = new StringBuilder("where ");
            
            if (whereByParams.containsKey(WHERE_PARAMS.CN_LIKE)) {
                whereBuilder.append("cn like :cn and ");
                namedParameters.put("cn", whereByParams.get(WHERE_PARAMS.CN_LIKE));
            }
            // EQ takes precidence over LIKE 
            if (whereByParams.containsKey(WHERE_PARAMS.EMAIL_EQ)) {
                log.debug("Searching for null email - check val ["+whereByParams.get(WHERE_PARAMS.EMAIL_EQ)+"]"); 
                if(whereByParams.get(WHERE_PARAMS.EMAIL_EQ) == null){
                    whereBuilder.append("email is null and ");
                } else {
                    whereBuilder.append("email = :email and ");
                    namedParameters.put("email", whereByParams.get(WHERE_PARAMS.EMAIL_EQ));
                }
            } else {
                if (whereByParams.containsKey(WHERE_PARAMS.EMAIL_LIKE)) {
                    whereBuilder.append("email like :email and ");
                    namedParameters.put("email", whereByParams.get(WHERE_PARAMS.EMAIL_LIKE));
                }
            }
            if (whereByParams.containsKey(WHERE_PARAMS.DN_LIKE)) {
                whereBuilder.append("dn like :dn and ");
                namedParameters.put("dn", whereByParams.get(WHERE_PARAMS.DN_LIKE));
            }
            if (whereByParams.containsKey(WHERE_PARAMS.ROLE_LIKE)) {
                whereBuilder.append("role like :role and ");
                namedParameters.put("role", whereByParams.get(WHERE_PARAMS.ROLE_LIKE));
            }
            if (whereByParams.containsKey(WHERE_PARAMS.STATUS_LIKE)) {
                whereBuilder.append("status like :status and ");
                namedParameters.put("status", whereByParams.get(WHERE_PARAMS.STATUS_LIKE));
            }
            if(whereByParams.containsKey(WHERE_PARAMS.DATA_LIKE)){
                whereBuilder.append("data like :data and "); 
                namedParameters.put("data", whereByParams.get(WHERE_PARAMS.DATA_LIKE)); 
            }
            if (whereByParams.containsKey(WHERE_PARAMS.NOTAFTER_GREATERTHAN_CURRENTTIME)) {
                String currentTime = getDateFormat().format(new Date());  
                whereBuilder.append("notafter > :current_time and "); 
                // for old posgres 7 can use a string and PG will do conversion for you to Long  
                //namedParameters.put("current_time", currentTime); 
                // PG 8.4 is more strict and requires a Long (it will not convert for you)
                namedParameters.put("current_time",  Long.parseLong(currentTime));  
            }
            if(whereByParams.containsKey(WHERE_PARAMS.DN_HAS_RA_LIKE)){
                whereBuilder.append("dn like :dn_has_ra_like and "); 
                namedParameters.put("dn_has_ra_like", whereByParams.get(WHERE_PARAMS.DN_HAS_RA_LIKE)); 
            }
            
            // Always trim leading/trailing whitespace and remove trailling and (if any) 
            whereClause = whereBuilder.toString().trim();
            if (whereClause.endsWith("and")) {
                whereClause = whereClause.substring(0, whereClause.length() - 3);
            }
            whereClause = whereClause.trim();
        }
        // Build up the sql statement. 
        String sql = selectStatement + whereClause;
        if(orderby){
            sql = sql + " order by cert_key"; 
        }
          
        if (limit != null) {
            sql = sql + " LIMIT :limit";
            namedParameters.put("limit", limit);
        }
        if (offset != null) {
            sql = sql + " OFFSET :offset";
            namedParameters.put("offset", offset);
        }
        log.debug(sql);
        return Pair.create(sql.trim(), namedParameters);
    }

}
