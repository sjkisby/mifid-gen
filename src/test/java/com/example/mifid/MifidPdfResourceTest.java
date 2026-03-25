package com.example.mifid;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MifidPdfResourceTest {

    @Test
    void sampleEndpoint_returnsPdf() {
        Response response = given()
                .when()
                .get("/api/mifid/sample")
                .then()
                .statusCode(200)
                .contentType("application/pdf")
                .extract().response();

        byte[] pdfBytes = response.asByteArray();
        assertTrue(pdfBytes.length > 1000, "PDF should be non-trivially sized");

        // PDF magic bytes: %PDF
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    void postEndpoint_withValidPayload_returnsPdf() {
        String payload = """
                {
                  "firmName": "Test Firm Ltd",
                  "firmAddress": "1 Test Street, London",
                  "firmFcaReference": "FCA Ref: 999999",
                  "clientName": "Test Client",
                  "clientReference": "CLI-TEST-001",
                  "clientAddress": "Test Address",
                  "statementDate": "2024-12-31",
                  "periodStart": "2024-01-01",
                  "periodEnd": "2024-12-31",
                  "documentReference": "TEST-001",
                  "portfolioName": "Test Portfolio",
                  "instrumentName": "Test Fund",
                  "isin": "GB0000000000",
                  "investmentAmount": 10000.00,
                  "currency": "GBP",
                  "costLineItems": [
                    {
                      "category": "Ongoing costs",
                      "description": "Management fee",
                      "amount": 100.00,
                      "percentage": 1.00,
                      "currency": "GBP"
                    }
                  ],
                  "totalCostsAmount": 100.00,
                  "totalCostsPercentage": 1.00,
                  "projectedReturnBeforeCosts": 5.00,
                  "projectedReturnAfterCosts": 4.00
                }
                """;

        given()
                .contentType("application/json")
                .body(payload)
                .when()
                .post("/api/mifid/statement")
                .then()
                .statusCode(200)
                .contentType("application/pdf")
                .header("Content-Disposition", containsString("TEST-001"));
    }
}
