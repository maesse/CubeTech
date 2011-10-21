
package cubetech.misc;

/**
 *
 * @author mads
 */
public class Profiler {
    public enum Sec {
        SERVER,
        CLIENT,
        MOVE,
        RENDER,
        RENDERWAIT,
        TEXMAN,
        SHADOWS,
        CLIENTCUBES
    ,   PHYSICS,
    ENDFRAME,
    SV_PACKET,
    CL_PACKET}

    private static long[] timeTable = new long[Sec.values().length];
    private static float[] msTable = new float[Sec.values().length];
    private static float[] frametimes = new float[60*3];
    private static int frametimesOffset = 0;
    private static int lastFrame;

    public static class SecTag {
        private Sec sec;
        private long startTime;

        private SecTag(Sec sec) {
            this.sec = sec;
            startTime = System.nanoTime();
        }

        public void ExitSection() {
            addTime(this);
            sec = null; // force error if used twice
        }
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
    }

    private static void calculateTimes() {
        for (int i= 0; i < timeTable.length; i++) {
            msTable[i] = timeTable[i] / 1000000f; // from nano -> milli
        }

        // Going to cheat a bit.. Pull out render time from client times
        msTable[Sec.CLIENT.ordinal()] -= msTable[Sec.ENDFRAME.ordinal()];
        msTable[Sec.CLIENT.ordinal()] -= msTable[Sec.RENDER.ordinal()];
        msTable[Sec.RENDER.ordinal()] -= msTable[Sec.SHADOWS.ordinal()];
        msTable[Sec.CLIENT.ordinal()] -= msTable[Sec.RENDERWAIT.ordinal()];
        msTable[Sec.CLIENT.ordinal()] -= msTable[Sec.PHYSICS.ordinal()];

        int thisTime = Ref.common.frametime;
        if(lastFrame != 0) {
            frametimes[frametimesOffset%frametimes.length] = thisTime - lastFrame;
            frametimesOffset++;
        }
        lastFrame = Ref.common.frametime;
    }

    // Add SecTag to time table
    private static void addTime(SecTag t) {
        long sectionTime = System.nanoTime() - t.startTime;
        timeTable[t.sec.ordinal()] += sectionTime;
    }

    public static SecTag EnterSection(Sec sec) {
        return new SecTag(sec);
    }
}
