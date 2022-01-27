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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ngs.domain.RalistRow;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO for the CA DB
 * <code>ralist</code> table.
 *
 * @author David Meredith
 */
@Repository
public class JdbcRalistDao {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private static final Log log = LogFactory.getLog(JdbcRalistDao.class);
    private static final String LIMIT_ROWS = "LIMIT :limit ";
    private static final String OFFSET_ROWS = "OFFSET :offset ";
    private static final String ORDER_BY = "order by order_id ";
    public static final String SQL_SELECT_ALL = "select ra_id, order_id, ou, l, active from ralist ";

    /**
     * Set the JDBC dataSource.
     *
     * @param dataSource
     */
    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    private static final class RalistRowMapper implements RowMapper<RalistRow> {

        @Override
        public RalistRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            RalistRow row = new RalistRow();
            row.setRa_id(rs.getInt("ra_id"));
            row.setOrder_id(rs.getInt("order_id"));
            row.setL(rs.getString("l"));
            row.setOu(rs.getString("ou"));
            row.setActive(rs.getBoolean("active"));
            return row;
        }
    }

    /**
     * Return all the rows in the
     * <code>ralist</code> table.
     *
     * @param limit to this many rows (null is for no limit)
     * @param offset results by this number of rows (null for no offset)
     * @return
     */

    /**
     * Return all the rows in the <code>ralist</code> table according to
     * whether the row is active (or not).
     *
     * @param active true or false (null for any active state)
     * @param limit  to this many rows (null is for no limit)
     * @param offset results by this number of rows (null for no offset)
     * @return
     */
    public List<RalistRow> findAllByActive(Boolean active, Integer limit, Integer offset) {
        Map<String, Object> namedParameters = new HashMap<>();
        String query = SQL_SELECT_ALL;
        if (active != null) {
            query += "where active = :active ";
            namedParameters.put("active", active);
        }
        query += ORDER_BY;

        // limit/offset 
        if (limit != null) {
            query += LIMIT_ROWS;
            namedParameters.put("limit", limit);
        }
        if (offset != null) {
            query += OFFSET_ROWS;
            namedParameters.put("offset", offset);
        }
        return this.jdbcTemplate.query(query, namedParameters, new JdbcRalistDao.RalistRowMapper());
    }
}
