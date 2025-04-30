package xiaozhi.modules.ota.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/**
 * 固件实体类
 */
@Data
@TableName("ai_ota")
public class AiOtaEntity {
    /**
     * ID
     */
    @TableId
    private String id;

    /**
     * 固件名称
     */
    private String firmwareName;

    /**
     * 固件类型
     */
    private String type;

    /**
     * 版本号
     */
    private String version;

    /**
     * 大小(字节)
     */
    private Long size;

    /**
     * 备注/说明
     */
    private String remark;

    /**
     * 固件路径
     */
    private String firmwarePath;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 创建者
     */
    private Long creator;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 更新者
     */
    private Long updater;

    /**
     * 更新时间
     */
    private Date updateDate;
} 