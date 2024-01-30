package com.jbm.util.db.load;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import com.jbm.util.db.sqltemplate.Configuration;
import com.jbm.util.db.sqltemplate.SqlMeta;
import com.jbm.util.db.sqltemplate.SqlTemplate;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.*;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;

/**
 * @author wesley
 */
@Slf4j
public class XmlLoader extends AbstractFileLoader {
    private final Configuration configuration = new Configuration();

    public XmlLoader(String sqlPath) {
        super(sqlPath);
    }

    @Override
    public String loadFile(String sqlName) {
        List<String> ss = StrUtil.split(sqlName, ".");
        String xmlFileName = ss.get(0);
        String fileName = buildFileName(xmlFileName, getExtension());
        return ResourceUtil.readUtf8Str(fileName);
    }


    @Override
    public SqlMeta renderSql(String sqlName, String fileContent, Object... params) {
        List<String> ss = StrUtil.split(sqlName, ".");
        String sqlMethod = ss.get(1);
        Document doc = XmlUtil.readXML(fileContent);
        String inSql = this.getSqlById(doc, sqlMethod);
        SqlTemplate template = configuration
                .getTemplate(inSql);
        if (params.length == 1) {
            return template.process(params[0]);
        }
        return template.process(params);
    }

    @Override
    public String getExtension() {
        return ".xml";
    }

    @Override
    public boolean canRead(String sqlName) {
        return StrUtil.contains(sqlName, ".");
    }

    public String getSqlById(Document doc, String targetId) {
        doc.getDocumentElement().normalize();
        // 遍历XML文档寻找id属性匹配的目标元素
        // 获取所有元素
        NodeList nodeList = doc.getElementsByTagName("*");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            try {
                if (node.getNodeType() == Node.ELEMENT_NODE && ((Element) node).getAttribute("id").equals(targetId)) {
                    Element selectElement = (Element) node;

                    // 创建一个新的Document，并将select元素及其子节点复制到新的文档中，这样就不会包含原始XML声明
                    DocumentFragment fragment = doc.createDocumentFragment();
                    NodeList children = selectElement.getChildNodes();
                    for (int j = 0; j < children.getLength(); j++) {
                        Node childNode = children.item(j);
                        fragment.appendChild(doc.importNode(childNode, true));
                    }

                    // 使用Transformer转换fragment为字符串
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    StringWriter writer = new StringWriter();
                    // 不输出XML声明
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    transformer.transform(new DOMSource(fragment), new StreamResult(writer));
                    return writer.getBuffer().toString();
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                return null;
            }
        }
        return null;
    }
}
