/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.ngs.dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import uk.ac.ngs.service.CrrManagerService;

/**
 * Generic tests.
 *
 * @author David Meredith
 */
public class GeneralTests {

    public GeneralTests() {
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
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //

    @Test 
    public void testCNformats(){
        // Note, this method uses 'matches()' method for a positive pattern match
        Pattern userCN_Pattern = Pattern.compile("^[\\-\\(\\)a-zA-Z0-9\\s]+$");
        assertTrue(userCN_Pattern.matcher("DAVE test the - (3rd)").matches()); 
        assertFalse(userCN_Pattern.matcher("DAVE test the - (3rd) +").matches()); 
        assertFalse(userCN_Pattern.matcher("DAVE - DAVE +").matches()); 
        assertFalse(userCN_Pattern.matcher("DAVE - DAVE /").matches()); 
        assertFalse(userCN_Pattern.matcher("' adfa ").matches()); 
        assertFalse(userCN_Pattern.matcher("davídó garçoné").matches()); 

        // Note, this method uses 'find()' to find an illegal char using negation       
        userCN_Pattern = Pattern.compile("[^A-Za-z0-9\\-\\(\\) ]");
        // no illegal chars - find = false
        assertFalse(userCN_Pattern.matcher("DAVE-DAVE").find()); 
        assertFalse(userCN_Pattern.matcher("DAVE - DAVE").find()); 
        assertFalse(userCN_Pattern.matcher("DAVE test the - (3rd) ").find()); 
        // with illegal chars - find = true
        assertTrue(userCN_Pattern.matcher("DAVE test the - (3rd) +").find()); 
        assertTrue(userCN_Pattern.matcher("DAVE - DAVE /").find()); 
        assertTrue(userCN_Pattern.matcher("' adfa ").find()); 
        assertTrue(userCN_Pattern.matcher("davídó garçoné").find()); 
        
        // Note, illegal multiple white space/tabs have to be tested separtely
        assertFalse(" hello world ".contains("  "));  // single space is ok  
        assertTrue("  hello".contains("  ")); // 2 spaces
        assertTrue("   hello".contains("  ")); // more than 2 spaces
        assertTrue("\ttab".contains("\t")); // and the tab 
        assertFalse(" hello world ".contains("\n"));  // newline 
        assertFalse(" hello world ".contains("\t"));  // tab 
        assertFalse(" hello world ".contains("\r"));  // carriage return 
        
    }

    @Test
    public void testMatcherRegex1() {
        Pattern DATA_NOTBEFORE_PATTERN = Pattern.compile("^\\s*NOTBEFORE\\s*=\\s*(.+)$", Pattern.MULTILINE);
        String dataCol =
                "some stuff\n"
                + "NOTBEFORE = Thu Jan 31 12:35:40 GMT 2013\n"
                + "some other stuff";

        Matcher m = DATA_NOTBEFORE_PATTERN.matcher(dataCol);
        assertTrue(m.find());
        assertEquals("Thu Jan 31 12:35:40 GMT 2013", m.group(1));
        //System.out.println(m.group(1));
    }

    @Test
    public void testDateFormat() {
        DateFormat utcTimeStampFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
        utcTimeStampFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        //String currentTime = utcTimeStampFormatter.format(new Date());
        //System.out.println("currentTime dave: " + currentTime);

        // Tue Apr 23 13:47:13 2013 UTC
        DateFormat df = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy zzz");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        //System.out.println("currentTime dave: " + df.format(new Date()));
        //System.out.println("currentTime dave: "+new Date().toString());   
    }

    @Test
    public void testLastActionDateRaopStringReplacements() {
        DateFormat utcDateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy zzz");
        utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String data = "-----BEGIN HEADER-----\n"
                + "CWIZPIN = FDCF0BE59E0F8283A38CCDC454F6C17263DDBDDF\n"
                + "NOTBEFORE = Tue Apr 23 13:32:57 BST 2013\n"
                + "PIN = FDCF0BE59E0F8283A38CCDC454F6C17263DDBDDF\n"
                + "PROFILE = UKPERSON\n"
                + "RA = Oxford OeSC\n"
                + "ROLE = User\n"
                + "SERIAL = 7493152\n"
                + "SUBJECT_ALT_NAME = Email: j.lidgard@physics.ox.ac.uk\n"
                + "TYPE = PKCS#10\n"
                + "VERSION = CertWizard 0.6\n"
                + "RAOP=36801\n"
                + "LAST_ACTION_DATE=Tue Apr 23 13:47:13 2013 UTC\n"
                + "-----END HEADER-----";

        //Pattern DATA_LAD_PATTERN = Pattern.compile("LAST_ACTION_DATE\\s?=\\s?(.+)$", Pattern.MULTILINE);
        Pattern DATA_LAD_PATTERN = Pattern.compile("LAST_ACTION_DATE\\s?=\\s?([^\\n]+)$", Pattern.MULTILINE);
        Matcher ladmatcher = DATA_LAD_PATTERN.matcher(data);
        String dateString = utcDateFormat.format(new Date()); 
        if (ladmatcher.find()) {
            //String lad = ladmatcher.group(1); 
            data = ladmatcher.replaceAll("LAST_ACTION_DATE = " + dateString);
        }

        Pattern DATA_RAOP_PATTERN = Pattern.compile("RAOP\\s?=\\s?([0-9]+)\\s*$", Pattern.MULTILINE);
        Matcher raopMatcher = DATA_RAOP_PATTERN.matcher(data);
        if (raopMatcher.find()) {
           data = raopMatcher.replaceAll("RAOP = 12345");  
        } 
        //System.out.println(data); 
        String expectedData = "-----BEGIN HEADER-----\n"
                + "CWIZPIN = FDCF0BE59E0F8283A38CCDC454F6C17263DDBDDF\n"
                + "NOTBEFORE = Tue Apr 23 13:32:57 BST 2013\n"
                + "PIN = FDCF0BE59E0F8283A38CCDC454F6C17263DDBDDF\n"
                + "PROFILE = UKPERSON\n"
                + "RA = Oxford OeSC\n"
                + "ROLE = User\n"
                + "SERIAL = 7493152\n"
                + "SUBJECT_ALT_NAME = Email: j.lidgard@physics.ox.ac.uk\n"
                + "TYPE = PKCS#10\n"
                + "VERSION = CertWizard 0.6\n"
                + "RAOP = 12345\n"
                + "LAST_ACTION_DATE = "+dateString+"\n"
                + "-----END HEADER-----"; 
        assertEquals(expectedData, data); 
    }

    @Test
    public void testLastActionDateRaopStringInserts() {
        DateFormat utcDateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy zzz");
        utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String data = "-----BEGIN HEADER-----\n"
                + "CWIZPIN = FDCF0BE59E0F8283A38CCDC454F6C17263DDBDDF\n"
                + "NOTBEFORE = Tue Apr 23 13:32:57 BST 2013\n"
                + "PIN = FDCF0BE59E0F8283A38CCDC454F6C17263DDBDDF\n"
                + "PROFILE = UKPERSON\n"
                + "RA = Oxford OeSC\n"
                + "ROLE = User\n"
                + "SERIAL = 7493152\n"
                + "SUBJECT_ALT_NAME = Email: j.lidgard@physics.ox.ac.uk\n"
                + "TYPE = PKCS#10\n"
                + "VERSION = CertWizard 0.6\n"
                + "-----END HEADER-----";

        String dateString = utcDateFormat.format(new Date()); 
        int raopId = 12345; 
        data = data.replaceAll("-----END HEADER-----", "RAOP = "+raopId+"\n-----END HEADER-----"); 
        data = data.replaceAll("-----END HEADER-----", "LAST_ACTION_DATE = "+dateString+"\n-----END HEADER-----"); 
        //System.out.println(data);

        //System.out.println(data); 
        String expectedData = "-----BEGIN HEADER-----\n"
                + "CWIZPIN = FDCF0BE59E0F8283A38CCDC454F6C17263DDBDDF\n"
                + "NOTBEFORE = Tue Apr 23 13:32:57 BST 2013\n"
                + "PIN = FDCF0BE59E0F8283A38CCDC454F6C17263DDBDDF\n"
                + "PROFILE = UKPERSON\n"
                + "RA = Oxford OeSC\n"
                + "ROLE = User\n"
                + "SERIAL = 7493152\n"
                + "SUBJECT_ALT_NAME = Email: j.lidgard@physics.ox.ac.uk\n"
                + "TYPE = PKCS#10\n"
                + "VERSION = CertWizard 0.6\n"
                + "RAOP = 12345\n"
                + "LAST_ACTION_DATE = "+dateString+"\n"
                + "-----END HEADER-----"; 
        assertEquals(expectedData, data); 
    }



    @Test
    public void testCRR_RAOPApproval() {
        DateFormat utcDateFormat = new SimpleDateFormat("E MMM dd HH:mm:ss yyyy zzz");
        utcDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String data = 
                "-----BEGIN HEADER----- \n" +
                "TYPE = CRR \n" +
                "SERIAL = 209665 \n" +
                "RAOP = 3503 \n" +
                "DELETED_DATE = Wed Feb  6 17:08:30 2013 UTC \n" +
                "-----END HEADER----- \n" +
                "SUBMIT_DATE = Wed Feb  6 17:08:30 2013 UTC \n";
        
        int raopId = 1111111; 
        JdbcCrrDao dao = new JdbcCrrDao(); 
        data = dao.updateDataCol_StatusRaop(data, CrrManagerService.CRR_STATUS.APPROVED.toString(), raopId);
        //System.out.println(data); 
        String expectedData = 
                 "-----BEGIN HEADER----- \n" +
                "TYPE = CRR \n" +
                "SERIAL = 209665 \n" +
                "RAOP = 3503 \n" +
                "RAOP = "+raopId+"\n" +
                "DELETED_DATE = Wed Feb  6 17:08:30 2013 UTC \n" +
                "-----END HEADER----- \n" +
                "SUBMIT_DATE = Wed Feb  6 17:08:30 2013 UTC \n";
        assertEquals(expectedData, data); 
    }


    @Test
    public void testExtractOwnerEmail(){
        // extract the OWNEREMAIL value
        Pattern DATA_OWNEREMAIL_PATTERN = Pattern.compile("OWNEREMAIL\\s?=\\s?([^\\n]+)$", Pattern.MULTILINE); 
        String data = "-----BEGIN HEADER-----\n" +
                "CSR_SERIAL=8432416\n" +
                "PIN=818107265254F8C6BA6FA3D15D7B62C97B1EF15D\n" +
                "emailAddress=tier1a-certificates@gridpp.rl.ac.uk\n" +
                "RAOP=38514\n" +
                "TYPE=PKCS#10\n" +
                "OWNEREMAIL=some.body@stfc.ac.uk\n" +
                "SUBJECT_ALT_NAME=DNS: fts-test03.gridpp.rl.ac.uk\n" +
                "OWNERSERIAL=38997\n" +
                "ROLE=User\n" +
                "OWNERDN=/C=UK/O=eScience/OU=CLRC/L=RAL/CN=some body\n" +
                "RA=CLRC RAL\n" +
                "PROFILE=UKHOST\n" +
                "-----END HEADER-----"; 
        
        Matcher ownerEmailMatcher = DATA_OWNEREMAIL_PATTERN.matcher(data);
        assertTrue(ownerEmailMatcher.find());
        String owneremail = ownerEmailMatcher.group(1).trim();   
        assertEquals("some.body@stfc.ac.uk", owneremail); 
       
        data = "-----BEGIN HEADER-----\n" +
                "CSR_SERIAL=8432416\n" +
                "PIN=818107265254F8C6BA6FA3D15D7B62C97B1EF15D\n" +
                "emailAddress=tier1a-certificates@gridpp.rl.ac.uk\n" +
                "RAOP=38514\n" +
                "TYPE=PKCS#10\n" +
                "OWNEREMAIL =  some.body@stfc.ac.uk  \n" +
                "SUBJECT_ALT_NAME=DNS: fts-test03.gridpp.rl.ac.uk\n" +
                "OWNERSERIAL=38997\n" +
                "ROLE=User\n" +
                "OWNERDN=/C=UK/O=eScience/OU=CLRC/L=RAL/CN=some body\n" +
                "RA=CLRC RAL\n" +
                "PROFILE=UKHOST\n" +
                "-----END HEADER-----"; 
        
        ownerEmailMatcher = DATA_OWNEREMAIL_PATTERN.matcher(data);
        assertTrue(ownerEmailMatcher.find());
        owneremail = ownerEmailMatcher.group(1).trim();   
        assertEquals("some.body@stfc.ac.uk", owneremail); 
    }

    
}
