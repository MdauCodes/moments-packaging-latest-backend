package com.mdau.momentspackagingbackendjavafirstclient.order.dto;

import com.mdau.momentspackagingbackendjavafirstclient.order.entity.OrderStatusHistory;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class OrderStatusHistoryDto {
    private final UUID   id;
    private final String fromStatus;
    private final String toStatus;
    private final String note;
    private final String changedBy;
    private final Instant changedAt;

    public OrderStatusHistoryDto(OrderStatusHistory h) {
        this.id         = h.getId();
        this.fromStatus = h.getFromStatus() != null ? h.getFromStatus().name() : null;
        this.toStatus   = h.getToStatus().name();
        this.note       = h.getNote();
        this.changedBy  = h.getChangedBy();
        this.changedAt  = h.getChangedAt();
    }
}