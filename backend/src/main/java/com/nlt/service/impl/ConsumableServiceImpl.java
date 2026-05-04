package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.CurrentUserScopeService;
import com.nlt.domain.dto.consumable.ConsumableSaveRequest;
import com.nlt.domain.dto.consumable.ConsumableStockUpdateRequest;
import com.nlt.domain.entity.ConsumableEntity;
import com.nlt.domain.entity.ConsumableStockLogEntity;
import com.nlt.domain.entity.ExperimentReportConsumableEntity;
import com.nlt.domain.entity.LabEntity;
import com.nlt.mapper.ConsumableMapper;
import com.nlt.mapper.ConsumableStockLogMapper;
import com.nlt.mapper.ExperimentReportMapper;
import com.nlt.mapper.LabMapper;
import com.nlt.service.ConsumableService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ConsumableServiceImpl implements ConsumableService {

    private static final String CONSUMABLE_CODE_PREFIX = "C";
    private static final int CONSUMABLE_CODE_DIGITS = 5;

    private final ConsumableMapper consumableMapper;
    private final ConsumableStockLogMapper consumableStockLogMapper;
    private final ExperimentReportMapper experimentReportMapper;
    private final LabMapper labMapper;
    private final CurrentUserScopeService currentUserScopeService;

    @Override
    public PageData<ConsumableEntity> page(int pageNum, int pageSize, Long labId, String labType, String consumableName,
        String consumableCode, Integer status, Boolean warningOnly) {
        int offset = (pageNum - 1) * pageSize;
        Long departmentId = currentUserScopeService.requireCurrentDepartmentId();
        return new PageData<>(
            consumableMapper.selectPage(offset, pageSize, labId, trimToNull(labType), trimToNull(consumableName), trimToNull(consumableCode),
                departmentId, status, Boolean.TRUE.equals(warningOnly)),
            consumableMapper.countPage(labId, trimToNull(labType), trimToNull(consumableName), trimToNull(consumableCode), departmentId,
                status, Boolean.TRUE.equals(warningOnly)),
            pageNum,
            pageSize
        );
    }

    @Override
    public List<ConsumableEntity> availableOptions(Long labId) {
        LabEntity lab = requireVisibleLab(labId);
        currentUserScopeService.ensureCurrentDepartmentAccessible(lab.getDepartmentId(), "实验室不存在");
        return consumableMapper.selectAvailableOptions(labId);
    }

    @Override
    @Transactional
    public ConsumableEntity create(ConsumableSaveRequest request) {
        ensureAdmin();
        ensureLabAccessible(request.getLabId());
        ConsumableEntity entity = new ConsumableEntity();
        applyEditableFields(entity, request);
        entity.setConsumableCode(generateNextConsumableCode());
        applyDefaults(entity);
        consumableMapper.insert(entity);
        return getById(entity.getId());
    }

    @Override
    public ConsumableEntity getById(Long id) {
        ConsumableEntity entity = consumableMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "耗材不存在");
        }
        ensureLabAccessible(entity.getLabId());
        return entity;
    }

    @Override
    public ConsumableEntity update(Long id, ConsumableSaveRequest request) {
        ensureAdmin();
        ConsumableEntity entity = getById(id);
        ensureLabAccessible(request.getLabId());
        applyEditableFields(entity, request);
        applyDefaults(entity);
        consumableMapper.update(entity);
        return getById(id);
    }

    @Override
    public void delete(Long id) {
        ensureAdmin();
        getById(id);
        consumableMapper.softDelete(id);
    }

    @Override
    public PageData<ConsumableEntity> warningList(int pageNum, int pageSize) {
        Long departmentId = currentUserScopeService.requireCurrentDepartmentId();
        var list = consumableMapper.selectWarningList(departmentId);
        int fromIndex = Math.min((pageNum - 1) * pageSize, list.size());
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        return new PageData<>(list.subList(fromIndex, toIndex), list.size(), pageNum, pageSize);
    }

    @Transactional
    @Override
    public ConsumableEntity updateStock(Long id, ConsumableStockUpdateRequest request, Long operatorUserId) {
        ensureAdmin();
        ConsumableEntity entity = getById(id);
        int beforeStock = entity.getStockQuantity();
        String changeType = normalizeChangeType(request.getChangeType());
        int afterStock = resolveAfterStock(beforeStock, changeType, request);
        if (afterStock < 0) {
            throw new BusinessException(400, "库存数量不能为负数");
        }
        consumableMapper.updateStock(id, afterStock);
        ConsumableStockLogEntity logEntity = new ConsumableStockLogEntity();
        logEntity.setConsumableId(id);
        logEntity.setChangeType(changeType);
        logEntity.setChangeAmount(afterStock - beforeStock);
        logEntity.setBeforeStock(beforeStock);
        logEntity.setAfterStock(afterStock);
        logEntity.setOperatorUserId(operatorUserId);
        logEntity.setRemark(request.getRemark());
        consumableStockLogMapper.insert(logEntity);
        return getById(id);
    }

    @Override
    public PageData<ExperimentReportConsumableEntity> usagePage(int pageNum, int pageSize, Integer status, Long labId,
        String keyword) {
        ensureAdmin();
        int offset = (pageNum - 1) * pageSize;
        Long departmentId = currentUserScopeService.resolveAdminDepartmentId();
        return new PageData<>(
            experimentReportMapper.selectUsagePage(offset, pageSize, departmentId, status, labId, trimToNull(keyword)),
            experimentReportMapper.countUsagePage(departmentId, status, labId, trimToNull(keyword)),
            pageNum,
            pageSize
        );
    }

    private void applyEditableFields(ConsumableEntity entity, ConsumableSaveRequest request) {
        entity.setLabId(request.getLabId());
        entity.setConsumableName(trimRequired(request.getConsumableName(), "耗材名称不能为空"));
        entity.setUnit(trimRequired(request.getUnit(), "单位不能为空"));
        entity.setStockQuantity(request.getStockQuantity());
        entity.setWarningThreshold(request.getWarningThreshold());
        entity.setStatus(request.getStatus());
        entity.setRemark(trimToNull(request.getRemark()));
    }

    private String generateNextConsumableCode() {
        String latestCode = consumableMapper.selectLatestConsumableCodeForUpdate();
        int nextNumber = 1;
        if (StringUtils.hasText(latestCode) && latestCode.length() > 1) {
            try {
                nextNumber = Integer.parseInt(latestCode.substring(1)) + 1;
            } catch (NumberFormatException ignored) {
                nextNumber = 1;
            }
        }
        return CONSUMABLE_CODE_PREFIX + String.format("%0" + CONSUMABLE_CODE_DIGITS + "d", nextNumber);
    }

    private String normalizeChangeType(String rawType) {
        String normalized = trimToNull(rawType);
        if (normalized == null) {
            return "ADJUST";
        }
        String upper = normalized.toUpperCase();
        if (!"IN".equals(upper) && !"OUT".equals(upper) && !"ADJUST".equals(upper)) {
            throw new BusinessException(400, "库存变更类型无效");
        }
        return upper;
    }

    private int resolveAfterStock(int beforeStock, String changeType, ConsumableStockUpdateRequest request) {
        if ("ADJUST".equals(changeType)) {
            if (request.getTargetStock() == null) {
                throw new BusinessException(400, "请填写调整后库存");
            }
            return request.getTargetStock();
        }

        if (request.getQuantity() == null) {
            throw new BusinessException(400, "请填写变更数量");
        }
        if (request.getQuantity() <= 0) {
            throw new BusinessException(400, "变更数量必须大于 0");
        }

        return "IN".equals(changeType)
            ? beforeStock + request.getQuantity()
            : beforeStock - request.getQuantity();
    }

    private void applyDefaults(ConsumableEntity entity) {
        if (entity.getStockQuantity() == null) {
            entity.setStockQuantity(0);
        }
        if (entity.getWarningThreshold() == null) {
            entity.setWarningThreshold(0);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(1);
        }
        if (entity.getStockQuantity() < 0 || entity.getWarningThreshold() < 0) {
            throw new BusinessException(400, "库存数量和预警阈值不能为负数");
        }
    }

    private void ensureAdmin() {
        if (!currentUserScopeService.isAdmin()) {
            throw new BusinessException(403, "仅管理员可以管理耗材");
        }
    }

    private void ensureLabAccessible(Long labId) {
        LabEntity lab = requireVisibleLab(labId);
        if (currentUserScopeService.isAdmin()) {
            currentUserScopeService.ensureDepartmentAccessible(lab.getDepartmentId(), "实验室不存在");
            return;
        }
        currentUserScopeService.ensureCurrentDepartmentAccessible(lab.getDepartmentId(), "实验室不存在");
    }

    private LabEntity requireVisibleLab(Long labId) {
        if (labId == null) {
            throw new BusinessException(400, "实验室不能为空");
        }
        LabEntity lab = labMapper.selectById(labId);
        if (lab == null || (lab.getDeleted() != null && lab.getDeleted() == 1)) {
            throw new BusinessException(404, "实验室不存在");
        }
        return lab;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String trimRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(400, message);
        }
        return normalized;
    }
}
