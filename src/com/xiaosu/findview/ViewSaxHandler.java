package com.xiaosu.findview;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.xiaosu.findview.model.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class ViewSaxHandler extends DefaultHandler {
    private String layoutPath = "";
    private Project project;
    private ArrayList<Element> elements;

    public static void main(String[] args) {
        String str = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "              android:orientation=\"vertical\"\n" +
                "              android:layout_width=\"fill_parent\"\n" +
                "              android:layout_height=\"fill_parent\">\n" +
                "    <TextView\n" +
                "            android:id=\"@id/hello_world\"\n" +
                "            android:layout_width=\"fill_parent\"\n" +
                "            android:layout_height=\"wrap_content\"\n" +
                "            android:text=\"Hello World, MyActivity\"/>\n" +
                "    <TextView\n" +
                "            android:id=\"@+id/hello_world_plus\"\n" +
                "            android:layout_width=\"fill_parent\"\n" +
                "            android:layout_height=\"wrap_content\"\n" +
                "            android:text=\"Hello World, MyActivity\"/>\n" +
                "</LinearLayout>\n" +
                "\n";

        ViewSaxHandler handler = new ViewSaxHandler();
        // InputStream stream = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        // try {
        //     handler.createViewList(stream);
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
        try {
            handler.createViewList(str);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createViewList(String string) throws ParserConfigurationException, SAXException, IOException {
        InputStream xmlStream = new ByteArrayInputStream(string.getBytes("UTF-8"));
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(xmlStream, this);
    }

    public void createViewList(InputStream xmlStream) throws Exception {

    }


    @Override
    public void startDocument() throws SAXException {
        elements = new ArrayList<>();
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("include")) {
            String includeLayout = attributes.getValue("layout");
            if (includeLayout != null) {
                File file = new File(getLayoutPath(), includeLayout.replace("@layout", "") + ".xml");
                if (file.exists()) {
                    VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
                    if (virtualFile == null) {
                        return;
                    }
                    PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(virtualFile);
                    try {
                        if (psiFile != null) {
                            this.createViewList(psiFile.getText());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            String id = attributes.getValue("android:id");
            attributes.getValue("class");
            if (id != null) {
                elements.add(new Element(qName, id));
//                Utils.getIDsFromLayout(include, elements);
                /*ViewPart viewPart = new ViewPart();
                viewPart.setType(qName);
                viewPart.setId(id.replace("@+id/", "").replace("@id/", ""));
                viewPartList.add(viewPart);*/
            }
        }

    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    }

    public String getLayoutPath() {
        return layoutPath;
    }

    public void setLayoutPath(String layoutPath) {
        this.layoutPath = layoutPath;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
