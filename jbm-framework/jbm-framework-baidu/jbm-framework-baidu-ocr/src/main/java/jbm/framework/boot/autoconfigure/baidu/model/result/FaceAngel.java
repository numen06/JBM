package jbm.framework.boot.autoconfigure.baidu.model.result;

import lombok.Data;

/**
 * 人脸在图片中的位置
 */
@Data
public class FaceAngel {

    /**
     * 	三维旋转之左右旋转角[-90(左), 90(右)]
     */
    private Double yaw;
    /**
     * 三维旋转之俯仰角度[-90(上), 90(下)]
     */
    private Double pitch;
    /**
     * 平面内旋转角[-180(逆时针), 180(顺时针)]
     */
    private Double roll;
    private Double height;
    private Double rotation;
}
