package jbm.framework.boot.autoconfigure.baidu.model.result;

import lombok.Data;

import java.util.List;

/**
 * 检测人脸结果
 */
@Data
public class DetectResult {

    /**
     * 人脸的数量
     */
    private Integer faceNum;
    /**
     * 人脸信息列表
     */
    private List<FaceInfo> faceList;

}
