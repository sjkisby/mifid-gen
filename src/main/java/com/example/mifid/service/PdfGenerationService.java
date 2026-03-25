package com.example.mifid.service;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import com.example.mifid.model.MifidStatement;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import java.net.URI;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;

/**
 * Converts a MifidStatement → HTML (via Qute) → PDF (via OpenHTMLtoPDF).
 *
 * Pipeline:
 *   MifidStatement  →  Qute template renders HTML  →  OpenHTMLtoPDF renders PDF bytes
 *
 * Key OpenHTMLtoPDF features used:
 *   - useFont()         : embed a custom font (e.g. a branded typeface)
 *   - useSVGDrawer()    : render SVG logos/charts inline
 *   - useDefaultPageSize: A4 portrait
 *   - PDF/A-1b output   : archival-quality, suitable for regulatory documents
 */
@ApplicationScoped
public class PdfGenerationService {

    private static final Logger LOG = Logger.getLogger(PdfGenerationService.class);

    @Inject
    Template mifidStatement; // Qute resolves src/main/resources/templates/mifidStatement.html

    /**
     * Render a MiFID II costs & charges statement to PDF bytes.
     *
     * @param statement  populated data model
     * @return           PDF as a byte array, ready to stream or store
     */
    public byte[] generateStatement(MifidStatement statement) throws IOException {

        // 1. Render HTML from template
        String html = mifidStatement
                .data("statement", statement)
                .render();

        LOG.debugf("Rendered HTML (%d chars) for statement %s",
                html.length(), statement.documentReference());

        // 2. Convert HTML → PDF
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PdfRendererBuilder builder = new PdfRendererBuilder();

            var fontsBase = "/fonts/";

            var regular = new File(getClass().getResource(fontsBase + "Inter_18pt-Regular.ttf").toURI());
            var medium  = new File(getClass().getResource(fontsBase + "Inter_18pt-Medium.ttf").toURI());
            var bold    = new File(getClass().getResource(fontsBase + "Inter_18pt-Bold.ttf").toURI());

            builder
                .useFont(regular, "Inter", 400, BaseRendererBuilder.FontStyle.NORMAL, true)
                .useFont(medium,  "Inter", 500, BaseRendererBuilder.FontStyle.NORMAL, true)
                .useFont(bold,    "Inter", 700, BaseRendererBuilder.FontStyle.NORMAL, true)
                .withHtmlContent(html, getBaseUri())
                .useSVGDrawer(new BatikSVGDrawer())
                .toStream(baos)
                .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_1_B)
                .run();

            LOG.infof("Generated PDF (%d bytes) for statement %s",
                    baos.size(), statement.documentReference());

            return baos.toByteArray();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IOException("Error generating PDF", e);
        }
    }

    /**
     * Base URI used to resolve relative resource paths (fonts, images) in the HTML.
     * In production, point this to your classpath resources base or a CDN.
     */
    private String getBaseUri() {
        var resource = getClass().getResource("/templates/");
        return resource != null ? resource.toString() : "";
    }
}
