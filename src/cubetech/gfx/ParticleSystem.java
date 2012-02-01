/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import cubetech.common.Common;
import cubetech.common.ICommand;
import cubetech.gfx.emitters.FireEmitter;
import cubetech.gfx.emitters.RingEmitter;
import cubetech.gfx.emitters.SmokeEmitter;
import cubetech.gfx.emitters.SparkEmitter;
import cubetech.gfx.emitters.TrailEmitter;
import cubetech.misc.Ref;
import java.util.ArrayList;
import java.util.EnumMap;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author mads
 */
public class ParticleSystem {
    private static ArrayList<IEmitter> emitters = new ArrayList<IEmitter>();
    private static boolean initialized = false;

//    public enum Type {
//        SMOKE,
//        SPARK,
//        FIRE,
//
//    }

    //private EnumMap

    public static void addEmitter(IEmitter emit) {
        emitters.add(emit);
    }

    public static void init() {
        if(initialized) return;
        initialized = true;
        
        Ref.commands.AddCommand("testemit", cmd_testemit);
        Ref.commands.AddCommand("clearemit", cmd_clearemit);
    }

    private static ICommand cmd_testemit = new ICommand() {
        public void RunCommand(String[] args) {
            Vector3f origin = Ref.cgame.cg.cur_lc.predictedPlayerEntity.lerpOrigin;
            Vector3f normal = new Vector3f(0, 0, 1);

            if(args.length < 2) {
                Common.Log("usage: testemit <type>");
                return;
            }

            String name = args[1];
            if(name.equalsIgnoreCase("smoke")) {
                SmokeEmitter emit = new SmokeEmitter(origin, 50, normal, 0.1f);
                addEmitter(emit);
            } else if(name.equalsIgnoreCase("spark")) {
                SparkEmitter emit = new SparkEmitter(origin, 120, 20);
                addEmitter(emit);
            } else if(name.equalsIgnoreCase("trail")) {
                TrailEmitter emit = new TrailEmitter(origin, 100, 10);
                addEmitter(emit);
            } else if(name.equalsIgnoreCase("ring")) {
                RingEmitter emit = new RingEmitter(origin, 100, new Vector3f(0, 0, 1));
                addEmitter(emit);
            } else if(name.equalsIgnoreCase("fire")) {
                FireEmitter emit = new FireEmitter(origin, 100, 20);
                addEmitter(emit);
            } else {
                Common.Log("Unknown emitter %s", name);
            }
        }
    };

    private static ICommand cmd_clearemit = new ICommand() {
        public void RunCommand(String[] args) {
            emitters.clear();
        }
    };

    private static int lastTime = 0;
    public static void update() {
        int msec;
        if(lastTime == 0) {
            lastTime = Ref.cgame.cg.time;
            msec = 16;
        } else {
            msec = Ref.cgame.cg.time - lastTime;
            lastTime = Ref.cgame.cg.time;
        }
        
        for (IEmitter iEmitter : emitters) {
            iEmitter.update(msec);
        }
    }
}
