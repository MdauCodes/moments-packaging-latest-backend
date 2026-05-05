package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.lead.entity.Lead;
import com.mdau.momentspackagingbackendjavafirstclient.lead.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeadDigestEmailJob {

    private final LeadRepository leadRepository;
    private final EmailService   emailService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter
            .ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
            .withZone(ZoneId.of("Africa/Nairobi"));

    @Scheduled(cron = "0 0 8 * * MON-FRI", zone = "Africa/Nairobi")
    @SchedulerLock(
        name = "LeadDigestEmailJob",
        lockAtLeastFor = "PT2M",
        lockAtMostFor  = "PT10M"
    )
    @Transactional(readOnly = true)
    public void run() {
        log.info("LeadDigestEmailJob started");
        try {
            Instant since = Instant.now().minus(1, ChronoUnit.DAYS);
            List<Lead> leads = leadRepository.findRecentUncontacted(since);
            String period = DATE_FMT.format(Instant.now());
            emailService.sendLeadDigest(leads, period);
            log.info("LeadDigestEmailJob completed — {} leads in digest", leads.size());
        } catch (Exception e) {
            log.error("LeadDigestEmailJob failed: {}", e.getMessage(), e);
        }
    }
}