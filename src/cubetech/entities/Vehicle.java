package cubetech.entities;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.vehicle.DefaultVehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.dynamics.vehicle.VehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.VehicleTuning;
import com.bulletphysics.dynamics.vehicle.WheelInfo;
import com.bulletphysics.linearmath.Transform;
import cubetech.Game.GPhysicsEntity;
import cubetech.Game.GameClient;
import cubetech.Game.Gentity;
import cubetech.Game.PhysicsSystem;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.IThinkMethod;
import cubetech.common.IUseMethod;
import cubetech.common.MoveQuery;
import cubetech.common.Quaternion;
import cubetech.common.Trajectory;
import cubetech.iqm.IQMModel;
import cubetech.misc.Ref;
import java.util.ArrayList;
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
    private VehicleRaycaster vehicleRayCaster;
    
    private Gentity[] wheelEntites;
    
    private int lastGroundTime;
    
    
    public void initVehicle() {
        if(physicsBody == null) updateFromPhysics();
        
        Vector3f tmphalfSize = Vector3f.sub(r.maxs, r.mins, null);
        tmphalfSize.scale(0.5f);
        javax.vecmath.Vector3f halfSize = PhysicsSystem.toScaledVecmath(tmphalfSize, null);
        
        
        {
            VehicleTuning tuning = new VehicleTuning();
            DynamicsWorld dynamicsWorld = Ref.game.level.physics.world;
            RigidBody carChassis = physicsBody;
            vehicleRayCaster = new DefaultVehicleRaycaster(dynamicsWorld);
            vehicle = new RaycastVehicle(tuning,carChassis,vehicleRayCaster);

            // never deactivate the vehicle
            carChassis.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
            dynamicsWorld.addVehicle(vehicle);

            float connectionHeight = -0.4f;
            boolean isFrontWheel=true;
            javax.vecmath.Vector3f wheelDirectionCS0 = new javax.vecmath.Vector3f(0,0,-1);
            javax.vecmath.Vector3f wheelAxleCS = new javax.vecmath.Vector3f(0,-1,0);

            // choose coordinate system
            vehicle.setCoordinateSystem(0,1,2);
            
            javax.vecmath.Vector3f connectionPointCS0 = new javax.vecmath.Vector3f(halfSize.x-setup.wheelRadius*2.8f,halfSize.y-(0.3f*setup.wheelWidth),connectionHeight);
            vehicle.addWheel(connectionPointCS0,wheelDirectionCS0,wheelAxleCS,setup.suspensionRestLength,setup.wheelRadius,tuning,isFrontWheel);
            connectionPointCS0 = new javax.vecmath.Vector3f(halfSize.x-setup.wheelRadius*2.8f,-halfSize.y+(0.3f*setup.wheelWidth),connectionHeight);
            vehicle.addWheel(connectionPointCS0,wheelDirectionCS0,wheelAxleCS,setup.suspensionRestLength,setup.wheelRadius,tuning,isFrontWheel);

            connectionPointCS0 = new javax.vecmath.Vector3f(-halfSize.x+setup.wheelRadius*3.5f,-halfSize.y+(0.3f*setup.wheelWidth),connectionHeight);
            isFrontWheel = false;
            vehicle.addWheel(connectionPointCS0,wheelDirectionCS0,wheelAxleCS,setup.suspensionRestLength,setup.wheelRadius,tuning,isFrontWheel);
            connectionPointCS0 = new javax.vecmath.Vector3f(-halfSize.x+setup.wheelRadius*3.5f,halfSize.y-(0.3f*setup.wheelWidth),connectionHeight);
            vehicle.addWheel(connectionPointCS0,wheelDirectionCS0,wheelAxleCS,setup.suspensionRestLength,setup.wheelRadius,tuning,isFrontWheel);

            applyWheelSetup();
        }
    }
    
    public ArrayList<String> getInfoStrings() {
        ArrayList<String> info = new ArrayList<String>();
        
        info.add("Speed: " + (int)vehicle.getCurrentSpeedKmHour() + " km/h");
        info.add(vehicle.getWheelInfo(0).raycastInfo.suspensionLength+ "");
        info.add("RPM: " + state.rpm);
        info.add("Torque: " + state.engineForce);
        info.add("Gear: " + controls.gear);
        info.add((controls.brake>0?" ^2BRK":"") + (controls.throttle>0?" ^3THR":""));
        return info;
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
    
    private void applyWheelSetup() {
        for (int i=0;i<vehicle.getNumWheels();i++)
        {
            WheelInfo wheel = vehicle.getWheelInfo(i);
            wheel.suspensionStiffness = setup.suspensionStiffness;
            wheel.wheelsDampingRelaxation = setup.suspensionDamping;
            wheel.wheelsDampingCompression = setup.suspensionCompression;
            wheel.frictionSlip = setup.wheelFriction;
            wheel.rollInfluence = setup.rollInfluence;
            wheel.wheelsSuspensionForce = setup.wheelsSuspensionForce;
        }
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
        
        boolean engineBackWheels = (setup.driveTrain == DriveTrain.ALL || setup.driveTrain == DriveTrain.BACK);
        boolean engineFrontWheels = (setup.driveTrain == DriveTrain.ALL || setup.driveTrain == DriveTrain.FRONT);
        
        
        int wheelIndex = 2;
        if(engineBackWheels) vehicle.applyEngineForce(state.engineForce,wheelIndex);
        vehicle.setBrake(controls.brake * setup.brakeForce * (1f-setup.brakeBias),wheelIndex);
        
        wheelIndex = 3;
        if(engineBackWheels) vehicle.applyEngineForce(state.engineForce,wheelIndex);
        vehicle.setBrake(controls.brake * setup.brakeForce * (1f-setup.brakeBias),wheelIndex);

        wheelIndex = 0;
        vehicle.setSteeringValue(controls.steering,wheelIndex);
        if(engineFrontWheels) vehicle.applyEngineForce(state.engineForce,wheelIndex);
        vehicle.setBrake(controls.brake * setup.brakeForce * (setup.brakeBias),wheelIndex);
        
        wheelIndex = 1;
        if(engineFrontWheels) vehicle.applyEngineForce(state.engineForce,wheelIndex);
        vehicle.setSteeringValue(controls.steering,wheelIndex);
        vehicle.setBrake(controls.brake * setup.brakeForce * (setup.brakeBias),wheelIndex);
        
        updateFromPhysics();
        
        for (int i = 0; i < 4; i++) {
            vehicle.updateWheelTransform(i, true);
            Transform t = vehicle.getWheelInfo(i).worldTransform;
            PhysicsSystem.toUnscaledVec(t.origin, wheelEntites[i].r.currentOrigin);
            wheelEntites[i].r.mins.set(-16,-16,-16);
            wheelEntites[i].r.maxs.set(16,16,16);
            wheelEntites[i].r.contents = Content.SOLID;
            wheelEntites[i].s.pos.type = Trajectory.INTERPOLATE;
            wheelEntites[i].s.pos.base.set(wheelEntites[i].r.currentOrigin);
            wheelEntites[i].s.apos.type = Trajectory.QUATERNION;
            Quaternion.setFromMatrix(t.basis, wheelEntites[i].s.apos.quater);
            wheelEntites[i].Link();
        }
            
        Link();
        lastthink = nextthink = Ref.game.level.time;
    }
    
    private void checkCarFlipped() {
        boolean frontOnGround = false;
        boolean backOnGround = false;
        for (WheelInfo wheelInfo : vehicle.wheelInfo) {
            if(wheelInfo.raycastInfo.isInContact) {
                if(wheelInfo.bIsFrontWheel) frontOnGround = true;
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
        tr.setIdentity();
        Transform oldTr = physicsBody.getMotionState().getWorldTransform(new Transform());
        tr.origin.set(oldTr.origin);
        tr.origin.z += 2f;
        physicsBody.setCenterOfMassTransform(tr);
        
        physicsBody.setLinearVelocity(new javax.vecmath.Vector3f(1,0,0));
        physicsBody.setAngularVelocity(new javax.vecmath.Vector3f(0,0,0));
        Ref.game.level.physics.world.getBroadphase().getOverlappingPairCache().cleanProxyFromPairs(physicsBody.getBroadphaseHandle(),Ref.game.level.physics.world.getDispatcher());
        if (vehicle != null)
        {
                vehicle.resetSuspension();
                for (int i=0;i<vehicle.getNumWheels();i++)
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
        controls.throttle = move.cmd.Forward?1:0;
        controls.brake = move.cmd.Back?1:0;
        
        float steerInput = (move.cmd.Left?1:0) + (move.cmd.Right?-1:0);
        
        float steerDelta = steerInput - controls.steering;
        
        // Modify steering delta
        boolean noInput = (move.cmd.Left == false && move.cmd.Right == false);
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
        r.contents = Content.PHYSICS;
        ClipMask = Content.SOLID;
        this.use = this;
        this.think = this;

        IQMModel model = Ref.ResMan.loadModel("data/models/datcar.iqm");
        r.maxs.set(model.getMaxs());
        r.mins.set(model.getMins());
        physicsObject = true;
        r.bmodel = true;
        r.s.modelindex = Ref.server.registerModel("data/models/datcar.iqm");
        lastthink = nextthink = Ref.game.level.time;
        
        Link();
        
        wheelEntites = new Gentity[4];
        for (int j = 0; j < 4; j++) {
            wheelEntites[j] = Ref.game.Spawn();
            wheelEntites[j].classname = "wheel";
            wheelEntites[j].s.eType = EntityType.GENERAL;
            wheelEntites[j].r.mins.set(-3,-3,-3);
            wheelEntites[j].r.maxs.set(-3,-3,-3);
            wheelEntites[j].r.contents = Content.PHYSICS;
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
        DriveTrain driveTrain = DriveTrain.FRONT;
        float wheelRadius = 0.34f;
	float wheelWidth = 0.3f;
        float suspensionRestLength = 0.65f;
        
	float wheelFriction = 1.4f;//1e30f;
	float suspensionStiffness = 80.f;
	
	float suspensionCompression = 4.4f;
        float suspensionDamping = 2.3f;
	float rollInfluence = 0.1f;//1.0f;
        float wheelsSuspensionForce = 10000;
        
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
    }

    public class VehicleState {
        public float rpm;
        public float engineForce;
        
    }

}
