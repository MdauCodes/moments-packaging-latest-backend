package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.backup.service.DatabaseBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily 2am dump of every table (data only) to Cloudinary, for disaster recovery. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseBackupJob {

    private final DatabaseBackupService databaseBackupService;

    @Scheduled(cron = "0 0 2 * * *", zone = "Africa/Nairobi")
    @SchedulerLock(name = "DatabaseBackupJob",
            lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void run() {
        log.info("DatabaseBackupJob started");
        try {
            DatabaseBackupService.BackupResult result = databaseBackupService.runBackup();
            log.info("DatabaseBackupJob: backed up {} table(s), {} row(s), {} bytes -> {}",
                    result.tableCount(), result.totalRows(), result.sizeBytes(), result.cloudinaryUrl());
        } catch (Exception e) {
            log.error("DatabaseBackupJob failed: {}", e.getMessage(), e);
        }
    }
}
