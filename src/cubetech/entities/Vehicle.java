package cubetech.entities;

import cubetech.Game.GameClient;
import cubetech.Game.Gentity;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.IThinkMethod;
import cubetech.common.IUseMethod;
import cubetech.common.MoveQuery;
import cubetech.iqm.IQMModel;
import cubetech.misc.Ref;
import org.lwjgl.util.vector.Vector3f;


/**
 *
 * @author mads
 */
public class Vehicle extends Gentity implements IUseMethod, IThinkMethod {
    public class VehicleControls {
        // Current player input
        public float throttle; // 0-1
        public float brake; // 0-1
        public float steering; // -1 -> 1?
        public int gear = 1;
    }

    public class VechileSetup {
        float drag = 0.4257f; // drag constant
        float rollResistance = drag * 30;
        float weight = 1400; // kg
        float lenght = 2f; // meters
        float fracToBack = 0.5f; // center -> back == lenght * fracToBack
        float height = 1f; // meters

        float transmissionEfficiency = 0.7f;
        float wheelRadius = 0.34f;
        float differential = 3.42f;
        float gearRatios[] = new float[] {
            2.90f, // 0 = reverse
            2.66f,
            1.78f,
            1.30f,
            1.0f,
            0.74f,
            0.50f
        };
    }

    public class VehicleState {
        public float rpm;
        public float torque;
        
    }

    private GameClient player;
    private VehicleControls controls = new VehicleControls();
    private VechileSetup setup = new VechileSetup();
    private VehicleState state = new VehicleState();
    private int lastthink;

    private Vector3f calcEngine(Vector3f forward) {
        Vector3f traction = new Vector3f(forward);

        float wheelrot = s.pos.delta.length() / setup.wheelRadius;
        float rpm = (float) (wheelrot * setup.gearRatios[controls.gear] * setup.differential * 60f / (2f * Math.PI));
        if(rpm < 1000) rpm = 1000;
        float availableTorque =  448;
        if(rpm > 7000) {
            availableTorque = 0;
        }

        
        

        float transmission = setup.gearRatios[controls.gear] * setup.differential * setup.transmissionEfficiency;
        float engineforce = availableTorque * controls.throttle;
        state.rpm = rpm;
        state.torque = (engineforce * transmission) / setup.wheelRadius;
        
        traction.scale(engineforce * transmission);
        traction.scale(1f / setup.wheelRadius);
        return traction;
    }

    // update vehicle physics
    public void think(Gentity ent) {
        if(getPassenger() == null) return;
        Vector3f longForces = new Vector3f();
        Vector3f velocity = s.pos.delta;
        float msec = (Ref.game.level.time - lastthink) * 0.001f;

        Vector3f forward = new Vector3f();
        Helper.AngleVectors(s.Angles, forward, null, null);
        
        // Engine traction
        Vector3f traction = calcEngine(forward);
        Vector3f.add(longForces, traction, longForces);

        // Brakes
        float veldot = Vector3f.dot(velocity, forward);
        if(veldot > 0) {
            float brakeforce = controls.brake * 10000;
            Vector3f brake = new Vector3f(forward);
            brake.scale(-brakeforce);
            Vector3f.add(longForces, brake, longForces);
        }

        // Air drag
        Vector3f drag = new Vector3f(velocity);
        drag.scale(-setup.drag * velocity.length());
        Vector3f.add(longForces, drag, longForces);

        // Mechanical drag
        Vector3f rdrag = new Vector3f(velocity);
        rdrag.scale(-setup.rollResistance);
        Vector3f.add(longForces, rdrag, longForces);

        // accel = Force / Mass
        Vector3f accel = new Vector3f(longForces);
        accel.scale(1f/setup.weight);

        // vel += accel * dt
        Helper.VectorMA(velocity, msec, accel, velocity);
        // pos += vel * dt (*32 for world scaling)
        Helper.VectorMA(s.pos.base, msec *32f, velocity, s.pos.base);

        r.currentOrigin.set(s.pos.base);
        Link();
        lastthink = nextthink = Ref.game.level.time;
    }

    // Use this vector for offseting origin to player view
    public Vector3f getPlayerViewOffset() {
        return new Vector3f();
    }

    // process player input
    public void processMove(MoveQuery move) {
        controls.throttle = move.cmd.Forward?1:0;
        controls.brake = move.cmd.Back?1:0;
        controls.steering = move.cmd.Left?-1:0;
        controls.steering += move.cmd.Right?1:0;

        move.ps.commandTime = move.cmd.serverTime;

        move.ps.origin.set(r.currentOrigin);
    }

    public GameClient getPassenger() {
        return player;
    }

    public void setPassenger(GameClient player) {
        if(this.player != null) {
            // todo: remove other player
        }

        if(player != null) {
            // put player in car
            this.player = player;

            // startEngine();
            nextthink = Ref.game.level.time;
        } else {
            this.player = null;
        }
    }

    @Override
    public void Init(int i) {
        super.Init(i);
        classname = "vehicle";
        r.mins.set(-50,-50,-50);
        r.maxs.set(50,50,50);
        s.eType = EntityType.GENERAL;
        r.contents = Content.SOLID;
        ClipMask = Content.SOLID;
        this.use = this;
        this.think = this;

        IQMModel model = Ref.ResMan.loadModel("data/models/boxcar.iqm");
        r.maxs.set(model.getMaxs());
        r.mins.set(model.getMins());
        r.bmodel = true;
        r.s.modelindex = Ref.server.registerModel("data/models/boxcar.iqm");
        Link();
    }

    public VehicleControls getControls() {
        return controls;
    }

    public VechileSetup getSetup() {
        return setup;
    }

    public VehicleState getState() {
        return state;
    }

    public void use(Gentity self, Gentity other, Gentity activator) {
        if(!activator.isClient()) return;
        Common.Log("Use triggered on vehicle");
        
        

        GameClient gc = activator.getClient();
        if(getPassenger() == gc) {
            // Already a passenger?
            gc.leaveVehicle();
        } else {
            if(getPassenger() != null) return; // No room
            gc.getInVehicle(this);
        }
    }
}
