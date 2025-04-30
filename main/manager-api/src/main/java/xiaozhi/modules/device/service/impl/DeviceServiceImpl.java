package xiaozhi.modules.device.service.impl;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.hutool.core.util.RandomUtil;
import xiaozhi.common.constant.Constant;
import xiaozhi.common.exception.RenException;
import xiaozhi.common.page.PageData;
import xiaozhi.common.service.impl.BaseServiceImpl;
import xiaozhi.common.user.UserDetail;
import xiaozhi.common.utils.ConvertUtils;
import xiaozhi.common.utils.DateUtils;
import xiaozhi.modules.device.dao.DeviceDao;
import xiaozhi.modules.device.dto.DevicePageUserDTO;
import xiaozhi.modules.device.dto.DeviceReportReqDTO;
import xiaozhi.modules.device.dto.DeviceReportRespDTO;
import xiaozhi.modules.device.entity.DeviceEntity;
import xiaozhi.modules.device.service.DeviceService;
import xiaozhi.modules.device.vo.UserShowDeviceListVO;
import xiaozhi.modules.security.user.SecurityUser;
import xiaozhi.modules.sys.service.SysParamsService;
import xiaozhi.modules.sys.service.SysUserUtilService;
import xiaozhi.modules.ota.entity.AiOtaEntity;
import xiaozhi.modules.ota.service.AiOtaService;

@Service
public class DeviceServiceImpl extends BaseServiceImpl<DeviceDao, DeviceEntity> implements DeviceService {

    private final DeviceDao deviceDao;

    private final SysUserUtilService sysUserUtilService;

    private final String frontedUrl;

    private final RedisTemplate<String, Object> redisTemplate;
    
    private final AiOtaService aiOtaService;

    // 添加构造函数来初始化 deviceMapper
    public DeviceServiceImpl(DeviceDao deviceDao, SysUserUtilService sysUserUtilService,
            SysParamsService sysParamsService,
            RedisTemplate<String, Object> redisTemplate,
            AiOtaService aiOtaService) {
        this.deviceDao = deviceDao;
        this.sysUserUtilService = sysUserUtilService;
        this.frontedUrl = sysParamsService.getValue(Constant.SERVER_FRONTED_URL, true);
        this.redisTemplate = redisTemplate;
        this.aiOtaService = aiOtaService;
    }

    @Override
    public DeviceEntity getDeviceById(String deviceId) {
        LambdaQueryWrapper<DeviceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceEntity::getId, deviceId);
        return deviceDao.selectOne(queryWrapper);
    }

    @Override
    public Boolean deviceActivation(String agentId, String activationCode) {
        if (StringUtils.isBlank(activationCode)) {
            throw new RenException("激活码不能为空");
        }
        String deviceKey = "ota:activation:code:" + activationCode;
        Object cacheDeviceId = redisTemplate.opsForValue().get(deviceKey);
        if (cacheDeviceId == null) {
            throw new RenException("激活码错误");
        }
        String deviceId = (String) cacheDeviceId;
        String safeDeviceId = deviceId.replace(":", "_").toLowerCase();
        String cacheDeviceKey = String.format("ota:activation:data:%s", safeDeviceId);
        Map<Object, Object> cacheMap = redisTemplate.opsForHash().entries(cacheDeviceKey);
        if (cacheMap == null) {
            throw new RenException("激活码错误");
        }
        String cachedCode = (String) cacheMap.get("activation_code");
        if (!activationCode.equals(cachedCode)) {
            throw new RenException("激活码错误");
        }
        // 检查设备有没有被激活
        if (selectById(deviceId) != null) {
            throw new RenException("设备已激活");
        }

        String macAddress = (String) cacheMap.get("mac_address");
        String board = (String) cacheMap.get("board");
        String appVersion = (String) cacheMap.get("app_version");
        UserDetail user = SecurityUser.getUser();
        if (user.getId() == null) {
            throw new RenException("用户未登录");
        }

        Date currentTime = new Date();
        DeviceEntity deviceEntity = new DeviceEntity();
        deviceEntity.setId(deviceId);
        deviceEntity.setBoard(board);
        deviceEntity.setAgentId(agentId);
        deviceEntity.setAppVersion(appVersion);
        deviceEntity.setMacAddress(macAddress);
        deviceEntity.setUserId(user.getId());
        deviceEntity.setCreator(user.getId());
        deviceEntity.setCreateDate(currentTime);
        deviceEntity.setUpdater(user.getId());
        deviceEntity.setUpdateDate(currentTime);
        deviceEntity.setLastConnectedAt(currentTime);
        deviceDao.insert(deviceEntity);

        // 清理redis缓存
        redisTemplate.delete(cacheDeviceKey);
        redisTemplate.delete(deviceKey);
        return true;
    }

    @Override
    public DeviceReportRespDTO checkDeviceActive(String macAddress, String clientId,
            DeviceReportReqDTO deviceReport) {
        DeviceReportRespDTO response = new DeviceReportRespDTO();
        response.setServer_time(buildServerTime());

        DeviceEntity deviceById = getDeviceById(macAddress);
        if (deviceById != null) { // 如果设备存在，则更新上次连接时间
            deviceById.setLastConnectedAt(new Date());
            deviceDao.updateById(deviceById);
            
            // 只有设备开启自动更新且上报版本信息不为空时才检查升级
            if (deviceById.getAutoUpdate() != null && deviceById.getAutoUpdate() == 1 
                    && deviceReport.getApplication() != null 
                    && StringUtils.isNotBlank(deviceReport.getApplication().getVersion())) {
                
                // 获取最新版本固件
                AiOtaEntity latestOta = aiOtaService.getLatestVersion();
                
                // 如果有最新版本固件且版本号不同，则返回固件更新信息
                if (latestOta != null && StringUtils.isNotBlank(latestOta.getVersion()) 
                        && StringUtils.isNotBlank(latestOta.getFirmwarePath())
                        && !latestOta.getVersion().equals(deviceReport.getApplication().getVersion())) {
                    
                    DeviceReportRespDTO.Firmware firmware = new DeviceReportRespDTO.Firmware();
                    firmware.setVersion(latestOta.getVersion());
                    firmware.setUrl(latestOta.getFirmwarePath());
                    response.setFirmware(firmware);
                }
            }
        } else { // 如果设备不存在，则生成激活码
            DeviceReportRespDTO.Activation code = buildActivation(macAddress, deviceReport);
            response.setActivation(code);
        }

        return response;
    }

    @Override
    public List<DeviceEntity> getUserDevices(Long userId, String agentId) {
        QueryWrapper<DeviceEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("agent_id", agentId);
        return baseDao.selectList(wrapper);
    }

    @Override
    public void unbindDevice(Long userId, String deviceId) {
        UpdateWrapper<DeviceEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("id", deviceId);
        baseDao.delete(wrapper);
    }

    @Override
    public void deleteByUserId(Long userId) {
        UpdateWrapper<DeviceEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId);
        baseDao.delete(wrapper);
    }

    @Override
    public Long selectCountByUserId(Long userId) {
        UpdateWrapper<DeviceEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId);
        return baseDao.selectCount(wrapper);
    }

    @Override
    public void deleteByAgentId(String agentId) {
        UpdateWrapper<DeviceEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("agent_id", agentId);
        baseDao.delete(wrapper);
    }

    @Override
    public PageData<UserShowDeviceListVO> page(DevicePageUserDTO dto) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Constant.PAGE, dto.getPage());
        params.put(Constant.LIMIT, dto.getLimit());
        IPage<DeviceEntity> page = baseDao.selectPage(
                getPage(params, "mac_address", true),
                // 定义查询条件
                new QueryWrapper<DeviceEntity>()
                        // 必须设备关键词查找
                        .like(StringUtils.isNotBlank(dto.getKeywords()), "alias", dto.getKeywords()));
        // 循环处理page获取回来的数据，返回需要的字段
        List<UserShowDeviceListVO> list = page.getRecords().stream().map(device -> {
            UserShowDeviceListVO vo = ConvertUtils.sourceToTarget(device, UserShowDeviceListVO.class);
            // 把最后修改的时间，改为简短描述的时间
            vo.setRecentChatTime(DateUtils.getShortTime(device.getUpdateDate()));
            sysUserUtilService.assignUsername(device.getUserId(),
                    vo::setBindUserName);
            vo.setDeviceType(device.getBoard());
            return vo;
        }).toList();
        // 计算页数
        return new PageData<>(list, page.getTotal());
    }

    @Override
    public DeviceEntity getDeviceByMacAddress(String macAddress) {
        if (StringUtils.isBlank(macAddress)) {
            return null;
        }
        QueryWrapper<DeviceEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("mac_address", macAddress);
        return baseDao.selectOne(wrapper);
    }

    private DeviceReportRespDTO.ServerTime buildServerTime() {
        DeviceReportRespDTO.ServerTime serverTime = new DeviceReportRespDTO.ServerTime();
        TimeZone tz = TimeZone.getDefault();
        serverTime.setTimestamp(Instant.now().toEpochMilli());
        serverTime.setTimeZone(tz.getID());
        serverTime.setTimezone_offset(tz.getOffset(System.currentTimeMillis()) / (60 * 1000));
        return serverTime;
    }

    @Override
    public String geCodeByDeviceId(String deviceId) {
        String dataKey = getDeviceCacheKey(deviceId);

        Map<Object, Object> cacheMap = redisTemplate.opsForHash().entries(dataKey);
        if (cacheMap != null && cacheMap.containsKey("activation_code")) {
            String cachedCode = (String) cacheMap.get("activation_code");
            return cachedCode;
        }
        return null;
    }

    private String getDeviceCacheKey(String deviceId) {
        String safeDeviceId = deviceId.replace(":", "_").toLowerCase();
        String dataKey = String.format("ota:activation:data:%s", safeDeviceId);
        return dataKey;
    }

    public DeviceReportRespDTO.Activation buildActivation(String deviceId, DeviceReportReqDTO deviceReport) {
        DeviceReportRespDTO.Activation code = new DeviceReportRespDTO.Activation();

        String cachedCode = geCodeByDeviceId(deviceId);

        if (StringUtils.isNotBlank(cachedCode)) {
            code.setCode(cachedCode);
            code.setMessage(frontedUrl + "\n" + cachedCode);
            code.setChallenge(deviceId);
        } else {
            String newCode = RandomUtil.randomNumbers(6);
            code.setCode(newCode);
            code.setMessage(frontedUrl + "\n" + newCode);
            code.setChallenge(deviceId);

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("id", deviceId);
            dataMap.put("mac_address", deviceId);

            dataMap.put("board", (deviceReport.getBoard() != null && deviceReport.getBoard().getType() != null)
                    ? deviceReport.getBoard().getType()
                    : (deviceReport.getChipModelName() != null ? deviceReport.getChipModelName() : "unknown"));
            dataMap.put("app_version", (deviceReport.getApplication() != null)
                    ? deviceReport.getApplication().getVersion()
                    : null);

            dataMap.put("deviceId", deviceId);
            dataMap.put("activation_code", newCode);

            // 写入主数据 key
            String dataKey = getDeviceCacheKey(deviceId);
            redisTemplate.opsForHash().putAll(dataKey, dataMap);
            redisTemplate.expire(dataKey, 24, TimeUnit.HOURS);

            // 写入反查激活码 key
            String codeKey = "ota:activation:code:" + newCode;
            redisTemplate.opsForValue().set(codeKey, deviceId, 24, TimeUnit.HOURS);
        }
        return code;
    }
}