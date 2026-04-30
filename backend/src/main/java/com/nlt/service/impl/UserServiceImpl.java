package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.user.PasswordResetRequest;
import com.nlt.domain.dto.user.PasswordUpdateRequest;
import com.nlt.domain.dto.user.UserCreateRequest;
import com.nlt.domain.dto.user.UserImportRowDto;
import com.nlt.domain.dto.user.UserProfileUpdateRequest;
import com.nlt.domain.dto.user.UserUpdateRequest;
import com.nlt.domain.entity.DepartmentEntity;
import com.nlt.domain.entity.RoleEntity;
import com.nlt.domain.entity.UserEntity;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.domain.vo.user.UserImportFailDetailVo;
import com.nlt.domain.vo.user.UserImportResultVo;
import com.nlt.domain.vo.user.UserVO;
import com.nlt.mapper.DepartmentMapper;
import com.nlt.mapper.RoleMapper;
import com.nlt.mapper.UserMapper;
import com.nlt.mapper.UserRoleMapper;
import com.nlt.service.UserService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_PASSWORD = "123456";

    private static final String[] IMPORT_HEADERS = {
        "学号/工号", "姓名", "性别", "手机号", "邮箱", "所属部门", "角色"
    };

    private static final String[] IMPORT_HEADER_ALIASES = {
        "userNo|学号/工号",
        "realName|姓名",
        "gender|性别",
        "phone|手机号",
        "email|邮箱",
        "departmentName|所属部门",
        "roleName|角色"
    };

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final DepartmentMapper departmentMapper;
    private final RoleMapper roleMapper;
    private final PlatformTransactionManager transactionManager;

    @Override
    public PageData<UserVO> page(int pageNum, int pageSize, String userNo, String realName, Long departmentId,
        String roleCode, Integer status) {
        int offset = (pageNum - 1) * pageSize;
        List<UserVO> list = userMapper.selectPage(offset, pageSize, userNo, realName, departmentId, roleCode, status)
            .stream()
            .map(this::toVo)
            .toList();
        return new PageData<>(list, userMapper.countPage(userNo, realName, departmentId, roleCode, status), pageNum, pageSize);
    }

    @Transactional
    @Override
    public UserVO create(UserCreateRequest request) {
        String userNo = normalizeUserNo(request.getUserNo());
        if (userNo == null) {
            throw new BusinessException(400, "学号/工号不能为空");
        }
        ensureUserNoAvailable(userNo, null);

        UserEntity entity = new UserEntity();
        BeanUtils.copyProperties(request, entity);
        entity.setUserNo(userNo);
        entity.setCreditScore(100);
        entity.setViolationCount(0);
        entity.setNormalReservationStreak(0);
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        try {
            userMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(400, "学号/工号已存在");
        }
        rebuildUserRoles(entity.getId(), request.getRoleIds());
        return getById(entity.getId());
    }

    @Override
    public UserVO getById(Long id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return toVo(entity);
    }

    @Transactional
    @Override
    public UserVO update(Long id, UserUpdateRequest request) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        String userNo = normalizeUserNo(request.getUserNo());
        if (userNo == null) {
            userNo = entity.getUserNo();
        }
        ensureUserNoAvailable(userNo, id);
        BeanUtils.copyProperties(request, entity);
        entity.setUserNo(userNo);
        userMapper.update(entity);
        rebuildUserRoles(id, request.getRoleIds());
        return getById(id);
    }

    @Override
    public List<OptionItem> options(Integer status) {
        return userMapper.selectOptions(status).stream()
            .map(item -> new OptionItem(item.getRealName(), item.getId()))
            .toList();
    }

    @Override
    public List<OptionItem> teacherOptions(Long departmentId) {
        return userMapper.selectTeacherOptions(departmentId).stream()
            .map(item -> new OptionItem(item.getRealName(), item.getId()))
            .toList();
    }

    @Override
    public void delete(Long id) {
        getById(id);
        userMapper.softDelete(id);
    }

    @Override
    public void resetPassword(Long id, PasswordResetRequest request) {
        getById(id);
        userMapper.updatePassword(id, request.getNewPassword());
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        getById(id);
        userMapper.updateStatus(id, status);
    }

    @Override
    public UserVO currentUser(Long userId) {
        return getById(userId);
    }

    @Override
    public UserVO updateProfile(Long userId, UserProfileUpdateRequest request) {
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        BeanUtils.copyProperties(request, entity);
        userMapper.updateProfile(entity);
        return getById(userId);
    }

    @Override
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (!entity.getPassword().equals(request.getOldPassword())) {
            throw new BusinessException(400, "旧密码错误");
        }
        if (request.getNewPassword().equals(request.getOldPassword())) {
            throw new BusinessException(400, "新密码不能与旧密码相同");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的新密码不一致");
        }
        userMapper.updatePassword(userId, request.getNewPassword());
    }

    @Override
    public UserImportResultVo importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请上传.xlsx文件");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessException(400, "仅支持.xlsx文件");
        }

        List<UserImportRowDto> rows;
        try {
            rows = parseImportRows(file);
        } catch (IOException ex) {
            throw new BusinessException(400, "Excel文件读取失败");
        }

        Map<String, DepartmentEntity> departmentMap = new HashMap<>();
        for (DepartmentEntity department : departmentMapper.selectOptions()) {
            if (department.getDepartmentName() != null) {
                departmentMap.put(normalizeKey(department.getDepartmentName()), department);
            }
        }

        Map<String, RoleEntity> roleMap = new HashMap<>();
        for (RoleEntity role : roleMapper.selectOptions()) {
            if (role.getRoleName() != null) {
                roleMap.put(normalizeKey(role.getRoleName()), role);
            }
        }

        Set<String> userNoSet = new HashSet<>();
        Set<String> phoneSet = new HashSet<>();
        Set<String> emailSet = new HashSet<>();
        List<ImportCandidate> candidates = new ArrayList<>();
        List<UserImportFailDetailVo> failDetails = new ArrayList<>();

        for (UserImportRowDto row : rows) {
            String reason = validateImportRow(row, departmentMap, roleMap, userNoSet, phoneSet, emailSet);
            if (reason != null) {
                failDetails.add(new UserImportFailDetailVo(row.getRowNum(), reason));
                continue;
            }
            candidates.add(new ImportCandidate(row,
                departmentMap.get(normalizeKey(row.getDepartmentName())),
                roleMap.get(normalizeKey(row.getRoleName()))));
        }

        int success = 0;
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        for (ImportCandidate candidate : candidates) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    UserEntity entity = new UserEntity();
                    entity.setDepartmentId(candidate.department().getId());
                    entity.setUserNo(normalizeUserNo(candidate.row().getUserNo()));
                    entity.setPassword(DEFAULT_PASSWORD);
                    entity.setRealName(candidate.row().getRealName().trim());
                    entity.setGender(resolveGender(candidate.row().getGender()));
                    entity.setPhone(candidate.row().getPhone().trim());
                    entity.setEmail(candidate.row().getEmail().trim());
                    entity.setCreditScore(100);
                    entity.setViolationCount(0);
                    entity.setNormalReservationStreak(0);
                    entity.setStatus(1);
                    userMapper.insert(entity);
                    userRoleMapper.insert(entity.getId(), candidate.role().getId());
                });
                success++;
            } catch (DuplicateKeyException ex) {
                failDetails.add(new UserImportFailDetailVo(candidate.row().getRowNum(), resolveDuplicateReason(candidate.row())));
            } catch (Exception ex) {
                failDetails.add(new UserImportFailDetailVo(candidate.row().getRowNum(),
                    ex.getMessage() == null || ex.getMessage().isBlank() ? "导入失败" : ex.getMessage()));
            }
        }

        UserImportResultVo result = new UserImportResultVo();
        result.setTotal(rows.size());
        result.setSuccess(success);
        result.setFail(rows.size() - success);
        result.setFailDetails(failDetails);
        return result;
    }

    @Override
    public byte[] downloadImportTemplate() {
        try (Workbook workbook = buildTemplateWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(500, "导入模板生成失败");
        }
    }

    @Override
    public int recoverCreditScores() {
        return userMapper.recoverCreditScore();
    }

    private UserVO toVo(UserEntity entity) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setRoleIds(new ArrayList<>(userRoleMapper.selectRoleIdsByUserId(entity.getId())));
        return vo;
    }

    private void rebuildUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            userRoleMapper.insert(userId, roleId);
        }
    }

    private void ensureUserNoAvailable(String userNo, Long currentUserId) {
        UserEntity existing = userMapper.selectByUserNo(userNo);
        if (existing != null && (currentUserId == null || !existing.getId().equals(currentUserId))) {
            throw new BusinessException(400, "学号/工号已存在");
        }
    }

    private List<UserImportRowDto> parseImportRows(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new BusinessException(400, "Excel文件中没有工作表");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null || !matchesHeader(headerRow)) {
                throw new BusinessException(400, "Excel模板表头不正确");
            }

            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<UserImportRowDto> rows = new ArrayList<>();
            for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlankRow(row, formatter, evaluator)) {
                    continue;
                }
                UserImportRowDto dto = new UserImportRowDto();
                dto.setRowNum(i + 1);
                dto.setUserNo(getCellText(row, 0, formatter, evaluator));
                dto.setRealName(getCellText(row, 1, formatter, evaluator));
                dto.setGender(getCellText(row, 2, formatter, evaluator));
                dto.setPhone(getCellText(row, 3, formatter, evaluator));
                dto.setEmail(getCellText(row, 4, formatter, evaluator));
                dto.setDepartmentName(getCellText(row, 5, formatter, evaluator));
                dto.setRoleName(getCellText(row, 6, formatter, evaluator));
                rows.add(dto);
            }
            return rows;
        }
    }

    private boolean matchesHeader(Row headerRow) {
        for (int i = 0; i < IMPORT_HEADERS.length; i++) {
            Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String value = cell == null ? "" : cell.toString().trim();
            if (!matchesHeaderCell(value, IMPORT_HEADER_ALIASES[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesHeaderCell(String value, String aliasGroup) {
        if (value == null) {
            return false;
        }
        for (String alias : aliasGroup.split("\\|")) {
            if (alias.equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (int i = 0; i < IMPORT_HEADERS.length; i++) {
            if (!getCellText(row, i, formatter, evaluator).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getCellText(Row row, int index, DataFormatter formatter, FormulaEvaluator evaluator) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private String validateImportRow(UserImportRowDto row, Map<String, DepartmentEntity> departmentMap,
        Map<String, RoleEntity> roleMap, Set<String> userNoSet, Set<String> phoneSet, Set<String> emailSet) {
        String userNo = normalizeUserNo(row.getUserNo());
        if (userNo == null) {
            return "学号/工号不能为空";
        }
        if (row.getRealName() == null || row.getRealName().trim().isEmpty()) {
            return "姓名不能为空";
        }
        if (row.getGender() == null || row.getGender().trim().isEmpty()) {
            return "性别不能为空";
        }
        if (row.getPhone() == null || row.getPhone().trim().isEmpty()) {
            return "手机号不能为空";
        }
        if (row.getEmail() == null || row.getEmail().trim().isEmpty()) {
            return "邮箱不能为空";
        }
        if (row.getDepartmentName() == null || row.getDepartmentName().trim().isEmpty()) {
            return "所属部门不能为空";
        }
        if (row.getRoleName() == null || row.getRoleName().trim().isEmpty()) {
            return "角色不能为空";
        }
        if (resolveGender(row.getGender()) == null) {
            return "性别值不合法";
        }
        if (!PHONE_PATTERN.matcher(row.getPhone().trim()).matches()) {
            return "手机号格式不合法";
        }
        if (!EMAIL_PATTERN.matcher(row.getEmail().trim()).matches()) {
            return "邮箱格式不合法";
        }

        if (!userNoSet.add(normalizeKey(userNo))) {
            return "Excel文件内学号/工号重复";
        }
        if (!phoneSet.add(normalizeKey(row.getPhone()))) {
            return "Excel文件内手机号重复";
        }
        if (!emailSet.add(normalizeKey(row.getEmail()))) {
            return "Excel文件内邮箱重复";
        }
        if (!departmentMap.containsKey(normalizeKey(row.getDepartmentName()))) {
            return "所属部门不存在";
        }
        if (!roleMap.containsKey(normalizeKey(row.getRoleName()))) {
            return "角色不存在";
        }
        if (userMapper.selectByUserNo(userNo) != null) {
            return "学号/工号已存在";
        }
        if (userMapper.selectByPhone(row.getPhone().trim()) != null) {
            return "手机号已存在";
        }
        if (userMapper.selectByEmail(row.getEmail().trim()) != null) {
            return "邮箱已存在";
        }
        return null;
    }

    private Integer resolveGender(String genderText) {
        if (genderText == null) {
            return null;
        }
        String value = genderText.trim();
        if (value.isEmpty()) {
            return null;
        }
        if ("男".equals(value) || "1".equals(value)) {
            return 1;
        }
        if ("女".equals(value) || "2".equals(value)) {
            return 2;
        }
        if ("未知".equals(value) || "0".equals(value)) {
            return 0;
        }
        return null;
    }

    private String normalizeKey(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUserNo(String userNo) {
        return userNo == null ? null : userNo.trim();
    }

    private String resolveDuplicateReason(UserImportRowDto row) {
        String userNo = normalizeUserNo(row.getUserNo());
        if (userMapper.selectByUserNo(userNo) != null) {
            return "学号/工号已存在";
        }
        if (userMapper.selectByPhone(row.getPhone().trim()) != null) {
            return "手机号已存在";
        }
        if (userMapper.selectByEmail(row.getEmail().trim()) != null) {
            return "邮箱已存在";
        }
        return "数据已存在或发生冲突";
    }

    private Workbook buildTemplateWorkbook() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("用户导入模板");
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < IMPORT_HEADERS.length; i++) {
            headerRow.createCell(i).setCellValue(IMPORT_HEADERS[i]);
            sheet.setColumnWidth(i, 18 * 256);
        }
        return workbook;
    }

    private record ImportCandidate(UserImportRowDto row, DepartmentEntity department, RoleEntity role) {
    }
}
