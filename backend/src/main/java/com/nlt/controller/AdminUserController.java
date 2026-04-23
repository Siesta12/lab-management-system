package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.TokenService;
import com.nlt.domain.vo.user.UserImportResultVo;
import com.nlt.service.UserService;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    private final TokenService tokenService;

    @PostMapping("/import")
    public ApiResponse<UserImportResultVo> importUsers(@RequestParam("file") MultipartFile file,
        jakarta.servlet.http.HttpServletRequest request) {
        ensureAdmin(request);
        return ApiResponse.success(userService.importUsers(file));
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate(jakarta.servlet.http.HttpServletRequest request) {
        ensureAdmin(request);
        byte[] data = userService.downloadImportTemplate();
        String fileName = "用户导入模板.xlsx";
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
            .body(data);
    }

    private void ensureAdmin(jakarta.servlet.http.HttpServletRequest request) {
        var roleCodes = tokenService.getCurrentRoleCodes(request);
        boolean admin = roleCodes != null && roleCodes.stream()
            .anyMatch(code -> code != null && code.toUpperCase().contains("ADMIN"));
        if (!admin) {
            throw new BusinessException(403, "无权限操作");
        }
    }
}
