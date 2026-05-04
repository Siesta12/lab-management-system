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

    @Override
    public ViolationRecordEntity create(ViolationSaveRequest request) {
        ViolationRecordEntity entity = new ViolationRecordEntity();
        BeanUtils.copyProperties(request, entity);
        violationMapper.insert(entity);
        return getById(entity.getId());
    }

    @Override
    public ViolationRecordEntity getById(Long id) {
        ViolationRecordEntity entity = violationMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(404, "违纪记录不存在");
        }
        return entity;
    }

    @Override
    public PageData<ViolationRecordEntity> mine(Long userId, int pageNum, int pageSize, Integer violationType, Integer scoreDirection) {
        int offset = (pageNum - 1) * pageSize;
        return new PageData<>(
            violationMapper.selectMine(userId, offset, pageSize, violationType, scoreDirection),
            violationMapper.countMine(userId, violationType, scoreDirection),
            pageNum,
            pageSize
        );
    }

    @Override
    public void delete(Long id) {
        getById(id);
        violationMapper.deleteById(id);
    }

}

