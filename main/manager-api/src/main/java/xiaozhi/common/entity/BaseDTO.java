package xiaozhi.common.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 基础DTO类，所有DTO都可以继承
 */
@Data
public abstract class BaseDTO implements Serializable {
    /**
     * id
     */
    private Long id;
    
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