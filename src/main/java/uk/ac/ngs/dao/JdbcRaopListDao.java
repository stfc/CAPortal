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
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.ac.ngs.domain.RaopListRow;

/**
 * DAO for <code>raoplist</code> table actions.  
 * 
 * @author David Meredith
 */
@Repository
public class JdbcRaopListDao {

    private NamedParameterJdbcTemplate jdbcTemplate;
    //private final static DateFormat utcTimeStampFormatter = new SimpleDateFormat("yyyyMMddHHmmss"); 
    private static final Log log = LogFactory.getLog(JdbcRaopListDao.class);

    public final String SELECT_PROJECT = "select ou, l, name, email, phone, "
                + "street, city, postcode, cn, manager, operator, trainingdate, "
                + "title, conemail, location, ra_id, department_hp, "
                + "institute_hp, active, ra_id2 from raoplist"; 
    
    /**
     * Set the JDBC dataSource. 
     * @param dataSource
     */
    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }
    
    
    private final static class RaopRowMapper implements RowMapper<RaopListRow> {

        @Override
        public RaopListRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            RaopListRow row = new RaopListRow(); 
            row.setOu(rs.getString("ou")); 
            row.setL(rs.getString("l"));
            row.setName(rs.getString("name"));
            row.setEmail(rs.getString("email"));
            row.setPhone(rs.getString("phone"));
            row.setStreet(rs.getString("street")); 
            row.setCity(rs.getString("city")); 
            row.setPostcode(rs.getString("postcode")); 
            row.setCn(rs.getString("cn")); 
            row.setManager(rs.getBoolean("manager"));
            row.setOperator(rs.getBoolean("operator"));
            row.setTrainingDate(rs.getDate("trainingdate")); 
            row.setTitle(rs.getString("title")); 
            row.setConeemail(rs.getString("conemail")); 
            row.setLocation(rs.getString("location"));  
            row.setRa_id(rs.getInt("ra_id"));
            row.setDepartment_hp(rs.getString("department_hp")); 
            row.setInstitute_hp(rs.getString("institute_hp"));  
            row.setActive(rs.getBoolean("active"));  
            row.setRa_id2(rs.getInt("ra_id2"));
            return row;
        }
        
    }
    
    /**
     * Search the <pre>raoplist</pre> table by the specified search parameters. 
     * All parameters are nullable. 
     * @param ou
     * @param l
     * @param cn
     * @param active
     * @return list of records that satisfy the specified search parameters. 
     */
    public List<RaopListRow> findBy(String ou, String l, String cn, Boolean active){
        Map<String, Object> namedParameters = new HashMap<String, Object>();
        StringBuilder whereBuilder = new StringBuilder(); 
        if(ou != null){
            whereBuilder.append("and ou = :ou "); 
            namedParameters.put("ou", ou); 
        }
        if(l != null){
            whereBuilder.append("and l = :l "); 
            namedParameters.put("l", l); 
        }
        if(cn != null){
            whereBuilder.append("and cn = :cn "); 
            namedParameters.put("cn", cn); 
        }
        if(active != null){
            whereBuilder.append("and active = :active "); 
            namedParameters.put("active", active); 
        }
        // if one of the params was set above, then insert the where clause 
        String sqlWhere = "";
        if(whereBuilder.toString().startsWith("and ")){
            sqlWhere = " where "+whereBuilder.toString().substring(4, whereBuilder.toString().length()); 
        } 
        return this.jdbcTemplate.query(SELECT_PROJECT+sqlWhere, 
                namedParameters, new RaopRowMapper());   
    }

    /**
     * Find the <tt>raoplist</tt> row with the specified ra_id. 
     * @param ra_id
     * @return RaopListRow or null if not found 
     */
    public RaopListRow findBy(long ra_id){
        String query = SELECT_PROJECT + " where ra_id = :ra_id"; 
        Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("ra_id", ra_id);
        try { 
            return this.jdbcTemplate.queryForObject(query, namedParameters, new RaopRowMapper());
        } catch (EmptyResultDataAccessException ex) {
            log.warn("No raoplist row found with [" + ra_id + "]");
            return null;
        } 
    }

    
    
    

}
