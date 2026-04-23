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

    /**
     * 查询用户信息列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param username 用户名
     * @param realName 真实姓名
     * @param departmentId 部门ID
     * @param status 状态值
     * @return 分页数据
     */
    PageData<UserVO> page(int pageNum, int pageSize, String username, String realName, Long departmentId, String roleCode, Integer status);

    /**
     * 新增用户信息
     * @param request 请求参数
     * @return 处理结果
     */
    UserVO create(UserCreateRequest request);

    /**
     * 查询用户信息
     * @param id 主键ID
     * @return 处理结果
     */
    UserVO getById(Long id);

    /**
     * 更新用户信息
     * @param id 主键ID
     * @param request 请求参数
     * @return 处理结果
     */
    UserVO update(Long id, UserUpdateRequest request);

    /**
     * 获取用户选项列表
     * @param status 状态值
     * @return 数据列表
     */
    List<OptionItem> options(Integer status);

    /**
     * 删除用户信息
     * @param id 主键ID
     */
    void delete(Long id);

    /**
     * 重置用户密码
     * @param id 主键ID
     * @param request 请求参数
     */
    void resetPassword(Long id, PasswordResetRequest request);

    /**
     * 更新用户状态
     * @param id 主键ID
     * @param status 状态值
     */
    void updateStatus(Long id, Integer status);

    /**
     * 获取当前登录用户信息
     * @param userId 用户ID
     * @return 处理结果
     */
    UserVO currentUser(Long userId);

    /**
     * 更新用户个人资料
     * @param userId 用户ID
     * @param request 请求参数
     * @return 处理结果
     */
    UserVO updateProfile(Long userId, UserProfileUpdateRequest request);

    /**
     * 更新用户密码
     * @param userId 用户ID
     * @param request 请求参数
     */
    void updatePassword(Long userId, PasswordUpdateRequest request);

    /**
     * 批量导入用户
     * @param file Excel 文件
     * @return 导入结果
     */
    UserImportResultVo importUsers(MultipartFile file);

    /**
     * 下载导入模板
     * @return 模板文件字节数组
     */
    byte[] downloadImportTemplate();

}

