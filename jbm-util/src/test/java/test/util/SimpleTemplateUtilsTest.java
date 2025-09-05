package test.util;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import com.github.pfmiles.minvelocity.TemplateUtil;
import com.github.pfmiles.org.apache.velocity.Template;
import com.jbm.util.MapUtils;
import com.jbm.util.SimpleTemplateUtils;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

public class SimpleTemplateUtilsTest extends TestCase {



    public void testFileTest() {
        System.out.println(SimpleTemplateUtils.render("temps/text.tmp", MapUtils.newParamMap("name", "wesley")));
    }

    public void testFileTest2() throws IOException {
//        String template = "temps/text-date.tmp";
        // 将格式化后的字符串放入上下文
        Map<String, Object> context = new HashMap<>();
        // 注入工具类实例
        context.put("date", new Object(){
            public String format(String format, Date date) {
                return DateUtil.format(date, format);
            }
        });
        // 注入到上下文
        context.put("currentDate", DateTime.now());
//        System.out.println(SimpleTemplateUtils.render(template, context));
        String templateString = "$currentDate.toString('yyyy-MM-dd HH:mm:ss')";
        System.out.println(SimpleTemplateUtils.renderStringTemplate(templateString, context));
    }

    public void testRenderStringTemp() {
        String templateString = "#foreach($i in ${list})\n$i\n#end";
        Map<String, Object> ctxPojo = new HashMap<String, Object>();
        List<String> list = new ArrayList<String>();
        list.add("one");
        list.add("two");
        list.add("three");
        ctxPojo.put("list", list);
        StringWriter out = new StringWriter();
        TemplateUtil.renderString(templateString, ctxPojo, out);
        System.out.println(out.toString());
        assertTrue("one\ntwo\nthree\n".equals(out.toString()));
    }

    public void testRenderTemplate() {
        Template temp = TemplateUtil.parseStringTemplate("#foreach($i in $list)\n$i\n#end");
        Map<String, Object> ctxPojo = new HashMap<String, Object>();
        List<String> list = new ArrayList<String>();
        list.add("one");
        list.add("two");
        list.add("three");
        ctxPojo.put("list", list);
        StringWriter out = new StringWriter();
        TemplateUtil.renderTemplate(temp, ctxPojo, out);
        System.out.println(out.toString());
        assertTrue("one\ntwo\nthree\n".equals(out.toString()));
    }

    public void testRefRendering() {
        Template temp = TemplateUtil.parseStringTemplate("hello ${code.split(\"[,]\")[1]} world");
        Map<String, Object> ctxPojo = new HashMap<String, Object>();
        StringReader stream = new StringReader("1234567890");
        ctxPojo.put("code", "1,2,3");
        StringWriter writer = new StringWriter();
        TemplateUtil.renderTemplate(temp, ctxPojo, writer);
        System.out.println(writer.toString());
        // assertTrue("hello 1234567890 world".equals(writer.toString()));
    }
}
