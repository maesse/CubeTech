package cubetech.misc;

import java.util.ArrayList;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Mads
 */
public class PoissonGenerator {
    public static ArrayList<Vector3f>  generateUnitSphere(Vector3f normal, float minimumDistance, int maxiter) {
        ArrayList<Vector3f> set = new ArrayList<Vector3f>();
        for (int i = 0; i < maxiter; i++) {
            Vector3f result = generatePoint(normal, set, minimumDistance, 30);
            if(result != null) set.add(result);
        }
        
        return set;
    }
    
    // Not guaranteed to hit the min or max
    public static ArrayList<Vector3f> generateUnitSphere(Vector3f normal, int minCount, int maxCount) {
        float minimumDistance = 0.7f; // 0.7 is about as good as the random dart method can do
        int maxIter = 100; // limit total amount of tries
        float step = -0.005f;
        ArrayList<Vector3f> der = null;
        ArrayList<Vector3f> closest = null;
        int closestOffset = 0;
        int i;
        for (i = 0; i < maxIter; i++) {
            der = generateUnitSphere(normal, minimumDistance, maxCount*2);
            if(der.size() >= minCount && der.size() <= maxCount) break;
            
            int offset = der.size() < minCount ? minCount-der.size() : der.size()-maxCount;
            if(closest == null || offset < closestOffset) {
                closest = der;
                closestOffset = offset;
            }
            minimumDistance += step;
        }
        if(i == maxIter) der = closest;
        
        return der;
    }
    
    // Dart throwing
    public static Vector3f generatePoint(Vector3f normal, ArrayList<Vector3f> pointSet, float minDist, int maxiter) {
        // Using squared distance to avoid sqrt calls
        float sqMinDist = minDist * minDist;
        Vector3f result = null;
        for (int iter = 0; iter < maxiter; iter++) {
            Vector3f rnd = getRandomPointInSphere();
            
            if(normal != null) {
                float dot = Vector3f.dot(normal, rnd);
                if(dot <= 0) continue;
            }
            
            boolean rejected = false;
            for (int i = 0; i < pointSet.size(); i++) {
                Vector3f test = pointSet.get(i);
                float dx = test.x - rnd.x;
                float dy = test.y - rnd.y;
                float dz = test.z - rnd.z;
                float sqDist = dx * dx + dy * dy + dz * dz;
                if(sqDist < sqMinDist) {
                    rejected = true;
                    break;
                }
            }
            if(!rejected) {
                result = rnd;
                break;
            }
        }
        return result;
    }
    
    private static Vector3f getRandomPointInSphere() {
        // Create random vector [-1;1]
        Vector3f v = new Vector3f(Ref.rnd.nextFloat()*2.0f - 1.0f, Ref.rnd.nextFloat()*2.0f - 1.0f, Ref.rnd.nextFloat()*2.0f - 1.0f);
        // normalize to unit sphere
        v.normalise();
        // apply random scale
        v.scale(Ref.rnd.nextFloat());
        return v;
    }
}
