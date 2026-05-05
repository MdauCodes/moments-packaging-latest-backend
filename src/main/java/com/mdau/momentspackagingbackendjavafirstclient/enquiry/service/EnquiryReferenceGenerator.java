package com.mdau.momentspackagingbackendjavafirstclient.enquiry.service;

import com.mdau.momentspackagingbackendjavafirstclient.enquiry.entity.EnquiryCounter;
import com.mdau.momentspackagingbackendjavafirstclient.enquiry.repository.EnquiryCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnquiryReferenceGenerator {

    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM");

    private final EnquiryCounterRepository counterRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate() {
        String yearMonth = LocalDate.now().format(MONTH_FMT);

        EnquiryCounter counter = counterRepository
                .findByYearMonthForUpdate(yearMonth)
                .orElseGet(() -> EnquiryCounter.builder()
                        .yearMonth(yearMonth)
                        .counter(0)
                        .build());

        counter.setCounter(counter.getCounter() + 1);
        counterRepository.save(counter);

        String seq = String.format("%04d", counter.getCounter());
        return "ENQ-" + yearMonth + "-" + seq;
    }
}