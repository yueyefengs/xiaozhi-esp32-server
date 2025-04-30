package xiaozhi.modules.ota.dao;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import xiaozhi.common.dao.BaseDao;
import xiaozhi.modules.ota.entity.AiOtaEntity;

/**
 * 固件DAO接口 - 新的OTA模块实现
 */
@Mapper
public interface OtaAiOtaDao extends BaseDao<AiOtaEntity> {
} 