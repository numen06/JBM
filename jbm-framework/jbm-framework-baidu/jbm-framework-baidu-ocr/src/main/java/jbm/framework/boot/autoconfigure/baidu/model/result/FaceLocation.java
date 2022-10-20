package jbm.framework.boot.autoconfigure.baidu.model.result;

import lombok.Data;

/**
 * 人脸在图片中的位置
 */
@Data
public class FaceLocation {

    private Double left;
    private Double top;
    private Double width;
    private Double height;
    private Double rotation;
}
