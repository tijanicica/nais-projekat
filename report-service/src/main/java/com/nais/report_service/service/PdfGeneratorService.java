package com.nais.report_service.service;

// IMPORT-OVI IZ iText 5 BIBLIOTEKE
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import com.nais.report_service.dto.LowBitrateExperienceDTO;
import com.nais.report_service.dto.ViewingHistoryDTO;
import com.nais.report_service.dto.ViewingProgressDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfGeneratorService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font TABLE_HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public ByteArrayOutputStream generateUserActivityReport(Long userId, List<ViewingHistoryDTO> history, List<ViewingProgressDTO> progress, List<LowBitrateExperienceDTO> lowBitrateExperiences) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);

        document.open();

        // Naslov
        Paragraph title = new Paragraph("Mesečni Izveštaj o Aktivnosti za Korisnika: " + userId, TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(Chunk.NEWLINE);

        // --- Sekcija 1: Istorija Gledanja ---
        addHistorySection(document, history);
        document.add(Chunk.NEWLINE);

        // --- Sekcija 2: Lista za Nastavak Gledanja ---
        addProgressSection(document, progress);
        document.add(Chunk.NEWLINE);

        // --- Sekcija 3: Analiza Kvaliteta Streama ---
        addAnalyticsSection(document, lowBitrateExperiences);

        document.close();
        return baos;
    }

    private void addHistorySection(Document document, List<ViewingHistoryDTO> history) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Istorija Gledanja", SECTION_FONT);
        document.add(sectionTitle);
        document.add(new Paragraph("Lista svih filmova koje je korisnik gledao.", NORMAL_FONT));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        table.addCell(createHeaderCell("ID Filma"));
        table.addCell(createHeaderCell("Gledano do (sekunde)"));
        table.addCell(createHeaderCell("Vreme"));

        for (ViewingHistoryDTO item : history) {
            table.addCell(createCell(String.valueOf(item.getMovieId())));
            table.addCell(createCell(String.valueOf(item.getStoppedAtSeconds())));
            table.addCell(createCell(item.getKey().getViewedAt().atZone(ZoneId.systemDefault()).format(FORMATTER)));
        }
        document.add(table);
    }

    private void addProgressSection(Document document, List<ViewingProgressDTO> progress) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Lista za Nastavak Gledanja", SECTION_FONT);
        document.add(sectionTitle);
        document.add(new Paragraph("Filmovi koje korisnik nije pogledao do kraja.", NORMAL_FONT));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        table.addCell(createHeaderCell("ID Filma"));
        table.addCell(createHeaderCell("Progres (sekunde)"));
        table.addCell(createHeaderCell("Poslednji put gledano"));

        for (ViewingProgressDTO item : progress) {
            table.addCell(createCell(String.valueOf(item.getKey().getMovieId())));
            table.addCell(createCell(String.valueOf(item.getProgressSeconds())));
            table.addCell(createCell(item.getLastWatchedAt().atZone(ZoneId.systemDefault()).format(FORMATTER)));
        }
        document.add(table);
    }

    private void addAnalyticsSection(Document document, List<LowBitrateExperienceDTO> lowBitrateExperiences) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Analiza Problema sa Kvalitetom Streama", SECTION_FONT);
        document.add(sectionTitle);
        document.add(new Paragraph("Ova sekcija prikazuje filmove gde je korisnik imao loš kvalitet slike (prosečni bitrate ispod 10000 Kbps) u poslednje 3 godine. Ovo je rezultat složenog upita nad analitičkom bazom.", NORMAL_FONT));

        if (lowBitrateExperiences.isEmpty()) {
            document.add(new Paragraph("Nisu zabeleženi problemi sa kvalitetom strima za ovog korisnika.", NORMAL_FONT));
            return;
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        table.addCell(createHeaderCell("ID Filma"));
        table.addCell(createHeaderCell("Prosečan Bitrate (Kbps)"));

        for (LowBitrateExperienceDTO item : lowBitrateExperiences) {
            table.addCell(createCell(item.getMovieId()));
            table.addCell(createCell(String.format("%.2f", item.getAverageBitrateKbps())));
        }
        document.add(table);
    }

    private PdfPCell createHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setPadding(5);
        return cell;
    }
}