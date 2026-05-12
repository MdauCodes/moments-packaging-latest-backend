package com.mdau.momentspackagingbackendjavafirstclient.order.entity;

public enum FulfillmentType {
    ZONE_DELIVERY,   // customer pays delivery fee -- active default
    PICKUP,          // customer collects -- disabled via setting
    OWN_COURIER      // customer arranges own courier -- disabled via setting
}
