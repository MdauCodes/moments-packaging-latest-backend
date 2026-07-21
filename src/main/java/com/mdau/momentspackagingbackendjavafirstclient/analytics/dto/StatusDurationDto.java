package com.mdau.momentspackagingbackendjavafirstclient.analytics.dto;

/** Average time an order spends in a given status before moving to the next one, in hours.
 *  sampleCount is how many completed (order already moved on) transitions the average is based
 *  on — a status with a low sample count is still the current stage for most orders, so a huge
 *  or tiny average there is expected, not a data problem. */
public record StatusDurationDto(String status, double avgHours, long sampleCount) {
}
