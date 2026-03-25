package com.example.mifid.model;

import java.math.BigDecimal;

/**
 * Represents a single cost line in the MiFID II costs & charges breakdown.
 * Maps to a row in the costs table — can repeat N times.
 */
public record CostLineItem(
        String category,        // e.g. "One-off costs", "Ongoing costs"
        String description,     // e.g. "Entry costs", "Portfolio management fee"
        BigDecimal amount,      // In reporting currency
        BigDecimal percentage,  // As % of investment amount
        String currency
) {
    public String formattedAmount() {
        return String.format("%,.2f %s", amount, currency);
    }

    public String formattedPercentage() {
        return String.format("%.2f%%", percentage);
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isZeroPercentage() {
        return percentage.compareTo(BigDecimal.ZERO) == 0;
    }
}
