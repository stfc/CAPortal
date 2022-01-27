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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ngs.common.CertUtil;
import uk.ac.ngs.common.Pair;
import uk.ac.ngs.domain.CSR_Flags.Csr_Types;
import uk.ac.ngs.domain.CSR_Flags.Profile;
import uk.ac.ngs.domain.PKCS10_RequestWrapper;
import uk.ac.ngs.domain.RequestRow;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DAO for the <code>request</code> table.
 *
 * @author David Meredith
 */
@Repository
public class JdbcRequestDao {

    // status: APPROVED, ARCHIVED, DELETED, HOLD, NEW, OBJSIGN, ON HOLD, RENEW, ROBOT
    private NamedParameterJdbcTemplate jdbcTemplate;
    //private final static DateFormat utcTimeStampFormatter = new SimpleDateFormat("yyyyMMddHHmmss"); 
    private static final Log log = LogFactory.getLog(JdbcRequestDao.class);
    /*
     * Define SQL query fragments used to build the DAOs queries.
     */
    public static final String SELECT_PROJECT =
            "select req_key, format, data, dn, cn, email, ra, rao, status, role, "
                    + "public_key, scep_tid, loa, bulk from request ";
    public static final String SELECT_COUNT = "select count(*) from request ";
    public static final String SQL_SELECT_BY_ID = SELECT_PROJECT + "where req_key = :req_key ";


    private final static Pattern DATA_LAD_PATTERN = Pattern.compile("LAST_ACTION_DATE\\s?=\\s?([^\\n]+)$", Pattern.MULTILINE);
    private final static Pattern DATA_RAOP_PATTERN = Pattern.compile("RAOP\\s?=\\s?([0-9]+)\\s*$", Pattern.MULTILINE);
    // Date will be of the form:  Tue Apr 23 13:47:13 2013 UTC 
    private final DateFormat utcDateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy zzz");

    // Both these formats have/may be used by different tools that build the 
    // CSR data column (OpenCA, CAServer) 
    private final DateFormat dataNotBeforeDateFormat1 = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy zzz");
    private final DateFormat dataNotBeforeDateFormat2 = new SimpleDateFormat("E MMM dd HH:mm:ss zzz yyyy");


    public JdbcRequestDao() {
        this.utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Keys for defining 'where' parameters and values for building queries.
     * <p>
     * If the key ends with '_LIKE' then the where clause is appended with a SQL
     * 'like' clause (e.g. <code>key like 'value'</code>).
     * If the key ends with '_EQ' then the where clause is appended  with a SQL
     * '=' clause (e.g. <code>key = 'value'</code>).
     * _EQ takes precedence over _LIKE so if both EMAIL_EQ and EMAIL_LIKE are given, then
     * only EMAIL_EQ is used to create the query.
     * <p>
     * STATUS_EQ_NEW_or_RENEW overrides STATUS_EQ (the former is a shortcut/hack
     * to limit the results to status = NEW or status = RENEW).
     */
    public enum WHERE_PARAMS {

        RA_EQ, ROLE_EQ, STATUS_EQ, STATUS_EQ_NEW_or_RENEW, DATA_LIKE,
        CN_LIKE, DN_LIKE, EMAIL_EQ, EMAIL_LIKE, PUBKEY_EQ, BULKID_EQ,
    }

    private final static Pattern DATA_NOTBEFORE_PATTERN = Pattern.compile("^\\s*NOTBEFORE\\s*=\\s*(.+)$", Pattern.MULTILINE);

    private static DateFormat getDateFormat() {
        DateFormat utcTimeStampFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
        utcTimeStampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcTimeStampFormatter;
    }

    /**
     * Set the JDBC dataSource.
     *
     * @param dataSource
     */
    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    private static class RequestRowMapper implements RowMapper<RequestRow> {

        public RequestRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            RequestRow row = new RequestRow();
            // rs.getLong() returns 0 if SQL Null is returned - so you have 
            // to test if the last return value was null ! 
            long lValreq_key = rs.getLong("req_key");
            if (!rs.wasNull()) {
                row.setReq_key(lValreq_key);
            }
            // rs.getString() returns null if the value was SQL Null
            row.setFormat(rs.getString("format"));
            row.setData(rs.getString("data"));
            row.setDn(rs.getString("dn"));
            row.setCn(rs.getString("cn"));
            row.setEmail(rs.getString("email"));
            row.setRa(rs.getString("ra"));
            row.setRao(rs.getString("rao"));
            row.setStatus(rs.getString("status"));
            row.setRole(rs.getString("role"));
            row.setPublic_key(rs.getString("public_key"));
            row.setScep_tid(rs.getString("scep_tid"));
            row.setLoa(rs.getString("loa"));

            long lValbulk = rs.getLong("bulk");
            if (!rs.wasNull()) {
                row.setBulk(lValbulk);
            }
            return row;
        }
    }

    /**
     * Query the 'request' table with the given SQL where clause values.
     * Multiple whereByParams are appended together using 'and' statements.
     *
     * @param whereByParams Search-by parameters used in where clause of SQL query.
     * @param limit         Limit the returned row count to this many rows or null not to specify a limit.
     * @param offset        Return rows from this row number or null not to specify an offset.
     * @return
     */
    public List<RequestRow> findBy(Map<WHERE_PARAMS, String> whereByParams, Integer limit, Integer offset) {
        Pair<String, Map<String, Object>> p = this.buildQuery(SELECT_PROJECT, whereByParams, limit, offset, true);
        return this.jdbcTemplate.query(p.first, p.second, new RequestRowMapper());
    }

    /**
     * Count the number of rows that match the given SQL where clause values.
     *
     * @param whereByParams Search-by parameters used in where clause of SQL query.
     * @return
     * @see #findBy(java.util.Map, java.lang.Integer, java.lang.Integer)
     */
    public int countBy(Map<WHERE_PARAMS, String> whereByParams) {
        Pair<String, Map<String, Object>> p = this.buildQuery(SELECT_COUNT, whereByParams, null, null, false);
        return this.jdbcTemplate.queryForObject(p.first, p.second, Integer.class);
    }

    /**
     * Count the number of rows that have the given public key with a <tt>status</tt>
     * that is not <tt>DELETED</tt>.
     *
     * @param public_key Must be formatted according to the format output by
     *                   <tt>openssl -text format</tt> of the public key
     * @return
     */
    public int countByPublicKeyNotDeleted(String public_key) {
        //String query = SELECT_PROJECT + "where public_key = :public_key and status <> 'DELETED'";
        String query = SELECT_COUNT + "where public_key = :public_key and status <> 'DELETED'";
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("public_key", public_key);
        //return this.jdbcTemplate.query(query, namedParameters, new RequestRowMapper()); 
        return this.jdbcTemplate.queryForObject(query, namedParameters, Integer.class);
    }

    /**
     * Does the given DN belong to a <tt>VALID</tt> non-expired certificate that
     * was requested as part of a bulk, if true return bulkId or null if false.
     * <p>
     * This can be re-phrased as: "Does the DN belong to a VALID bulk certificate".
     * In the case that there are many (historic) requests for the certificate all having
     * (historic) bulk Ids, then return the maximum value (i.e. the last bulkId).
     *
     * @param rfc2253RenewDN
     * @return bulkID or null if DN does not map to a VALID bulk certificate
     */
    public Long getBulkIdForCertBy_Dn_Valid_NotExpired(String rfc2253RenewDN) {
        // The certificate may be expired and still be marked as "VALID" (and NOT as "EXPIRED"), 
        // we therefore need the manual timestamp check to test for expired certs. 
        Calendar calendarLocal = Calendar.getInstance(); // current time in default locale 
        // Get time in past 'notafterTolerance' number of days ago - used to define how many 
        // days a certificate can be expired but still eligible for renewal.  
        // E.g. to subtract 5 days from the current time of the calendar, specify -5
        //calendarLocal.add(Calendar.DAY_OF_MONTH, notafterTolerance);
        String currentTimeUTC = getDateFormat().format(calendarLocal.getTime());

        // manually join across certificate and request table using req_key as FK 
        String query = "SELECT max(bulk) "
                + "FROM request WHERE req_key IN "
                + "(SELECT req_key FROM certificate WHERE dn = :dn AND status = 'VALID' AND notafter > :currentTime)"
                //+ "(SELECT req_key FROM certificate WHERE dn ILIKE :dn AND status = 'VALID' AND notafter > :currentTime)" 
                + " AND bulk IS NOT NULL";
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("dn", rfc2253RenewDN);
        namedParameters.put("currentTime", Long.valueOf(currentTimeUTC));

        // queryForObject returns null in case of SQL NULL (note, the use of the
        // SQL max() function means the query always returns a value and so we don't 
        // get an 'IncorrectResultSizeDataAccessException' if there is no bulk 
        Number number = this.jdbcTemplate.queryForObject(query, namedParameters, Long.class);
        Long maxBulkId = (number != null ? number.longValue() : null);

        return maxBulkId;
    }


    /**
     * Count the number of requests that already exists in the <tt>request</tt> table
     * with the specified DN and with a request.status of 'NEW', 'APPROVED' or 'RENEW'.
     * The DN check is case-insensitive (case is ignored).
     * <p>
     * CSRs in the <tt>request</tt> table usually have an RFC 2253 form:
     * <tt>CN=serv01.foo.esc.rl.ac.uk,L=DL,OU=CLRC,O=eScience,C=UK</tt>
     * i.e. no email address or other attributes, but this is not strictly the case.
     *
     * @param rfc2253DN DN String
     * @return true if a request exists with the above pre-conditions, otherwise false.
     */
    public int countByDnWhereStatusNewApprovedRenew(String rfc2253DN) {
        String query = SELECT_COUNT +
                " where dn ILIKE :dn and (status='NEW' or status = 'APPROVED' or status = 'RENEW')";
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("dn", rfc2253DN);
        return this.jdbcTemplate.queryForObject(query, namedParameters, Integer.class);
    }


    /**
     * Find the specified request that has the given req_key value.
     *
     * @param req_key
     * @return row or null if no row is found
     */
    public RequestRow findById(long req_key) {
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("req_key", req_key);
        try {
            return this.jdbcTemplate.queryForObject(SQL_SELECT_BY_ID, namedParameters, new RequestRowMapper());
        } catch (EmptyResultDataAccessException ex) {
            log.warn("No request row found with [" + req_key + "]");
            return null;
        }
    }

    /**
     * Iterate the given rows and set each <code>row.dataNotBefore</code> property
     * using the value of the NOTBEFORE field within the <code>data</code> column (if present).
     *
     * @param rows
     * @return The updated rows.
     */
    public List<RequestRow> setDataNotBefore(List<RequestRow> rows) {
        for (RequestRow row : rows) {
            String data = row.getData();
            if (data != null) {
                Matcher matcher = DATA_NOTBEFORE_PATTERN.matcher(data);
                if (matcher.find()) {
                    String dateTimeString = matcher.group(1);
                    Date d = null;
                    try {
                        d = dataNotBeforeDateFormat1.parse(dateTimeString);
                    } catch (ParseException ex) {
                    }
                    if (d == null) {
                        try {
                            d = dataNotBeforeDateFormat2.parse(dateTimeString);
                        } catch (ParseException ex) {
                        }
                    }
                    if (d == null) {
                        log.error("Error - can't parse CSR Data column NOTBEFORE attribute [" + dateTimeString + "]");
                    }
                    row.setDataNotBefore(d);
                }
            }
        }
        return rows;
    }

    /**
     * Update the 'request' table with the values from the given RequestRow (csr).
     * The given RequestRow must have a populated <code>req_key</code> value to
     * identify the correct row in the DB.
     *
     * @param csr Extracts values from this csr to update the db.
     * @return Number of rows updated (should always be 1)
     */
    public int updateRequestRow(RequestRow csr) {
        if (csr.getReq_key() <= 0) {
            throw new IllegalArgumentException("Invalid csr, csr.req_key is zero or negative");
        }
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("req_key", csr.getReq_key());
        namedParameters.put("format", csr.getFormat());
        namedParameters.put("data", csr.getData());
        namedParameters.put("dn", csr.getDn());
        namedParameters.put("cn", csr.getCn());
        namedParameters.put("email", csr.getEmail());
        namedParameters.put("ra", csr.getRa());
        namedParameters.put("rao", csr.getRao());
        namedParameters.put("status", csr.getStatus());
        namedParameters.put("role", csr.getRole());

        namedParameters.put("public_key", csr.getPublic_key());
        namedParameters.put("scep_tid", csr.getScep_tid());
        namedParameters.put("loa", csr.getLoa());
        namedParameters.put("bulk", csr.getBulk());
        //namedParameters.put("exported", csr.getExported());

        String updateStmt = "update request set "
                + "format=:format, data=:data, dn=:dn, cn=:cn, email=:email, "
                + "ra=:ra, rao=:rao, status=:status, role=:role, public_key=:public_key, "
                + "scep_tid=:scep_tid, loa=:loa, bulk=:bulk "
                + "where req_key=:req_key";
        return this.jdbcTemplate.update(updateStmt, namedParameters);
    }

    /**
     * Insert a new row into the 'request' table with the values from the given RequetRow (csr).
     * The given RequestRow must have a populated <code>req_key</code> value as
     * this is the PK which has to be provided, use: {@link #getNextPrimaryKey()}
     * to get the next value.
     *
     * @param csr
     */
    public void insertRequestRow(RequestRow csr) {
        if (csr.getReq_key() <= 0) {
            throw new IllegalArgumentException("Invalid csr, csr.req_key is zero or negative");
        }
        this.jdbcTemplate.getJdbcOperations().update("insert into request "
                        + "(req_key, format, data, dn, cn, email, ra, rao, status, role, "
                        + "public_key, scep_tid, loa, bulk) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                csr.getReq_key(), csr.getFormat(), csr.getData(), csr.getDn(), csr.getCn(), csr.getEmail(),
                csr.getRa(), csr.getRao(), csr.getStatus(), csr.getRole(),
                csr.getPublic_key(), csr.getScep_tid(), csr.getLoa(), csr.getBulk());
    }

    /**
     * Gets the next request PK.
     *
     * @return req_key
     */
    public long getNextPrimaryKey() {
        synchronized (JdbcRequestDao.class) {
            Long current = this.jdbcTemplate.getJdbcOperations().queryForObject("select max(req_key) from request", Long.class);
            return current + 256;
        }
    }


    /**
     * Update the LAST_ACTION_DATE and RAOP values in the given data string.
     * The LAST_ACTION_DATE key-value pair is updated/replaced if present or added if not.
     * A new RAOP key-value pair is appended after the last RAOP entry.
     *
     * @param data   String to update. If null, then an empty string is created/modified and returned.
     * @param raopId the RA operator id used to update the RAOP key value or -1 to exclude RAOP update.
     * @return updated string (never null)
     */
    public String updateDataCol_LastActionDateRaop(String data, long raopId) {
        if (data == null) {
            data = "-----END HEADER-----";
        }
        // Update/add RAOP 
        if (raopId != -1) {
            Matcher raopMatcher = DATA_RAOP_PATTERN.matcher(data);
            if (raopMatcher.find()) {
                // append a new 'RAOP = raopId' line after the last found RAOP line
                int lastIndex = raopMatcher.end();
                while (raopMatcher.find()) {
                    lastIndex = raopMatcher.end();
                }
                String dataStart = data.substring(0, lastIndex);
                String dataMiddle = "\nRAOP = " + raopId;
                String dataEnd = data.substring(lastIndex);
                data = dataStart + dataMiddle + dataEnd;
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
     * Build up the query using the given where by parameters in the map and
     * return the query and the named parameter map for subsequent
     * parameter-binding/execution.
     */
    protected Pair<String, Map<String, Object>> buildQuery(String selectStatement,
                                                           Map<WHERE_PARAMS, String> whereByParams, Integer limit, Integer offset, boolean orderby) {

        String whereClause = "";
        Map<String, Object> namedParameters = new HashMap<>();
        if (whereByParams != null && !whereByParams.isEmpty()) {
            StringBuilder whereBuilder = new StringBuilder("where ");
            if (whereByParams.containsKey(WHERE_PARAMS.RA_EQ)) {
                whereBuilder.append("ra = :ra and ");
                namedParameters.put("ra", whereByParams.get(WHERE_PARAMS.RA_EQ));
            }

            // status 
            if (whereByParams.containsKey(WHERE_PARAMS.STATUS_EQ_NEW_or_RENEW)) {
                whereBuilder.append("(status = :status1 or status = :status2) and ");
                namedParameters.put("status1", "NEW");
                namedParameters.put("status2", "RENEW");
            } else if (whereByParams.containsKey(WHERE_PARAMS.STATUS_EQ)) {
                whereBuilder.append("status = :status and ");
                namedParameters.put("status", whereByParams.get(WHERE_PARAMS.STATUS_EQ));
            }

            if (whereByParams.containsKey(WHERE_PARAMS.ROLE_EQ)) {
                whereBuilder.append("role = :role and ");
                namedParameters.put("role", whereByParams.get(WHERE_PARAMS.ROLE_EQ));
            }
            if (whereByParams.containsKey(WHERE_PARAMS.DATA_LIKE)) {
                whereBuilder.append("data like :data and ");
                namedParameters.put("data", whereByParams.get(WHERE_PARAMS.DATA_LIKE));
            }
            if (whereByParams.containsKey(WHERE_PARAMS.CN_LIKE)) {
                whereBuilder.append("cn like :cn and ");
                namedParameters.put("cn", whereByParams.get(WHERE_PARAMS.CN_LIKE));
            }
            if (whereByParams.containsKey(WHERE_PARAMS.DN_LIKE)) {
                whereBuilder.append("dn like :dn and ");
                namedParameters.put("dn", whereByParams.get(WHERE_PARAMS.DN_LIKE));
            }
            if (whereByParams.containsKey(WHERE_PARAMS.PUBKEY_EQ)) {
                whereBuilder.append("public_key = :public_key and ");
                namedParameters.put("public_key", whereByParams.get(WHERE_PARAMS.PUBKEY_EQ));
            }

            // EQ takes precidence over LIKE 
            if (whereByParams.containsKey(WHERE_PARAMS.EMAIL_EQ)) {
                //log.debug("Searching for null email - check val ["+whereByParams.get(WHERE_PARAMS.EMAIL_EQ)+"]"); 
                if (whereByParams.get(WHERE_PARAMS.EMAIL_EQ) == null) {
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
            if (whereByParams.containsKey(WHERE_PARAMS.BULKID_EQ)) {
                whereBuilder.append("bulk = :bulk and ");
                long bulkId = Long.parseLong(whereByParams.get(WHERE_PARAMS.BULKID_EQ));
                namedParameters.put("bulk", bulkId);
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
        if (orderby) {
            sql = sql + " order by req_key";
        }

        if (limit != null) {
            sql = sql + " LIMIT :limit";
            namedParameters.put("limit", limit);
        }
        if (offset != null) {
            sql = sql + " OFFSET :offset";
            namedParameters.put("offset", offset);
        }
        return Pair.create(sql.trim(), namedParameters);
    }


    /**
     * Builds a value for the <tt>data</tt> column from the given parameters.
     *
     * @param req_key    This is the PK of the request table which is also stored
     *                   for reference in the data column.
     * @param csrWrapper Collates various parameters for saving in data column.
     * @param pinHash    A base64 encoded hash of the user's PIN number.
     * @param bulk       If this request belongs to a bulk, provide the bulk ID or null for non-bulk.
     * @param version    The value of the 'VERSION' attribute
     * @param role
     * @return Data column value as a string
     */
    public String getDataColumnValue(long req_key, PKCS10_RequestWrapper csrWrapper, String pinHash, Long bulk, String version, String role) {
        String header = "-----BEGIN HEADER-----";
        String footer = "-----END HEADER-----";
        String NEWLINE = "\n";
        String dataColumnBlob = header + NEWLINE;

        // I don't think data header attributes need to be in a certain order 
        // for subsequent reading by OpenCA?

        if (Csr_Types.NEW.equals(csrWrapper.getCsr_type()) && Profile.UKHOST.equals(csrWrapper.getProfile())) {
            // Following client* data are from the personal cert that
            // was used to authenticate this request (e.g. to pass PPPK) 
            // 
            // clientDN is in RFC2253 format (e.g. "CN=david meredith ral,L=RAL,OU=CLRC,O=eScienceDev,C=UK")
            // however, in the OWNERDN attribute of the 'data' field is formatted
            // in reverse and uses the / char as the separator, so the DN becomes: 
            // "/C=UK/O=eScienceDev/OU=CLRC/L=RAL/CN=david meredith ral" 
            dataColumnBlob += "OWNERDN = " + CertUtil.getReverseSlashSeparatedDN(csrWrapper.getClientDN()) + NEWLINE;
            if (csrWrapper.getClientEmail() != null) {
                dataColumnBlob += "OWNEREMAIL = " + csrWrapper.getClientEmail() + NEWLINE;
            }
            dataColumnBlob += "OWNERSERIAL = " + csrWrapper.getClientSerial() + NEWLINE;
        }

        dataColumnBlob = dataColumnBlob + "TYPE = PKCS#10" + NEWLINE;  // PKCS#10 
        dataColumnBlob = dataColumnBlob + "VERSION = " + version + NEWLINE;
        dataColumnBlob = dataColumnBlob + "PROFILE = " + csrWrapper.getProfile() + NEWLINE; // UKHOST or UKPERSON
        if (bulk != null) {
            dataColumnBlob += "BULK = " + bulk + NEWLINE;
        }
        dataColumnBlob = dataColumnBlob + "SERIAL = " + req_key + NEWLINE; //getRequestPrimaryKey() + NEWLINE; // next request key PK
        dataColumnBlob = dataColumnBlob + "NOTBEFORE = " + (new Date()) + NEWLINE;
        dataColumnBlob = dataColumnBlob + "PIN = " + pinHash + NEWLINE;
        // We never use ADDITIONAL_ATTRIBUTE_EMAIL - is this correct?

        dataColumnBlob = dataColumnBlob + "RA = " + csrWrapper.getP10Ou() + " " + csrWrapper.getP10Loc() + NEWLINE;
        dataColumnBlob = dataColumnBlob + "ROLE = " + role + NEWLINE;
        // result = result + "RAOP = " + NEWLINE; // This is done later on RA approval: 

        // For personal certs there should be a subject alternative name entry 
        // defined in this data column header. 
        // This should be the SAME email of the personal email address. 
        // Host certs do not have Email in the sub alt name (but they could do). 
        // This email is the email address that is submitted in the 
        // <Email> element of the csr! 
        if (Profile.UKPERSON.equals(csrWrapper.getProfile())) {
            dataColumnBlob = dataColumnBlob + "SUBJECT_ALT_NAME = Email: " + csrWrapper.getEmail() + NEWLINE;
        } else if (Profile.UKHOST.equals(csrWrapper.getProfile())) {
            dataColumnBlob = dataColumnBlob + "SUBJECT_ALT_NAME = DNS: " + csrWrapper.getP10CN() + NEWLINE;
        }
        if (Csr_Types.RENEW.equals(csrWrapper.getCsr_type())) {
            dataColumnBlob = dataColumnBlob + "RENEWAL = true" + NEWLINE;
        }

        dataColumnBlob = dataColumnBlob + footer + NEWLINE;
        dataColumnBlob = dataColumnBlob + csrWrapper.getCsrPemString();
        return dataColumnBlob;
    }

}
