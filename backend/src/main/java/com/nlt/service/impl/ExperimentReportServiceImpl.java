package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.CurrentUserScopeService;
import com.nlt.domain.dto.experiment.ExperimentReportConsumableRequest;
import com.nlt.domain.dto.experiment.ExperimentReportReviewRequest;
import com.nlt.domain.dto.experiment.ExperimentReportSaveRequest;
import com.nlt.domain.entity.ExperimentReportConsumableEntity;
import com.nlt.domain.entity.ExperimentReportEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.entity.UserEntity;
import com.nlt.mapper.ExperimentReportMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.mapper.UserMapper;
import com.nlt.service.ExperimentReportService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.TextAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STJc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ExperimentReportServiceImpl implements ExperimentReportService {

    private static final int STATUS_DRAFT = 1;
    private static final int STATUS_PENDING = 2;
    private static final int STATUS_APPROVED = 3;
    private static final int STATUS_RETURNED = 4;

    private final ExperimentReportMapper experimentReportMapper;
    private final CurrentUserScopeService currentUserScopeService;
    private final UserMapper userMapper;
    private final LabMapper labMapper;

    @Override
    public PageData<ExperimentReportEntity> page(int pageNum, int pageSize, Integer status, String keyword) {
        int offset = (pageNum - 1) * pageSize;
        Long departmentId = null;
        Long studentId = null;
        Long teacherId = null;
        if (currentUserScopeService.isStudent()) {
            studentId = currentUserScopeService.currentUserIdOrNull();
        } else if (currentUserScopeService.isTeacher()) {
            teacherId = currentUserScopeService.currentUserIdOrNull();
        } else if (currentUserScopeService.isAdmin()) {
            departmentId = currentUserScopeService.requireCurrentDepartmentId();
        } else {
            throw new BusinessException(403, "无权访问实验报告");
        }
        return new PageData<>(
            experimentReportMapper.selectPage(offset, pageSize, departmentId, studentId, teacherId, status, trimToNull(keyword)),
            experimentReportMapper.countPage(departmentId, studentId, teacherId, status, trimToNull(keyword)),
            pageNum,
            pageSize
        );
    }

    @Override
    public ExperimentReportEntity getById(Long id) {
        ExperimentReportEntity report = requireReport(id);
        ensureVisible(report);
        fillConsumables(report);
        return report;
    }

    @Transactional
    @Override
    public ExperimentReportEntity createDraft(ExperimentReportSaveRequest request) {
        ensureStudent();
        ExperimentReportEntity entity = buildReport(new ExperimentReportEntity(), request);
        entity.setReportNo(nextReportNo());
        entity.setStudentId(currentUserScopeService.currentUserIdOrNull());
        entity.setDepartmentId(currentUserScopeService.requireCurrentDepartmentId());
        entity.setStatus(STATUS_DRAFT);
        experimentReportMapper.insert(entity);
        replaceConsumables(entity.getId(), request.getConsumables());
        return getById(entity.getId());
    }

    @Transactional
    @Override
    public ExperimentReportEntity updateDraft(Long id, ExperimentReportSaveRequest request) {
        ensureStudent();
        ExperimentReportEntity entity = requireReport(id);
        ensureStudentOwner(entity);
        if (entity.getStatus() != STATUS_DRAFT && entity.getStatus() != STATUS_RETURNED) {
            throw new BusinessException(400, "当前状态不允许编辑");
        }
        buildReport(entity, request);
        entity.setStatus(STATUS_DRAFT);
        entity.setTeacherComment(null);
        entity.setSubmittedAt(null);
        entity.setReviewedAt(null);
        experimentReportMapper.updateDraft(entity);
        replaceConsumables(entity.getId(), request.getConsumables());
        return getById(id);
    }

    @Transactional
    @Override
    public ExperimentReportEntity submit(Long id) {
        ensureStudent();
        ExperimentReportEntity entity = requireReport(id);
        ensureStudentOwner(entity);
        if (entity.getStatus() != STATUS_DRAFT && entity.getStatus() != STATUS_RETURNED) {
            throw new BusinessException(400, "当前状态不允许提交");
        }
        if (entity.getTeacherId() == null) {
            throw new BusinessException(400, "请选择指导教师");
        }
        experimentReportMapper.submit(id);
        return getById(id);
    }

    @Transactional
    @Override
    public ExperimentReportEntity review(Long id, ExperimentReportReviewRequest request) {
        ensureTeacher();
        if (request.getStatus() == null || (request.getStatus() != STATUS_APPROVED && request.getStatus() != STATUS_RETURNED)) {
            throw new BusinessException(400, "审核状态不合法");
        }
        ExperimentReportEntity entity = requireReport(id);
        if (!currentUserScopeService.currentUserIdOrNull().equals(entity.getTeacherId())) {
            throw new BusinessException(403, "无权审核该报告");
        }
        if (entity.getStatus() != STATUS_PENDING) {
            throw new BusinessException(400, "仅待审核报告可审核");
        }
        experimentReportMapper.review(id, request.getStatus(), trimToNull(request.getTeacherComment()),
            currentUserScopeService.currentUserIdOrNull());
        return getById(id);
    }

    @Override
    public byte[] exportWord(Long id) {
        ExperimentReportEntity report = getById(id);
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configureDocument(document);
            addCoverPage(document, report);
            addContentPage(document, report);
            document.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(500, "Word生成失败");
        }
    }

    private ExperimentReportEntity buildReport(ExperimentReportEntity entity, ExperimentReportSaveRequest request) {
        LabEntity lab = labMapper.selectById(request.getLabId());
        if (lab == null || (lab.getDeleted() != null && lab.getDeleted() == 1)) {
            throw new BusinessException(404, "实验室不存在");
        }
        currentUserScopeService.ensureCurrentDepartmentAccessible(lab.getDepartmentId(), "实验室不存在");
        UserEntity teacher = userMapper.selectById(request.getTeacherId());
        if (teacher == null || teacher.getDepartmentId() == null || !teacher.getDepartmentId().equals(lab.getDepartmentId())) {
            throw new BusinessException(404, "指导教师不存在");
        }
        boolean teacherRoleMatched = userMapper.selectTeacherOptions(lab.getDepartmentId()).stream()
            .anyMatch(item -> item.getId().equals(request.getTeacherId()));
        if (!teacherRoleMatched) {
            throw new BusinessException(404, "指导教师不存在");
        }
        entity.setTeacherId(request.getTeacherId());
        entity.setLabId(request.getLabId());
        entity.setReservationId(request.getReservationId());
        entity.setTitle(request.getTitle().trim());
        entity.setExperimentName(request.getExperimentName().trim());
        entity.setExperimentDate(request.getExperimentDate());
        entity.setPurpose(trimToNull(request.getPurpose()));
        entity.setPrinciple(trimToNull(request.getPrinciple()));
        entity.setSteps(trimToNull(request.getSteps()));
        entity.setResultData(trimToNull(request.getResultData()));
        entity.setAnalysis(trimToNull(request.getAnalysis()));
        entity.setConclusion(trimToNull(request.getConclusion()));
        return entity;
    }

    private void replaceConsumables(Long reportId, List<ExperimentReportConsumableRequest> consumables) {
        experimentReportMapper.deleteConsumables(reportId);
        if (consumables == null) {
            return;
        }
        for (ExperimentReportConsumableRequest item : consumables) {
            if (item == null || !StringUtils.hasText(item.getConsumableName())) {
                continue;
            }
            ExperimentReportConsumableEntity entity = new ExperimentReportConsumableEntity();
            entity.setReportId(reportId);
            entity.setConsumableName(item.getConsumableName().trim());
            entity.setSpecification(trimToNull(item.getSpecification()));
            entity.setQuantity(item.getQuantity() == null ? 0 : item.getQuantity());
            entity.setUnit(trimToNull(item.getUnit()));
            entity.setRemark(trimToNull(item.getRemark()));
            experimentReportMapper.insertConsumable(entity);
        }
    }

    private ExperimentReportEntity requireReport(Long id) {
        ExperimentReportEntity report = experimentReportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException(404, "实验报告不存在");
        }
        return report;
    }

    private void ensureVisible(ExperimentReportEntity report) {
        Long userId = currentUserScopeService.currentUserIdOrNull();
        if (currentUserScopeService.isStudent() && userId != null && userId.equals(report.getStudentId())) {
            return;
        }
        if (currentUserScopeService.isTeacher() && userId != null && userId.equals(report.getTeacherId())) {
            return;
        }
        if (currentUserScopeService.isAdmin()) {
            currentUserScopeService.ensureCurrentDepartmentAccessible(report.getDepartmentId(), "实验报告不存在");
            return;
        }
        throw new BusinessException(403, "无权访问实验报告");
    }

    private void ensureStudentOwner(ExperimentReportEntity report) {
        if (!currentUserScopeService.currentUserIdOrNull().equals(report.getStudentId())) {
            throw new BusinessException(403, "无权操作该报告");
        }
    }

    private void fillConsumables(ExperimentReportEntity report) {
        report.setConsumables(experimentReportMapper.selectConsumables(report.getId()));
    }

    private void ensureStudent() {
        if (!currentUserScopeService.isStudent()) {
            throw new BusinessException(403, "仅学生可操作报告");
        }
    }

    private void ensureTeacher() {
        if (!currentUserScopeService.isTeacher()) {
            throw new BusinessException(403, "仅教师可审核报告");
        }
    }

    private String nextReportNo() {
        return "ER" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void configureDocument(XWPFDocument document) {
        CTSectPr section = document.getDocument().getBody().isSetSectPr()
            ? document.getDocument().getBody().getSectPr()
            : document.getDocument().getBody().addNewSectPr();
        section.addNewPgSz().setOrient(STPageOrientation.PORTRAIT);
        CTPageMar margin = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
        margin.setTop(BigInteger.valueOf(1440));
        margin.setBottom(BigInteger.valueOf(1440));
        margin.setLeft(BigInteger.valueOf(1440));
        margin.setRight(BigInteger.valueOf(1440));
    }

    private void addCoverPage(XWPFDocument document, ExperimentReportEntity report) {
        addBlankParagraph(document, 6);
        addCenteredText(document, "实验报告", 26, true, 0, 1600);
        addCoverInfoTable(document, report);
        addBlankParagraph(document, 5);
        addCenteredText(document, "XXXX大学", 14, false, 0, 160);
        addCenteredText(document, defaultText(report.getDepartmentName()), 14, false, 0, 0);

        XWPFParagraph breakParagraph = document.createParagraph();
        breakParagraph.createRun().addBreak(BreakType.PAGE);
    }

    private void addCoverInfoTable(XWPFDocument document, ExperimentReportEntity report) {
        XWPFTable table = document.createTable(5, 4);
        table.setTableAlignment(TableRowAlign.CENTER);
        setTableWidth(table, 7800);

        writeCoverRow(table.getRow(0), "实验名称", report.getExperimentName(), "", "");
        mergeCells(table.getRow(0), 1, 3);
        writeCoverRow(table.getRow(1), "实验教室", report.getLabName(), "实验日期", formatChineseDate(report.getExperimentDate()));
        writeCoverRow(table.getRow(2), "学    号", report.getStudentNo(), "姓    名", report.getStudentName());
        writeCoverRow(table.getRow(3), "所属学院", report.getDepartmentName(), "", "");
        mergeCells(table.getRow(3), 1, 3);
        writeCoverRow(table.getRow(4), "指导教师", report.getTeacherName(), "", "");
        mergeCells(table.getRow(4), 1, 3);
    }

    private void writeCoverRow(XWPFTableRow row, String label1, String value1, String label2, String value2) {
        writeCell(row.getCell(0), label1, true, ParagraphAlignment.CENTER, 12);
        writeCell(row.getCell(1), value1, false, ParagraphAlignment.CENTER, 12);
        writeCell(row.getCell(2), label2, true, ParagraphAlignment.CENTER, 12);
        writeCell(row.getCell(3), value2, false, ParagraphAlignment.CENTER, 12);
    }

    private void addContentPage(XWPFDocument document, ExperimentReportEntity report) {
        XWPFTable table = document.createTable(6, 1);
        table.setTableAlignment(TableRowAlign.CENTER);
        setTableWidth(table, 8600);

        writeSectionCell(table.getRow(0).getCell(0), "一、 实验目的", numberedLines(report.getPurpose()));
        writeSectionCell(table.getRow(1).getCell(0), "二、 实验原理", defaultText(report.getPrinciple()));
        writeSectionCell(table.getRow(2).getCell(0), "三、 实验内容及结果", combineText(report.getSteps(), report.getResultData()));
        writeSectionCell(table.getRow(3).getCell(0), "四、 实验过程分析与讨论", defaultText(report.getAnalysis()));
        writeSectionCell(table.getRow(4).getCell(0), "五、 实验结论", defaultText(report.getConclusion()));
        writeSectionCell(table.getRow(5).getCell(0), "六、 指导教师意见",
            defaultText(report.getTeacherComment()) + "\n\n审核状态：" + statusText(report.getStatus())
                + "\n\n指导教师签字： " + defaultText(report.getTeacherName())
                + "        日期： " + formatChineseDate(resolveReviewDate(report)));
    }

    private void writeSectionCell(XWPFTableCell cell, String title, String content) {
        cell.removeParagraph(0);
        XWPFParagraph heading = cell.addParagraph();
        heading.setAlignment(ParagraphAlignment.LEFT);
        setParagraphSpacing(heading, 80, 120, ParagraphAlignment.LEFT);
        XWPFRun headingRun = heading.createRun();
        styleRun(headingRun, title, true, 14);

        for (String line : defaultText(content).split("\\R", -1)) {
            XWPFParagraph paragraph = cell.addParagraph();
            paragraph.setAlignment(ParagraphAlignment.LEFT);
            paragraph.setIndentationLeft(360);
            setParagraphSpacing(paragraph, 40, 120, ParagraphAlignment.LEFT);
            XWPFRun run = paragraph.createRun();
            styleRun(run, line, false, 13);
        }
    }

    private void writeCell(XWPFTableCell cell, String text, boolean bold, ParagraphAlignment alignment, int fontSize) {
        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setAlignment(alignment);
        paragraph.setVerticalAlignment(TextAlignment.CENTER);
        setParagraphSpacing(paragraph, 0, 0, alignment);
        XWPFRun run = paragraph.createRun();
        styleRun(run, defaultText(text), bold, fontSize);
    }

    private void addBlankParagraph(XWPFDocument document, int count) {
        for (int i = 0; i < count; i++) {
            XWPFParagraph paragraph = document.createParagraph();
            setParagraphSpacing(paragraph, 0, 120, ParagraphAlignment.LEFT);
        }
    }

    private void addCenteredText(XWPFDocument document, String text, int fontSize, boolean bold, int before, int after) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        setParagraphSpacing(paragraph, before, after, ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        styleRun(run, text, bold, fontSize);
    }

    private void setTableWidth(XWPFTable table, long width) {
        CTTblPr tblPr = table.getCTTbl().getTblPr() == null ? table.getCTTbl().addNewTblPr() : table.getCTTbl().getTblPr();
        CTTblWidth tblW = tblPr.isSetTblW() ? tblPr.getTblW() : tblPr.addNewTblW();
        tblW.setType(STTblWidth.DXA);
        tblW.setW(BigInteger.valueOf(width));
    }

    private void mergeCells(XWPFTableRow row, int fromCell, int toCell) {
        for (int i = fromCell; i <= toCell; i++) {
            XWPFTableCell cell = row.getCell(i);
            CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
            if (i == fromCell) {
                tcPr.addNewHMerge().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge.RESTART);
            } else {
                tcPr.addNewHMerge().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge.CONTINUE);
            }
        }
    }

    private void styleRun(XWPFRun run, String text, boolean bold, int fontSize) {
        run.setText(text);
        run.setBold(bold);
        run.setFontFamily("宋体");
        run.setFontSize(fontSize);
        run.setColor("000000");
    }

    private void setParagraphSpacing(XWPFParagraph paragraph, int before, int after, ParagraphAlignment alignment) {
        CTPPr ppr = paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
        CTSpacing spacing = ppr.isSetSpacing() ? ppr.getSpacing() : ppr.addNewSpacing();
        spacing.setBefore(BigInteger.valueOf(before));
        spacing.setAfter(BigInteger.valueOf(after));
        spacing.setLine(BigInteger.valueOf(360));
        spacing.setLineRule(org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.AUTO);
        CTJc jc = ppr.isSetJc() ? ppr.getJc() : ppr.addNewJc();
        if (alignment == ParagraphAlignment.CENTER) {
            jc.setVal(STJc.CENTER);
        } else if (alignment == ParagraphAlignment.RIGHT) {
            jc.setVal(STJc.RIGHT);
        } else if (alignment == ParagraphAlignment.BOTH) {
            jc.setVal(STJc.BOTH);
        } else {
            jc.setVal(STJc.LEFT);
        }
    }

    private String combineText(String first, String second) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(first)) {
            builder.append(first.trim());
        }
        if (StringUtils.hasText(second)) {
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append(second.trim());
        }
        return builder.isEmpty() ? "无" : builder.toString();
    }

    private String numberedLines(String value) {
        if (!StringUtils.hasText(value)) {
            return "无";
        }
        String[] lines = value.trim().split("\\R+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(i + 1).append("：").append(lines[i].trim());
        }
        return builder.toString();
    }

    private String consumablesText(List<ExperimentReportConsumableEntity> consumables) {
        if (consumables == null || consumables.isEmpty()) {
            return "无";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < consumables.size(); i++) {
            ExperimentReportConsumableEntity item = consumables.get(i);
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(i + 1).append("：")
                .append(defaultText(item.getConsumableName()))
                .append("，规格：").append(defaultText(item.getSpecification()))
                .append("，数量：").append(item.getQuantity() == null ? 0 : item.getQuantity())
                .append(defaultText(item.getUnit()))
                .append(StringUtils.hasText(item.getRemark()) ? "，备注：" + item.getRemark() : "");
        }
        return builder.toString();
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "无";
    }

    private LocalDate resolveReviewDate(ExperimentReportEntity report) {
        if (report.getReviewedAt() != null) {
            return report.getReviewedAt().toLocalDate();
        }
        if (report.getSubmittedAt() != null) {
            return report.getSubmittedAt().toLocalDate();
        }
        return report.getExperimentDate();
    }

    private String formatChineseDate(LocalDate date) {
        if (date == null) {
            return "无";
        }
        return date.format(DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日"));
    }

    private String statusText(Integer status) {
        if (status != null && status == STATUS_PENDING) {
            return "待审核";
        }
        if (status != null && status == STATUS_APPROVED) {
            return "已通过";
        }
        if (status != null && status == STATUS_RETURNED) {
            return "已退回";
        }
        return "草稿";
    }
}
