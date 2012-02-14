
package cubetech.misc;

import cubetech.common.Common;
import cubetech.common.Common.ErrorCode;
import java.util.ArrayList;
import java.util.Stack;
import org.lwjgl.opengl.GL11;

/**
 *
 * @author mads
 */
public class Profiler {

    
    public enum Sec {
        COMMON,
        SERVER,
        CLIENT,
        MOVE,
        CG_RENDER,
        RENDER,
        RENDER_WORLD,
        RENDER_IQM,
        RENDER_SPRITE,
        RENDERWAIT,
        TEXMAN,
        SHADOWS,
        CLIENTCUBES,
        PHYSICS,
        DIR_LIGHT,
        POINT_LIGHT,
        SSAO,
        AMBIENT_PASS,
    ENDFRAME,
    SV_PACKET,
    CL_PACKET,
    VBO_MAP,
    RENDER_POLYBATCH}

    private static long[] timeTable = new long[Sec.values().length];
    private static float[] msTable = new float[Sec.values().length];
    private static float[] frametimes = new float[60*3];
    private static int frametimesOffset = 0;
    private static int lastFrame;
    private static boolean forceOpenGLFinish = false;
    
    private static ArrayList<StackEntry> stackEntries = new ArrayList<StackEntry>();
    private static ArrayList<StackEntry> swapStackEntries = new ArrayList<StackEntry>();
    private static StackEntry stackEntry = null;
    
    private static void pushSecTag(SecTag tag) {
        StackEntry entry = new StackEntry(tag);
        if(stackEntry == null) {
            // First in list
            stackEntry = entry;
        } else {
            // Push to current stackEntry's subtags
            if(stackEntry.subTags == null) stackEntry.subTags = new ArrayList<StackEntry>();
            stackEntry.subTags.add(entry);
            
            // Point back to parent
            entry.parent = stackEntry;
            stackEntry = entry;
        }
    }
    
    public static ArrayList<StackEntry> getStackFrames() {
        return swapStackEntries;
    }
    
    private static void popSecTag(SecTag tag) {
        if(stackEntry == null) Ref.common.Error(ErrorCode.FATAL, "Popping empty profiler stack");
        if(tag != stackEntry.tag) Ref.common.Error(ErrorCode.FATAL, "Unaligned profiler stack poppin");
        if(stackEntry.parent == null) {
            // Returned to level 0, move the branch to stackEntries
            stackEntries.add(stackEntry);
            stackEntry = null;
        } else {
            // pop parent
            stackEntry = stackEntry.parent;
        }
    }
    
    
    
    
    
    public static class StackEntry {
        SecTag tag;
        StackEntry parent;
        public ArrayList<StackEntry> subTags = null;
        public float selfTime;
        public float sumTime;
        public int calls = 1;
        StackEntry(SecTag tag) {
            this.tag = tag;
        }
        StackEntry(Sec tag) {
            this.tag = new SecTag(tag);
        }
        private float calcStackTimes() {
            float ms = 0.0f;
            if(subTags != null) {
                for (StackEntry entry : subTags) {
                    ms += entry.calcStackTimes();
                }
            }
            selfTime = tag.delta - ms;
            sumTime = tag.delta;
            return sumTime;
        }
        @Override
        public String toString() {
            return String.format("%s: %.2fms self, %.2fms total (%d calls)", tag.sec.toString(), selfTime, sumTime, calls);
        }
    }
    
    public static SecTag EnterSection(Sec sec) {
        if(forceOpenGLFinish) GL11.glFinish();
        SecTag tag =  new SecTag(sec);
        pushSecTag(tag);
        return tag;
    }

    public static class SecTag {
        private Sec sec;
        private long startTime;
        private long endTime;
        private float delta;

        private SecTag(Sec sec) {
            this.sec = sec;
            startTime = System.nanoTime();
            endTime = 0;
        }

        public void ExitSection() {
            if(endTime != 0) Ref.common.Error(ErrorCode.FATAL, "Multiple uses of same SecTag");
            if(forceOpenGLFinish) GL11.glFinish();
            // Add SecTag to time table
            endTime = System.nanoTime();
            long deltal = endTime - startTime;
            delta = deltal  / 1000000f;
            addToSum(sec, deltal);
            
            popSecTag(this);
        }
    }
    
    public static void setForceGLFinish(boolean value) {
        forceOpenGLFinish = value;
    }

    public static float[] getTimes() {
        return msTable;
    }

    public static float[] getFrameTimes() {
        return frametimes;
    }

    public static int getFrametimeOffset() {
        return frametimesOffset;
    }

    public static void reset() {
        calculateTimes();
        timeTable = new long[Sec.values().length];
        ArrayList<StackEntry> temp = stackEntries;
        stackEntries = swapStackEntries;
        swapStackEntries = temp;
        stackEntries.clear();
        if(stackEntry != null) {
            Common.Log("Garbage branch left on profiler stack: " + stackEntry.tag.sec);
            stackEntry = null;
        }
        
        
        
        for (StackEntry entry : swapStackEntries) {
            entry.calcStackTimes();
        }
        
        cleanStack(swapStackEntries);
    }
    
    private static void cleanStack(ArrayList<StackEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            StackEntry entry = entries.get(i);
            
            for (int j = entries.size()-1; j > i; j--) {
                StackEntry subEntry = entries.get(j);
                if(entry.tag.sec.equals(subEntry.tag.sec)) {
                    // wrap into entry, merge subcalls
                    entry.calls += subEntry.calls;
                    entry.selfTime += subEntry.selfTime;
                    entry.sumTime += subEntry.sumTime;
                    if(subEntry.subTags != null) {
                        if(entry.subTags == null) {
                            entry.subTags = subEntry.subTags;
                            subEntry.subTags = null;
                        } else {
                            entry.subTags.addAll(subEntry.subTags);
                        }
                    }
                    entries.remove(j);
                }
            }
            if(entry.subTags != null) cleanStack(entry.subTags);
        }
    }

    private static void calculateTimes() {
        for (int i= 0; i < timeTable.length; i++) {
            msTable[i] = timeTable[i] / 1000000f; // from nano -> milli
        }

        // Going to cheat a bit.. Pull out render time from client times
        //msTable[Sec.CLIENT.ordinal()] -= msTable[Sec.ENDFRAME.ordinal()];
        //msTable[Sec.CLIENT.ordinal()] -= msTable[Sec.RENDER.ordinal()];
        //msTable[Sec.RENDER.ordinal()] -= msTable[Sec.SHADOWS.ordinal()];
        //msTable[Sec.CLIENT.ordinal()] -= msTable[Sec.RENDERWAIT.ordinal()];
        //msTable[Sec.CLIENT.ordinal()] -= msTable[Sec.PHYSICS.ordinal()];

        int thisTime = Ref.common.frametime;
        if(lastFrame != 0) {
            frametimes[frametimesOffset%frametimes.length] = thisTime - lastFrame;
            frametimesOffset++;
        }
        lastFrame = Ref.common.frametime;
    }

    
    
    private static void addToSum(Sec tag, long delta) {
        timeTable[tag.ordinal()] += delta;
    }

    
}
