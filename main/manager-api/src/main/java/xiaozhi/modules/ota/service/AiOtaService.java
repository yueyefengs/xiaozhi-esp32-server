package xiaozhi.modules.ota.service;

import xiaozhi.common.page.PageData;
import xiaozhi.common.service.CrudService;
import xiaozhi.modules.ota.dto.AiOtaDTO;
import xiaozhi.modules.ota.entity.AiOtaEntity;

/**
 * 固件服务接口
 */
public interface AiOtaService extends CrudService<AiOtaEntity, AiOtaDTO> {
    /**
     * 分页查询固件列表
     * @param params 查询参数
     * @return 分页数据
     */
    PageData<AiOtaDTO> page(AiOtaDTO params);
    
    /**
     * 获取最新版本的固件
     * @return 最新版本的固件信息
     */
    AiOtaEntity getLatestVersion();
} 