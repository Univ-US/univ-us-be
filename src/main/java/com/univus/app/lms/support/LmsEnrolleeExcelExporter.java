package com.univus.app.lms.support;

import com.univus.app.lms.dto.LmsLectureStudentsResponseDto;
import com.univus.app.lms.dto.LmsStudentRowDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/** PLM-003 수강생 명단 Excel(.xlsx) 생성 (Apache POI) */
@Slf4j
@Component
public class LmsEnrolleeExcelExporter {

    private static final String[] HEADERS = {"학번", "이름", "출석률(%)", "과제 제출", "평균 점수"};
    private static final int[] COLUMN_WIDTH_CHARS = {18, 14, 12, 12, 12}; // 문자 수 (헤드리스 안전: autoSize 대신 고정폭)

    public byte[] toExcel(LmsLectureStudentsResponseDto data) {
        List<LmsStudentRowDto> students = (data == null || data.getStudents() == null)
                ? List.of() : data.getStudents();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("수강생 명단");
            writeHeader(workbook, sheet);

            int rowIdx = 1;
            for (LmsStudentRowDto student : students) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nullSafe(student.getStudentNo()));
                row.createCell(1).setCellValue(nullSafe(student.getStudentName()));
                row.createCell(2).setCellValue(student.getAttendanceRate());
                row.createCell(3).setCellValue(submittedText(student));
                if (student.getAverageScore() != null) {
                    row.createCell(4).setCellValue(student.getAverageScore());
                } else {
                    row.createCell(4).setCellValue("-");
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            Long lecId = (data != null && data.getLecture() != null) ? data.getLecture().getLecId() : null;
            log.error("수강생 명단 엑셀 생성 실패 lecId={}", lecId, e);
            throw new UncheckedIOException("수강생 명단 엑셀 생성 실패", e);
        }
    }

    private void writeHeader(Workbook workbook, Sheet sheet) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, COLUMN_WIDTH_CHARS[i] * 256); // POI 폭 단위 = 1/256 문자
        }
    }

    private String submittedText(LmsStudentRowDto student) {
        int submitted = student.getSubmittedCount() == null ? 0 : student.getSubmittedCount();
        int total = student.getTotalAssignments() == null ? 0 : student.getTotalAssignments();
        return submitted + " / " + total;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
