package com.nlt.service;

import com.nlt.domain.dto.checkin.CheckinSubmitRequest;
import com.nlt.domain.entity.LabEntity;
import com.nlt.domain.vo.checkin.CheckinResultVo;

public interface CheckinService {

    CheckinResultVo submit(CheckinSubmitRequest request, LabEntity lab, Long currentUserId);

    int handleNoShowReservations();

    int handleAutoCompleteReservations();
}
