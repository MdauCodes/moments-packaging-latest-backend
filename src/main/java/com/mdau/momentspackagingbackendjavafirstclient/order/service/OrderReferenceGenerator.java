package com.mdau.momentspackagingbackendjavafirstclient.order.service;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderCounter;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.OrderCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OrderReferenceGenerator {

    private final OrderCounterRepository counterRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate() {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        OrderCounter counter = counterRepository.findByYearMonthForUpdate(yearMonth)
                .orElseGet(() -> counterRepository.save(
                        OrderCounter.builder().yearMonth(yearMonth).counter(0).build()));

        counter.setCounter(counter.getCounter() + 1);
        counterRepository.save(counter);

        return String.format("ORD-%s-%04d", yearMonth, counter.getCounter());
    }
}