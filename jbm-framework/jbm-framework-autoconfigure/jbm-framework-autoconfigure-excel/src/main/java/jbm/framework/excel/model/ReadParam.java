package jbm.framework.excel.model;

import lombok.Data;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-12 22:47
 **/
@Data
public class ReadParam {


    private int offsetLine = 0;
    private int limitLine = Integer.MAX_VALUE;
    private int sheetIndex = 0;


    public ReadParam() {
    }

    public ReadParam(int offsetLine, int limitLine, int sheetIndex) {
        this.offsetLine = offsetLine;
        this.limitLine = limitLine;
        this.sheetIndex = sheetIndex;
    }

}
