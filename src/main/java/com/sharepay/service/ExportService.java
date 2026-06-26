package com.sharepay.service;

import com.sharepay.domain.Event;
import com.sharepay.dto.AnalyticsDtos.CategoryBreakdown;
import com.sharepay.dto.AnalyticsDtos.SummaryResponse;
import com.sharepay.dto.MemberDtos.MemberSummaryResponse;
import com.sharepay.dto.SettlementDtos.SettlementResponse;
import com.sharepay.dto.SettlementDtos.TransferResponse;
import com.sharepay.exception.BadRequestException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Renders an event's summary, member balances and settlement plan to Excel or CSV.
 * (PDF export is a planned follow-up — it needs a dedicated PDF library.)
 */
@Service
public class ExportService {

    private final EventService eventService;
    private final AnalyticsService analyticsService;
    private final SettlementService settlementService;

    public ExportService(EventService eventService,
                         AnalyticsService analyticsService,
                         SettlementService settlementService) {
        this.eventService = eventService;
        this.analyticsService = analyticsService;
        this.settlementService = settlementService;
    }

    @Transactional(readOnly = true)
    public byte[] exportExcel(Long userId, Long eventId) {
        Event event = eventService.getViewableEvent(userId, eventId);
        SummaryResponse summary = analyticsService.summary(userId, eventId);
        SettlementResponse settlement = settlementService.getSettlement(userId, eventId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(workbook);

            writeSummarySheet(workbook, header, event, summary);
            writeMembersSheet(workbook, header, summary);
            writeSettlementSheet(workbook, header, settlement);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Failed to generate Excel export");
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(Long userId, Long eventId) {
        eventService.getViewableEvent(userId, eventId);
        SummaryResponse summary = analyticsService.summary(userId, eventId);
        SettlementResponse settlement = settlementService.getSettlement(userId, eventId);

        StringBuilder sb = new StringBuilder();
        sb.append("Members\n");
        sb.append("Member,Paid,Owed,Sponsored,Balance\n");
        for (MemberSummaryResponse m : summary.memberBalances()) {
            sb.append(csv(m.displayName())).append(',')
                    .append(m.paid()).append(',')
                    .append(m.owed()).append(',')
                    .append(m.sponsored()).append(',')
                    .append(m.balance()).append('\n');
        }
        sb.append("\nSettlement\n");
        sb.append("From,To,Amount\n");
        for (TransferResponse t : settlement.transfers()) {
            sb.append(csv(t.fromName())).append(',')
                    .append(csv(t.toName())).append(',')
                    .append(t.amount()).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // --- Excel sheet writers ---

    private void writeSummarySheet(Workbook wb, CellStyle header, Event event, SummaryResponse summary) {
        Sheet sheet = wb.createSheet("Summary");
        int r = 0;
        r = kv(sheet, r, header, "Event", event.getName());
        r = kv(sheet, r, header, "Currency", event.getCurrency());
        r = kv(sheet, r, header, "Total Expense", summary.totals().totalExpense());
        r = kv(sheet, r, header, "Total Paid", summary.totals().totalPaid());
        r = kv(sheet, r, header, "Total Sponsored", summary.totals().totalSponsored());
        r = kv(sheet, r, header, "Net Shared", summary.totals().netShared());
        r = kv(sheet, r, header, "Participants", summary.totals().participantCount());
        r++;

        Row catHeader = sheet.createRow(r++);
        headerCell(catHeader, 0, header, "Category");
        headerCell(catHeader, 1, header, "Amount");
        headerCell(catHeader, 2, header, "% of Expense");
        for (CategoryBreakdown c : summary.expenseByCategory()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(c.category());
            setNumber(row.createCell(1), c.amount());
            setNumber(row.createCell(2), c.percentage());
        }
        autoSize(sheet, 3);
    }

    private void writeMembersSheet(Workbook wb, CellStyle header, SummaryResponse summary) {
        Sheet sheet = wb.createSheet("Members");
        Row head = sheet.createRow(0);
        headerCell(head, 0, header, "Member");
        headerCell(head, 1, header, "Paid");
        headerCell(head, 2, header, "Owed");
        headerCell(head, 3, header, "Sponsored");
        headerCell(head, 4, header, "Balance");
        int r = 1;
        for (MemberSummaryResponse m : summary.memberBalances()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(m.displayName());
            setNumber(row.createCell(1), m.paid());
            setNumber(row.createCell(2), m.owed());
            setNumber(row.createCell(3), m.sponsored());
            setNumber(row.createCell(4), m.balance());
        }
        autoSize(sheet, 5);
    }

    private void writeSettlementSheet(Workbook wb, CellStyle header, SettlementResponse settlement) {
        Sheet sheet = wb.createSheet("Settlement");
        Row head = sheet.createRow(0);
        headerCell(head, 0, header, "From");
        headerCell(head, 1, header, "To");
        headerCell(head, 2, header, "Amount");
        int r = 1;
        for (TransferResponse t : settlement.transfers()) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(t.fromName());
            row.createCell(1).setCellValue(t.toName());
            setNumber(row.createCell(2), t.amount());
        }
        autoSize(sheet, 3);
    }

    // --- helpers ---

    private int kv(Sheet sheet, int r, CellStyle header, String key, Object value) {
        Row row = sheet.createRow(r);
        headerCell(row, 0, header, key);
        Cell cell = row.createCell(1);
        if (value instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
        return r + 1;
    }

    private void headerCell(Row row, int col, CellStyle style, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setNumber(Cell cell, BigDecimal value) {
        cell.setCellValue(value != null ? value.doubleValue() : 0d);
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
