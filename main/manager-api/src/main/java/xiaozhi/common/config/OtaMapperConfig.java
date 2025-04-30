package xiaozhi.common.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Properties;

/**
 * 解决OTA模块DAO冲突的配置
 * 
 * 注意：此配置已不再需要，已通过重命名DAO接口解决冲突
 * @deprecated 保留此类仅作为记录，实际上可以删除
 */
@Configuration
public class OtaMapperConfig {

    /**
     * 配置特定的扫描器，只扫描OTA模块的Mapper接口
     */
    @Bean
    @Primary
    public MapperScannerConfigurer otaMapperScannerConfigurer() {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        // 只扫描新的OTA模块的包路径
        mapperScannerConfigurer.setBasePackage("xiaozhi.modules.ota.dao");
        mapperScannerConfigurer.setAnnotationClass(Mapper.class);
        return mapperScannerConfigurer;
    }

    /**
     * 配置除OTA模块外的设备模块Mapper接口扫描器
     */
    @Bean
    public MapperScannerConfigurer deviceMapperScannerConfigurer() {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        // 排除冲突的包，但包含其他所有包
        mapperScannerConfigurer.setBasePackage("xiaozhi.modules.device.dao,xiaozhi.modules.agent.dao,xiaozhi.modules.config.dao,xiaozhi.modules.model.dao,xiaozhi.modules.security.dao,xiaozhi.modules.sys.dao,xiaozhi.modules.timbre.dao");
        mapperScannerConfigurer.setAnnotationClass(Mapper.class);
        return mapperScannerConfigurer;
    }
} 