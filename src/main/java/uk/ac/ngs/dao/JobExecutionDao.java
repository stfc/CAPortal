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

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Repository
public class JobExecutionDao {
    private NamedParameterJdbcTemplate jdbcTemplate;
    private static final Log log = LogFactory.getLog(JobExecutionDao.class);

    public JobExecutionDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public LocalDate getLastRunDate(String jobName) {

        String sql = """
                SELECT last_run_date
                FROM job_execution_tracker
                WHERE job_name = :jobName
                """;

        Map<String, Object> params = Map.of("jobName", jobName);

        try {
            LocalDate result = jdbcTemplate.queryForObject(sql, params,
                    (rs, rowNum) -> rs.getDate("last_run_date").toLocalDate());

            log.info("Last run date for job '" + jobName + "' is " + result);
            return result;

        } catch (EmptyResultDataAccessException ex) {
            log.warn("No last run date found for job '" + jobName + "'. Assuming first run.");
            return null; // first run case
        }
    }

    public void updateLastRunDate(String jobName, LocalDate lastRunDate) {
        // This query inserts a new job record if it doesn’t exist, 
        // or updates its last run date if it already exists, using PostgreSQL’s UPSERT mechanism.
        String sql = """
                INSERT INTO job_execution_tracker (job_name, last_run_date)
                VALUES (:jobName, :lastRunDate)
                ON CONFLICT (job_name)
                DO UPDATE SET last_run_date = EXCLUDED.last_run_date
                """;

        Map<String, Object> params = Map.of(
                "jobName", jobName,
                "lastRunDate", java.sql.Date.valueOf(lastRunDate));

        int rows = jdbcTemplate.update(sql, params);

        log.info("Updated last run date for job '" + jobName + "' to " + lastRunDate + " (rows affected: " + rows + ")");
    }

}
