package com.nlt.service;

import com.nlt.common.api.PageData;
import com.nlt.domain.dto.user.PasswordResetRequest;
import com.nlt.domain.dto.user.PasswordUpdateRequest;
import com.nlt.domain.dto.user.UserCreateRequest;
import com.nlt.domain.dto.user.UserProfileUpdateRequest;
import com.nlt.domain.dto.user.UserUpdateRequest;
import com.nlt.domain.vo.common.OptionItem;
import com.nlt.domain.vo.user.UserImportResultVo;
import com.nlt.domain.vo.user.UserVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    PageData<UserVO> page(int pageNum, int pageSize, String userNo, String realName, Long departmentId, String roleCode, Integer status);

    UserVO create(UserCreateRequest request);

    UserVO getById(Long id);

    UserVO update(Long id, UserUpdateRequest request);

    List<OptionItem> options(Integer status);

    void delete(Long id);

    void resetPassword(Long id, PasswordResetRequest request);

    void updateStatus(Long id, Integer status);

    UserVO currentUser(Long userId);

    UserVO updateProfile(Long userId, UserProfileUpdateRequest request);

    void updatePassword(Long userId, PasswordUpdateRequest request);

    UserImportResultVo importUsers(MultipartFile file);

    int recoverCreditScores();

    byte[] downloadImportTemplate();
}
