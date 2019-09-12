package jbm.framework.excel.handler;


import org.apache.poi.ss.usermodel.Sheet;

/**
 * @program: JBM
 * @author: wesley.zhang
 * @create: 2019-09-12 22:31
 **/
public interface SheetHandler {


    void SheetProcess(Integer sheetAt, Sheet sheet);
}
