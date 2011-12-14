package cubetech.iqm;

import cubetech.gfx.ResourceManager;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author mads
 */
public class ShapeKeyLoader {
    public static ShapeKeyCollection loadSkapeKey(String file, boolean verbose) throws Exception {
        BufferedInputStream bis = ResourceManager.OpenFileAsInputStream(file);
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bis);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("object");
        if(verbose) System.out.println("ShapeKey loading from file: " + file);

        ArrayList<ShapeKeyObject> objects = new ArrayList<ShapeKeyObject>();
        // Objects
        for (int i= 0; i < nodeList.getLength(); i++) {
            Node fstNode = nodeList.item(i);
            String objectName = fstNode.getNodeName();
            if(verbose) System.out.println(String.format("Object \"%s\" found", objectName));
            if(fstNode.getNodeType() != Node.ELEMENT_NODE) continue;
            Element fstElem = (Element)fstNode;
            ShapeKeyObject object = new ShapeKeyObject(objectName);
            objects.add(object);
            // Shapekeys container
            NodeList keyElem = fstElem.getElementsByTagName("shape_keys");
            for (int j= 0; j < keyElem.getLength(); j++) {
                Node keyNode = keyElem.item(j);
                if(keyNode.getNodeType() != Node.ELEMENT_NODE) break;

                Element keyCont = (Element)keyNode;
                NodeList keyList = keyCont.getElementsByTagName("key");
                // for each shapekey
                for (int k= 0; k < keyList.getLength(); k++) {
                    Node keykeyNode = keyList.item(k);
                    String shapeName = keykeyNode.getAttributes().getNamedItem("name").getNodeValue();
                    // Containing a list of verts
                    if(verbose) System.out.println(String.format("[%s] Shapekey \"%s\" found", objectName, shapeName));
                    ShapeKey key = new ShapeKey(shapeName);
                    object.addShapeKey(key);
                    Element keykeyElem = (Element)keykeyNode;
                    NodeList vertexListNode = keykeyElem.getElementsByTagName("vertex");
                    key.vertices = new float[vertexListNode.getLength()*3];
                    key.indices = new int[vertexListNode.getLength()];
                    for (int l= 0; l < vertexListNode.getLength(); l++) {
                        Node vertexNode = vertexListNode.item(l);
                        // Read vertex attributes
                        String id = vertexNode.getAttributes().getNamedItem("id").getNodeValue();
                        String x = vertexNode.getAttributes().getNamedItem("x").getNodeValue();
                        String y = vertexNode.getAttributes().getNamedItem("y").getNodeValue();
                        String z = vertexNode.getAttributes().getNamedItem("z").getNodeValue();

                        int intId = Integer.parseInt(id);
                        float fx = Float.parseFloat(x);
                        float fy = Float.parseFloat(y);
                        float fz = Float.parseFloat(z);

                        key.indices[l] = intId;
                        key.vertices[l * 3 + 0] = fx;
                        key.vertices[l * 3 + 1] = fy;
                        key.vertices[l * 3 + 2] = fz;
                    }
                }
            }
            
        }
        return new ShapeKeyCollection(objects);
        
    }
}
