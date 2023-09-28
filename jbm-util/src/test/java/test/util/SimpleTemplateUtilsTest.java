package test.util;

import com.github.pfmiles.minvelocity.TemplateUtil;
import com.github.pfmiles.org.apache.velocity.Template;
import com.github.pfmiles.org.apache.velocity.VelocityContext;
import com.github.pfmiles.org.apache.velocity.context.Context;
import com.jbm.util.MapUtils;
import com.jbm.util.SimpleTemplateUtils;
import junit.framework.TestCase;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleTemplateUtilsTest extends TestCase {

    public void testFileTest() {
        System.out.println(SimpleTemplateUtils.render("temps/text.tmp", MapUtils.newParamMap("name", "wesley")));
    }

    public void testFileTest2() {
        Template template= SimpleTemplateUtils.getTemplate("temps/text.tmp");
        System.out.println( TemplateUtil.renderTemplate(template, MapUtils.newParamMap("name", "wesley2")));
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
