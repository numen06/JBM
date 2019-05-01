package jbm.framework.code.test;

import jbm.framework.code.GenerateMasterData;
import jbm.framework.code.test.entity.TestAutoEntity;
import jbm.framework.code.test.entity.TestAutoTreeEntity;
import junit.framework.TestCase;

public class TestCode extends TestCase {

    public void testGen() {
        GenerateMasterData generateMasterData = new GenerateMasterData(TestAutoEntity.class);
        generateMasterData.generateAll();
    }

    public void testGen2() {
        GenerateMasterData generateMasterData = new GenerateMasterData(TestAutoTreeEntity.class);
        generateMasterData.generateAll();
    }


}
