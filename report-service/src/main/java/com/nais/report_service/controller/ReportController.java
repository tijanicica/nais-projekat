package com.nais.report_service.controller;
// package com.nais.reportservice.controller;
import com.itextpdf.text.DocumentException;
import com.nais.report_service.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping(value = "/user/{userId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getUserReport(@PathVariable Long userId) {
        try {
            byte[] pdfContents = reportService.generatePdfReportForUser(userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // Pretraživač će ponuditi da se fajl sačuva pod ovim imenom
            String filename = "report_user_" + userId + ".pdf";
            headers.setContentDispositionFormData(filename, filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContents);

        } catch (DocumentException e) {
            // U slučaju greške pri generisanju PDF-a
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            // U slučaju problema sa pozivanjem drugih servisa
            e.printStackTrace();
            return ResponseEntity.status(503).build(); // Service Unavailable
        }
    }
}