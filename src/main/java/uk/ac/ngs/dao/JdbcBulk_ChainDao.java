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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * DAO for the <code>bulk_chain</code> table.
 * DAO assumes the <code>seq_bulk</code> has also been deployed. 
 *
 * @author David Meredith
 */
@Repository
public class JdbcBulk_ChainDao {

    private NamedParameterJdbcTemplate jdbcTemplate;
    //private final static DateFormat utcTimeStampFormatter = new SimpleDateFormat("yyyyMMddHHmmss"); 
    private static final Log log = LogFactory.getLog(JdbcBulk_ChainDao.class);



    public  JdbcBulk_ChainDao() {
    }

    /**
     * For the given oldId return the corresponding newId OR insert a new row 
     * that maps oldId to a newly created newId (and return newId). 
     * <p>
     * The newid is created using <code>nextval('seq_bulk')</code>.
     * 
     * @param oldId
     * @return newid or null if a newid value can't be created/returned  
     */
    public Long getCreateNewIdForOldId(long oldId){
        log.debug("select newid from bulk_chain where oldid = "+oldId);
        
        String query = "SELECT newid FROM bulk_chain WHERE oldid = :oldId"; 
        Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("oldId", oldId); 

        // If newBulkId is not null, then a certificate from this bulk has 
        // already been renewed (or a renew was previously submitted but was not approved, i.e. pending/deleted). 
        // In this case, the newid already exists for this bulk and we must re-use it. 
        Long newBulkId = this.jdbcTemplate.query(query, namedParameters, new ResultSetExtractor<Long>() {
            @Override
            public Long extractData(ResultSet rs) throws SQLException, DataAccessException {
               return rs.next() ? rs.getLong("newid") : null; 
            }
        }); 

        if(newBulkId != null){
            log.debug("newid exists reusing - "+newBulkId);
        } else {
            // A null newBulkId means that this is the first certificate 
            // renewal for this bulk (note, this could be the very first renewal 
            // following the initial bulk-submission or a renewal of a bulk that has 
            // been renewed before, i.e. renew of previously renewed cert). 
            // We therefore need to create a new entry in 
            // bulk_chain table that correlates the old bulkId to its new bulkid. 
            // Before doing this however, we check once more using a table lock. 
        
            // lock table and try again! (DM: don't think we strictly need lock statement but heyho?)  
            // LOCK TABLE can only be used in transaction blocks. 
            // LOCK TABLE obtains a table-level lock, waiting if necessary for any conflicting locks to be released. 
            // If a tx is going to change the data in the table, then it should use SHARE ROW EXCLUSIVE lock mode instead of SHARE mode.
            // @see: http://www.postgresql.org/docs/8.3/static/sql-lock.html 
            this.jdbcTemplate.getJdbcOperations().execute("LOCK TABLE bulk_chain IN SHARE ROW EXCLUSIVE MODE");
            
            newBulkId = this.jdbcTemplate.query(query, namedParameters, new ResultSetExtractor<Long>() {
                @Override
                public Long extractData(ResultSet rs) throws SQLException, DataAccessException {
                    return rs.next() ? rs.getLong("newid") : null;
                }
            }); 
            if (newBulkId == null) {
                log.debug("newid was null, inserting new row into bulk_chain"); 

                // newid really does not exist, so insert and get the next newid value
                String insert = "INSERT INTO bulk_chain( oldid, newid ) VALUES( " + oldId + ", nextval('seq_bulk') )";
                this.jdbcTemplate.getJdbcOperations().update(insert); // returns number of rows affected (we know it is 1)
                // re-issue query to get the new 'newid' - note newid is created in above insert statement
                // so we can issue a 'queryForObject' query (this query can throw 
                // EmptyResultDataAccessException if value is not returned but we don't expect that)   
                Number number = this.jdbcTemplate.queryForObject(query, namedParameters, Long.class);
                newBulkId = (number != null ? number.longValue() : null); // deal with SQL NULL 
                if(newBulkId == null){
                    throw new RuntimeException("newid was SQL NULL - DB appears to be in an inconsistent state"); 
                }

            }
        }
        return newBulkId;
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
}
