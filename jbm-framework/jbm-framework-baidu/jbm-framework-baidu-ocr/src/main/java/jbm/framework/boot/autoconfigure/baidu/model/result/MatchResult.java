package jbm.framework.boot.autoconfigure.baidu.model.result;


import lombok.Data;

import java.util.List;

@Data
public class MatchResult {

    private Double score;

    private List<FaceToken> face_list;
}
