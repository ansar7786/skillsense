package com.skillsense.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.skillsense.analytics.dto.InsightResponse;
import com.skillsense.analytics.dto.RecommendationItem;
import com.skillsense.analytics.dto.SurveyAnalyticsResponse;
import com.skillsense.analytics.service.AnalyticsService;
import com.skillsense.common.error.ApiException;
import com.skillsense.common.error.ErrorCode;
import com.skillsense.survey.entity.Survey;
import com.skillsense.user.User;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class ReportExportService {

    private static final Logger log = LoggerFactory.getLogger(ReportExportService.class);

    private final AnalyticsService analyticsService;
    private final com.skillsense.survey.repository.SurveyRepository surveyRepository;
    private final ObjectMapper objectMapper;

    public ReportExportService(AnalyticsService analyticsService,
                               com.skillsense.survey.repository.SurveyRepository surveyRepository,
                               ObjectMapper objectMapper) {
        this.analyticsService = analyticsService;
        this.surveyRepository = surveyRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Escape spreadsheet formula injection characters (=, +, -, @).
     * If the text begins with one of these characters, prepend an apostrophe (').
     */
    public static String escapeFormula(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        char first = input.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + input;
        }
        return input;
    }

    @Transactional
    public byte[] exportReport(Long surveyId, String format, User user) {
        if (format == null) {
            format = "pdf";
        }
        String fmt = format.trim().toLowerCase();

        // 1. Authorize and fetch survey
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Survey not found: " + surveyId));
        analyticsService.assertCanViewAnalytics(survey, user);

        // 2. Fetch analytics, trend, and insights using existing analytics pipeline
        SurveyAnalyticsResponse analytics = analyticsService.getSurveyAnalytics(surveyId, user);

        JsonNode trendNode = null;
        if (survey.getVersion() != null && survey.getVersion() > 1) {
            try {
                Object trendObj = analyticsService.getSurveyTrend(surveyId, user);
                if (trendObj instanceof JsonNode) {
                    trendNode = (JsonNode) trendObj;
                } else if (trendObj != null) {
                    trendNode = objectMapper.valueToTree(trendObj);
                }
            } catch (Exception e) {
                log.debug("Trend data unavailable for survey export {}: {}", surveyId, e.getMessage());
            }
        }

        InsightResponse insights = null;
        if (analytics.isAvailable()) {
            try {
                insights = analyticsService.getSurveyInsights(surveyId, false, user);
            } catch (Exception e) {
                log.debug("Insights unavailable for survey export {}: {}", surveyId, e.getMessage());
            }
        }

        // 3. Dispatch format
        if ("pdf".equals(fmt)) {
            return generatePdf(survey, analytics, trendNode, insights);
        } else if ("xlsx".equals(fmt)) {
            return generateXlsx(survey, analytics, trendNode, insights);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
                    "Unsupported export format: " + format + ". Supported formats are 'pdf' and 'xlsx'.");
        }
    }

    // =========================================================================
    // PDF GENERATION (OpenPDF)
    // =========================================================================

    private byte[] generatePdf(Survey survey, SurveyAnalyticsResponse analytics, JsonNode trendNode, InsightResponse insights) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(com.lowagie.text.PageSize.A4, 36, 36, 40, 40);

        try {
            PdfWriter.getInstance(document, out);

            // Footer: "Self-assessed data. Generated by SkillSense on {date}"
            String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            HeaderFooter footer = new HeaderFooter(
                    new Phrase("Self-assessed data. Generated by SkillSense on " + dateStr + " | Page ",
                            FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY)),
                    true
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setBorder(Rectangle.NO_BORDER);
            document.setFooter(footer);

            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 41, 59));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
            Font sectionHeadingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(30, 41, 59));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(30, 41, 59));
            Font italicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(100, 116, 139));

            // Title & Subtitle
            Paragraph titlePara = new Paragraph(survey.getTitle(), titleFont);
            titlePara.setSpacingAfter(4);
            document.add(titlePara);

            String termStr = survey.getTerm() != null ? survey.getTerm().getLabel() : "—";
            String categoryStr = survey.getCategory() != null ? survey.getCategory().name() : "GENERAL";
            String anonStr = Boolean.TRUE.equals(survey.getIsAnonymous()) ? "Anonymous Survey (K=5 Privacy Protected)" : "Identified Survey (Non-Anonymous)";
            Paragraph metaPara = new Paragraph(
                    "Category: " + categoryStr + " | Term: " + termStr + " | Version: v" + (survey.getVersion() != null ? survey.getVersion() : 1) + " | " + anonStr,
                    subTitleFont
            );
            metaPara.setSpacingAfter(14);
            document.add(metaPara);

            // Survey Summary Table
            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(16);

            addHeaderCell(summaryTable, "Metric", boldFont);
            addHeaderCell(summaryTable, "Value", boldFont);
            addHeaderCell(summaryTable, "Metric", boldFont);
            addHeaderCell(summaryTable, "Value", boldFont);

            long responseCount = analytics.getHave();
            int eligibleCount = survey.getEligibleCount() != null ? survey.getEligibleCount() : 0;
            double completionRate = eligibleCount > 0 ? ((double) responseCount / eligibleCount) * 100.0 : 0.0;

            JsonNode dataNode = analytics.isAvailable() && analytics.getData() instanceof JsonNode ? (JsonNode) analytics.getData() : null;
            if (dataNode == null && analytics.getData() != null) {
                dataNode = objectMapper.valueToTree(analytics.getData());
            }

            double satisfactionOrReadiness = 0.0;
            if (dataNode != null) {
                if (dataNode.has("readiness_index")) {
                    satisfactionOrReadiness = dataNode.get("readiness_index").asDouble();
                } else if (dataNode.has("satisfaction_index")) {
                    satisfactionOrReadiness = dataNode.get("satisfaction_index").asDouble();
                } else if (dataNode.has("index")) {
                    satisfactionOrReadiness = dataNode.get("index").asDouble();
                }
            }

            addCell(summaryTable, "Responses Collected", regularFont);
            addCell(summaryTable, String.valueOf(responseCount), boldFont);
            addCell(summaryTable, "Eligible Students", regularFont);
            addCell(summaryTable, eligibleCount > 0 ? String.valueOf(eligibleCount) : "All College", boldFont);

            addCell(summaryTable, "Completion Rate", regularFont);
            addCell(summaryTable, String.format("%.1f%%", completionRate), boldFont);
            addCell(summaryTable, "Satisfaction / Readiness Index", regularFont);
            addCell(summaryTable, String.format("%.1f", satisfactionOrReadiness), boldFont);

            addCell(summaryTable, "Survey Status", regularFont);
            addCell(summaryTable, survey.getStatus() != null ? survey.getStatus().name() : "—", boldFont);
            addCell(summaryTable, "Close Date", regularFont);
            addCell(summaryTable, survey.getClosesAt() != null ? survey.getClosesAt().format(DateTimeFormatter.ISO_LOCAL_DATE) : "Open", boldFont);

            document.add(summaryTable);

            // K=5 Privacy State Check
            if (!analytics.isAvailable() && Boolean.TRUE.equals(survey.getIsAnonymous())) {
                Paragraph privacyNotice = new Paragraph(
                        "ANONYMITY PRIVACY NOTICE: This survey is anonymous. In accordance with the K=5 privacy policy, aggregate results, skill distributions, and individual response details are withheld until at least 5 responses have been submitted. Currently, "
                                + analytics.getHave() + " of " + analytics.getNeed() + " responses have been collected.",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(180, 83, 9))
                );
                privacyNotice.setSpacingBefore(12);
                privacyNotice.setSpacingAfter(12);
                document.add(privacyNotice);
                document.close();
                return out.toByteArray();
            }

            // 1. Readiness Band Distribution (if scored and available)
            if (dataNode != null && dataNode.has("bands") && !dataNode.get("bands").isNull()) {
                Paragraph secHeading = new Paragraph("Readiness Band Distribution", sectionHeadingFont);
                secHeading.setSpacingBefore(10);
                secHeading.setSpacingAfter(6);
                document.add(secHeading);

                PdfPTable bandTable = new PdfPTable(3);
                bandTable.setWidthPercentage(100);
                bandTable.setSpacingAfter(14);

                addHeaderCell(bandTable, "Band", boldFont);
                addHeaderCell(bandTable, "Count", boldFont);
                addHeaderCell(bandTable, "Share (%)", boldFont);

                JsonNode bandsNode = dataNode.get("bands");
                for (String bandKey : List.of("READY", "DEVELOPING", "NEEDS_SUPPORT")) {
                    if (bandsNode.has(bandKey)) {
                        JsonNode b = bandsNode.get(bandKey);
                        int count = b.has("count") ? b.get("count").asInt() : 0;
                        double share = b.has("percentage") ? b.get("percentage").asDouble() * 100.0 :
                                (b.has("share") ? b.get("share").asDouble() * 100.0 : 0.0);
                        addCell(bandTable, bandKey.replace('_', ' '), regularFont);
                        addCell(bandTable, String.valueOf(count), regularFont);
                        addCell(bandTable, String.format("%.1f%%", share), regularFont);
                    }
                }
                document.add(bandTable);
            }

            // 2. Skill Targets & Gaps
            if (dataNode != null && dataNode.has("skills") && dataNode.get("skills").isArray() && dataNode.get("skills").size() > 0) {
                Paragraph secHeading = new Paragraph("Skill Gap Analysis", sectionHeadingFont);
                secHeading.setSpacingBefore(10);
                secHeading.setSpacingAfter(6);
                document.add(secHeading);

                PdfPTable skillTable = new PdfPTable(6);
                skillTable.setWidthPercentage(100);
                skillTable.setSpacingAfter(14);

                addHeaderCell(skillTable, "Skill", boldFont);
                addHeaderCell(skillTable, "Cohort Mean", boldFont);
                addHeaderCell(skillTable, "Target", boldFont);
                addHeaderCell(skillTable, "Gap", boldFont);
                addHeaderCell(skillTable, "Band", boldFont);
                addHeaderCell(skillTable, "Priority", boldFont);

                for (JsonNode sk : dataNode.get("skills")) {
                    String skillName = sk.has("skill") ? sk.get("skill").asText().toUpperCase() : "—";
                    double mean = sk.has("cohort_mean") ? sk.get("cohort_mean").asDouble() : 0.0;
                    double target = sk.has("target") ? sk.get("target").asDouble() : 0.0;
                    double gap = sk.has("gap") ? sk.get("gap").asDouble() : 0.0;
                    String band = sk.has("band") ? sk.get("band").asText() : "—";
                    String priority = sk.has("priority") ? sk.get("priority").asText() : "—";

                    addCell(skillTable, skillName, regularFont);
                    addCell(skillTable, String.format("%.1f", mean), regularFont);
                    addCell(skillTable, String.format("%.1f", target), regularFont);
                    addCell(skillTable, String.format("%.1f", gap), regularFont);
                    addCell(skillTable, band, regularFont);
                    addCell(skillTable, priority, boldFont);
                }
                document.add(skillTable);
            }

            // 3. Question-level aggregate results
            if (dataNode != null && dataNode.has("questions") && dataNode.get("questions").isArray()) {
                Paragraph secHeading = new Paragraph("Question-Level Results", sectionHeadingFont);
                secHeading.setSpacingBefore(10);
                secHeading.setSpacingAfter(6);
                document.add(secHeading);

                for (JsonNode qNode : dataNode.get("questions")) {
                    String qKey = qNode.has("key") ? qNode.get("key").asText() : "";
                    String prompt = qNode.has("prompt") ? qNode.get("prompt").asText() : qKey;
                    String type = qNode.has("type") ? qNode.get("type").asText() : "";
                    int qCount = qNode.has("count") ? qNode.get("count").asInt() : 0;

                    Paragraph qTitle = new Paragraph(prompt + " (" + type + ", n=" + qCount + ")", boldFont);
                    qTitle.setSpacingBefore(6);
                    qTitle.setSpacingAfter(4);
                    document.add(qTitle);

                    if (qNode.has("distribution") && !qNode.get("distribution").isNull()) {
                        PdfPTable qTable = new PdfPTable(3);
                        qTable.setWidthPercentage(100);
                        qTable.setSpacingAfter(8);

                        addHeaderCell(qTable, "Option / Score", boldFont);
                        addHeaderCell(qTable, "Count", boldFont);
                        addHeaderCell(qTable, "Share (%)", boldFont);

                        JsonNode dist = qNode.get("distribution");
                        Iterator<Map.Entry<String, JsonNode>> fields = dist.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> entry = fields.next();
                            int c = entry.getValue().asInt();
                            double share = qCount > 0 ? ((double) c / qCount) * 100.0 : 0.0;
                            addCell(qTable, entry.getKey(), regularFont);
                            addCell(qTable, String.valueOf(c), regularFont);
                            addCell(qTable, String.format("%.1f%%", share), regularFont);
                        }
                        document.add(qTable);
                    } else if (qNode.has("mean")) {
                        double meanVal = qNode.get("mean").asDouble();
                        double medianVal = qNode.has("median") ? qNode.get("median").asDouble() : meanVal;
                        Paragraph statPara = new Paragraph(String.format("Mean: %.2f | Median: %.2f", meanVal, medianVal), regularFont);
                        statPara.setSpacingAfter(6);
                        document.add(statPara);
                    }
                }
            }

            // 4. Demographic Breakdowns (with privacy threshold enforcement)
            if (dataNode != null && dataNode.has("breakdown") && !dataNode.get("breakdown").isNull()) {
                Paragraph secHeading = new Paragraph("Demographic Breakdowns", sectionHeadingFont);
                secHeading.setSpacingBefore(10);
                secHeading.setSpacingAfter(6);
                document.add(secHeading);

                JsonNode bdNode = dataNode.get("breakdown");

                // By Department
                if (bdNode.has("by_department")) {
                    Paragraph depTitle = new Paragraph("Breakdown by Department", boldFont);
                    depTitle.setSpacingAfter(4);
                    document.add(depTitle);

                    PdfPTable depTable = new PdfPTable(4);
                    depTable.setWidthPercentage(100);
                    depTable.setSpacingAfter(10);

                    addHeaderCell(depTable, "Department", boldFont);
                    addHeaderCell(depTable, "Count", boldFont);
                    addHeaderCell(depTable, "Score / Mean", boldFont);
                    addHeaderCell(depTable, "Status", boldFont);

                    Iterator<Map.Entry<String, JsonNode>> deps = bdNode.get("by_department").fields();
                    while (deps.hasNext()) {
                        Map.Entry<String, JsonNode> entry = deps.next();
                        JsonNode val = entry.getValue();
                        boolean isSuppressed = val.has("suppressed") && val.get("suppressed").asBoolean();
                        int c = val.has("count") ? val.get("count").asInt() : 0;

                        addCell(depTable, entry.getKey(), regularFont);
                        if (isSuppressed || c < 5) {
                            addCell(depTable, "—", italicFont);
                            addCell(depTable, "—", italicFont);
                            addCell(depTable, "Hidden: fewer than 5 responses", italicFont);
                        } else {
                            addCell(depTable, String.valueOf(c), regularFont);
                            double mean = val.has("mean") ? val.get("mean").asDouble() : 0.0;
                            addCell(depTable, String.format("%.1f", mean), regularFont);
                            addCell(depTable, "Reported", regularFont);
                        }
                    }
                    document.add(depTable);
                }

                // By Study Year
                if (bdNode.has("by_study_year")) {
                    Paragraph yearTitle = new Paragraph("Breakdown by Study Year", boldFont);
                    yearTitle.setSpacingAfter(4);
                    document.add(yearTitle);

                    PdfPTable yearTable = new PdfPTable(4);
                    yearTable.setWidthPercentage(100);
                    yearTable.setSpacingAfter(10);

                    addHeaderCell(yearTable, "Year", boldFont);
                    addHeaderCell(yearTable, "Count", boldFont);
                    addHeaderCell(yearTable, "Score / Mean", boldFont);
                    addHeaderCell(yearTable, "Status", boldFont);

                    Iterator<Map.Entry<String, JsonNode>> yrs = bdNode.get("by_study_year").fields();
                    while (yrs.hasNext()) {
                        Map.Entry<String, JsonNode> entry = yrs.next();
                        JsonNode val = entry.getValue();
                        boolean isSuppressed = val.has("suppressed") && val.get("suppressed").asBoolean();
                        int c = val.has("count") ? val.get("count").asInt() : 0;

                        addCell(yearTable, "Year " + entry.getKey(), regularFont);
                        if (isSuppressed || c < 5) {
                            addCell(yearTable, "—", italicFont);
                            addCell(yearTable, "—", italicFont);
                            addCell(yearTable, "Hidden: fewer than 5 responses", italicFont);
                        } else {
                            addCell(yearTable, String.valueOf(c), regularFont);
                            double mean = val.has("mean") ? val.get("mean").asDouble() : 0.0;
                            addCell(yearTable, String.format("%.1f", mean), regularFont);
                            addCell(yearTable, "Reported", regularFont);
                        }
                    }
                    document.add(yearTable);
                }
            }

            // 5. Trend Information (if version > 1)
            if (trendNode != null && trendNode.has("comparison") && trendNode.get("comparison").isArray()) {
                Paragraph secHeading = new Paragraph("Historical Trend Analysis", sectionHeadingFont);
                secHeading.setSpacingBefore(10);
                secHeading.setSpacingAfter(6);
                document.add(secHeading);

                PdfPTable trendTable = new PdfPTable(5);
                trendTable.setWidthPercentage(100);
                trendTable.setSpacingAfter(14);

                addHeaderCell(trendTable, "Skill / Metric", boldFont);
                addHeaderCell(trendTable, "Previous Mean", boldFont);
                addHeaderCell(trendTable, "Current Mean", boldFont);
                addHeaderCell(trendTable, "Delta", boldFont);
                addHeaderCell(trendTable, "Status", boldFont);

                for (JsonNode row : trendNode.get("comparison")) {
                    String name = row.has("skill") ? row.get("skill").asText().toUpperCase() :
                            (row.has("label") ? row.get("label").asText() : "Overall");
                    double prev = row.has("previous_mean") ? row.get("previous_mean").asDouble() : 0.0;
                    double curr = row.has("current_mean") ? row.get("current_mean").asDouble() : 0.0;
                    double delta = row.has("delta") ? row.get("delta").asDouble() : 0.0;
                    String status = row.has("status") ? row.get("status").asText() : "FLAT";

                    addCell(trendTable, name, regularFont);
                    addCell(trendTable, String.format("%.1f", prev), regularFont);
                    addCell(trendTable, String.format("%.1f", curr), regularFont);
                    addCell(trendTable, String.format("%+.1f", delta), boldFont);
                    addCell(trendTable, status, boldFont);
                }
                document.add(trendTable);
            }

            // 6. Executive Insights & Recommendations
            if (insights != null) {
                Paragraph secHeading = new Paragraph("Executive Insights & Recommendations", sectionHeadingFont);
                secHeading.setSpacingBefore(10);
                secHeading.setSpacingAfter(4);
                document.add(secHeading);

                String sourceLabel = "AI".equalsIgnoreCase(insights.getSource()) ? "AI Generated (Gemini)" : "Generated without AI (Rules-based)";
                Paragraph sourcePara = new Paragraph("Source: " + sourceLabel, boldFont);
                sourcePara.setSpacingAfter(6);
                document.add(sourcePara);

                if (insights.getHeadline() != null && !insights.getHeadline().isEmpty()) {
                    Paragraph headlinePara = new Paragraph(insights.getHeadline(), boldFont);
                    headlinePara.setSpacingAfter(6);
                    document.add(headlinePara);
                }

                if (insights.getKeyFindings() != null && !insights.getKeyFindings().isEmpty()) {
                    Paragraph findingsHead = new Paragraph("Key Findings:", boldFont);
                    findingsHead.setSpacingBefore(4);
                    findingsHead.setSpacingAfter(2);
                    document.add(findingsHead);

                    for (String finding : insights.getKeyFindings()) {
                        Paragraph p = new Paragraph("• " + finding, regularFont);
                        p.setIndentationLeft(12);
                        document.add(p);
                    }
                }

                if (insights.getRecommendations() != null && !insights.getRecommendations().isEmpty()) {
                    Paragraph recsHead = new Paragraph("Recommendations:", boldFont);
                    recsHead.setSpacingBefore(6);
                    recsHead.setSpacingAfter(4);
                    document.add(recsHead);

                    PdfPTable recTable = new PdfPTable(4);
                    recTable.setWidthPercentage(100);
                    recTable.setSpacingAfter(10);

                    addHeaderCell(recTable, "Priority", boldFont);
                    addHeaderCell(recTable, "Action", boldFont);
                    addHeaderCell(recTable, "Rationale", boldFont);
                    addHeaderCell(recTable, "Owner", boldFont);

                    for (RecommendationItem item : insights.getRecommendations()) {
                        addCell(recTable, "P" + item.getPriority() + " (" + (item.getTitle() != null ? item.getTitle() : "Action") + ")", boldFont);
                        addCell(recTable, item.getAction() != null ? item.getAction() : "—", regularFont);
                        addCell(recTable, item.getRationale() != null ? item.getRationale() : "—", regularFont);
                        addCell(recTable, item.getSuggestedOwner() != null ? item.getSuggestedOwner() : "—", regularFont);
                    }
                    document.add(recTable);
                }

                if (insights.getWatchOuts() != null && !insights.getWatchOuts().isEmpty()) {
                    Paragraph watchHead = new Paragraph("Watch-outs:", boldFont);
                    watchHead.setSpacingBefore(4);
                    watchHead.setSpacingAfter(2);
                    document.add(watchHead);

                    for (String watch : insights.getWatchOuts()) {
                        Paragraph p = new Paragraph("⚠️ " + watch, regularFont);
                        p.setIndentationLeft(12);
                        document.add(p);
                    }
                }
            }

            document.close();
            return out.toByteArray();

        } catch (DocumentException e) {
            log.error("Failed to generate survey PDF report for {}: {}", survey.getSurveyId(), e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.BAD_REQUEST, "PDF generation failed: " + e.getMessage());
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(241, 245, 249)); // Slate-100
        cell.setPadding(6);
        cell.setBorderColor(new Color(226, 232, 240)); // Slate-200
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }

    // =========================================================================
    // EXCEL / XLSX GENERATION (Apache POI)
    // =========================================================================

    private byte[] generateXlsx(Survey survey, SurveyAnalyticsResponse analytics, JsonNode trendNode, InsightResponse insights) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Styles
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCellStyle boldStyle = workbook.createCellStyle();
            XSSFFont bFont = workbook.createFont();
            bFont.setBold(true);
            boldStyle.setFont(bFont);

            JsonNode dataNode = analytics.isAvailable() && analytics.getData() instanceof JsonNode ? (JsonNode) analytics.getData() : null;
            if (dataNode == null && analytics.getData() != null) {
                dataNode = objectMapper.valueToTree(analytics.getData());
            }

            // 1. Sheet: Summary
            XSSFSheet summarySheet = workbook.createSheet("Summary");
            summarySheet.createFreezePane(0, 1);

            int sRow = 0;
            XSSFRow sumHdr = summarySheet.createRow(sRow++);
            createHeaderCell(sumHdr, 0, "Property", headerStyle);
            createHeaderCell(sumHdr, 1, "Value", headerStyle);

            long responseCount = analytics.getHave();
            int eligibleCount = survey.getEligibleCount() != null ? survey.getEligibleCount() : 0;
            double completionRate = eligibleCount > 0 ? ((double) responseCount / eligibleCount) * 100.0 : 0.0;
            String termStr = survey.getTerm() != null ? survey.getTerm().getLabel() : "—";
            String categoryStr = survey.getCategory() != null ? survey.getCategory().name() : "GENERAL";
            String anonStr = Boolean.TRUE.equals(survey.getIsAnonymous()) ? "Anonymous (K=5 Protected)" : "Non-Anonymous";

            addStringRow(summarySheet, sRow++, "Survey Title", survey.getTitle());
            addStringRow(summarySheet, sRow++, "Category", categoryStr);
            addStringRow(summarySheet, sRow++, "Academic Term", termStr);
            addNumericRow(summarySheet, sRow++, "Version", survey.getVersion() != null ? survey.getVersion() : 1);
            addStringRow(summarySheet, sRow++, "Anonymity Mode", anonStr);
            addStringRow(summarySheet, sRow++, "Status", survey.getStatus() != null ? survey.getStatus().name() : "—");
            addNumericRow(summarySheet, sRow++, "Responses Collected", responseCount);
            addNumericRow(summarySheet, sRow++, "Eligible Students", eligibleCount);
            addNumericRow(summarySheet, sRow++, "Completion Rate (%)", completionRate);

            double satisfactionOrReadiness = 0.0;
            if (dataNode != null) {
                if (dataNode.has("readiness_index")) {
                    satisfactionOrReadiness = dataNode.get("readiness_index").asDouble();
                } else if (dataNode.has("satisfaction_index")) {
                    satisfactionOrReadiness = dataNode.get("satisfaction_index").asDouble();
                } else if (dataNode.has("index")) {
                    satisfactionOrReadiness = dataNode.get("index").asDouble();
                }
            }
            addNumericRow(summarySheet, sRow++, "Satisfaction / Readiness Index", satisfactionOrReadiness);
            addStringRow(summarySheet, sRow++, "Opens At", survey.getOpensAt() != null ? survey.getOpensAt().toString() : "—");
            addStringRow(summarySheet, sRow++, "Closes At", survey.getClosesAt() != null ? survey.getClosesAt().toString() : "—");

            // K=5 Privacy State check
            if (!analytics.isAvailable() && Boolean.TRUE.equals(survey.getIsAnonymous())) {
                addStringRow(summarySheet, sRow++, "Privacy Notice",
                        "ANONYMITY PRIVACY NOTICE: Results withheld until 5 responses are submitted. Currently "
                                + analytics.getHave() + " of " + analytics.getNeed() + " responses collected.");
                workbook.write(out);
                return out.toByteArray();
            }

            // Parse question schema for prompts and option labels
            Map<String, String> questionPrompts = new HashMap<>();
            Map<String, Map<String, String>> optionLabels = new HashMap<>();
            if (survey.getQuestionSchema() != null && !survey.getQuestionSchema().isBlank()) {
                try {
                    JsonNode schemaNode = objectMapper.readTree(survey.getQuestionSchema());
                    if (schemaNode.has("questions") && schemaNode.get("questions").isArray()) {
                        for (JsonNode qDef : schemaNode.get("questions")) {
                            String k = qDef.has("key") ? qDef.get("key").asText() : "";
                            if (qDef.has("prompt")) {
                                questionPrompts.put(k, qDef.get("prompt").asText());
                            }
                            if (qDef.has("options") && qDef.get("options").isArray()) {
                                Map<String, String> opts = new HashMap<>();
                                for (JsonNode opt : qDef.get("options")) {
                                    String v = opt.has("value") ? opt.get("value").asText() : "";
                                    String lbl = opt.has("label") ? opt.get("label").asText() : v;
                                    opts.put(v, lbl);
                                }
                                optionLabels.put(k, opts);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse question schema for export: {}", e.getMessage());
                }
            }

            // 2. Sheet: Questions
            XSSFSheet questionsSheet = workbook.createSheet("Questions");
            questionsSheet.createFreezePane(0, 1);
            int qRow = 0;
            XSSFRow qHdr = questionsSheet.createRow(qRow++);
            createHeaderCell(qHdr, 0, "Question Key", headerStyle);
            createHeaderCell(qHdr, 1, "Prompt", headerStyle);
            createHeaderCell(qHdr, 2, "Type", headerStyle);
            createHeaderCell(qHdr, 3, "Total Responses (n)", headerStyle);
            createHeaderCell(qHdr, 4, "Option / Score", headerStyle);
            createHeaderCell(qHdr, 5, "Count", headerStyle);
            createHeaderCell(qHdr, 6, "Share (%)", headerStyle);
            createHeaderCell(qHdr, 7, "Mean", headerStyle);
            createHeaderCell(qHdr, 8, "Median", headerStyle);

            if (dataNode != null && dataNode.has("questions") && dataNode.get("questions").isArray()) {
                for (JsonNode qn : dataNode.get("questions")) {
                    String qKey = qn.has("key") ? qn.get("key").asText() : "";
                    String prompt = questionPrompts.getOrDefault(qKey, qn.has("prompt") ? qn.get("prompt").asText() : qKey);
                    String type = qn.has("type") ? qn.get("type").asText() : "";
                    int count = qn.has("count") ? qn.get("count").asInt() : 0;
                    double mean = qn.has("mean") ? qn.get("mean").asDouble() : 0.0;
                    double median = qn.has("median") ? qn.get("median").asDouble() : mean;

                    if (qn.has("distribution") && !qn.get("distribution").isNull()) {
                        JsonNode dist = qn.get("distribution");
                        Iterator<Map.Entry<String, JsonNode>> fields = dist.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, JsonNode> entry = fields.next();
                            int c = entry.getValue().asInt();
                            double share = count > 0 ? ((double) c / count) * 100.0 : 0.0;
                            String optVal = entry.getKey();
                            String optDisplay = optionLabels.getOrDefault(qKey, Map.of()).getOrDefault(optVal, optVal);

                            XSSFRow r = questionsSheet.createRow(qRow++);
                            r.createCell(0).setCellValue(escapeFormula(qKey));
                            r.createCell(1).setCellValue(escapeFormula(prompt));
                            r.createCell(2).setCellValue(escapeFormula(type));
                            r.createCell(3).setCellValue(count);
                            r.createCell(4).setCellValue(escapeFormula(optDisplay));
                            r.createCell(5).setCellValue(c);
                            r.createCell(6).setCellValue(share);
                            r.createCell(7).setCellValue(mean);
                            r.createCell(8).setCellValue(median);
                        }
                    } else {
                        XSSFRow r = questionsSheet.createRow(qRow++);
                        r.createCell(0).setCellValue(escapeFormula(qKey));
                        r.createCell(1).setCellValue(escapeFormula(prompt));
                        r.createCell(2).setCellValue(escapeFormula(type));
                        r.createCell(3).setCellValue(count);
                        r.createCell(4).setCellValue("—");
                        r.createCell(5).setCellValue(count);
                        r.createCell(6).setCellValue(100.0);
                        r.createCell(7).setCellValue(mean);
                        r.createCell(8).setCellValue(median);
                    }
                }
            }

            // 3. Sheet: Skills
            XSSFSheet skillsSheet = workbook.createSheet("Skills");
            skillsSheet.createFreezePane(0, 1);
            int skRow = 0;
            XSSFRow skHdr = skillsSheet.createRow(skRow++);
            createHeaderCell(skHdr, 0, "Skill Name", headerStyle);
            createHeaderCell(skHdr, 1, "Cohort Mean", headerStyle);
            createHeaderCell(skHdr, 2, "Target", headerStyle);
            createHeaderCell(skHdr, 3, "Gap", headerStyle);
            createHeaderCell(skHdr, 4, "Readiness Band", headerStyle);
            createHeaderCell(skHdr, 5, "Priority", headerStyle);

            if (dataNode != null && dataNode.has("skills") && dataNode.get("skills").isArray()) {
                for (JsonNode sk : dataNode.get("skills")) {
                    XSSFRow r = skillsSheet.createRow(skRow++);
                    r.createCell(0).setCellValue(escapeFormula(sk.has("skill") ? sk.get("skill").asText().toUpperCase() : ""));
                    r.createCell(1).setCellValue(sk.has("cohort_mean") ? sk.get("cohort_mean").asDouble() : 0.0);
                    r.createCell(2).setCellValue(sk.has("target") ? sk.get("target").asDouble() : 0.0);
                    r.createCell(3).setCellValue(sk.has("gap") ? sk.get("gap").asDouble() : 0.0);
                    r.createCell(4).setCellValue(escapeFormula(sk.has("band") ? sk.get("band").asText() : "—"));
                    r.createCell(5).setCellValue(escapeFormula(sk.has("priority") ? sk.get("priority").asText() : "—"));
                }
            }

            // 4. Sheet: Breakdown (Demographics)
            XSSFSheet breakdownSheet = workbook.createSheet("Breakdown");
            breakdownSheet.createFreezePane(0, 1);
            int bdRow = 0;
            XSSFRow bdHdr = breakdownSheet.createRow(bdRow++);
            createHeaderCell(bdHdr, 0, "Dimension", headerStyle);
            createHeaderCell(bdHdr, 1, "Group Name", headerStyle);
            createHeaderCell(bdHdr, 2, "Count", headerStyle);
            createHeaderCell(bdHdr, 3, "Score / Mean", headerStyle);
            createHeaderCell(bdHdr, 4, "Privacy Status", headerStyle);

            if (dataNode != null && dataNode.has("breakdown") && !dataNode.get("breakdown").isNull()) {
                JsonNode bd = dataNode.get("breakdown");

                if (bd.has("by_department")) {
                    Iterator<Map.Entry<String, JsonNode>> it = bd.get("by_department").fields();
                    while (it.hasNext()) {
                        Map.Entry<String, JsonNode> e = it.next();
                        JsonNode val = e.getValue();
                        boolean isSupp = val.has("suppressed") && val.get("suppressed").asBoolean();
                        int c = val.has("count") ? val.get("count").asInt() : 0;

                        XSSFRow r = breakdownSheet.createRow(bdRow++);
                        r.createCell(0).setCellValue("Department");
                        r.createCell(1).setCellValue(escapeFormula(e.getKey()));

                        if (isSupp || c < 5) {
                            r.createCell(2).setCellValue("—");
                            r.createCell(3).setCellValue("—");
                            r.createCell(4).setCellValue("Hidden: fewer than 5 responses");
                        } else {
                            r.createCell(2).setCellValue(c);
                            r.createCell(3).setCellValue(val.has("mean") ? val.get("mean").asDouble() : 0.0);
                            r.createCell(4).setCellValue("Reported");
                        }
                    }
                }

                if (bd.has("by_study_year")) {
                    Iterator<Map.Entry<String, JsonNode>> it = bd.get("by_study_year").fields();
                    while (it.hasNext()) {
                        Map.Entry<String, JsonNode> e = it.next();
                        JsonNode val = e.getValue();
                        boolean isSupp = val.has("suppressed") && val.get("suppressed").asBoolean();
                        int c = val.has("count") ? val.get("count").asInt() : 0;

                        XSSFRow r = breakdownSheet.createRow(bdRow++);
                        r.createCell(0).setCellValue("Study Year");
                        r.createCell(1).setCellValue(escapeFormula("Year " + e.getKey()));

                        if (isSupp || c < 5) {
                            r.createCell(2).setCellValue("—");
                            r.createCell(3).setCellValue("—");
                            r.createCell(4).setCellValue("Hidden: fewer than 5 responses");
                        } else {
                            r.createCell(2).setCellValue(c);
                            r.createCell(3).setCellValue(val.has("mean") ? val.get("mean").asDouble() : 0.0);
                            r.createCell(4).setCellValue("Reported");
                        }
                    }
                }
            }

            // 5. Sheet: Trend
            XSSFSheet trendSheet = workbook.createSheet("Trend");
            trendSheet.createFreezePane(0, 1);
            int trRow = 0;
            XSSFRow trHdr = trendSheet.createRow(trRow++);
            createHeaderCell(trHdr, 0, "Metric / Skill", headerStyle);
            createHeaderCell(trHdr, 1, "Previous Mean", headerStyle);
            createHeaderCell(trHdr, 2, "Current Mean", headerStyle);
            createHeaderCell(trHdr, 3, "Delta", headerStyle);
            createHeaderCell(trHdr, 4, "Indicator Status", headerStyle);

            if (trendNode != null && trendNode.has("comparison") && trendNode.get("comparison").isArray()) {
                for (JsonNode row : trendNode.get("comparison")) {
                    String name = row.has("skill") ? row.get("skill").asText().toUpperCase() :
                            (row.has("label") ? row.get("label").asText() : "Overall");
                    double prev = row.has("previous_mean") ? row.get("previous_mean").asDouble() : 0.0;
                    double curr = row.has("current_mean") ? row.get("current_mean").asDouble() : 0.0;
                    double delta = row.has("delta") ? row.get("delta").asDouble() : 0.0;
                    String status = row.has("status") ? row.get("status").asText() : "FLAT";

                    XSSFRow r = trendSheet.createRow(trRow++);
                    r.createCell(0).setCellValue(escapeFormula(name));
                    r.createCell(1).setCellValue(prev);
                    r.createCell(2).setCellValue(curr);
                    r.createCell(3).setCellValue(delta);
                    r.createCell(4).setCellValue(escapeFormula(status));
                }
            }

            // 6. Sheet: Insights
            XSSFSheet insightsSheet = workbook.createSheet("Insights");
            insightsSheet.createFreezePane(0, 1);
            int inRow = 0;
            XSSFRow inHdr = insightsSheet.createRow(inRow++);
            createHeaderCell(inHdr, 0, "Section", headerStyle);
            createHeaderCell(inHdr, 1, "Priority / Title", headerStyle);
            createHeaderCell(inHdr, 2, "Content / Action", headerStyle);
            createHeaderCell(inHdr, 3, "Rationale", headerStyle);
            createHeaderCell(inHdr, 4, "Suggested Owner", headerStyle);
            createHeaderCell(inHdr, 5, "Source", headerStyle);

            if (insights != null) {
                String src = "AI".equalsIgnoreCase(insights.getSource()) ? "AI Generated (Gemini)" : "Generated without AI (Rules-based)";

                if (insights.getHeadline() != null && !insights.getHeadline().isEmpty()) {
                    XSSFRow r = insightsSheet.createRow(inRow++);
                    r.createCell(0).setCellValue("Headline");
                    r.createCell(1).setCellValue("Executive Summary");
                    r.createCell(2).setCellValue(escapeFormula(insights.getHeadline()));
                    r.createCell(5).setCellValue(escapeFormula(src));
                }

                if (insights.getKeyFindings() != null) {
                    for (String kf : insights.getKeyFindings()) {
                        XSSFRow r = insightsSheet.createRow(inRow++);
                        r.createCell(0).setCellValue("Key Finding");
                        r.createCell(2).setCellValue(escapeFormula(kf));
                        r.createCell(5).setCellValue(escapeFormula(src));
                    }
                }

                if (insights.getRecommendations() != null) {
                    for (RecommendationItem item : insights.getRecommendations()) {
                        XSSFRow r = insightsSheet.createRow(inRow++);
                        r.createCell(0).setCellValue("Recommendation");
                        r.createCell(1).setCellValue(escapeFormula("P" + item.getPriority() + " (" + (item.getTitle() != null ? item.getTitle() : "Action") + ")"));
                        r.createCell(2).setCellValue(escapeFormula(item.getAction()));
                        r.createCell(3).setCellValue(escapeFormula(item.getRationale()));
                        r.createCell(4).setCellValue(escapeFormula(item.getSuggestedOwner()));
                        r.createCell(5).setCellValue(escapeFormula(src));
                    }
                }

                if (insights.getWatchOuts() != null) {
                    for (String wo : insights.getWatchOuts()) {
                        XSSFRow r = insightsSheet.createRow(inRow++);
                        r.createCell(0).setCellValue("Watch-Out");
                        r.createCell(2).setCellValue(escapeFormula(wo));
                        r.createCell(5).setCellValue(escapeFormula(src));
                    }
                }
            }

            // Auto-size columns for first 5 columns on each sheet
            for (XSSFSheet s : List.of(summarySheet, questionsSheet, skillsSheet, breakdownSheet, trendSheet, insightsSheet)) {
                for (int col = 0; col < 6; col++) {
                    s.autoSizeColumn(col);
                }
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Failed to generate survey XLSX report for {}: {}", survey.getSurveyId(), e.getMessage());
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.BAD_REQUEST, "Excel generation failed: " + e.getMessage());
        }
    }

    private void createHeaderCell(XSSFRow row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void addStringRow(XSSFSheet sheet, int rowIdx, String label, String value) {
        XSSFRow row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(escapeFormula(label));
        row.createCell(1).setCellValue(escapeFormula(value));
    }

    private void addNumericRow(XSSFSheet sheet, int rowIdx, String label, double value) {
        XSSFRow row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(escapeFormula(label));
        row.createCell(1).setCellValue(value);
    }
}
