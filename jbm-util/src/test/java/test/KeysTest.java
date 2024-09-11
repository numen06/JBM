package test;

import cn.hutool.core.lang.Console;
import com.jbm.util.key.KeyArray;
import com.jbm.util.key.Keys;
import org.junit.jupiter.api.Test;

public class KeysTest {

    @Test
    public void test() {
        KeyArray keyArray = Keys.ofArray("1", "2", "3");
        Console.log("key1:{}", keyArray.toString());
        KeyArray keyArray2 = Keys.ofArray("1", "2", "3");
        Console.log("key2:{}", keyArray2.toString());
        String keyString = keyArray2.toString();
        Console.log("keyStr:{}", keyString);
        KeyArray keyArray3 = Keys.fromArray(keyString);
        Console.log("key3:{}", keyArray3.toString());
        System.out.println(keyArray3.equals(keyArray));
    }
}
