package jbm.framework.boot.autoconfigure.baidu.model.result;

import lombok.Data;

/**
 * 人脸信息
 */
@Data
public class FaceInfo {

    /**
     * 人脸图片的唯一标识
     */
    private String faceToken;

    /**
     * 人脸置信度，范围【0~1】，代表这是一张人脸的概率，0最小、1最大。
     */
    private Double faceProbability;

    /**
     * 人脸坐标信息
     */
    private FaceLocation location;

    /**
     * 人脸三维信息
     */
    private FaceAngel angel;

    /**
     * 年龄 ，当face_field包含age时返回
     */
    private Double age;


    private Long beauty;

}
