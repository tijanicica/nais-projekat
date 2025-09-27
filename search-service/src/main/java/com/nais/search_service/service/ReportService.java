package com.nais.search_service.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private ActorService actorService;

    public byte[] generateActorsReport() throws DocumentException, IOException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // Add title
        addTitle(document);

        // SIMPLE SECTION 1: American actors born before 1970
        addSimpleSection1(document);

        // SIMPLE SECTION 2: Number of British actors
        addSimpleSection2(document);

        // COMPLEX SECTION: Complex search with a vector query
        addComplexSection(document);

        // Add footer
        addFooter(document);

        document.close();

        return out.toByteArray();
    }

    private void addTitle(Document document) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, BaseColor.DARK_GRAY);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.GRAY);

        Paragraph title = new Paragraph("ACTOR REPORT", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);

        Paragraph subtitle = new Paragraph("Generated from Weaviate vector database", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
        Paragraph timestamp = new Paragraph("Generation time: " + currentTime, subtitleFont);
        timestamp.setAlignment(Element.ALIGN_RIGHT);
        timestamp.setSpacingAfter(30);
        document.add(timestamp);
    }

    private void addSimpleSection1(Document document) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph sectionTitle = new Paragraph("1. SIMPLE SECTION - American actors born before 1970", sectionFont);
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        // Calling the service to retrieve data
        List<Map<String, Object>> actors = actorService.filterActors("American", 1970);

        if (actors != null && !actors.isEmpty()) {
            // Creating the table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(20f);

            // Table headers
            addTableHeader(table, new String[]{"ID", "Name", "Birth Year", "Nationality"});

            // Adding data
            for (Map<String, Object> actor : actors) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(actor.get("actorId")), textFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(actor.get("name")), textFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(actor.get("birthYear")), textFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(actor.get("nationality")), textFont)));
            }

            document.add(table);

            // Adding analysis
            Paragraph analysis = new Paragraph("Analysis: Found " + actors.size() +
                    " American actors born before 1970.", textFont);
            analysis.setSpacingAfter(20);
            document.add(analysis);
        } else {
            Paragraph noData = new Paragraph("No data to display.", textFont);
            document.add(noData);
        }
    }

    private void addSimpleSection2(Document document) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font highlightFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.BLUE);

        Paragraph sectionTitle = new Paragraph("2. SIMPLE SECTION - British Actor Statistics", sectionFont);
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        // Calling the service for counting
        long britishActorsCount = actorService.countActorsByNationality("British");

        // Creating a table to display statistics
        PdfPTable statsTable = new PdfPTable(2);
        statsTable.setWidthPercentage(60);
        statsTable.setSpacingBefore(10f);
        statsTable.setSpacingAfter(20f);

        PdfPCell labelCell = new PdfPCell(new Phrase("Total number of British actors:", textFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(10);
        statsTable.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(String.valueOf(britishActorsCount), highlightFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(10);
        statsTable.addCell(valueCell);

        document.add(statsTable);

        // Adding an explanation
        Paragraph explanation = new Paragraph("This section displays aggregated statistics for British actors " +
                "in the database using Weaviate's aggregate functionality.", textFont);
        explanation.setSpacingAfter(20);
        document.add(explanation);
    }

    private void addComplexSection(Document document) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
        Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font queryFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.DARK_GRAY);

        Paragraph sectionTitle = new Paragraph("3. COMPLEX SECTION - Semantic Search with Filtering", sectionFont);
        sectionTitle.setSpacingBefore(20);
        sectionTitle.setSpacingAfter(15);
        document.add(sectionTitle);

        // Explanation of query complexity
        Paragraph complexity = new Paragraph("This query combines:", textFont);
        complexity.setSpacingAfter(5);
        document.add(complexity);

        Paragraph features = new Paragraph("• Vector search by actor's biography\n" +
                "• Filtering by nationality (British)\n" +
                "• Filtering by birth year (≥ 1995)\n" +
                "• Ranking results by semantic similarity", textFont);
        features.setSpacingAfter(15);
        features.setIndentationLeft(20);
        document.add(features);

        // Displaying the query
        Paragraph queryLabel = new Paragraph("Query: 'won an Oscar for best actor'", queryFont);
        queryLabel.setSpacingAfter(10);
        document.add(queryLabel);

        // Calling the service for a complex search
        List<Map<String, Object>> complexResults = actorService.searchActorsWithComplexFilter(
                "won an Oscar for best actor", "British", 1995, 10);

        if (complexResults != null && !complexResults.isEmpty()) {
            // Creating a table with an additional column for semantic distance
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(20f);

            // Table headers
            addTableHeader(table, new String[]{"Name", "Nationality", "Birth Year", "Semantic Distance", "Relevance"});

            // Adding data
            for (Map<String, Object> actor : complexResults) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(actor.get("name")), textFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(actor.get("nationality")), textFont)));
                table.addCell(new PdfPCell(new Phrase(String.valueOf(actor.get("birthYear")), textFont)));

                // Parsing distance from the _additional object
                Object additionalObj = actor.get("_additional");
                String distance = "N/A";
                String relevance = "N/A";

                if (additionalObj instanceof Map) {
                    Map<String, Object> additional = (Map<String, Object>) additionalObj;
                    if (additional.get("distance") != null) {
                        double dist = ((Number) additional.get("distance")).doubleValue();
                        distance = String.format("%.4f", dist);
                        // Converting distance to relevance (smaller distance = higher relevance)
                        double relevanceScore = (1.0 - dist) * 100;
                        relevance = String.format("%.1f%%", relevanceScore);
                    }
                }

                table.addCell(new PdfPCell(new Phrase(distance, textFont)));
                table.addCell(new PdfPCell(new Phrase(relevance, textFont)));
            }

            document.add(table);

            // Analysis of results
            double avgDistance = complexResults.stream()
                    .mapToDouble(actor -> {
                        Object additionalObj = actor.get("_additional");
                        if (additionalObj instanceof Map) {
                            Map<String, Object> additional = (Map<String, Object>) additionalObj;
                            if (additional.get("distance") != null) {
                                return ((Number) additional.get("distance")).doubleValue();
                            }
                        }
                        return 0.0;
                    })
                    .average().orElse(0.0);

            Paragraph analysis = new Paragraph(String.format("Complex query analysis:\n" +
                            "• Found %d results matching the criteria\n" +
                            "• Average semantic distance: %.4f\n" +
                            "• Average relevance: %.1f%%\n" +
                            "• The query combines vector search with structural filters",
                    complexResults.size(), avgDistance, (1.0 - avgDistance) * 100), textFont);
            analysis.setSpacingAfter(20);
            document.add(analysis);
        } else {
            Paragraph noData = new Paragraph("No data matches the complex search criteria.", textFont);
            document.add(noData);
        }
    }

    private void addFooter(Document document) throws DocumentException {
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);

        Paragraph footer = new Paragraph("This report was automatically generated from the Weaviate vector database. " +
                "It contains 2 simple sections (filtering and aggregation) and 1 complex section (semantic search with filtering).",
                footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30);
        document.add(footer);
    }

    private void addTableHeader(PdfPTable table, String[] headers) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);

        for (String header : headers) {
            PdfPCell headerCell = new PdfPCell(new Phrase(header, headerFont));
            headerCell.setBackgroundColor(BaseColor.DARK_GRAY);
            headerCell.setPadding(8);
            table.addCell(headerCell);
        }
    }
}