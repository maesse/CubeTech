package cubetech.iqm;

import cubetech.common.Common;
import cubetech.common.Helper;
import java.util.HashMap;

/**
 * Contains information about a joint-mesh pair used for setting
 * up physics constraints
 * @author Mads
 */
public class BoneMeshInfo {
    public IQMMesh mesh;
    public IQMJoint joint;
    public Type type;
    
    public float mass = 10f;
    
    // Angular rotation limits
    public float rminx = -0.3f;
    public float rmaxx = 0.3f;
    
    public float rminy = -0.0f;
    public float rmaxy = 0.0f;
    
    public float rminz = -0.3f;
    public float rmaxz = 0.3f;
    
    // Damped spring
    public boolean springx = false;
    public boolean springy = false;
    public boolean springz = false;
    
    public float sforcex = 2000f;
    public float sforcey = 2000f;
    public float sforcez = 2000f;
    
    public float sdampingx = 0.3f;
    public float sdampingy = 0.3f;
    public float sdampingz = 0.3f;
    
    // Use this joint's bonemesh as parent.
    // Usefull for advanced bone hiareachy
    public String parentBoneMeshJoint = null;

    
    // Reads in the given parameters
    public BoneMeshInfo(HashMap<String, String> params) {
        try {
            String v;
            if((v = params.get("parent")) != null) parentBoneMeshJoint = v;
            if((v = params.get("mass")) != null) mass = Float.parseFloat(v);
            if((v = params.get("rminx")) != null) rminx = Float.parseFloat(v);
            if((v = params.get("rminy")) != null) rminy = Float.parseFloat(v);
            if((v = params.get("rminz")) != null) rminz = Float.parseFloat(v);

            if((v = params.get("rmaxx")) != null) rmaxx = Float.parseFloat(v);
            if((v = params.get("rmaxy")) != null) rmaxy = Float.parseFloat(v);
            if((v = params.get("rmaxz")) != null) rmaxz = Float.parseFloat(v);

            if((v = params.get("sforcex")) != null) {sforcex = Float.parseFloat(v); springx = true;}
            if((v = params.get("sforcey")) != null) {sforcey = Float.parseFloat(v); springy = true;}
            if((v = params.get("sforcez")) != null) {sforcez = Float.parseFloat(v); springz = true;}

            if((v = params.get("sdampingx")) != null) {sdampingx = Float.parseFloat(v); springx = true;}
            if((v = params.get("sdampingy")) != null) {sdampingy = Float.parseFloat(v); springy = true;}
            if((v = params.get("sdampingz")) != null) {sdampingz = Float.parseFloat(v); springz = true;}
        
        } catch (NumberFormatException ex) {
            Common.Log("Invalid IQMScript info: " + Common.getExceptionString(ex));
        }
    }
    
    public BoneMeshInfo() {}
    
    public boolean equals(BoneMeshInfo other) {
        if(other == null) return false;
        // lulz
        return (type == other.type && 
                other.mesh == mesh &&
                other.joint == joint &&
                other.type == type &&
                other.mass == mass &&
                other.rminx == rminx &&
                other.rminy == rminy &&
                other.rminz == rminz &&
                other.rmaxx == rmaxx &&
                other.rmaxy == rmaxy &&
                other.rmaxz == rmaxz &&
                other.springx == springx &&
                other.springy == springy &&
                other.springz == springz &&
                other.sforcex == sforcex &&
                other.sforcey == sforcey &&
                other.sforcez == sforcez &&
                other.sdampingx == sdampingx &&
                other.sdampingy == sdampingy &&
                other.sdampingz == sdampingz &&
                Helper.Equals(other.parentBoneMeshJoint, parentBoneMeshJoint));
    }
    
    public BoneMeshInfo clone(BoneMeshInfo info) {
        if (info == null) info = new BoneMeshInfo();
        info.mesh = mesh;
        info.joint = joint;
        info.type = type;
        info.mass = mass;
        info.rminx = rminx;
        info.rminy = rminy;
        info.rminz = rminz;
        info.rmaxx = rmaxx;
        info.rmaxy = rmaxy;
        info.rmaxz = rmaxz;
        info.springx = springx;
        info.springy = springy;
        info.springz = springz;
        info.sforcex = sforcex;
        info.sforcey = sforcey;
        info.sforcez = sforcez;
        info.sdampingx = sdampingx;
        info.sdampingy = sdampingy;
        info.sdampingz = sdampingz;
        info.parentBoneMeshJoint = parentBoneMeshJoint;
        return info;
    }
    
    public enum Type {
        RIGID, // follows the body
        FLEXIBLE, // twist and turn
        SPRING // flexible + springy
    }
}
