package com.mdau.momentspackagingbackendjavafirstclient.order.entity;

public enum FulfillmentType {
    ZONE_DELIVERY,   // customer pays delivery fee calculated by zone — active default
    PICKUP,          // customer collects from shop — enabled via setting
    OWN_COURIER      // customer arranges own courier (matatu / parcel service / Bolt Send / rider).
                     // Goods price paid at checkout; transport cost confirmed by phone at dispatch.
                     // Customer can then pay transport or revert to PICKUP — handled off-platform.
}