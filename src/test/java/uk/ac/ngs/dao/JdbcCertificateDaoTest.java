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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.ac.ngs.common.Pair;
import uk.ac.ngs.dao.JdbcCertificateDao.WHERE_PARAMS;
import uk.ac.ngs.domain.CertificateRow;

/**
 *
 * @author David Meredith
 */
public class JdbcCertificateDaoTest {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private JdbcCertificateDao jdbcCertificateDao;
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        jdbcCertificateDao = new JdbcCertificateDao(jdbcTemplate);
    }
    
    @After
    public void tearDown() {
    }


    /**
     * Test of findBy method, of class JdbcCertificateDao.
     */
    @Test
    public void testFindBy() {
        System.out.println("findBy");
        Map<WHERE_PARAMS, String> params = new HashMap<>();
        params.put(WHERE_PARAMS.CN_LIKE, "david meredith");
        params.put(WHERE_PARAMS.ROLE_LIKE, "CA Operator"); 
        params.put(WHERE_PARAMS.STATUS_LIKE, "VALID"); 
        
        Integer limit = 10;
        Integer offset = 0;
        String expResult = "select cert_key, data, dn, cn, email, status, role, notafter from certificate where cn like :cn and role like :role and status like :status LIMIT :limit OFFSET :offset";
        Pair<String, Map<String, Object>> p = jdbcCertificateDao.buildQuery(JdbcCertificateDao.SELECT_PROJECT, params, limit, offset, false);
        System.out.println(p.first); 
        assertEquals(expResult, p.first);
        
        expResult = "select count(*) from certificate where cn like :cn and role like :role and status like :status LIMIT :limit OFFSET :offset";
        Pair<String, Map<String, Object>> p2 = jdbcCertificateDao.buildQuery(JdbcCertificateDao.SELECT_COUNT, params, limit, offset, false);
        System.out.println(p2.first); 
        assertEquals(expResult, p2.first);
        
        expResult = "select count(*) from certificate";
        Pair<String, Map<String, Object>> p3 = jdbcCertificateDao.buildQuery(JdbcCertificateDao.SELECT_COUNT, null, null, null, false);
        System.out.println(p3.first); 
        assertEquals(expResult, p3.first);
        
        
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    @Test
    public void testFindActiveUserAndRAOperatorBy_withValidInputs_returnsResults() {
        String ou = "OU1";
        String o = "O1";
        String loc = "LOC1";

        List<CertificateRow> expectedRows = List.of(new CertificateRow(), new CertificateRow());

        // Stub jdbcTemplate
        when(jdbcTemplate.query(anyString(), anyMap(), ArgumentMatchers.<RowMapper<CertificateRow>>any()))
                .thenReturn(expectedRows);

        // Act
        List<CertificateRow> result = jdbcCertificateDao.findActiveUserAndRAOperatorBy(ou, o, loc);

        // Assert
        assertEquals(2, result.size());

        // Capture the query string
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(queryCaptor.capture(), anyMap(), ArgumentMatchers.<RowMapper<CertificateRow>>any());

        String capturedQuery = queryCaptor.getValue();

        // Assert parts of the query
        assertTrue(capturedQuery.contains("role = 'RA Operator'"));
        assertTrue(capturedQuery.contains("role = 'User'"));
        assertTrue(capturedQuery.contains("dn like :ra"));
        assertTrue(capturedQuery.contains("status = 'VALID'"));
        assertTrue(capturedQuery.contains("notafter > :current_time"));
    }

    @Test
    public void testFindActiveCAs_withValidInputs_returnsResults() {

        List<CertificateRow> expectedRows = List.of(new CertificateRow(), new CertificateRow());

        // Stub jdbcTemplate
        when(jdbcTemplate.query(anyString(), anyMap(), ArgumentMatchers.<RowMapper<CertificateRow>>any()))
                .thenReturn(expectedRows);

        // Act
        List<CertificateRow> result = jdbcCertificateDao.findActiveCAs();

        // Assert
        assertEquals(2, result.size());

        // Capture the query string
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(queryCaptor.capture(), anyMap(), ArgumentMatchers.<RowMapper<CertificateRow>>any());

        String capturedQuery = queryCaptor.getValue();

        // Assert parts of the query
        assertTrue(capturedQuery.contains("role = 'CA Operator'"));
        assertTrue(capturedQuery.contains("status = 'VALID'"));
        assertTrue(capturedQuery.contains("notafter > :current_time"));
    }

}
