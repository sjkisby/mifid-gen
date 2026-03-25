package com.example.mifid.resource;

import com.example.mifid.model.CostLineItem;
import com.example.mifid.model.MifidStatement;
import com.example.mifid.service.PdfGenerationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * REST endpoint for MiFID II PDF generation.
 *
 * POST /api/mifid/statement  → accepts JSON, returns PDF bytes
 * GET  /api/mifid/sample     → returns a sample PDF for PoC testing
 */
@Path("/api/mifid")
public class MifidPdfResource {

    private static final Logger LOG = Logger.getLogger(MifidPdfResource.class);

    @Inject
    PdfGenerationService pdfService;

    /**
     * Generate a PDF from a provided MiFID II statement payload.
     */
    @POST
    @Path("/statement")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("application/pdf")
    public Response generateStatement(MifidStatement statement) {
        try {
            byte[] pdf = pdfService.generateStatement(statement);
            String filename = "mifid-statement-" + statement.documentReference() + ".pdf";

            return Response.ok(pdf)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Length", pdf.length)
                    .build();

        } catch (IOException e) {
            LOG.errorf(e, "Failed to generate PDF for statement");
            return Response.serverError()
                    .entity("PDF generation failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * PoC convenience endpoint — returns a realistic sample statement.
     * Hit GET /api/mifid/sample in your browser to see the output.
     */
    @GET
    @Path("/sample")
    @Produces("application/pdf")
    public Response sampleStatement() {
        try {
            MifidStatement sample = buildSampleStatement();
            byte[] pdf = pdfService.generateStatement(sample);

            return Response.ok(pdf)
                    .header("Content-Disposition", "inline; filename=\"sample-mifid-statement.pdf\"")
                    .build();

        } catch (IOException e) {
            LOG.errorf(e, "Failed to generate sample PDF");
            return Response.serverError().build();
        }
    }

    // ─── Sample data builder ────────────────────────────────────────────────────

    private MifidStatement buildSampleStatement() {
        return new MifidStatement(
                // Firm
                "Acme Wealth Management Ltd",
                "1 Financial Square, London, EC2V 8RT",
                "FCA Ref: 123456",

                // Client
                "Jane Smith",
                "CLI-00123",
                "45 Investor Lane, London, SW1A 1AA",

                // Metadata
                LocalDate.now(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                "MIFID-2024-00123",

                // Investment
                "Model Portfolio – Balanced Growth",
                "Global Equity UCITS ETF",
                "IE00B4L5Y983",
                new BigDecimal("50000.00"),
                "GBP",

                // Cost line items — variable length, this is what breaks image overlays
                List.of(
                        new CostLineItem("One-off costs", "Entry costs (initial charge)", new BigDecimal("250.00"), new BigDecimal("0.50"), "GBP"),
                        new CostLineItem("One-off costs", "Exit costs (redemption fee)", new BigDecimal("0.00"), new BigDecimal("0.00"), "GBP"),
                        new CostLineItem("Ongoing costs", "Portfolio management fee", new BigDecimal("500.00"), new BigDecimal("1.00"), "GBP"),
                        new CostLineItem("Ongoing costs", "Administration & custody", new BigDecimal("125.00"), new BigDecimal("0.25"), "GBP"),
                        new CostLineItem("Ongoing costs", "Underlying fund OCF (ETF)", new BigDecimal("100.00"), new BigDecimal("0.20"), "GBP"),
                        new CostLineItem("Transaction costs", "Explicit transaction costs (commission)", new BigDecimal("75.00"), new BigDecimal("0.15"), "GBP"),
                        new CostLineItem("Transaction costs", "Implicit transaction costs (spread)", new BigDecimal("45.00"), new BigDecimal("0.09"), "GBP"),
                        new CostLineItem("Incidental costs", "Performance fee", new BigDecimal("0.00"), new BigDecimal("0.00"), "GBP"),
                        new CostLineItem("Ancillary costs", "Foreign exchange charges", new BigDecimal("30.00"), new BigDecimal("0.06"), "GBP")
                ),

                // Totals
                new BigDecimal("1125.00"),
                new BigDecimal("2.25"),

                // Effect on return
                new BigDecimal("6.50"),
                new BigDecimal("4.25")
        );
    }
}
