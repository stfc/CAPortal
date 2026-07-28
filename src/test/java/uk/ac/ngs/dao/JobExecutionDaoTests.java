package uk.ac.ngs.dao;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class JobExecutionDaoTests {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private JobExecutionDao jobExecutionDao;

    @Before
    public void setUp() {
        jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        jobExecutionDao = new JobExecutionDao(jdbcTemplate);
    }

    @Test
    public void shouldReturnLastRunDate() {

        String jobName = "CERT_EXPIRY_REMINDER";
        LocalDate expectedDate = LocalDate.of(2026, 5, 10);

        when(jdbcTemplate.queryForObject(
                anyString(),
                any(Map.class),
                any(RowMapper.class)))
                .thenReturn(expectedDate);

        LocalDate result = jobExecutionDao.getLastRunDate(jobName);

        assertNotNull(result);
        assertEquals(expectedDate, result);

        verify(jdbcTemplate).queryForObject(
                anyString(),
                argThat((Map<String, Object> params) -> jobName.equals(params.get("jobName"))),
                ArgumentMatchers.<RowMapper<LocalDate>>any());
    }

    @Test
    public void shouldReturnNullWhenNoRecordExists() {

        when(jdbcTemplate.queryForObject(
                anyString(),
                any(Map.class),
                any(RowMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        LocalDate result = jobExecutionDao.getLastRunDate("CERT_EXPIRY_REMINDER");

        assertNull(result);
    }

    @Test
    public void shouldMapSqlDateToLocalDate() throws Exception {

        ResultSet rs = mock(ResultSet.class);

        java.sql.Date sqlDate = java.sql.Date.valueOf("2026-05-10");

        when(rs.getDate("last_run_date")).thenReturn(sqlDate);

        RowMapper<LocalDate> mapper = (r, rowNum) -> r.getDate("last_run_date").toLocalDate();

        LocalDate result = mapper.mapRow(rs, 1);

        assertEquals(LocalDate.of(2026, 5, 10), result);
    }

    @Test
    public void shouldUpdateLastRunDate() {

        String jobName = "CERT_EXPIRY_REMINDER";
        LocalDate date = LocalDate.of(2026, 5, 11);

        when(jdbcTemplate.update(anyString(), any(Map.class)))
                .thenReturn(1);

        jobExecutionDao.updateLastRunDate(jobName, date);

        verify(jdbcTemplate).update(
                anyString(),
                argThat((Map<String, Object> params) -> jobName.equals(params.get("jobName")) &&
                        java.sql.Date.valueOf(date).equals(params.get("lastRunDate"))));

    }

    @Test
    public void shouldExecuteCorrectSqlForUpdate() {

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        when(jdbcTemplate.update(sqlCaptor.capture(), any(Map.class)))
                .thenReturn(1);

        jobExecutionDao.updateLastRunDate(
                "CERT_EXPIRY_REMINDER",
                LocalDate.now());

        String executedSql = sqlCaptor.getValue();

        assertTrue(executedSql.contains("INSERT INTO job_execution_tracker"));
        assertTrue(executedSql.contains("ON CONFLICT"));
    }

    @Test
    public void shouldStillExecuteUpdateEvenIfZeroRowsAffected() {

        when(jdbcTemplate.update(anyString(), any(Map.class)))
                .thenReturn(0);

        jobExecutionDao.updateLastRunDate(
                "CERT_EXPIRY_REMINDER",
                LocalDate.now());

        verify(jdbcTemplate).update(anyString(), any(Map.class));
    }

}
