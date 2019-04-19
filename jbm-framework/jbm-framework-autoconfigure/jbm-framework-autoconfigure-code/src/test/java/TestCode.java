import jbm.framework.code.GenerateMasterData;
import junit.framework.TestCase;

public class TestCode extends TestCase {

    public void testGen() {
        GenerateMasterData generateMasterData = new GenerateMasterData(GenerateMasterData.class);
        generateMasterData.generateAll();
    }

}
