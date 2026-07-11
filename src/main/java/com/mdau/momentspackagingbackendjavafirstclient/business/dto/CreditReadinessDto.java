package com.mdau.momentspackagingbackendjavafirstclient.business.dto;

import lombok.Getter;

/**
 * A transparent, informational readiness signal shown to a Business Account
 * holder and to admin — explicitly NOT a credit score and NOT used to
 * auto-approve anything. Real trade-credit decisions (Phase 2) require
 * documents and a human review regardless of this number. The breakdown is
 * always shown alongside the total so nothing here reads as a black box.
 */
@Getter
public class CreditReadinessDto {

    /** 0-100. */
    private final int score;
    private final String label;

    private final int orderCountPoints;
    private final int spendPoints;
    private final int accountAgePoints;
    private final int recencyPoints;

    /** Max points available per factor, for rendering progress bars. */
    private final int orderCountMax = 30;
    private final int spendMax = 35;
    private final int accountAgeMax = 20;
    private final int recencyMax = 15;

    public CreditReadinessDto(int orderCountPoints, int spendPoints, int accountAgePoints, int recencyPoints) {
        this.orderCountPoints = orderCountPoints;
        this.spendPoints = spendPoints;
        this.accountAgePoints = accountAgePoints;
        this.recencyPoints = recencyPoints;
        this.score = orderCountPoints + spendPoints + accountAgePoints + recencyPoints;
        this.label = score >= 70 ? "Strong" : score >= 40 ? "Promising" : "Building";
    }
}
