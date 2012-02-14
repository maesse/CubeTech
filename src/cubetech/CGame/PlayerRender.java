package cubetech.CGame;

import cubetech.Game.Game;
import cubetech.client.LocalClient;
import cubetech.common.Animations;
import cubetech.common.CVar;
import cubetech.common.Common;
import cubetech.common.Content;
import cubetech.common.Helper;
import cubetech.common.PlayerState;
import cubetech.common.items.Weapon;
import cubetech.common.items.WeaponInfo;
import cubetech.common.items.WeaponItem;
import cubetech.common.items.WeaponState;
import cubetech.entities.EntityFlags;
import cubetech.gfx.CubeMaterial;
import cubetech.gfx.PolyVert;
import cubetech.iqm.BoneAttachment;
import cubetech.iqm.BoneController;
import cubetech.iqm.BoneMeshInfo;
import cubetech.iqm.IQMAnim;
import cubetech.iqm.IQMFrame;
import cubetech.iqm.IQMJoint;
import cubetech.iqm.IQMModel;
import cubetech.iqm.RigidBoneMesh;
import cubetech.misc.Ref;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import nbullet.collision.shapes.CollisionShape;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * Deals with client-side rendering and physics for player models, weapons, etc.
 * @author Mads
 */
public class PlayerRender {
    private CGame game;
    private CVar cg_showbmesh = Ref.cvars.Get("cg_showbmesh", "0", null);
    
    public PlayerRender(CGame game) {
        this.game = game;
    }
    
    // Render a player
    public void renderPlayer(CEntity cent) {
        ClientInfo ci = game.cgs.clientinfo[cent.currentState.number];
        if(!ci.infoValid) return;
        if((cent.currentState.eFlags & EntityFlags.NODRAW) != 0) return;
        RenderEntity ent = Ref.render.createEntity(REType.MODEL);
        
        
        
        
        IQMModel model = Ref.ResMan.loadModel(ci.modelName);
        
        if(model == null) return;
        if(cent.pe.boneMeshModel != null) {
            boolean wasdead = false;
            boolean isdead = false;
                    
            if(cent.currentState.contents != cent.pe.lastcontents) {
            
                wasdead = (cent.pe.lastcontents & Content.CORPSE) != 0;
                isdead = (cent.currentState.contents & Content.CORPSE) != 0;
            } 
//            else {
//                for (int i = 0; i < Ref.cgame.cg.localClients.length; i++) {
//                    LocalClient cl = Ref.cgame.cg.localClients[i];
//                    if(cl.validPPS && cl.clientNum == cent.currentState.number) {
//                        wasdead = cl.wasDead;
//                        isdead = cl.predictedPlayerState.stats.Health <= 0;
//                        break;
//                    }
//                }
//            }
            if(wasdead != isdead) {
                if(!wasdead && isdead) {
                    // Make ragdoll
                    makeRagdoll(cent);
                    Common.Log("Making ragdoll!");
                } else if(wasdead && !isdead) {
                    // restore bonemeshes
                    unmakeRagdoll(cent);
                    Common.Log("Unmaking ragdoll!");
                }
            }
        }
        cent.pe.lastcontents = cent.currentState.contents;
        
        
        // set origin & angles
        ent.origin.set(cent.lerpOrigin);
        ent.origin.z += Game.PlayerMins.z;

        Vector3f angles = new Vector3f(1,0,0);

        angles.set(cent.lerpAngles);
        if(angles.x > 1) angles.x = 1;
        if(angles.x < -25) angles.x = -25;
        ent.axis = Helper.AnglesToAxis(angles, ent.axis);

        ArrayList<BoneController> controllers = playerAngles(cent, ent);

        playerAnimation(cent, ent);
        handleMeshBonesPre(cent, ent, model, controllers);
        ent.model = model.buildFrame(ent.frame, ent.oldframe, ent.backlerp, controllers);
        
        handleMeshBonesPost(cent, ent);
        
        

        // Fade out when close to camera
        Vector3f cameraOrigin = Ref.cgame.cg.refdef.Origin;
        Vector3f entityOrigin = ent.origin;

        float distance = Helper.VectorDistance(cameraOrigin, entityOrigin);
        float maxdist = 60; // where fadeout starts
        float mindist = 30; // where model is completely transparent
        if(distance < maxdist) {
            distance -= mindist;
            if(distance < 2) distance = 2;
            distance /= maxdist-mindist;
        }

        ent.color = new Vector4f(255, 255, 255, 255f);

        Ref.render.addRefEntity(ent);
        addWeaponToPlayer(cent, ent);
        
    }
    
    
    
    // mostly deals with syncing the animation to the current physics state.
    // need to be done before iqmframe creation for proper handling of childbones
    private void handleMeshBonesPre(CEntity cent, RenderEntity ent, IQMModel model, ArrayList<BoneController> controllers) {
        // Check for model change
        boolean hasRigidBoneMeshes = cent.pe.boneMeshes != null;
        boolean hasBoneMeshes = !model.boneMeshInfo.isEmpty();
        if(hasRigidBoneMeshes && cent.pe.boneMeshModel != model) {
            game.cleanPhysicsFromCEntity(cent);
            game.centitiesWithPhysics.remove(cent);
            hasRigidBoneMeshes = false;
            //Common.LogDebug("Cleaned physics from centity");
        }
        
        if(hasRigidBoneMeshes && hasBoneMeshes) {
            // Update rigid bodies
            for (RigidBoneMesh rigidBoneMesh : cent.pe.boneMeshes) {
                // Ensure that the bones are connected appropiately
                if(rigidBoneMesh.info.type != BoneMeshInfo.Type.RIGID) {
                    rigidBoneMesh.connectToParent(cent);
                }
                
                if(!rigidBoneMesh.isBoneToPhysics()) {
                    // Read transforms from physicssystem
                    // Undo scale & transpose axis
                    Matrix4f meshMatrix = rigidBoneMesh.rigidBody.getMotionState().getWorldTransform((Matrix4f)null);
                    meshMatrix.m30 *= CGPhysics.INV_SCALE_FACTOR;
                    meshMatrix.m31 *= CGPhysics.INV_SCALE_FACTOR;
                    meshMatrix.m32 *= CGPhysics.INV_SCALE_FACTOR;
                    Helper.transposeAxis(meshMatrix);

                    // Bring into model space
                    Matrix4f modelMatrix = Helper.getModelMatrix(ent.axis, ent.origin, null);
                    modelMatrix.invert();
                    Matrix4f poseMatrix = Matrix4f.mul(modelMatrix, meshMatrix, null);
                    Matrix4f boneMatrix = rigidBoneMesh.getBoneFromPosedModelMatrix(poseMatrix, null);
                    
                    // Override the current bone pose with a bonecontroller
                    BoneController controller = new BoneController(BoneController.Type.ABSOLUTE, 
                            rigidBoneMesh.getJoint().getName(), boneMatrix);
                    controllers.add(controller);
                }
            }
        }
    }
    
    // Requires that there's a bonemesh for the models rootbone
    public void makeRagdoll(CEntity cent) {
        boolean hasRigidBoneMeshes = cent.pe.boneMeshes != null;
        if(hasRigidBoneMeshes) {
            for (RigidBoneMesh rigidBoneMesh : cent.pe.boneMeshes) {
                if(rigidBoneMesh.isBoneToPhysics() && rigidBoneMesh.getJoint().getParent() < 0) {
                    // Found root bone
                    rigidBoneMesh.setBoneToPhysics(false);
                } else {
                    // make into a flexible meshbone
                    BoneMeshInfo newinfo = rigidBoneMesh.info.clone(null);
                    newinfo.type = BoneMeshInfo.Type.FLEXIBLE;
                    rigidBoneMesh.changeInfo(newinfo, cent);
                }
            }
        }
        
    }
    
    // unragdollify
    private void unmakeRagdoll(CEntity cent) {
        boolean hasRigidBoneMeshes = cent.pe.boneMeshes != null;
        if(hasRigidBoneMeshes) {
            for (RigidBoneMesh rigidBoneMesh : cent.pe.boneMeshes) {
                if(rigidBoneMesh.isBoneToPhysics() && rigidBoneMesh.getJoint().getParent() < 0) {
                    // Found root bone
                    rigidBoneMesh.setBoneToPhysics(true);
                } else {
                    // Wipe any old constraints and recreate original
                    rigidBoneMesh.restoreInfo(cent);
                    // if the original info was rigid, assume it was animation->physics
                    if(rigidBoneMesh.info.type == BoneMeshInfo.Type.RIGID) {
                        rigidBoneMesh.setBoneToPhysics(true);
                    }
                }
            }
        }
    }
    
    // Mostly deals with syncronizing the physics system to the animations
    private void handleMeshBonesPost(CEntity cent, RenderEntity ent) {
        IQMFrame frame = ent.model;
        IQMModel model = frame.getModel();
        ArrayList<BoneMeshInfo> boneMeshes = model.boneMeshInfo;
        
        boolean hasRigidBoneMeshes = cent.pe.boneMeshes != null;
        boolean hasBoneMeshes = !boneMeshes.isEmpty();
        
        if(!hasRigidBoneMeshes && hasBoneMeshes) {
            // Set up new rigidbonemeshes
            cent.pe.boneMeshModel = model;
            cent.pe.boneMeshes = new ArrayList<RigidBoneMesh>();
            
            // Add each mesh according to it's info
            for (BoneMeshInfo boneMeshInfo : boneMeshes) {
                // prepare the mesh
                RigidBoneMesh boneMesh = new RigidBoneMesh(game.physics.world, model, boneMeshInfo);
                cent.pe.boneMeshes.add(boneMesh);
                
                Matrix4f modelMatrix = Helper.getModelMatrix(ent.axis, ent.origin, null);
                // create it using the current frame
                boneMesh.createRigidBody(modelMatrix, frame);
            }
            //Common.LogDebug("Added physics for centity");
            game.centitiesWithPhysics.add(cent);
        } else if(hasRigidBoneMeshes && hasBoneMeshes) {
            // Update rigid bodies
            for (RigidBoneMesh rigidBoneMesh : cent.pe.boneMeshes) {
                if(rigidBoneMesh.isBoneToPhysics()) {
                    // Put current bone transforms into the physics system
                    Matrix4f poseMatrix = rigidBoneMesh.getPosedModelToBone(frame, null);
                    Matrix4f modelMatrix = Helper.getModelMatrix(ent.axis, ent.origin, null);
                    Matrix4f.mul(modelMatrix, poseMatrix, modelMatrix);
                    Helper.transposeAxis(modelMatrix);
                    modelMatrix.m30 *= CGPhysics.SCALE_FACTOR;
                    modelMatrix.m31 *= CGPhysics.SCALE_FACTOR;
                    modelMatrix.m32 *= CGPhysics.SCALE_FACTOR;
                    rigidBoneMesh.rigidBody.getMotionState().setWorldTransform(modelMatrix);
                    
                }
                
                // Visualize the physicsmesh:
                if(cg_showbmesh.isTrue()) {
                    RenderEntity rent = Ref.render.createEntity(REType.POLY);
                    rent.flags = RenderEntity.FLAG_NOLIGHT | RenderEntity.FLAG_NOSHADOW | RenderEntity.FLAG_SPRITE_AXIS;
                    Matrix4f modelMatrix = rigidBoneMesh.rigidBody.getMotionState().getWorldTransform((Matrix4f)null);
                    Helper.matrixToAxis(modelMatrix, rent.axis);
                    Helper.transform(modelMatrix, new Vector3f(0, 0, 0), rent.origin);
                    rent.origin.scale(CGPhysics.INV_SCALE_FACTOR);
                    Vector3f[] localmesh = rigidBoneMesh.getLocalMesh();
                    rent.frame = localmesh.length;
                    rent.oldframe = GL11.GL_LINE_STRIP;
                    rent.verts = new PolyVert[rent.frame];
                    for (int i = 0; i < rent.frame; i++) {
                        rent.verts[i] = new PolyVert();
                        rent.verts[i].xyz = localmesh[i];
                    }
                    Ref.render.addRefEntity(rent);
                }
            }
        }
        
        
        // Go over each potential bonemesh and create it if it isn't already
        
    }
    
    private void addWeaponToPlayer(CEntity cent, RenderEntity ent) {
        // Add player weapon
        if(cent.currentState.weapon == Weapon.NONE) return;

        WeaponItem w = Ref.common.items.getWeapon(cent.currentState.weapon);
        IQMModel weaponModel = w.getWeaponInfo().worldModel;
        if(weaponModel == null || ent.model == null) return;

        
        BoneAttachment bone = ent.model.getAttachment("weapon");
        if(bone == null) return;

        Vector3f boneOrigin = new Vector3f(bone.lastposition.x
                    , bone.lastposition.y
                    , bone.lastposition.z);

        RenderEntity went = Ref.render.createEntity(REType.MODEL);
        went.color.set(255,255,255,255);
        
        went.model = weaponModel.buildFrame(0, 0, 0, null);

        Helper.transform(ent.axis, ent.origin, boneOrigin);
        went.origin.set(boneOrigin);

        Vector3f[] test = new Vector3f[3];
        test[2] = new Vector3f(3,0,0);
        test[0] = new Vector3f(0,-3,0);
        test[1] = new Vector3f(0,0,-3);
        

        Helper.mul(test, bone.axis, test);
        Helper.mul(test, ent.axis, test);

        went.axis = test;

        Ref.render.addRefEntity(went);
    }

    private void playerAnimation(CEntity cent, RenderEntity ent) {
        float speedScale = 1.0f;
        
        ClientInfo ci = Ref.cgame.cgs.clientinfo[cent.currentState.number];
        // Change animation based on what weapon the client has
        // Figure out what animation was requested
        Animations anim = cent.currentState.frameAsAnimation();
        if(anim == null) {
            Common.LogDebug("Invalid animation %d", cent.currentState.frame);
            return;
        }
        if(cent.currentState.weapon == Weapon.AK47) {
            if(anim == Animations.IDLE) anim = Animations.IDLE_GUN1;
            if(anim == Animations.WALK) anim = Animations.WALK_GUN1;
        }
        runLerpFrame(ci, cent.pe.torso, anim.ordinal() | (cent.currentState.frame & 128), speedScale);

        ent.oldframe = cent.pe.torso.oldFrame;
        ent.frame = cent.pe.torso.frame;
        ent.backlerp = cent.pe.torso.backlerp;
    }
    
    private void runLerpFrame(ClientInfo ci, LerpFrame lf, int newanimation, float speedScale) {
        // see if the animation sequence is switching
        if(newanimation != lf.animationNumber || lf.animation == null) {
            setLerpFrameAnimation(ci, lf, newanimation);
        }

        if(lf.animation == null) {
            return; // no animation for you!
        }

        // if we have passed the current frame, move it to
	// oldFrame and calculate a new frame
        if(Ref.cgame.cg.time >= lf.frametime) {
            lf.oldFrame = lf.frame;
            lf.oldFrameTime = lf.frametime;

            // get the next frame based on the animation
            IQMAnim anim = lf.animation;
            if(anim.frameLerp == 0) return; // shouldn't happen

            if(Ref.cgame.cg.time < lf.animationTime) {
                lf.frametime = lf.animationTime;
            } else {
                lf.frametime = lf.oldFrameTime + anim.frameLerp;
            }
            float f = (lf.frametime - lf.animationTime) / anim.frameLerp;
            f *= speedScale;

            int numFrames = anim.num_frames;
            if(f >= numFrames) {
                f -= numFrames;
                if(anim.loopFrames != 0) {
                    f %= anim.loopFrames;
                    f += anim.num_frames - anim.loopFrames;
                } else {
                    f = numFrames - 1;
                    // the animation is stuck at the end, so it
                    // can immediately transition to another sequence
                    lf.frametime = Ref.cgame.cg.time;
                }
            }

            if(f < 0) f = -f;
            lf.frame = (int) (anim.first_frame + f);
            if(Ref.cgame.cg.time > lf.frametime) {
                lf.frametime = Ref.cgame.cg.time;

            }
        }

        if(lf.frametime > Ref.cgame.cg.time + 200) lf.frametime = Ref.cgame.cg.time;
        if(lf.oldFrameTime > Ref.cgame.cg.time) lf.oldFrameTime = Ref.cgame.cg.time;

        // calculate current lerp value
        if(lf.frametime == lf.oldFrameTime) {
            lf.backlerp = 0;
        } else {
            lf.backlerp = 1f - (float)(Ref.cgame.cg.time - lf.oldFrameTime) / (lf.frametime - lf.oldFrameTime);
        }
    }

    private void setLerpFrameAnimation(ClientInfo ci, LerpFrame lf, int newanimation) {
        lf.animationNumber = newanimation;
        
        // Figure out what animation was requested
        newanimation &= ~128;
        if(newanimation < 0 || newanimation >= Animations.values().length) {
            Common.LogDebug("Invalid animation %d", newanimation);
            return;
        }
        Animations animation = Animations.values()[newanimation];

        // Try to load the model
        IQMModel model = Ref.ResMan.loadModel(ci.modelName);
        if(model == null) {
            Common.LogDebug("Can't find model %s", ci.modelName);
            return;
        }

        // Check if we have a valid animation
        IQMAnim anim = model.getAnimation(animation);
        
        if(anim == null) {
            //Common.LogDebug("Can't find animation %s in model %s", animation.toString(), ci.modelName);
            if(model.anims == null) return;

            // Don't have the right animation, just grab the first
            anim = model.anims[0];
        }

        lf.animation = anim;
        lf.animationTime = lf.frametime + anim.initialLerp;

    }

    private static final int[] moveOffsets = new int[] {0,22,45,-22,0,22,-45,-22};
    private ArrayList<BoneController> playerAngles(CEntity cent, RenderEntity rent) {
        Vector3f headAngle = new Vector3f(cent.lerpAngles);
        CVar noangles = Ref.cvars.Find("cg_noangles");
        if(noangles != null && noangles.isTrue()) {
            headAngle.set(1,0,0);
        }
        headAngle.y = Helper.AngleMod(headAngle.y);

        int dir = (int)cent.currentState.Angles2.y;
        if(dir < 0 || dir > 7) {
            Common.LogDebug("Invalid movedirection %d", dir);
            return null;
        }
        Vector3f legsAngle = new Vector3f();
        Vector3f torsoAngle = new Vector3f();
        
        if(cent.pe.lastRenderFrame != Ref.client.framecount) {
            cent.pe.legs.pitching = false;
            cent.pe.torso.pitching = false;
            
            // yaw
            torsoAngle.y = headAngle.y + 0.35f * moveOffsets[dir];
            legsAngle.y = headAngle.y + moveOffsets[dir];
            swingAngles(torsoAngle.y, 10, 90, game.cg_swingspeed.fValue, cent.pe.torso, true, 1f); // yaw
            Animations anim = cent.currentState.frameAsAnimation();
            if(anim != null && anim == Animations.IDLE) {
                cent.pe.legs.yawing = false;
                swingAngles(legsAngle.y, 20, 110, game.cg_swingspeed.fValue*0.5f, cent.pe.legs, true, 1f); // yaw
            } else if(cent.pe.legs.yawing) {
                swingAngles(legsAngle.y, 0, 110, game.cg_swingspeed.fValue, cent.pe.legs, true, 1f); // yaw
            } else {
                swingAngles(legsAngle.y, 40, 110, game.cg_swingspeed.fValue, cent.pe.legs, true, 1f); // yaw
            }
        }
        cent.pe.lastRenderFrame = Ref.client.framecount;
        
        torsoAngle.y = cent.pe.torso.yawAngle;
        legsAngle.y = cent.pe.legs.yawAngle;

        // pitch
        float dest;
        if(headAngle.x < 0 ) {
            dest = (headAngle.x) * 0.75f;
            if(dest < -60) dest = -60;
        } else {
            dest = headAngle.x * 0.75f;
            if(dest> 60) dest = 60;
        }
        
        swingAngles(dest, 0, 30, 0.1f, cent.pe.torso, false, 1f);
        headAngle.x = cent.pe.torso.pitchAngle;
        BoneController hips = new BoneController(BoneController.Type.ADDITIVE, "Hip", new Vector3f(-torsoAngle.x, legsAngle.y-torsoAngle.y, 0));
        BoneController head = new BoneController(BoneController.Type.ADDITIVE, "Spine", new Vector3f(0,torsoAngle.x-headAngle.x,0));
        rent.axis = Helper.AnglesToAxis(new Vector3f(0, torsoAngle.y, torsoAngle.z), rent.axis);
        ArrayList<BoneController> controllers = new ArrayList<BoneController>();
        //controllers.add(hips);
        controllers.add(head);
        return controllers;
    }

    private void swingAngles(float dest, int swingTolerance, int clampTolerance,
            float speed, LerpFrame out, boolean isYaw, float swingScale) {
        boolean swinging = isYaw?out.yawing:out.pitching;
        float angle = isYaw?out.yawAngle:out.pitchAngle;
        float swing;
        if(!swinging) {
            // see if a swing should be started
            swing = Helper.AngleSubtract(angle, dest) * swingScale;
            if(swing > swingTolerance || swing < -swingTolerance) {
                swinging = true;
            }
        }

        if(!swinging) {
            return;
        }

        // modify the speed depending on the delta
	// so it doesn't seem so linear
        swing = Helper.AngleSubtract(dest, angle) ;
        float scale = Math.abs(swing);
        scale /= (float)clampTolerance;
//        scale /= swingTolerance;
        if(scale < 0.0) scale = 0.0f;
        if(scale > 1.0) scale = 1.0f;
        float minscale = 0.1f;
        scale *= 1f/(1f+minscale);
        scale += minscale;
        scale = Helper.SimpleSpline(scale);
        

        // swing towards the destination angle
        float move;
        if(swing >=0) {
            move = game.cg.frametime * scale * speed;
            if(move >= swing) {
                move = swing;
                swinging = false;
            }
            angle = Helper.AngleMod(angle + move);
        } else if(swing < 0) {
            move = game.cg.frametime * scale * -speed;
            if(move <= swing) {
                move = swing;
                swinging = false;
            }
            angle = Helper.AngleMod(angle + move);
        }

        // clamp to no more than tolerance
        swing = Helper.AngleSubtract(dest, angle);
        if(swing > clampTolerance) {
            angle = Helper.AngleMod(dest - (clampTolerance -1));
        } else if(swing < -clampTolerance) {
            angle = Helper.AngleMod(dest + (clampTolerance-1));
        }

        if(isYaw) {
            out.yawing = swinging;
            out.yawAngle = angle;
        } else {
            out.pitching = swinging;
            out.pitchAngle = angle;
        }
    }
    
    public void renderViewModel(PlayerState ps) {
        // no gun if in third person view or a camera is active
        if (Ref.cgame.cg_tps.isTrue() || (Ref.cgame.cg_freecam.isTrue() && Ref.cgame.cg.playingdemo)) return;

        // don't draw if testing a gun model
        if(Ref.cgame.cg.testGun) return;

        RenderEntity ent = Ref.render.createEntity(REType.MODEL);
        ent.flags |= RenderEntity.FLAG_NOSHADOW | RenderEntity.FLAG_WEAPONPROJECTION;
        // get clientinfo for animation map
        WeaponItem wi = Ref.common.items.getWeapon(ps.weapon);
        if(wi == null) return;
        
        // Let weapon render any effects
        wi.renderClientEffects();

        
        WeaponInfo winfo = wi.getWeaponInfo();
        if(winfo == null || winfo.viewModel == null) return;
        IQMModel model = winfo.viewModel;
        

        Vector3f weaponAngles = new Vector3f();
        CalculateWeaponPosition(ent.origin, weaponAngles);
        Vector3f angles = new Vector3f();
        angles.set(game.cg.refdef.Angles);
        if(ps.weaponState == WeaponState.DROPPING) {
            float dropFrac = 1f - ((float)ps.weaponTime / wi.getDropTime());
            if(dropFrac < 0) dropFrac = 0;
            if(dropFrac > 1) dropFrac = 1f;
            angles.x += dropFrac * 90;
        } else if(ps.weaponState == WeaponState.RAISING) {
            float dropFrac = ((float)ps.weaponTime / wi.getRaiseTime());
            if(dropFrac < 0) dropFrac = 0;
            if(dropFrac > 1) dropFrac = 1f;
            angles.x += dropFrac * 90;
        }
        
        ent.axis = Helper.AnglesToAxis(angles, ent.axis);

        Helper.VectorMA(ent.origin, game.cg.cg_gun_x.fValue, ent.axis[0], ent.origin);
        Helper.VectorMA(ent.origin, game.cg.cg_gun_y.fValue, ent.axis[1], ent.origin);
        Helper.VectorMA(ent.origin, game.cg.cg_gun_z.fValue, ent.axis[2], ent.origin);
        boolean isFiring = ps.weaponState == WeaponState.FIRING &&
                    ps.stats.getAmmo(ps.weapon)>0;
        if(model.anims != null && model.anims.length > 0) {
            IQMAnim anim = model.anims[0];
            
            if(ps.weaponState == WeaponState.READY && model.getAnimation(Animations.READY) != null) {
                anim = model.getAnimation(Animations.READY);
            } else if(isFiring && model.getAnimation(Animations.FIRING) != null ) {
                anim = model.getAnimation(Animations.FIRING);
            }
            
            int num = anim.num_frames;
            int first = anim.first_frame;
            if(ps.weaponTime > 0 && isFiring) {
                float frac = 1f-(ps.weaponTime / (float)wi.getFireTime());
                ent.frame = (int) Math.ceil(frac * num);
                
                float lerp = frac * num;
                ent.backlerp = -(lerp-ent.frame);
//                ent.backlerp = 0f;
                ent.oldframe = ent.frame - 1;
                if(ent.oldframe < 0) ent.oldframe = num-1;
                
                ent.frame += first;
                ent.oldframe += first;
            } else {
                float left = (Ref.cgame.cg.time/50f) - (Ref.cgame.cg.time/50);
                ent.backlerp = 1f-left;
                ent.frame = (Ref.cgame.cg.time/50) % num;
                ent.oldframe = ent.frame - 1;
                if(ent.oldframe < 0) ent.oldframe = num-1;
                ent.frame += first;
                ent.oldframe += first;
            }
        }

        Ref.render.addRefEntity(ent);
        ArrayList<BoneController> ctrls = new ArrayList<BoneController>();
        Vector3f boneAngles = new Vector3f((float)Math.sin(Ref.cgame.cg.time/1000f)*16f,0,0);
        BoneController gunBone = new BoneController(BoneController.Type.ADDITIVE, "WeaponController", weaponAngles);
        ctrls.add(gunBone);
        ent.model = model.buildFrame(ent.frame, ent.oldframe, ent.backlerp, ctrls);
        BoneAttachment muzzleBone = ent.model.getAttachment("muzzle");
        if(muzzleBone == null || winfo.flashTexture == null) return;
//
//        // Add muzzle flash
        CEntity cent = Ref.cgame.cg.cur_lc.predictedPlayerEntity;
        if(Ref.cgame.cg.time - cent.muzzleFlashTime > 20) return;
//
        RenderEntity flash = Ref.render.createEntity(REType.SPRITE);
        flash.flags = RenderEntity.FLAG_SPRITE_AXIS | RenderEntity.FLAG_NOLIGHT | RenderEntity.FLAG_NOSHADOW | RenderEntity.FLAG_WEAPONPROJECTION;
        flash.mat = Ref.ResMan.LoadTexture(winfo.flashTexture).asMaterial();
        flash.mat.blendmode = CubeMaterial.BlendMode.ONE;
        flash.outcolor.set(255,255,255,255);
        flash.radius = 5f;
        Matrix4f modelMatrix = createModelMatrix(ent.axis, ent.origin, null);
        Vector4f vec = new Vector4f(muzzleBone.lastposition.x, muzzleBone.lastposition.y, muzzleBone.lastposition.z, 1);
        Matrix4f.transform(modelMatrix, vec, vec);
        flash.origin.set(vec.x, vec.y, vec.z);
        // Get attachment point
        Vector3f[] rotatedMuzzleAxis = new Vector3f[3];
        rotatedMuzzleAxis[0] = muzzleBone.axis[0];//muzzleBone.axis[1];
        rotatedMuzzleAxis[1] = muzzleBone.axis[2];
        rotatedMuzzleAxis[2] = muzzleBone.axis[0];
        Helper.rotateAroundDirection(rotatedMuzzleAxis, (Ref.rnd.nextFloat()-0.5f)*60f);
        Helper.mul(rotatedMuzzleAxis, ent.axis, flash.axis);
        Ref.render.addRefEntity(flash);
    }
    
    private void CalculateWeaponPosition(Vector3f position, Vector3f angles) {
        position.set(game.cg.refdef.Origin);
        
        angles.set(game.cg.refdef.Angles);
        Vector3f angleScale = new Vector3f(0.1f, 0.3f, 1f);
        
        
        // on odd legs, invert some angles
        float scale;
        if((game.cg.cur_lc.bobcycle & 1) != 0) {
            scale = -game.cg.cur_lc.xyspeed;
        } else {
            scale = game.cg.cur_lc.xyspeed;
        }

        // gun angles from bobbing
        angles.z += scale * game.cg.cur_lc.bobfracsin * 0.008;
        //angles.y += scale * game.cg.cur_lc.bobfracsin * 0.01;
        angles.x += game.cg.cur_lc.xyspeed * game.cg.cur_lc.bobfracsin * 0.08;

        
        CEntity cent = game.cg.cur_lc.predictedPlayerEntity;
        swingAngles(angles.x, 0, 10, 0.1f, cent.pe.torso, true, 1f);
        swingAngles(angles.y, 0, 30, 0.2f, cent.pe.torso, false, 1f);
        angles.x = cent.pe.torso.yawAngle;
        angles.y = cent.pe.torso.pitchAngle;
        
        angles.x = Helper.AngleSubtract(angles.x, game.cg.refdef.Angles.x)*angleScale.x;
        angles.y = Helper.AngleSubtract(angles.y, game.cg.refdef.Angles.y)*angleScale.y;
        angles.z = Helper.AngleSubtract(angles.z, game.cg.refdef.Angles.z)*angleScale.z;
        
        float tmp = angles.x;
        angles.x = angles.y;
        angles.y = tmp;
        
    }
    
    private Matrix4f createModelMatrix(Vector3f[] axis, Vector3f position, Matrix4f dest) {
        if(dest == null) dest = new Matrix4f();
        // Set rotation matrix
        FloatBuffer viewbuffer = Ref.glRef.matrixBuffer;
        viewbuffer.position(0);
        viewbuffer.put(axis[0].x);viewbuffer.put(axis[1].x);viewbuffer.put(axis[2].x);viewbuffer.put(0);
        viewbuffer.put(axis[0].y);viewbuffer.put(axis[1].y);viewbuffer.put(axis[2].y); viewbuffer.put(0);
        viewbuffer.put(axis[0].z); viewbuffer.put(axis[1].z); viewbuffer.put(axis[2].z);viewbuffer.put(0);
        viewbuffer.put(0); viewbuffer.put(0); viewbuffer.put(0); viewbuffer.put(1);
        viewbuffer.flip();
        Matrix4f mMatrix = (Matrix4f) dest.load(viewbuffer);
        mMatrix.invert();
        mMatrix.m30 = position.x;
        mMatrix.m31 = position.y;
        mMatrix.m32 = position.z;

        viewbuffer.clear();
        return mMatrix;
    }

    
    
    
}
