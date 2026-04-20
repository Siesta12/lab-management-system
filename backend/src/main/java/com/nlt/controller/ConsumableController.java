package com.nlt.controller;

import com.nlt.common.api.ApiResponse;
import com.nlt.common.api.PageData;
import com.nlt.common.security.TokenService;
import com.nlt.domain.dto.consumable.ConsumableSaveRequest;
import com.nlt.domain.dto.consumable.ConsumableStockUpdateRequest;
import com.nlt.domain.entity.ConsumableEntity;
import com.nlt.service.ConsumableService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consumables")
@RequiredArgsConstructor
public class ConsumableController {

    private final ConsumableService consumableService;
    private final TokenService tokenService;

    /**
     * 鏌ヨ鑰楁潗淇℃伅鍒楄〃
     * @param pageNum 椤电爜
     * @param pageSize 姣忛〉鏉℃暟
     * @param labId 瀹為獙瀹D
     * @param consumableName 鍙傛暟
     * @param consumableCode 鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping
    public ApiResponse<PageData<ConsumableEntity>> page(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) Long labId,
        @RequestParam(required = false) String consumableName,
        @RequestParam(required = false) String consumableCode) {
        return ApiResponse.success(consumableService.page(pageNum, pageSize, labId, consumableName, consumableCode));
    }

    /**
     * 鏂板鑰楁潗淇℃伅
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PostMapping
    public ApiResponse<ConsumableEntity> create(@Valid @RequestBody ConsumableSaveRequest request) {
        return ApiResponse.created(consumableService.create(request));
    }

    /**
     * 澶勭悊鑰楁潗淇℃伅
     * @param pageNum 椤电爜
     * @param pageSize 姣忛〉鏉℃暟
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/low-stock")
    public ApiResponse<PageData<ConsumableEntity>> lowStock(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(consumableService.warningList(pageNum, pageSize));
    }

    /**
     * 澶勭悊鑰楁潗淇℃伅
     * @param pageNum 椤电爜
     * @param pageSize 姣忛〉鏉℃暟
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/warning-list")
    public ApiResponse<PageData<ConsumableEntity>> warningList(@RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(consumableService.warningList(pageNum, pageSize));
    }

    /**
     * 鏌ヨ鑰楁潗淇℃伅
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @GetMapping("/{id}")
    public ApiResponse<ConsumableEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(consumableService.getById(id));
    }

    /**
     * 鏇存柊鑰楁潗淇℃伅
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @return 鍝嶅簲缁撴灉
     */
    @PutMapping("/{id}")
    public ApiResponse<ConsumableEntity> update(@PathVariable Long id,
        @RequestBody ConsumableSaveRequest request) {
        return ApiResponse.success(consumableService.update(id, request));
    }

    /**
     * 鍒犻櫎鑰楁潗淇℃伅
     * @param id 涓婚敭ID
     * @return 鍝嶅簲缁撴灉
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        consumableService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 鏇存柊鑰楁潗淇℃伅
     * @param id 涓婚敭ID
     * @param request 璇锋眰鍙傛暟
     * @param servletRequest HTTP璇锋眰瀵硅薄
     * @return 鍝嶅簲缁撴灉
     */
    @PatchMapping("/{id}/stock")
    public ApiResponse<ConsumableEntity> updateStock(@PathVariable Long id,
        @Valid @RequestBody ConsumableStockUpdateRequest request,
        HttpServletRequest servletRequest) {
        Long userId = tokenService.getCurrentUserId(servletRequest);
        return ApiResponse.success(consumableService.updateStock(id, request, userId));
    }

}
