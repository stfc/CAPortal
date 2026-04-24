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

package uk.ac.ngs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import uk.ac.ngs.service.CertificateService;

@Configuration
@EnableScheduling
public class TaskConfig implements SchedulingConfigurer {
    public static final Logger log = LoggerFactory.getLogger(TaskConfig.class);

    private CertificateService certificateService;

    public TaskConfig(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Scheduled(cron = "0 44 15 * * ?")
    public void runDailyCertificateExpiryReminderJob() {

        log.info("Starting daily certificate expiry reminder job");

        long startTime = System.currentTimeMillis();

        try {
            certificateService.sendCertificateExpiryReminders();

            long durationMs = System.currentTimeMillis() - startTime;
            log.info("Completed daily certificate expiry reminder job successfully in {} ms",
                    durationMs);

        } catch (Exception ex) {
            log.error("Daily certificate expiry reminder job failed", ex);
            throw ex;
        }

    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());
    }

    
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("cert-expiry-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}
