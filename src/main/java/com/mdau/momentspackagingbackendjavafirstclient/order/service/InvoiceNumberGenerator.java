package com.mdau.momentspackagingbackendjavafirstclient.order.service;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.InvoiceCounter;
import com.mdau.momentspackagingbackendjavafirstclient.order.repository.InvoiceCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Generates this business's own internal sequential invoice number — issued
 * only once payment is confirmed (see PaymentService), not at checkout,
 * since not every checkout results in a paid order.
 *
 * Deliberately prefixed "MP-INV-" rather than any format resembling KRA's
 * actual eTIMS control-unit invoice numbering: this is an internal sales
 * document, not a KRA-transmitted tax invoice, and should never look like
 * one. See the invoice PDF's disclaimer for the same reasoning.
 */
@Service
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceCounterRepository counterRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generate() {
        String year = String.valueOf(LocalDate.now().getYear());

        InvoiceCounter counter = counterRepository.findByYearForUpdate(year)
                .orElseGet(() -> counterRepository.save(
                        InvoiceCounter.builder().year(year).counter(0).build()));

        counter.setCounter(counter.getCounter() + 1);
        counterRepository.save(counter);

        return String.format("MP-INV-%s-%06d", year, counter.getCounter());
    }
}
