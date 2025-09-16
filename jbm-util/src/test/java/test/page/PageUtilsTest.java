package test.page;

import cn.hutool.core.lang.Console;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import com.jbm.util.PageUtils;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PageUtilsTest extends TestCase {


    public void testPage() {
        Page startPage = new Page(1, 10);
        List<String > all = new ArrayList<>();
        AtomicInteger count = new AtomicInteger();
        PageUtils.loopPage(all, (page) -> {
            PageResult<String> pageResult = new PageResult<>();
            pageResult.setTotal(100);
            pageResult.add("1");
            count.getAndIncrement();
            Console.log("调用{}次",count);
            return pageResult;
        }, startPage);
        Console.log(all);
    }
}
