package com.example.mifid.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Top-level data model for a MiFID II Costs and Charges Statement.
 * This is the object passed into the Qute template.
 */
public record MifidStatement(

        // ── Firm details ──────────────────────────────────────────────────
        String firmName,
        String firmAddress,
        String firmFcaReference,

        // ── Client details ────────────────────────────────────────────────
        String clientName,
        String clientReference,
        String clientAddress,

        // ── Statement metadata ────────────────────────────────────────────
        LocalDate statementDate,
        LocalDate periodStart,
        LocalDate periodEnd,
        String documentReference,

        // ── Investment details ────────────────────────────────────────────
        String portfolioName,
        String instrumentName,
        String isin,
        BigDecimal investmentAmount,
        String currency,

        // ── Cost breakdown ────────────────────────────────────────────────
        // Repeating rows — this is the key structure image-overlay can't handle
        List<CostLineItem> costLineItems,

        // ── Aggregated totals ─────────────────────────────────────────────
        BigDecimal totalCostsAmount,
        BigDecimal totalCostsPercentage,

        // ── Effect on return ──────────────────────────────────────────────
        BigDecimal projectedReturnBeforeCosts,
        BigDecimal projectedReturnAfterCosts

) {
    /** Convenience: formatted investment amount */
    public String formattedInvestmentAmount() {
        return String.format("%,.2f %s", investmentAmount, currency);
    }

    public String formattedTotalCosts() {
        return String.format("%,.2f %s", totalCostsAmount, currency);
    }

    public String formattedTotalCostsPercentage() {
        return String.format("%.2f%%", totalCostsPercentage);
    }
}
