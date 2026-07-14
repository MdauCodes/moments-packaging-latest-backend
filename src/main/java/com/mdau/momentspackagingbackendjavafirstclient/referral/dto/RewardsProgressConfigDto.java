package com.mdau.momentspackagingbackendjavafirstclient.referral.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Public, non-personal config the cart/checkout FAB banner needs to compute
 * "spend KES X more to unlock Y" client-side, once combined with the
 * customer's own lifetime points (from GET /customer/referral/wallet when
 * logged in). Anonymous visitors get this same config but have no wallet, so
 * the frontend shows a create-account nudge instead.
 */
@Getter
@AllArgsConstructor
public class RewardsProgressConfigDto {
    private int pointsPer100Kes;
    private List<RewardsTierConfigDto> tiers;
}
