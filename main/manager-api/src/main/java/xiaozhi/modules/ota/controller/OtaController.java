package xiaozhi.modules.ota.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import xiaozhi.common.page.PageData;
import xiaozhi.common.utils.Result;
import xiaozhi.common.validator.ValidatorUtils;
import xiaozhi.common.validator.group.AddGroup;
import xiaozhi.common.validator.group.DefaultGroup;
import xiaozhi.common.validator.group.UpdateGroup;
import xiaozhi.modules.ota.dto.AiOtaDTO;
import xiaozhi.modules.ota.entity.AiOtaEntity;
import xiaozhi.modules.ota.service.AiOtaService;

import java.util.Map;

/**
 * OTA管理控制器
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ota")
@Tag(name = "OTA管理", description = "固件OTA接口")
public class OtaController {
    
    private final AiOtaService aiOtaService;

    @GetMapping("page")
    @Operation(summary = "固件分页列表")
    public Result<PageData<AiOtaDTO>> page(AiOtaDTO params) {
        PageData<AiOtaDTO> page = aiOtaService.page(params);
        return new Result<PageData<AiOtaDTO>>().ok(page);
    }
    
    @GetMapping("{id}")
    @Operation(summary = "获取固件详情")
    public Result<AiOtaDTO> get(@PathVariable("id") String id) {
        AiOtaDTO data = aiOtaService.get(id);
        return new Result<AiOtaDTO>().ok(data);
    }
    
    @PostMapping
    @Operation(summary = "保存固件")
    public Result save(@RequestBody AiOtaDTO dto) {
        ValidatorUtils.validateEntity(dto, AddGroup.class, DefaultGroup.class);
        aiOtaService.save(dto);
        return new Result();
    }
    
    @PutMapping
    @Operation(summary = "修改固件")
    public Result update(@RequestBody AiOtaDTO dto) {
        ValidatorUtils.validateEntity(dto, UpdateGroup.class, DefaultGroup.class);
        aiOtaService.update(dto);
        return new Result();
    }
    
    @DeleteMapping
    @Operation(summary = "删除固件")
    public Result delete(@RequestBody String[] ids) {
        aiOtaService.delete(ids);
        return new Result();
    }
    
    @GetMapping("latest")
    @Operation(summary = "获取最新版本固件")
    public Result<AiOtaEntity> getLatestVersion() {
        AiOtaEntity latest = aiOtaService.getLatestVersion();
        return new Result<AiOtaEntity>().ok(latest);
    }
} 