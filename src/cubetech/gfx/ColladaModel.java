//package cubetech.gfx;
//
//import cubetech.misc.Ref;
//import galileo.collada.ReadCollada;
//import galileo.collada.nodes.RootColladaNode;
//import java.io.File;
//import org.lwjgl.opengl.GL11;
//import org.lwjgl.util.vector.Vector3f;
//
///**
// *
// * @author mads
// */
//public class ColladaModel {
//    private RootColladaNode root = null;
//
//    public void render(Vector3f position) {
//        GL11.glEnable(GL11.GL_LIGHTING);
//        GL11.glEnable(GL11.GL_LIGHT0);
//        Light.test();
//        Ref.glRef.setShader("litobject");
//        GL11.glMatrixMode(GL11.GL_MODELVIEW);
//        GL11.glPushMatrix();
//        GL11.glMaterialf(GL11.GL_FRONT_AND_BACK, GL11.GL_SHININESS, 100f);
//
//        GL11.glTranslatef(position.x, position.y, position.z);
//
//        GL11.glRotatef(Ref.client.realtime/10f, 0, 1, 0);
////        GL11.glRotatef(Ref.client.realtime/9f, 0, 1, 0);
////        GL11.glRotatef(Ref.client.realtime/8f, 1, 0, 0);
//        if(root != null) {
//            root.renderObject(0);
//        }
//        GL11.glDisable(GL11.GL_LIGHTING);
//        GL11.glDisable(GL11.GL_LIGHT0);
//        GL11.glPopMatrix();
//        Ref.glRef.setShader("sprite");
//    }
//
//
//    public static ColladaModel load(String filename) {
//        ColladaModel m = new ColladaModel();
//        File f = ResourceManager.OpenFileAsFile(filename);
//
//        m.root = ReadCollada.read(f, GL11.GL_SHORT, true, false, 20.0f);
//        m.root.setup();
//        return m;
//    }
//}
