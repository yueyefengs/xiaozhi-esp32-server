package xiaozhi.modules.ota.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;
import xiaozhi.common.page.PageData;
import xiaozhi.common.user.UserDetail;
import xiaozhi.common.utils.Result;
import xiaozhi.modules.ota.dto.AiOtaDTO;
import xiaozhi.modules.ota.entity.AiOtaEntity;
import xiaozhi.modules.ota.service.AiOtaService;
import xiaozhi.modules.security.user.SecurityUser;

import java.util.Date;
import java.util.Map;

/**
 * 固件管理控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ota/firmware")
@Tag(name = "固件管理", description = "固件管理相关接口")
public class AiOtaController {
    
    private final AiOtaService aiOtaService;
    
    @GetMapping("page")
    @Operation(summary = "分页查询固件列表")
    @RequiresPermissions("sys:role:admin")
    public Result<PageData<AiOtaDTO>> page(AiOtaDTO params) {
        PageData<AiOtaDTO> page = aiOtaService.page(params);
        return new Result<PageData<AiOtaDTO>>().ok(page);
    }
    
    @GetMapping("{id}")
    @Operation(summary = "获取固件详情")
    @RequiresPermissions("sys:role:admin")
    public Result<AiOtaDTO> get(@PathVariable("id") String id) {
        AiOtaDTO data = aiOtaService.get(id);
        return new Result<AiOtaDTO>().ok(data);
    }
    
    @PostMapping
    @Operation(summary = "新增固件")
    @RequiresPermissions("sys:role:admin")
    public Result<Void> save(@RequestBody AiOtaDTO dto) {
        UserDetail user = SecurityUser.getUser();
        Date now = new Date();
        
        // 生成ID
        if (StringUtils.isBlank(dto.getId())) {
            dto.setId(RandomStringUtils.randomAlphanumeric(32));
        }
        
        dto.setCreateDate(now);
        dto.setUpdateDate(now);
        dto.setCreator(user.getId());
        dto.setUpdater(user.getId());
        
        // 默认排序值
        if (dto.getSort() == null) {
            dto.setSort(0);
        }
        
        aiOtaService.save(dto);
        
        return new Result<>();
    }
    
    @PutMapping
    @Operation(summary = "修改固件")
    @RequiresPermissions("sys:role:admin")
    public Result<Void> update(@RequestBody AiOtaDTO dto) {
        UserDetail user = SecurityUser.getUser();
        dto.setUpdateDate(new Date());
        dto.setUpdater(user.getId());
        
        aiOtaService.update(dto);
        
        return new Result<>();
    }
    
    @DeleteMapping
    @Operation(summary = "删除固件")
    @RequiresPermissions("sys:role:admin")
    public Result<Void> delete(@RequestBody String[] ids) {
        aiOtaService.delete(ids);
        return new Result<>();
    }
    
    @GetMapping("latest")
    @Operation(summary = "获取最新版本固件")
    public Result<AiOtaEntity> getLatestVersion() {
        AiOtaEntity latestVersion = aiOtaService.getLatestVersion();
        return new Result<AiOtaEntity>().ok(latestVersion);
    }
} 