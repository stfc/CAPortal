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
import uk.ac.ngs.common.Pair;
import uk.ac.ngs.domain.CrrRow;

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
 * DAO for the <code>crr</code> DB table.
 *
 * @author David Meredith
 */
@Repository
public class JdbcCrrDao {

    private NamedParameterJdbcTemplate jdbcTemplate;
    //private final static DateFormat utcTimeStampFormatter = new SimpleDateFormat("yyyyMMddHHmmss"); 
    private static final Log log = LogFactory.getLog(JdbcCrrDao.class);
    private final static Pattern DATA_SUBMIT_DATE_PATTERN = Pattern.compile("^\\s*SUBMIT_DATE\\s*=\\s*(.+)$", Pattern.MULTILINE);
    /*
     * Define SQL query fragments used to build the DAOs queries.
     */
    public static final String SELECT_PROJECT = "select crr_key, cert_key, submit_date, "
            + "format, data, dn, cn, email, ra, rao, status, reason, loa from crr ";
    public static final String SELECT_COUNT = "select count(*) from crr ";
    private static final String SELECT_BY_ID = SELECT_PROJECT + "where crr_key = :crr_key ";
    private final DateFormat utcDateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy zzz");
    private final static Pattern DATA_RAOP_PATTERN = Pattern.compile("RAOP\\s?=\\s?([0-9]+)\\s*$", Pattern.MULTILINE);
    private final static Pattern DATA_DELETED_DATE_PATTERN = Pattern.compile("DELETED_DATE\\s?=\\s?([^\\n]+)$", Pattern.MULTILINE);

    // Both these formats have/may be used by different tools that build the
    // CSR data column (OpenCA, CAServer) 
    private final DateFormat dataNotBeforeDateFormat1 = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy zzz");
    private final DateFormat dataNotBeforeDateFormat2 = new SimpleDateFormat("E MMM dd HH:mm:ss zzz yyyy");


    public JdbcCrrDao() {
        this.utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Keys for defining 'where' parameters and values for building queries.
     * <p>
     * If the key ends with '_LIKE' then the where clause is appended with a SQL
     * 'like' clause (e.g. <code>key like 'value'</code>).
     * If the key ends with '_EQ' then the where clause is appended  with a SQL
     * '=' clause (e.g. <code>key = 'value'</code>).
     */
    public enum WHERE_PARAMS {

        DN_HAS_RA_LIKE, RA_EQ, STATUS_EQ, DN_LIKE, DATA_LIKE, CN_LIKE
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

    private final static class CrrRowMapper implements RowMapper<CrrRow> {

        public CrrRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            CrrRow row = new CrrRow();
            long lValcrr_key = rs.getLong("crr_key");
            if (!rs.wasNull()) {
                row.setCrr_key(lValcrr_key);
            }
            long lValcert_key = rs.getLong("cert_key");
            if (!rs.wasNull()) {
                row.setCert_key(lValcert_key);
            }
            row.setSubmit_date(rs.getString("submit_date"));
            row.setFormat(rs.getString("format"));
            row.setData(rs.getString("data"));
            row.setDn(rs.getString("dn"));
            row.setCn(rs.getString("cn"));
            row.setEmail(rs.getString("email"));
            row.setRa(rs.getString("ra"));
            row.setRao(rs.getString("rao"));
            row.setStatus(rs.getString("status"));
            row.setReason(rs.getString("reason"));
            row.setLoa(rs.getString("loa"));
            return row;
        }
    }

    /**
     * Find the specified revocation request in the <code>crr</code> table with
     * the given <code>crr_key</code>.
     *
     * @param crr_key
     * @return or null if no row is found.
     */
    public CrrRow findById(long crr_key) {
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("crr_key", crr_key);
        try {
            return this.jdbcTemplate.queryForObject(SELECT_BY_ID, namedParameters, new CrrRowMapper());
        } catch (EmptyResultDataAccessException ex) {
            log.warn("No crr row found with [" + crr_key + "]");
            return null;
        }
    }

    /**
     * Search for revocation requests using the search criteria specified in the given
     * where-by parameter map.
     * Multiple whereByParams are appended together using 'and' statements.
     *
     * @param whereByParams Search-by parameters used in where clause of SQL query.
     * @param limit         Limit the returned row count to this many rows or null not to specify a limit.
     * @param offset        Return rows from this row number or null not to specify an offset.
     * @return
     */
    public List<CrrRow> findBy(Map<WHERE_PARAMS, String> whereByParams, Integer limit, Integer offset) {
        Pair<String, Map<String, Object>> p = this.buildQuery(SELECT_PROJECT, whereByParams, limit, offset, true);
        log.debug("query: " + p.first);
        return this.jdbcTemplate.query(p.first, p.second, new CrrRowMapper());
    }

    /**
     * Count the total number of revocation requests using the search criteria specified
     * in the given where-by parameter map.
     *
     * @see #findBy(java.util.Map, java.lang.Integer, java.lang.Integer)
     */
    public int countBy(Map<WHERE_PARAMS, String> whereByParams) {
        Pair<String, Map<String, Object>> p = this.buildQuery(SELECT_COUNT, whereByParams, null, null, false);
        return this.jdbcTemplate.queryForObject(p.first, p.second, Integer.class);
    }

    /**
     * Iterate the given rows and set each
     * <code>row.dataSubmit_Date</code> property using the value of the SUBMIT_DATE
     * field within the <code>data</code> column (if present).
     *
     * @param rows
     * @return The updated rows.
     */
    public List<CrrRow> setSubmitDateFromData(List<CrrRow> rows) {
        for (CrrRow row : rows) {
            String data = row.getData();
            if (data != null) {
                Matcher matcher = DATA_SUBMIT_DATE_PATTERN.matcher(data);
                if (matcher.find()) {
                    // TODO create a date here instead of string 
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
                    row.setDataSubmit_Date(d);
                    //row.setDataSubmit_Date(dateTimeString);
                }
            }
        }
        return rows;
    }


    /**
     * Update the 'crr' table with the values from the given CrrRow.
     * The given CrrRow must have a populated <code>crr_key</code> value to
     * identify the correct row in the DB.
     *
     * @param crr Extracts values from this crr to update the db.
     * @return Number of rows updated (should always be 1)
     */
    public int updateCrrRow(CrrRow crr) {
        if (crr.getCrr_key() <= 0) {
            throw new IllegalArgumentException("Invalid crr, crr.crr_key is zero or negative");
        }
        String UPDATE_CRR_BY_CRR_KEY = "update crr set cert_key=:cert_key, submit_date=:submit_date, "
                + "format=:format, data=:data, dn=:dn, cn=:cn, email=:email, ra=:ra, "
                + "rao=:rao, status=:status, reason=:reason, loa=:loa where crr_key=:crr_key";

        Map<String, Object> namedParameters = this.buildParameterMap(crr);
        return this.jdbcTemplate.update(UPDATE_CRR_BY_CRR_KEY, namedParameters);
    }


    /**
     * Update the given data string according to the value of the given status and RAOP id.
     * A new RAOP key-value pair is appended after the last RAOP entry.
     *
     * @param data
     * @param newStatus
     * @param raopId
     * @return updated string (never null)
     */
    public String updateDataCol_StatusRaop(String data, String newStatus, long raopId) {
        if (data == null) {
            data = "-----END HEADER-----";
        }
        // Always Update/add RAOP 
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
        // if status is deleted, then update or add DELETED_DATE 
        if ("DELETED".equals(newStatus)) {
            // Update or add DATA_DELETED_DATE_PATTERN (add if it can't be found)
            Matcher delDatematcher = DATA_DELETED_DATE_PATTERN.matcher(data);
            String dateString = utcDateFormat.format(new Date());
            if (delDatematcher.find()) {
                data = delDatematcher.replaceAll("DELETED_DATE = " + dateString);
            } else {
                data = data.replaceAll("-----END HEADER-----", "DELETED_DATE = " + dateString + "\n-----END HEADER-----");
            }
        }
        // no other status supported yet
        return data;
    }


    /**
     * Get the next 'crr' table public key.
     *
     * @return
     */
    public long getNextCrr_key() {
        // We need to synch on the same lock regardless of the JdbcCrrDao 
        // instance (thus lock on the shared class object). 
        synchronized (JdbcCrrDao.class) {
            // I have no idea why we have to add 256 - back compatiblity with OpenCA ?
            return this.jdbcTemplate.getJdbcOperations().queryForObject("select max(crr_key) from crr", Long.class) + 256;
        }
    }

    /**
     * Insert a new row into the 'crr' table using the values from the given CrrRow.
     * Important: the crr_key (PK) must be set to a value that is not already in use
     * (the row PK is set by the calling client and not a db sequence -
     * this is an inherited legacy issue).
     *
     * @param crr
     * @return the number of rows affected (should always be 1)
     */
    public int insertCrrRow(CrrRow crr) {
        if (crr.getCrr_key() <= 0) {
            throw new IllegalArgumentException("Invalid crr, crr.crr_key is zero or negative");
        }
        Map<String, Object> namedParameters = this.buildParameterMap(crr);
        String INSERT_CRR = "insert into crr (crr_key, cert_key, submit_date, format, data, dn, cn, email, ra, rao, status, reason, loa) "
                + "values(:crr_key, :cert_key, :submit_date, :format, :data, :dn, :cn, :email, :ra, :rao, :status, :reason, :loa)";

        return this.jdbcTemplate.update(INSERT_CRR, namedParameters);
    }

    private Map<String, Object> buildParameterMap(CrrRow crr) {
        Map<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("cert_key", crr.getCert_key());
        namedParameters.put("submit_date", crr.getSubmit_date());
        namedParameters.put("format", crr.getFormat());
        namedParameters.put("data", crr.getData());
        namedParameters.put("dn", crr.getDn());
        namedParameters.put("cn", crr.getCn());
        namedParameters.put("email", crr.getEmail());
        namedParameters.put("ra", crr.getRa());
        namedParameters.put("rao", crr.getRao());
        namedParameters.put("status", crr.getStatus());
        namedParameters.put("reason", crr.getReason());
        namedParameters.put("loa", crr.getLoa());
        namedParameters.put("crr_key", crr.getCrr_key());
        return namedParameters;
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
            if (whereByParams.containsKey(WHERE_PARAMS.DN_HAS_RA_LIKE)) {
                whereBuilder.append("dn like :dnhasralike and ");
                namedParameters.put("dnhasralike", whereByParams.get(WHERE_PARAMS.DN_HAS_RA_LIKE));
            }
            if (whereByParams.containsKey(WHERE_PARAMS.STATUS_EQ)) {
                whereBuilder.append("status = :status and ");
                namedParameters.put("status", whereByParams.get(WHERE_PARAMS.STATUS_EQ));
            }
            if (whereByParams.containsKey(WHERE_PARAMS.DN_LIKE)) {
                whereBuilder.append("dn like :dn and ");
                namedParameters.put("dn", whereByParams.get(WHERE_PARAMS.DN_LIKE));
            }
            if (whereByParams.containsKey(WHERE_PARAMS.CN_LIKE)) {
                whereBuilder.append("cn like :cn and ");
                namedParameters.put("cn", whereByParams.get(WHERE_PARAMS.CN_LIKE));
            }
            if (whereByParams.containsKey(WHERE_PARAMS.DATA_LIKE)) {
                whereBuilder.append("data like :data and ");
                namedParameters.put("data", whereByParams.get(WHERE_PARAMS.DATA_LIKE));
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
            sql = sql + " order by crr_key";
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
}
