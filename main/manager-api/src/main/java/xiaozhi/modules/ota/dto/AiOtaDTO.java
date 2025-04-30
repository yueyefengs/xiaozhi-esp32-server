package xiaozhi.modules.ota.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 固件DTO
 */
@Data
@Schema(description = "固件信息")
public class AiOtaDTO {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "固件名称")
    private String firmwareName;

    @Schema(description = "固件类型")
    private String type;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "大小(字节)")
    private Long size;

    @Schema(description = "备注/说明")
    private String remark;

    @Schema(description = "固件路径/下载链接")
    private String firmwarePath;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建者")
    private Long creator;

    @Schema(description = "创建时间")
    private Date createDate;

    @Schema(description = "更新者")
    private Long updater;

    @Schema(description = "更新时间")
    private Date updateDate;
    
    @Schema(description = "当前页码")
    private Integer page;
    
    @Schema(description = "每页条数")
    private Integer limit;
} 