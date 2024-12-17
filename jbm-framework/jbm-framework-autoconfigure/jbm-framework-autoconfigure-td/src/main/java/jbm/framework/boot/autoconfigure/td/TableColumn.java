package jbm.framework.boot.autoconfigure.td;

import lombok.Data;

@Data
public class TableColumn {

    private String field;
    private String type;
    private Integer length;
    private String note;
    private String encode;
    private String compress;
    private String level;
}
