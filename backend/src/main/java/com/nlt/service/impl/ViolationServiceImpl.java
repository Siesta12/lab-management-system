package com.nlt.service.impl;

import com.nlt.common.api.PageData;
import com.nlt.common.exception.BusinessException;
import com.nlt.domain.dto.violation.ViolationSaveRequest;
import com.nlt.domain.entity.ViolationRecordEntity;
import com.nlt.mapper.ViolationMapper;
import com.nlt.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViolationServiceImpl implements ViolationService {

    private final ViolationMapper violationMapper;

    /**
     * 查询违规记录列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param userId 用户ID
     * @param reservationId 预约ID
     * @param violationType 违规类型
     * @return 分页数据
     */
    @Override
    public PageData<ViolationRecordEntity> page(int pageNum, int pageSize, Long userId, Long reservationId, Integer violationType) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(
                violationMapper.selectPage(offset, pageSize, userId, reservationId, violationType),
                violationMapper.countPage(userId, reservationId, violationType),
                pageNum,
                pageSize
        );
    }

    /**
     * 新增违规记录
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public ViolationRecordEntity create(ViolationSaveRequest request) {
        ViolationRecordEntity entity = new ViolationRecordEntity();
        BeanUtils.copyProperties(request, entity);
        violationMapper.insert(entity);
        return getById(entity.getId());
    }

    /**
     * 查询违规记录
     * @param id 主键ID
     * @return 处理结果
     */
    @Override
    public ViolationRecordEntity getById(Long id) {
        ViolationRecordEntity entity = violationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "违纪记录不存在");
        }
        return entity;
    }

    /**
     * 查询当前用户预约信息列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分页数据
     */
    @Override
    public PageData<ViolationRecordEntity> mine(Long userId, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(violationMapper.selectMine(userId, offset, pageSize), violationMapper.countMine(userId), pageNum, pageSize);
    }

    /**
     * 删除违规记录
     * @param id 主键ID
     */
    @Override
    public void delete(Long id) {
        getById(id);
        violationMapper.deleteById(id);
    }

}

