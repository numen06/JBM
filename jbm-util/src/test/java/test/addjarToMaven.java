package test;

import com.jbm.util.StringUtils;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;


public class addjarToMaven {

    //批量安装jar所在目录
//  public static String filePath = "D:/develop/apache-tomcat-8.0.9/apache-tomcat-8.0.9/webapps/activiti-rest/WEB-INF/lib";
    public static String filePath = "D:/libs";

    public static void main(String[] args) {
        LinkedList<File> linkedList = new LinkedList<>();
        File f = new File(filePath);
        File[] file = f.listFiles();
        for (int i = 0; i < file.length; i++) {
            linkedList.add(file[i]);
        }
        printFile(linkedList);
        printFiles(linkedList);
    }

    /**
     * 功能：批量安装maven的本地的jar
     *
     * @param args
     */
    public static void printFile(LinkedList<File> linkedList) {
        String sb = "C:\\apache-maven-3.5.4\\bin\\mvn install:install-file -Dfile=" + filePath + "/AA.jar -DgroupId=com.tdenergys -DartifactId=CCC -Dversion=1.0.0 -Dpackaging=jar";
        for (Iterator<File> iterator = linkedList.iterator(); iterator.hasNext(); ) {
            File file = iterator.next();
            if (file.isFile()) {
                String fileName = file.getName();
                String ver = StringUtils.remove(StringUtils.right(fileName, 18), ".jar");
                String packinfo = StringUtils.remove(fileName, "-" + ver + ".jar");
                String temp = StringUtils.replace(sb, "AA", fileName.substring(0, fileName.lastIndexOf(".")));
                temp = StringUtils.replace(temp, "1.0.0", ver);
                temp = StringUtils.replace(temp, "CCC", packinfo);
                System.out.println(temp);
                linkedList.remove(file);
                printFile(linkedList);
            } else {
                break;
            }
        }
    }


    /**
     * 功能：批量引入maven的pom
     *
     * @param args
     */
    public static void printFiles(LinkedList<File> linkedList) {
        String sb = "<dependency><groupId>com.tdenergys</groupId><artifactId>BBB</artifactId><version>1.0.0</version><type>jar</type></dependency>";
        for (Iterator<File> iterator = linkedList.iterator(); iterator.hasNext(); ) {
            File file = iterator.next();
            if (file.isFile()) {
                String fileName = file.getName();
                String ver = StringUtils.remove(StringUtils.right(fileName, 18), ".jar");
                String packinfo = StringUtils.remove(fileName, "-" + ver + ".jar");
                String temp = sb.replace("BBB", packinfo);
                temp = temp.replace("1.0.0", ver);
                System.out.println(temp);
                linkedList.remove(file);
                printFiles(linkedList);
            } else {
                break;
            }
        }
    }
}
