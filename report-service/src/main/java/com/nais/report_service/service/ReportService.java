package com.nais.report_service.service;
// package com.nais.reportservice.service;
import com.itextpdf.text.DocumentException;
import com.nais.report_service.client.AnalyticsServiceClient;
import com.nais.report_service.client.HistoryServiceClient;
import com.nais.report_service.dto.LowBitrateExperienceDTO;
import com.nais.report_service.dto.ViewingHistoryDTO;
import com.nais.report_service.dto.ViewingProgressDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final HistoryServiceClient historyServiceClient;
    private final AnalyticsServiceClient analyticsServiceClient;
    private final PdfGeneratorService pdfGeneratorService;

    public byte[] generatePdfReportForUser(Long userId) throws DocumentException {
        List<ViewingHistoryDTO> history = historyServiceClient.getHistoryForUser(userId);
        List<ViewingProgressDTO> progress = historyServiceClient.getProgressForUser(userId);

        List<LowBitrateExperienceDTO> allLowBitrateUsers = analyticsServiceClient.getLowBitrateUsers("3y", 10000);

        List<LowBitrateExperienceDTO> userLowBitrateExperiences = allLowBitrateUsers.stream()
                .filter(exp -> exp.getUserId() != null && exp.getUserId().trim().equals(String.valueOf(userId)))
                .collect(Collectors.toList());

        ByteArrayOutputStream baos = pdfGeneratorService.generateUserActivityReport(userId, history, progress, userLowBitrateExperiences);

        return baos.toByteArray();
    }
}