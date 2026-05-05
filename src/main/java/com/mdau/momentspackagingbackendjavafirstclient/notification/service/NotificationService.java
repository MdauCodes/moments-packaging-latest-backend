package com.mdau.momentspackagingbackendjavafirstclient.notification.service;

import com.mdau.momentspackagingbackendjavafirstclient.email.service.EmailService;
import com.mdau.momentspackagingbackendjavafirstclient.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final SmsService   smsService;

    @Async
    public void onOrderCreated(Order order) {
        try {
            emailService.sendOrderConfirmedEmail(order);
        } catch (Exception e) {
            log.error("onOrderCreated email failed for {}: {}", order.getReference(), e.getMessage());
        }
        try {
            smsService.sendSms(order.getPhone(),
                    "Asante! Order " + order.getReference() + " imepokelewa. KES "
                    + order.getTotalAmount() + ". - Moments Packaging");
        } catch (Exception e) {
            log.error("onOrderCreated SMS failed for {}: {}", order.getReference(), e.getMessage());
        }
    }

    @Async
    public void onOrderPaid(Order order) {
        try {
            emailService.sendOrderPaidEmail(order);
        } catch (Exception e) {
            log.error("onOrderPaid email failed for {}: {}", order.getReference(), e.getMessage());
        }
        try {
            smsService.sendSms(order.getPhone(),
                    "Malipo yamepokelewa! Order " + order.getReference()
                    + " iko in production. - Moments Packaging");
        } catch (Exception e) {
            log.error("onOrderPaid SMS failed for {}: {}", order.getReference(), e.getMessage());
        }
    }

    @Async
    public void onOrderInProduction(Order order) {
        try {
            emailService.sendOrderInProductionEmail(order);
        } catch (Exception e) {
            log.error("onOrderInProduction email failed for {}: {}", order.getReference(), e.getMessage());
        }
        try {
            smsService.sendSms(order.getPhone(),
                    "Order " + order.getReference()
                    + " iko production. Itakuwa tayari siku 7-14. - Moments Packaging");
        } catch (Exception e) {
            log.error("onOrderInProduction SMS failed for {}: {}", order.getReference(), e.getMessage());
        }
    }

    @Async
    public void onOrderDispatched(Order order) {
        try {
            emailService.sendOrderDispatchedEmail(order);
        } catch (Exception e) {
            log.error("onOrderDispatched email failed for {}: {}", order.getReference(), e.getMessage());
        }
        try {
            smsService.sendSms(order.getPhone(),
                    "Order " + order.getReference()
                    + " imesafirishwa! Utawasiliana nawe kwa delivery. - Moments Packaging");
        } catch (Exception e) {
            log.error("onOrderDispatched SMS failed for {}: {}", order.getReference(), e.getMessage());
        }
    }

    @Async
    public void onOrderDelivered(Order order) {
        try {
            emailService.sendOrderDeliveredEmail(order);
        } catch (Exception e) {
            log.error("onOrderDelivered email failed for {}: {}", order.getReference(), e.getMessage());
        }
        try {
            smsService.sendSms(order.getPhone(),
                    "Order " + order.getReference()
                    + " imewasilishwa! Asante kwa kuchagua Moments Packaging!");
        } catch (Exception e) {
            log.error("onOrderDelivered SMS failed for {}: {}", order.getReference(), e.getMessage());
        }
    }

    @Async
    public void onOrderCancelled(Order order) {
        try {
            emailService.sendOrderCancelledEmail(order);
        } catch (Exception e) {
            log.error("onOrderCancelled email failed for {}: {}", order.getReference(), e.getMessage());
        }
        try {
            smsService.sendSms(order.getPhone(),
                    "Order " + order.getReference()
                    + " imefutwa. Maswali? WhatsApp +254700000000");
        } catch (Exception e) {
            log.error("onOrderCancelled SMS failed for {}: {}", order.getReference(), e.getMessage());
        }
    }
}