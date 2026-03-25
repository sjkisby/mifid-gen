# MiFID II PDF Generation PoC — Quarkus + OpenHTMLtoPDF

## Architecture

```
HTTP POST /api/mifid/statement
        │
        ▼
MifidStatement (record)          ← JSON deserialisation (Jackson)
        │
        ▼
Qute template render             ← mifidStatement.html (classpath)
        │  (produces well-formed XHTML)
        ▼
OpenHTMLtoPDF PdfRendererBuilder ← pure JVM, no native deps
        │  (CSS paged media, PDF/A-1b)
        ▼
byte[]  →  HTTP response (application/pdf)
```

## Why OpenHTMLtoPDF over image-overlay iText

| Concern | Image Overlay | HTML → PDF |
|---|---|---|
| Variable row count | ❌ Must pre-allocate rows | ✅ Table grows naturally |
| Template redesign | ❌ Code change (pixel coords) | ✅ Edit HTML/CSS only |
| Page overflow | ❌ Manual stitch | ✅ CSS paged media handles it |
| Accessibility / PDF/A | ❌ Rasterised text | ✅ Selectable text, PDF/A-1b |
| Regulatory audit trail | ⚠️ Hard to diff | ✅ HTML templates in VCS |
| Multi-language support | ❌ Font per image | ✅ CSS font-face |

## Dependencies

```xml
<!-- OpenHTMLtoPDF — pure JVM, no native binaries needed -->
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-pdfbox</artifactId>
    <version>1.0.10</version>
</dependency>
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-svg-support</artifactId>
    <version>1.0.10</version>
</dependency>
```

## Running

```bash
# Dev mode with live reload of templates
./mvnw quarkus:dev

# Fetch a sample PDF
curl http://localhost:8080/api/mifid/sample --output sample.pdf
open sample.pdf

# POST a custom payload
curl -X POST http://localhost:8080/api/mifid/statement \
     -H "Content-Type: application/json" \
     -d @payload.json \
     --output statement.pdf
```

## Key OpenHTMLtoPDF features used

### CSS Paged Media (@page)
```css
@page {
    size: A4 portrait;
    margin: 18mm 15mm 22mm 15mm;
    @bottom-center {
        content: "Page " counter(page) " of " counter(pages);
    }
}
```
Running headers/footers with auto page numbering — impossible with image overlay.

### PDF/A Conformance
```java
builder.usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_1_B)
```
Required for long-term regulatory archiving (ISO 19005-1).

### SVG Support
```java
builder.useSVGDrawer(new BatikSVGDrawer())
```
Embed your firm logo as SVG — scales perfectly at any print resolution.

### Custom Fonts
```java
builder.useFont(new File("/fonts/Inter-Regular.ttf"), "Inter");
```
Embed a branded typeface. In the HTML: `font-family: 'Inter', sans-serif;`

## Qute template tips

```html
{! Iterate repeating cost rows — the key structure image overlay can't handle !}
{#for item in statement.costLineItems}
    {! Emit category header on group change !}
    {#if item_index == 0 || prev.category != item.category}
    <tr class="category-header"><td colspan="3">{item.category}</td></tr>
    {/if}
    <tr><td>{item.description}</td><td>{item.formattedAmount}</td></tr>
{/for}
```

## Quarkus native image

OpenHTMLtoPDF works with Quarkus native builds but requires these reflection
registrations in `native-image.properties`:

```
Args = --initialize-at-run-time=com.openhtmltopdf \
       -H:ReflectionConfigurationFiles=reflection-config.json
```
Provide a `reflection-config.json` listing the PDFBox and OpenHTMLtoPDF renderer
classes. See the [Quarkus native guide](https://quarkus.io/guides/building-native-image)
for details.

## MiFID II compliance checklist

- [x] All cost categories (one-off, ongoing, transaction, incidental, ancillary)
- [x] Amount in currency + percentage of investment
- [x] Aggregated totals row
- [x] Effect on return (gross vs net)
- [x] Reporting period dates
- [x] Firm FCA reference
- [x] Regulatory disclosure text
- [x] PDF/A-1b for archival
- [ ] Digital signature (add via PDFBox `PDSignature` after generation)
- [ ] Welsh language version (swap template locale — same service)
