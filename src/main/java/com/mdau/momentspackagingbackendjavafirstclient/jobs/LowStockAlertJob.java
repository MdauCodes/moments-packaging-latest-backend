package com.mdau.momentspackagingbackendjavafirstclient.jobs;

import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.product.entity.Product;
import com.mdau.momentspackagingbackendjavafirstclient.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LowStockAlertJob {

    private final ProductRepository productRepository;
    private final EmailService      emailService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Africa/Nairobi")
    @SchedulerLock(name = "LowStockAlertJob",
            lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    @Transactional(readOnly = true)
    public void run() {
        log.info("LowStockAlertJob started");
        try {
            List<Product> lowStock = productRepository.findLowStockProducts();
            if (lowStock.isEmpty()) {
                log.info("LowStockAlertJob: no low stock products");
                return;
            }
            emailService.sendLowStockAlert(lowStock);
            log.info("LowStockAlertJob: alerted on {} products", lowStock.size());
        } catch (Exception e) {
            log.error("LowStockAlertJob failed: {}", e.getMessage(), e);
        }
    }
}