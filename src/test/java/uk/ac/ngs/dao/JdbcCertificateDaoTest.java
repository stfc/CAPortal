/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.dao;

import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import uk.ac.ngs.common.Pair;
import uk.ac.ngs.dao.JdbcCertificateDao.WHERE_PARAMS;

/**
 *
 * @author David Meredith
 */
public class JdbcCertificateDaoTest {
    
    public JdbcCertificateDaoTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
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
        Map<WHERE_PARAMS, String> params = new HashMap<WHERE_PARAMS, String>();
        params.put(WHERE_PARAMS.CN_LIKE, "david meredith");
        params.put(WHERE_PARAMS.ROLE_LIKE, "CA Operator"); 
        params.put(WHERE_PARAMS.STATUS_LIKE, "VALID"); 
        
        Integer limit = 10;
        Integer offset = 0;
        JdbcCertificateDao instance = new JdbcCertificateDao();
        String expResult = "select cert_key, data, dn, cn, email, status, role, notafter from certificate where cn like :cn and role like :role and status like :status LIMIT :limit OFFSET :offset";
        Pair<String, Map<String, Object>> p = instance.buildQuery(JdbcCertificateDao.SELECT_PROJECT, params, limit, offset, false);
        System.out.println(p.first); 
        assertEquals(expResult, p.first);
        
        expResult = "select count(*) from certificate where cn like :cn and role like :role and status like :status LIMIT :limit OFFSET :offset";
        Pair<String, Map<String, Object>> p2 = instance.buildQuery(JdbcCertificateDao.SELECT_COUNT, params, limit, offset, false);
        System.out.println(p2.first); 
        assertEquals(expResult, p2.first);
        
        expResult = "select count(*) from certificate";
        Pair<String, Map<String, Object>> p3 = instance.buildQuery(JdbcCertificateDao.SELECT_COUNT, null, null, null, false);
        System.out.println(p3.first); 
        assertEquals(expResult, p3.first);
        
        
        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }




}
