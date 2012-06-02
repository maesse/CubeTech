package cubetech.entities;


import cubetech.Game.GPhysicsEntity;
import cubetech.Game.GameClient;
import cubetech.Game.Gentity;
import cubetech.collision.DefaultPhysics;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.IThinkMethod;
import cubetech.common.IUseMethod;
import cubetech.common.MoveQuery;
import cubetech.common.Quaternion;
import cubetech.common.Trajectory;
import cubetech.input.PlayerInput;
import cubetech.iqm.IQMJoint;
import cubetech.iqm.IQMModel;
import cubetech.misc.Ref;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import nbullet.PhysicsSystem;
import nbullet.objects.CollisionObject.ActivationStates;
import nbullet.objects.RigidBody;
import nbullet.util.Transform;
import nbullet.vehicle.DefaultVehicleRaycaster;
import nbullet.vehicle.RaycastVehicle;
import nbullet.vehicle.VehicleTuning;
import nbullet.vehicle.WheelInfo;
import org.lwjgl.util.vector.Vector3f;


/**
 *
 * @author mads
 */
public class Vehicle extends GPhysicsEntity implements IUseMethod, IThinkMethod {
    private VehicleControls controls = new VehicleControls();
    private VechileSetup setup = new VechileSetup();
    private VehicleState state = new VehicleState();
    private GameClient player;
    private int lastthink;

    private RaycastVehicle vehicle;
    private DefaultVehicleRaycaster vehicleRayCaster;
    
    private Gentity[] wheelEntites;
    
    private int lastGroundTime;
    
    private static final int FRONTLEFT = 0;
    private static final int FRONTRIGHT = 1;
    private static final int REARLEFT = 2;
    private static final int REARRIGHT = 3;
    
    
    public void initVehicle() {
        // Ensure physics model is loaded
        if(physicsBody == null) updateFromPhysics();
        
        VehicleTuning tuning = setup.getTuning();
        PhysicsSystem dynamicsWorld = Ref.game.level.physics.getWorld();
        RigidBody carChassis = physicsBody;
        vehicleRayCaster = new DefaultVehicleRaycaster(dynamicsWorld);
        
        // Create vehicle
        vehicle = new RaycastVehicle(tuning,carChassis,vehicleRayCaster);
        // never deactivate the vehicle
        carChassis.setActivationState(ActivationStates.DISABLE_DEACTIVATION);
        dynamicsWorld.addVehicle(vehicle);
        // choose coordinate system
        vehicle.setCoordinateSystem(0,2,1);

        // Set up wheels
        vehicle.initWheelCount(4); // Very important!
        Vector3f[] wheelOffsets = new Vector3f[4];

        IQMModel model = getModel();
        if(model != null) {
            // Check for wheel attachment bones
            String[] names = new String[] {"frontleft", "frontright", "rearleft", "rearright"};
            IQMJoint[] joints = model.getJoints();
            for (int i = 0; i < joints.length; i++) {
                String name = joints[i].getName().toLowerCase();
                int j;
                for (j = 0; j < names.length; j++) {
                    if(name.equals(names[j])) break;
                }
                if(j == names.length) continue;
                // extraxt bone position
                wheelOffsets[j] = joints[i].getJointOrigin(null);
                
                wheelOffsets[j].scale(DefaultPhysics.SCALE_FACTOR);
                centerOfMass.transform(wheelOffsets[j]);
                Common.LogDebug("Wheel %s connected to %s", name, wheelOffsets[j]);
            }
        }

        // Fallback to bbox wheelpositioning
        Vector3f halfSize = Vector3f.sub(r.maxs, r.mins, null);
        halfSize.scale(0.5f*DefaultPhysics.SCALE_FACTOR);
        if(wheelOffsets[FRONTLEFT] == null) {
            wheelOffsets[FRONTLEFT] = new Vector3f(halfSize.x-setup.wheelRadius,halfSize.y-setup.wheelWidth,-halfSize.z+setup.wheelWidth);
        }
        if(wheelOffsets[FRONTRIGHT] == null) {
            wheelOffsets[FRONTRIGHT] = new Vector3f(halfSize.x-setup.wheelRadius,-halfSize.y+setup.wheelWidth,-halfSize.z+setup.wheelWidth);
        }
        if(wheelOffsets[REARLEFT] == null) {
            wheelOffsets[REARLEFT] = new Vector3f(-halfSize.x+setup.wheelRadius,-halfSize.y+setup.wheelWidth,-halfSize.z+setup.wheelWidth);
        }
        if(wheelOffsets[REARRIGHT] == null) {
            wheelOffsets[REARRIGHT] = new Vector3f(-halfSize.x+setup.wheelRadius,halfSize.y-setup.wheelWidth,-halfSize.z+setup.wheelWidth);
        }
        
        Vector3f wheelDirectionCS0 = new Vector3f(0,0,-1);
        Vector3f wheelAxleCS = new Vector3f(0,-1,0);

        // front left
        vehicle.addWheel(wheelOffsets[FRONTLEFT],wheelDirectionCS0,wheelAxleCS,setup.suspensionRestLength,setup.wheelRadius,tuning,true);
        // Front right
        vehicle.addWheel(wheelOffsets[FRONTRIGHT],wheelDirectionCS0,wheelAxleCS,setup.suspensionRestLength,setup.wheelRadius,tuning,true);
        // back left
        vehicle.addWheel(wheelOffsets[REARLEFT],wheelDirectionCS0,wheelAxleCS,setup.suspensionRestLength,setup.wheelRadius,tuning,false);
        // back right
        vehicle.addWheel(wheelOffsets[REARRIGHT],wheelDirectionCS0,wheelAxleCS,setup.suspensionRestLength,setup.wheelRadius,tuning,false);
    }
    
    
    
    private void applyWheelSetup() {
        for (int i=0;i<vehicle.getWheelCount();i++)
        {
            WheelInfo wheel = vehicle.getWheelInfo(i);
            float s = (float)Math.abs(Math.sin(Ref.cgame.cg.time/1000f));
            wheel.setSuspensionRestLength(1.15f*s);
            wheel.setMaxSuspensionTravelCm(1000*s);
            wheel.setSuspensionStiffness(setup.suspensionStiffness);
            wheel.setWheelsDampingCompression(setup.suspensionCompression);
            wheel.setWheelsDampingRelaxation(setup.suspensionDamping);
            wheel.setFrictionSlip(setup.wheelFriction);
            wheel.setRollInfluence(setup.rollInfluence);
            wheel.setMaxSuspensionForce(setup.wheelsSuspensionForce);
        }
    }
    
    public DefaultVehicleRaycaster getRayCaster() {
        return vehicleRayCaster;
    }
//
//    // update vehicle physics
    public void think(Gentity ent) {
        if(getPassenger() == null) {
            controls.brake = 1.0f;
            controls.throttle = 0.0f;
        }
        applyWheelSetup();
        
        
        checkCarFlipped();

        calcEngine();
        if(controls.brake > 0) {
            state.engineForce = -state.engineForce;
        }
        int wheelIndex;
        boolean engineBackWheels = (setup.driveTrain == DriveTrain.ALL || setup.driveTrain == DriveTrain.BACK);
        boolean engineFrontWheels = (setup.driveTrain == DriveTrain.ALL || setup.driveTrain == DriveTrain.FRONT);
        
        
        wheelIndex = 2;
        if(engineBackWheels) vehicle.applyEngineForce(state.engineForce,wheelIndex);
        vehicle.setBrake(controls.brake * setup.brakeForce * (1f-setup.brakeBias),wheelIndex);
        
        wheelIndex = 3;
        if(engineBackWheels) vehicle.applyEngineForce(state.engineForce,wheelIndex);
        vehicle.setBrake(controls.brake * setup.brakeForce * (1f-setup.brakeBias),wheelIndex);

        // Front left
        wheelIndex = 0;
        vehicle.setSteeringValue(controls.steering,wheelIndex);
        if(engineFrontWheels) vehicle.applyEngineForce(state.engineForce,wheelIndex);
        vehicle.setBrake(controls.brake * setup.brakeForce * (setup.brakeBias),wheelIndex);
        
        // Front right
        wheelIndex = 1;
        if(engineFrontWheels) vehicle.applyEngineForce(state.engineForce,wheelIndex);
        vehicle.setSteeringValue(controls.steering,wheelIndex);
        vehicle.setBrake(controls.brake * setup.brakeForce * (setup.brakeBias),wheelIndex);
        
        updateFromPhysics();
//        
        for (int i = 0; i < vehicle.getWheelCount(); i++) {
            vehicle.updateWheelTransform(i, true);
            WheelInfo info = vehicle.getWheelInfo(i);
            int offset = vehicle.getWheelInfoOffset(i);
            //Transform t = vehicle.getWheelTransform(i);
            //WheelInfo info = vehicle.getWheelInfo(i);
            Transform t = info.getWorldTransform();
            t.origin.scale(DefaultPhysics.INV_SCALE_FACTOR);
            wheelEntites[i].r.currentOrigin.set(t.origin);
            wheelEntites[i].r.mins.set(-16,-16,-16);
            wheelEntites[i].r.maxs.set(16,16,16);
            wheelEntites[i].s.contents = Content.SOLID;
            wheelEntites[i].s.pos.type = Trajectory.INTERPOLATE;
            wheelEntites[i].s.pos.base.set(wheelEntites[i].r.currentOrigin);
            wheelEntites[i].s.apos.type = Trajectory.QUATERNION;
            t.basis.invert();
            Quaternion.setFromMatrix(t.basis, wheelEntites[i].s.apos.quater);
            wheelEntites[i].Link();
        }
            
        
            
        Link();
        lastthink = nextthink = Ref.game.level.time;
    }
    
    private void checkCarFlipped() {
        boolean frontOnGround = false;
        boolean backOnGround = false;
        for (int i = 0; i < vehicle.getWheelCount(); i++) {
            WheelInfo info = vehicle.getWheelInfo(i);
            if(info.getRayIsInContact()) {
                if(info.getIsFrontWheel()) frontOnGround = true;
                else backOnGround = true;
            }
        }

        
        boolean engineBackWheels = (setup.driveTrain == DriveTrain.ALL || setup.driveTrain == DriveTrain.BACK);
        boolean engineFrontWheels = (setup.driveTrain == DriveTrain.ALL || setup.driveTrain == DriveTrain.FRONT);
        boolean poweredWheelHasContact = (engineBackWheels && backOnGround) || (engineFrontWheels && frontOnGround);
        
        if(poweredWheelHasContact) lastGroundTime = Ref.game.level.time;
        if(lastGroundTime + 5000 < Ref.game.level.time) {
            resetCar();
            lastGroundTime = Ref.game.level.time;
        }
    }
    
    public void resetCar() {
        Transform tr = new Transform();
        Transform oldTr = new Transform(physicsBody.getMotionState().getWorldTransform((FloatBuffer)null));
        
        tr.origin.set(oldTr.origin);
        tr.origin.z += 2f;
        physicsBody.setWorldTransform(tr.store(PhysicsSystem.vecBuffer64));
        physicsBody.setLinearVelocity(new Vector3f());
        physicsBody.setAngularVelocity(new Vector3f());
        Ref.game.level.physics.clearOverlappingCache(physicsBody);
        
        if (vehicle != null)
        {
            vehicle.resetSuspension();
            for (int i=0;i<vehicle.getWheelCount();i++)
            {
                // synchronize the wheels with the (interpolated) chassis worldtransform
                vehicle.updateWheelTransform(i,true);
            }
        }
    }

    // Use this vector for offseting origin to player view
    public Vector3f getPlayerViewOffset() {
        return new Vector3f();
    }

    // process player input
    public void processMove(MoveQuery move) {
        float forward = PlayerInput.byteAsFloat(move.cmd.forward);
        float side = PlayerInput.byteAsFloat(move.cmd.side);
        controls.throttle = forward > 0?forward:0;
        controls.brake = forward < 0?-forward:0;
        
        float steerInput = side*-1f;
        
        float steerDelta = steerInput - controls.steering;
        
        // Modify steering delta
        boolean noInput = (move.cmd.side == 0 && move.cmd.forward == 0);
        boolean sameDirection = (controls.steering > 0 == steerInput > 0); // changing steering direction?
        float absSteering = Math.abs(controls.steering);
        float springDampenStartFrac = 0.1f;
        float steerSpeed = 3;
        float time = (move.cmd.serverTime - move.ps.commandTime) / 1000f;
        // return to center fast
        if(noInput) steerDelta *= 2f;
        // spring dampen steering
        else if(sameDirection && absSteering > springDampenStartFrac) steerDelta *= 1-springDampenStartFrac - absSteering;
        steerDelta *= steerSpeed * time;
        
        // Add steering change
        controls.steering += steerDelta;
        controls.steering = Helper.Clamp(controls.steering, -1, 1);
        
        if(move.cmd.Mouse1 && move.cmd.Mouse1Diff) {
            // gear up
            controls.gear++;
            if(controls.gear > 6) controls.gear = 6;
        }
        if(move.cmd.Mouse2 && move.cmd.Mouse2Diff) {
            controls.gear--;
            if(controls.gear < -1) controls.gear = -1;
        }
        
        move.ps.commandTime = move.cmd.serverTime;
        move.ps.origin.set(r.currentOrigin);
    }
    
    public ArrayList<String> getInfoStrings() {
        ArrayList<String> info = new ArrayList<String>();
        
        info.add("Speed: " + (int)vehicle.getCurrentSpeedKmHour() + " km/h");
//        info.add(vehicle.getWheelInfo(0).getRaySuspensionLength()+ "");
        info.add("RPM: " + state.rpm);
        //info.add("Torque: " + state.engineForce);
        info.add("Gear: " + controls.gear);
        info.add((controls.brake>0?" ^2BRK":"") + (controls.throttle>0?" ^3THR":""));
//        Vector3f rayEnd = vehicle.getWheelInfo(0).getRayContactPoint();
//        Vector3f rayStart = vehicle.getWheelInfo(0).getRayHardPoint();
//        rayEnd.scale(DefaultPhysics.INV_SCALE_FACTOR);
//        rayStart.scale(DefaultPhysics.INV_SCALE_FACTOR);
        return info;
    }

    public RaycastVehicle getRaycastVehicle() {
        return vehicle;
    }
    
    private class TorqueCurve {
        int[] rpms;
        float[] torques;

        public TorqueCurve(int[] rpm, float[] torque) {
            rpms = rpm;
            torques = torque;
        }
        
        float getTorque(int rpm) {
            int min = 0, max = 0;
            for (int i = 0; i < rpms.length; i++) {
                if(rpms[i] < rpm) min = i;
                if(rpms[i] > rpm && max == 0) max = i;
            }
            float frac =  (float)(rpm - rpms[min]) / (rpms[max] - rpms[min]);
            return torques[min] + frac * (torques[max] - torques[min]);
        }
    }
    
    
    private void calcEngine() {
        
        float wheelrot = (vehicle.getCurrentSpeedKmHour() / 3.6f) / setup.wheelRadius;
        float rpm = (float) (wheelrot * setup.getGearRatio(controls.gear) * setup.differential * 60f / (2f * Math.PI));
        if(rpm < 1000) rpm = 1000;
        
          
        float availableTorque = setup.torqueCurve.getTorque((int)rpm);
        if(rpm > 7000) {
            availableTorque = 0;
        }

        float transmission = setup.getGearRatio(controls.gear) * setup.differential * setup.transmissionEfficiency;
        float engineforce = availableTorque * controls.throttle;
        state.rpm = rpm;
        state.engineForce = (engineforce * transmission);
    }
    
    @Override
    public void runItem() {
        runThink();
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
        s.contents = Content.PHYSICS;
        ClipMask = Content.SOLID;
        this.use = this;
        this.think = this;

        IQMModel model = Ref.ResMan.loadModel("data/models/evo.iqm");
        r.maxs.set(model.getMaxs());
        r.mins.set(model.getMins());
        r.bmodel = true;
        r.s.modelindex = Ref.server.registerModel("data/models/evo.iqm");
        lastthink = nextthink = Ref.game.level.time;
        
        Link();
        
        wheelEntites = new Gentity[4];
        for (int j = 0; j < 4; j++) {
            wheelEntites[j] = Ref.game.Spawn();
            wheelEntites[j].classname = "wheel";
            wheelEntites[j].s.eType = EntityType.GENERAL;
            wheelEntites[j].r.mins.set(-3,-3,-3);
            wheelEntites[j].r.maxs.set(-3,-3,-3);
            wheelEntites[j].s.contents = Content.PHYSICS;
            wheelEntites[j].r.bmodel = true;
            wheelEntites[j].s.modelindex = Ref.server.registerModel("data/models/wheel.iqm");
            wheelEntites[j].Link();
        }
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
    
    //
    public VehicleControls getControls() {
        return controls;
    }

    public VechileSetup getSetup() {
        return setup;
    }

    public VehicleState getState() {
        return state;
    }
    
        public class VehicleControls {
        // Current player input
        public float throttle; // 0-1
        public float brake; // 0-1
        public float steering; // -1 -> 1?
        public int gear = 1;
    }
    public enum DriveTrain {
            FRONT,
            BACK,
            ALL
        }
    public class VechileSetup {
        TorqueCurve torqueCurve = new TorqueCurve(new int[] {1000,3500,4600,6000,6100}, new float[] {390f, 450f,470f, 390f,0f});
        DriveTrain driveTrain = DriveTrain.ALL;
        float wheelRadius = 0.34f;
	float wheelWidth = 0.3f;
        
        // Suspension
        float suspensionRestLength = 0.65f; // maximum length
        float suspensionMaxTravel = 0.6f * 100f; // max suspension compression
        float suspensionStiffness = 20.f; // 10 = offroad, 50 = sport, 200 = f1
        float suspensionCompression = 4.4f;
        float suspensionDamping = 2.3f;
        float wheelsSuspensionForce = 10000;
        
	float wheelFriction = 100.4f;// ~1 for realistic, 10000 for kart style
	float rollInfluence = 0.1f;// 1 = physical behavior, 0 = no behavior
        
        
        float brakeForce = 50;
        float brakeBias = 0.6f; // to the front
        
        float drag = 0.4257f; // drag constant
        float rollResistance = drag * 30;
        float weight = 1400; // kg
        float lenght = 2f; // meters
        float fracToBack = 0.5f; // center -> back == lenght * fracToBack
        float height = 1f; // meters

        float transmissionEfficiency = 0.7f;
        float differential = 3.42f;
        float getGearRatio(int gear) {
            if(gear == 0) return 0f;
            if(gear == -1) return gearRatios[0];
            if(gear >= gearRatios.length) gear = gearRatios.length-1;
            return gearRatios[gear];
        }
        float gearRatios[] = new float[] {
            -2.90f, // 0 = reverse
            2.66f,
            1.78f,
            1.30f,
            1.0f,
            0.74f,
            0.50f
        };

        private VehicleTuning getTuning() {
            VehicleTuning tuning = new VehicleTuning();
            tuning.suspensionStiffness = suspensionStiffness;
            tuning.maxSuspensionForce = wheelsSuspensionForce;
            tuning.suspensionCompression = suspensionCompression;
            tuning.suspensionDamping = suspensionDamping;
            tuning.frictionSlip = wheelFriction;
            return tuning;
        }
    }

    public class VehicleState {
        public float rpm;
        public float engineForce;
        
    }

}
