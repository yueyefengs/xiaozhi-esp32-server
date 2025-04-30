package xiaozhi.modules.ota.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.page.PageData;
import xiaozhi.common.service.impl.CrudServiceImpl;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.modules.ota.dao.OtaAiOtaDao;
import xiaozhi.modules.ota.dto.AiOtaDTO;
import xiaozhi.modules.ota.entity.AiOtaEntity;
import xiaozhi.modules.ota.service.AiOtaService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 固件服务实现类
 */
@Service
public class AiOtaServiceImpl extends CrudServiceImpl<OtaAiOtaDao, AiOtaEntity, AiOtaDTO> implements AiOtaService {

    @Override
    public PageData<AiOtaDTO> page(AiOtaDTO params) {
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put(Constant.PAGE, params.getPage());
        queryParams.put(Constant.LIMIT, params.getLimit());
        
        // 构建查询条件
        QueryWrapper<AiOtaEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_date");
        
        // 添加名称和版本号搜索条件
        if (params != null) {
            if (StringUtils.isNotBlank(params.getFirmwareName())) {
                queryWrapper.like("firmware_name", params.getFirmwareName());
            }
            if (StringUtils.isNotBlank(params.getVersion())) {
                queryWrapper.like("version", params.getVersion());
            }
            if (StringUtils.isNotBlank(params.getType())) {
                queryWrapper.eq("type", params.getType());
            }
        }
        
        // 执行分页查询
        IPage<AiOtaEntity> page = baseDao.selectPage(getPage(queryParams, "sort", false), queryWrapper);
        
        // 转换结果
        List<AiOtaDTO> dtoList = ConvertUtils.sourceToTarget(page.getRecords(), AiOtaDTO.class);
        
        return new PageData<>(dtoList, page.getTotal());
    }

    @Override
    public AiOtaEntity getLatestVersion() {
        QueryWrapper<AiOtaEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_date");
        queryWrapper.last("LIMIT 1");
        return baseDao.selectOne(queryWrapper);
    }
    
    @Override
    public QueryWrapper<AiOtaEntity> getWrapper(Map<String, Object> params) {
        QueryWrapper<AiOtaEntity> wrapper = new QueryWrapper<>();
        
        // 添加默认排序
        wrapper.orderByDesc("create_date");
        
        // 根据参数添加查询条件
        String firmwareName = (String) params.get("firmwareName");
        if (StringUtils.isNotBlank(firmwareName)) {
            wrapper.like("firmware_name", firmwareName);
        }
        
        String version = (String) params.get("version");
        if (StringUtils.isNotBlank(version)) {
            wrapper.like("version", version);
        }
        
        String type = (String) params.get("type");
        if (StringUtils.isNotBlank(type)) {
            wrapper.eq("type", type);
        }
        
        return wrapper;
    }
} 