package com.mdau.momentspackagingbackendjavafirstclient.lead.dto;

import com.mdau.momentspackagingbackendjavafirstclient.lead.entity.Lead;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class LeadDto {

    private final UUID    id;
    private final String  email;
    private final String  persona;
    private final String  source;
    private final String  trigger;
    private final Boolean contacted;
    private final Instant createdAt;
    private final Instant updatedAt;

    public LeadDto(Lead lead) {
        this.id        = lead.getId();
        this.email     = lead.getEmail();
        this.persona   = lead.getPersona();
        this.source    = lead.getSource();
        this.trigger   = lead.getTrigger();
        this.contacted = lead.getContacted();
        this.createdAt = lead.getCreatedAt();
        this.updatedAt = lead.getUpdatedAt();
    }
}