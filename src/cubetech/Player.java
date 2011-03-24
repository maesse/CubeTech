//package cubetech;
//
//import cubetech.gfx.CubeTexture;
//import cubetech.gfx.Sprite;
//import cubetech.gfx.SpriteManager;
//import cubetech.gfx.SpriteManager.Type;
//import cubetech.gfx.TextManager.Align;
//import cubetech.input.Key;
//import cubetech.misc.Button;
//import cubetech.misc.Ref;
//import cubetech.spatial.SpatialQuery;
//import cubetech.state.MenuState;
//import java.net.URL;
//import java.security.MessageDigest;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import javax.crypto.Cipher;
//import javax.crypto.SecretKey;
//import javax.crypto.spec.IvParameterSpec;
//import javax.crypto.spec.SecretKeySpec;
//import org.lwjgl.input.Keyboard;
//import org.lwjgl.util.Color;
//import org.lwjgl.util.vector.Vector2f;
//import org.lwjgl.util.vector.Vector4f;
//
//
///**
// *
// * @author mads
// */
//public final class Player {
//    // Entity
//    public Vector2f position;
//    public Vector2f centerpos = new Vector2f();
//    public Vector2f velocity;
//    public Vector2f extent;
//
//    // Constants
//    float speed = 130f; // max speed
//    float accel = 8f;
//    float friction = 4f;
//    float stopspeed = 15f;
//    float healthOnKill = 5f;
//    World world;
//
//    // Player Info
//    int lives;
//    float health;
//    public int score;
//    float energy = 0f;
//    int DrainDelayTime = 1000; // drain life every second
//    float DrainDamage = 100f/60f; // 24
//    long time;
//    long nextWaveTime;
//    boolean inWave = false;
//
//    // Textures
//    CubeTexture heart;
//    CubeTexture healthBar;
//    CubeTexture healthBar2;
//    CubeTexture energyBar;
//    CubeTexture heartguy;
//    CubeTexture gameoverTex;
//    CubeTexture background;
//
//    Button retryButton;
//    Button submitButton;
//
//    // Animation info
//    long damageTime;
//
//    // FLow control
//    long dieTime = 0;
//    long nextTailTime = 0;
//    boolean gameover;
//    long countTime = 0;
//    float TimeToNextTail = 300f;
//    long lastConnect = 0;
//    Vector2f LastDrop = new Vector2f();
//    boolean submitting = false;
//
//    int animFrame = 0;
//    int nextFrame = 0;
//    float lastAngle = 0;
//    long nextHealthDrainTime = 0;
//    String scorename = "";
//
//    int lastrandomCreature = 1;
//    int stepTime = 100;
//
//
//    ArrayList<CircleSplotion> splotions = new ArrayList<CircleSplotion>();
//    TailPart[] Tail = new TailPart[128];
//    int NextTail = 0;
//    ArrayList<EnemyBase> enemies = new ArrayList<EnemyBase>();
//    ArrayList<EnemyBase> enemiesToRemove = new ArrayList<EnemyBase>();
//
//    // Health drawing offsets
//    Vector2f healthPosition = new Vector2f(0.03f, 0.77f);
//    Vector2f healthSize = new Vector2f(0.2f, 0.2f);
//    Vector2f healthTexOffset = new Vector2f(0f, 0);
//    Vector2f healthTexSize = new Vector2f(0.5f, 1f);
//    Vector2f healthPosition2 = new Vector2f(0.049f, 0.818f);
//    Vector2f healthTex2Offset = new Vector2f(0f, 0);
//    Vector2f healthTex2Size = new Vector2f(1f, 1f);
//    Vector2f healthSize2 = new Vector2f(0.157f, 0.2f);
//    long nextRandomSpawn ;
//    int spawnSound;
//    int stepSound;
//    int healthSound;
//    int painSound;
//    long healthsoundtime = 0;
//
//    public Player(World world, Vector2f spawn) {
//        this.world = world;
//        position = spawn;
//        heart = Ref.ResMan.LoadTexture("data/heart.png");
//        healthBar = Ref.ResMan.LoadTexture("data/healthheart.png");
//        healthBar2 = Ref.ResMan.LoadTexture("data/health2.png");
//        energyBar = Ref.ResMan.LoadTexture("data/energybar.png");
//        heartguy = Ref.ResMan.LoadTexture("data/heartguy.png");
//        background = Ref.ResMan.LoadTexture("data/menubutton.png");
//        gameoverTex  = Ref.ResMan.LoadTexture("data/gameover.png");
//        retryButton = new Button("New Game", new Vector2f(0.15f, 0.15f), new Vector2f(0.2f, 0.07f), background);
//        submitButton = new Button("Submit Score", new Vector2f(0.4f, 0.15f), new Vector2f(0.2f, 0.07f), background);
//        for (int i= 0; i < Tail.length; i++) {
//            Tail[i] = new TailPart(heart);
//        }
//        ResetPlayer();
//
//        stepSound = Ref.soundMan.AddWavSound("data/footsteps.wav");
//        spawnSound = Ref.soundMan.AddWavSound("data/sound13.wav");
//        healthSound = Ref.soundMan.AddWavSound("data/sound10.wav");
//        painSound = Ref.soundMan.AddWavSound("data/hurt.wav");
//
//        //enemies.add(new TestEnemy(50, 50));
////        enemies.add(new Enemy6(30, 50, 0.5f*3.14f, 1500));
////        enemies.add(new Enemy6(30, 40, 0.75f*3.14f, 1500));
////        enemies.add(new Enemy6(30, 30, 2.512f*3.14f, 1500));
////        enemies.add(new Enemy6(30, 20, 1f*3.14f, 1500));
////        enemies.add(new Enemy6(30, 10, 1.25f*3.14f, 1500));
//
//        nextWaveTime = 20*1000; // first wave after 20 secs
//        //nextRandomSpawn = Ref.loop.time+1000;
//    }
//
//
//
//
//
//    public void Update(int msec) {
//
//        // Handle die
//        if(dieTime != 0) {
////            if(dieTime < Ref.loop.time) {
////                // Player can spawn
////                if(Ref.Input.playerInput.Mouse1 || Ref.Input.playerInput.Up ||
////                        Ref.Input.playerInput.Down || Ref.Input.playerInput.Left ||
////                        Ref.Input.playerInput.Right || Ref.Input.playerInput.Jump) {
////                    Respawn();
////                }
////            }
//
//            // Handle death zoom
//            if(dieTime + 2000 > Ref.client.realtime) {
//                Ref.world.camera.VisibleSize.x = 40 + ((Ref.world.camera.DefaultSize.x - 40f) * (1f-((Ref.client.realtime-dieTime)/2000f)));
//                Ref.world.camera.VisibleSize.y = 30 + ((Ref.world.camera.DefaultSize.y - 30f) * (1f-((Ref.client.realtime-dieTime)/2000f)));
//                Ref.world.camera.Position.x = position.x  - Ref.world.camera.VisibleSize.x/2f;
//                Ref.world.camera.Position.y = position.y - Ref.world.camera.VisibleSize.y/2f;
//                velocity.x = (float) Math.cos(lastAngle-Math.PI/2f+(Math.PI/2f*(1f-((Ref.client.realtime-dieTime)/2000f))*(float)msec)/50f);
//                velocity.y = (float) Math.sin(lastAngle-Math.PI/2f+(Math.PI/2f*(1f-((Ref.client.realtime-dieTime)/2000f))*(float)msec)/50f);
//            } else if(dieTime + 3000 < Ref.client.realtime) {
//                // Fully zoomed, and showing death-picture
//                Vector2f mousePos = Ref.Input.playerInput.MousePos;
//                if(retryButton.Intersects(mousePos) && Ref.Input.playerInput.Mouse1) {
//                    // Exit Cubetech
//                    world.StartNewEmptyGame();
//                } else if(!submitting && submitButton.Intersects(mousePos) && Ref.Input.playerInput.Mouse1 ) {
//                    submitting = true;
//                    SubmitScore(scorename, score);
//                    try {
//                        Ref.StateMan.SetState("menu");
//                        ((MenuState)Ref.StateMan.GetGameState("menu")).ShowScoeboard();
//                    } catch (Exception ex) {
//                        Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//
//                }
//
//
//            }
//            return;
//        }
//
//        if(gameover)
//            return;
//
//        time += msec;
//
//        if(health < 30 && healthsoundtime < Ref.client.realtime) {
//            Ref.soundMan.playEffect(healthSound, 1f);
//            healthsoundtime = Ref.client.realtime + 3450;
//        }
//
//        if(nextHealthDrainTime < Ref.client.realtime) {
//            TakeDamage(DrainDamage, true);
//            nextHealthDrainTime = Ref.client.realtime + DrainDelayTime;
//        }
//
//        EnemySpawner();
//
//        // Update enemies
//        Vector2f tempdist = new Vector2f();
//        for (int i= 0; i < enemies.size(); i++) {
//            EnemyBase enem = enemies.get(i);
//            enem.Update(msec, centerpos);
//
//            // Does it need to be removed?
//            if(enem.RemoveMe)
//                enemiesToRemove.add(enem);
//            else {
//                Vector2f.sub(centerpos, enem.getPosition(), tempdist);
//
//                float len = (float) Math.sqrt(tempdist.x * tempdist.x + tempdist.y * tempdist.y);
//                if(len < 10) {
//                    enem.Die();
//                    TakeDamage(10, false);
//                }
//            }
//        }
//
//        // Remove those that wanted to
//        for (int i= 0; i < enemiesToRemove.size(); i++) {
//            enemies.remove(enemiesToRemove.get(i));
//        }
//        enemiesToRemove.clear();
//
//
//        // Direction player wants to move
//        Vector2f wishdir = new Vector2f();
//        if(Ref.Input.playerInput.Left)
//            wishdir.x -= 1f;
//        if(Ref.Input.playerInput.Right)
//            wishdir.x += 1f;
//        if(Ref.Input.playerInput.Up)
//            wishdir.y += 1f;
//        if(Ref.Input.playerInput.Down)
//            wishdir.y -= 1f;
//
//        float len = (float)Math.sqrt(wishdir.x * wishdir.x + wishdir.y * wishdir.y);
//        if(len != 0f) {
//            wishdir.x /= len;
//            wishdir.y /= len;
//        }
//
//        wishdir.x *= speed; // speed;
//        wishdir.y *= speed; // speed;
//
//        float currspeed = (float)Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
//        float speedFrac = currspeed/(speed*0.5f);
//        if(speedFrac > 1.0f)
//            speedFrac = 1.0f;
//
//        stepTime -= 1000 * speedFrac * (float)msec/1000f;
//        if(stepTime <= 0) {
//            Ref.soundMan.playEffect(stepSound, 1.0f);
//
//            stepTime = 300;
//        }
////        if(speedFrac < 0.4f)
////            speedFrac = 0.4f;
//        nextFrame -= (int)((float)msec * speedFrac);
//        if(nextFrame <= 0) {
//            nextFrame = 100;
//            animFrame++;
//        }
//
//        Friction(msec);
//
//        WalkMove(wishdir, msec);
//        nextTailTime -= msec;
//        if(nextTailTime < 0)
//            nextTailTime = 0;
//
//        int toRemove = -1;
//
//        for (int i= 0; i < splotions.size(); i++) {
//            splotions.get(i).Update(msec);
//            if(toRemove == -1 && splotions.get(i).time == 0)
//                toRemove = i;
//        }
//
//        if(toRemove != -1)
//            splotions.remove(toRemove);
//
//        // Do the tail
//        Vector2f dropDist = new Vector2f();
//        if(velocity.x != 0 || velocity.y != 0) {
//            // Check dist to LastDrop
//            Vector2f.sub(LastDrop, position, dropDist);
//            float dist = (float) Math.sqrt(dropDist.x * dropDist.x + dropDist.y * dropDist.y);
//            if(dist > 7) {
//                SpawnTail();
//                LastDrop.x = position.x;
//                LastDrop.y = position.y ;
//            }
//        }
//
//        boolean collided = false;
//        Vector2f hags = new Vector2f();
//        int collideIndex = 0;
//        int minPart = NextTail - 15;
//        if(minPart < 0)
//            minPart += Tail.length;
//
//        for (int i= 0; i < Tail.length; i++) {
//            Tail[i].Update(msec); // let it fade out
//
//            if(collided || Tail[i].time == 0)
//                continue;
//
//            if((NextTail-i < 10 && NextTail-i >= 0) || (NextTail-i < 0 && NextTail+Tail.length-i < 10))
//                continue;
//
//            // Check for collision
//            Vector2f.sub(Tail[i].Position, centerpos, hags);
//            float lens = (float)Math.sqrt(hags.x * hags.x + hags.y * hags.y);
//            if(lens < (8 + Tail[i].GetRadius())) {
//                collided = true;
//                collideIndex = i;
//            }
//        }
//
//        if(collided && Tail[collideIndex].Age >= 500) {
//            // from collideIndex to NextTail is connecting
//            if(lastConnect + 1000 < Ref.client.realtime)
//                TailConnect(collideIndex);
//        }
//    }
//
//
//    void EnemySpawner() {
//        if(inWave && nextWaveTime > Ref.client.realtime)
//            return; // currently in a wave
//
//        if(!inWave && nextWaveTime < Ref.client.realtime) {
//            // Time for a new wave
//            HandleWave();
//            return;
//        }
//
//        if(nextRandomSpawn < Ref.client.realtime) {
//           // Do random spawns
//            float secondsToNextRandom = 5;
//            nextRandomSpawn = Ref.client.realtime + (int)(secondsToNextRandom * 1000f);
//            SpawnRandom();
////            System.out.println("Spawning");
//        }
//    }
//
//    void HandleWave() {
//        // Use time to figure out how many to spawn
//        nextWaveTime = Ref.client.realtime +  1000;
//    }
//
//    void SpawnRandom() {
//        int maxcreature = 1;
//        int nCreeps = 2;
//        if(time < 5000) {
//
//        }
//        else if(time > 5000 && time < 20000) {
//            maxcreature = 2;
//            nCreeps = Ref.rnd.nextInt(3)+1;
//        }
//        else if(time < 30000) {
//            maxcreature = 3;
//            nCreeps = Ref.rnd.nextInt(6)+1;
//        }
//        else if(time < 40000) {
//            maxcreature = 4;
//            nCreeps = Ref.rnd.nextInt(8)+1;
//        }
//       else if(time < 50000) {
//            maxcreature = 5;
//            nCreeps = Ref.rnd.nextInt(10)+1;
//        }else{
//            maxcreature = 5;
//            nCreeps = Ref.rnd.nextInt(10)+(int)time/50000;
//        }
//
//
//        int creatureType = Ref.rnd.nextInt(maxcreature+1);
//        while(lastrandomCreature == creatureType) {
//            creatureType = Ref.rnd.nextInt(maxcreature+1);
//        }
//        lastrandomCreature = creatureType;
//
//        Vector2f[] creatureSpawns = new Vector2f[nCreeps];
//        boolean vertical = Ref.rnd.nextBoolean();
//        if(creatureType == 2) {
//            // Custom position spawns
//            Vector2f base = GetRandomPositionAwayFromPlayer();
//
//            if(vertical) {
//                for (int i= 0; i < nCreeps; i++) {
//                    creatureSpawns[i] = new Vector2f(base.x, base.y+10*i);
//                }
//            } else {
//                for (int i= 0; i < nCreeps; i++) {
//                    creatureSpawns[i] = new Vector2f(base.x+10*i, base.y);
//                }
//            }
//        } else {
//            for (int i= 0; i < nCreeps; i++) {
//                creatureSpawns[i] = GetRandomPositionAwayFromPlayer();
//            }
//        }
//
//        System.out.println("Spawning" + creatureType);
//        Ref.soundMan.playEffect(spawnSound, 1.0f);
//
//        switch(creatureType) {
//            case 0:
//                for (int i= 0; i < nCreeps; i++) {
//                    enemies.add(new EnemyCrap((int)creatureSpawns[i].x, (int)creatureSpawns[i].y));
//                }
//                break;
//           case 1:
//                for (int i= 0; i < nCreeps; i++) {
//                    enemies.add(new EnemyPlane((int)creatureSpawns[i].x, (int)creatureSpawns[i].y));
//                }
//                break;
//            case 2:
//                for (int i= 0; i < nCreeps; i++) {
//                    enemies.add(new EnemyCar((int)creatureSpawns[i].x, (int)creatureSpawns[i].y, (float) (vertical ? Math.PI / 2f : 0f), 1500));
//                }
//                break;
//            case 3:
//                for (int i= 0; i < nCreeps; i++) {
//                    enemies.add(new EnemySlowguy((int)creatureSpawns[i].x, (int)creatureSpawns[i].y));
//                }
//                break;
//            case 4:
//                for (int i= 0; i < nCreeps; i++) {
//                    enemies.add(new EnemyNadeGuy((int)creatureSpawns[i].x, (int)creatureSpawns[i].y));
//                }
//                break;
//            case 5:
//                for (int i= 0; i < nCreeps; i++) {
//                    enemies.add(new EnemyBlaster((int)creatureSpawns[i].x, (int)creatureSpawns[i].y));
//                }
//                break;
//        }
//    }
//
//    Vector2f GetRandomPositionAwayFromPlayer() {
//        Vector2f pos = new Vector2f();
//        float dist = 0f;
//        do {
//        pos.x = Ref.rnd.nextInt((int)(Ref.world.WorldMaxs.x-Ref.world.WorldMins.x+1))+Ref.world.WorldMins.x;
//        dist = pos.x - position.x;
//        } while(pos.x <= Ref.world.WorldMins.x || pos.x >= Ref.world.WorldMaxs.x || (dist < 20 && dist > -20));
//        do {
//        pos.y = Ref.rnd.nextInt((int)(Ref.world.WorldMaxs.y-Ref.world.WorldMins.y+1))+Ref.world.WorldMins.y;
//        dist = pos.y - position.y;
//        } while(pos.y <= Ref.world.WorldMins.y || pos.y >= Ref.world.WorldMaxs.y || (dist < 20 && dist > -20));
//        return pos;
//    }
//
//    void KillEnemiesInCircle(Vector2f center, float radius) {
//        Vector2f dist = new Vector2f();
//        for (int i= 0; i < enemies.size(); i++) {
//            EnemyBase enem = enemies.get(i);
//            Vector2f.sub(enem.getPosition(), center, dist);
//            float dista = (float) Math.sqrt(dist.x * dist.x + dist.y * dist.y);
//            if(dista <= radius) {
//                enem.Die();
//                if(health < 100) {
//                    health += healthOnKill;
//                    if(health > 100)
//                        health = 100;
//                }
//            }
//        }
//    }
//
//    void SpawnTail() {
//        Tail[NextTail].Position.x = position.x;
//        Tail[NextTail].Position.y = position.y;
//        Tail[NextTail].SetTime(4000, velocity);
//
//        NextTail++;
//        if(NextTail >= Tail.length)
//            NextTail = 0;
//    }
//
//    void TailConnect(int collideIndex) {
//        // Get center point
//        int nParts;
//        if(collideIndex > NextTail)
//            nParts = Tail.length - collideIndex - 2 + NextTail;
//        else
//            nParts = NextTail - collideIndex;
//
//        float x = 0, y = 0;
//        for (int i= 0; i < nParts; i++) {
//            int tailIndex = i + collideIndex;
//            if(tailIndex >= Tail.length)
//                tailIndex -= Tail.length;
//            x += Tail[tailIndex].Position.x/(float)nParts;
//            y += Tail[tailIndex].Position.y/(float)nParts;
//        }
//
//        Vector2f center = new Vector2f(x, y);
//
//        // Get average distance to center
//        Vector2f tempDist = new Vector2f();
//        float avgLenght = 0f;
//        for (int i= 0; i < nParts; i++) {
//            int tailIndex = i + collideIndex;
//            if(tailIndex >= Tail.length)
//                tailIndex -= Tail.length;
//            Vector2f.sub(center, Tail[tailIndex].Position, tempDist);
//            float tmplen = (float) Math.sqrt(tempDist.x * tempDist.x + tempDist.y * tempDist.y);
//            avgLenght += tmplen/(float)nParts;
//
//        }
//
//        int nTail = 0;
//        for (int i= 0; i < Tail.length; i++) {
//            int offset = NextTail+i;
//            if(offset >= Tail.length)
//                offset -= Tail.length;
//
//            if(Tail[offset].time != 0) {
//                Tail[offset].SetPopTime(nTail*10);
//                nTail++;
//            }
//        }
//
//        avgLenght *= 1.2f;
//
//        splotions.add(new CircleSplotion(center, avgLenght));
//        KillEnemiesInCircle(center, avgLenght);
//        lastConnect = Ref.client.realtime;
//    }
//
//
//
//    void Respawn() {
//        if(dieTime == 0)
//            return;
//
//        if(lives == 0)
//            return;
//
//        dieTime = 0;
//        int oldLife = lives;
//        ResetPlayer();
//        lives = oldLife;
//
//        world.WorldUpdated(false);
//
//         // Set at spawn position
//         for (int i= 0; i < world.NextBlockHandle; i++) {
//            Block block = world.Blocks[i];
//            if(block == null || block.CustomVal != 1)
//                continue;
//
//            position.x = block.getPosition().x + block.getSize().x/2f;
//            position.y = block.getPosition().y + block.getSize().y/2f;
//            break;
//        }
//    }
//
//    void WalkMove(Vector2f wishdir, int msec) {
//       // normalize
//       float wishspeed = (float)Math.sqrt(wishdir.x * wishdir.x + wishdir.y * wishdir.y);
//       if(wishspeed > 0) {
//           wishdir.x /= wishspeed;
//           wishdir.y /= wishspeed;
//       }
//       if(wishspeed > speed)
//       {
//           wishdir.x *= (speed/wishspeed);
//           wishdir.y *= (speed/wishspeed);
//           wishspeed = speed;
//       }
//
//       float currentSpeed = Vector2f.dot(velocity, wishdir);
//       float addSpeed = wishspeed  - currentSpeed;
//       if(addSpeed > 0f)
//       {
//           float accelspeed = accel * (float)msec/1000f * wishspeed;
//           if(accelspeed > addSpeed)
//               accelspeed = addSpeed;
//
//           velocity.x += accelspeed * wishdir.x;
//           velocity.y += accelspeed * wishdir.y;
//       }
//
//       float speed2 = (float)Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
//       if(speed2 < 1f)
//       {
//           velocity.x = 0f;
//           velocity.y = 0f;
//           return;
//       }
//       float destx = position.x + velocity.x * (float)msec/1000f;
//       float desty = position.y + velocity.y * (float)msec/1000f;
//       if( destx < Ref.world.WorldMins.x || destx > Ref.world.WorldMaxs.x) {
//        velocity.x = 0;
//       }
//
//       if( desty < Ref.world.WorldMins.y || desty > Ref.world.WorldMaxs.y) {
//        velocity.y = 0;
//       }
//
//       position.x += velocity.x * (float)msec/1000f;
//       position.y += velocity.y * (float)msec/1000f;
//       centerpos.x = position.x ;
//       centerpos.y = position.y ;
//   }
//
//    void Friction(int msec) {
//       float speed2 = (float)Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
//       if(speed2 < 0.1f)
//           return;
//
//       float fric = friction;
//
//       float control = (speed2 < stopspeed ? stopspeed : speed2);
//       float drop = control * fric * (float)msec/1000f;
//
//       float newspeed = speed2 - drop;
//       if(newspeed < 0)
//           newspeed = 0;
//
//       newspeed /= speed2;
//
//       velocity.x *= newspeed;
//       velocity.y *= newspeed;
//   }
//
//    // Resets the player for a new game
//    void ResetPlayer() {
//        extent = new Vector2f(6,8);
//        velocity = new Vector2f();
//        //bigguy = false;
//        health = 100;
//
//        lives = 3;
//        energy = 0f;
//    }
//
//    // Called when something dies
//    public void AddScore(int score) {
//        if(this.score % 1000 > (this.score + score) % 1000)
//            lives++;
//        this.score+= score;
//        GotKill();
//        countTime = 3;
//    }
//
//    // Kills adds energy
//    void GotKill() {
//
//
//    }
//
//    public void TakeDamage(float damage, boolean force) {
////        damage *= 1.5f;
//       //if(bigguy)
//       //     damage /= 2f; // BigGuy only takes 50% dmg
//        if(gameover)
//            return; // Cant take dmg when not playing
//        if(damageTime + 300 > Ref.client.realtime && !force)
//            return;
//        if(!force) {
//            Ref.soundMan.playEffect(painSound, 1.0f);
//        }
//        damageTime = Ref.client.realtime;
//        this.health -= damage;
//        if(health <= 0f)
//            Die();
//    }
//
//    void Die() {
//        gameover = true;
//        dieTime = Ref.client.realtime;
//        scorename = "";
//        submitting = false;
//        // Implement
////        if(dieTime == 0) {
////            dieTime = Ref.loop.time + 1000; // When to start again
////            lives--;
////            }
//    }
//
//    public void Render() {
//        for (int i= 0; i < enemies.size(); i++) {
//            enemies.get(i).Render();
//        }
//
//        for (int i= 0; i < splotions.size(); i++) {
//            splotions.get(i).Render();
//        }
//
//        for (int i= 0; i < Tail.length; i++) {
//            int index = NextTail + i + 1;
//            if(index >= Tail.length)
//                index -= Tail.length;
//            Tail[index].Render();
//        }
//
//        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
//        int index = animFrame % 3;
//        Vector2f offset = new Vector2f(0.00f,0.00f);
//        Vector2f size = new Vector2f(0.50f,0.50f);
//        if(index == 2) {
//           offset.y += 0.5f;
//        } else if(index == 1) {
//            offset.x += 0.5f;
//        }
//
//
//        float angle = 0;
//        if(velocity.x == 0f && velocity.y == 0f) {
//            angle = lastAngle;
//        } else {
//            angle = (float) (Math.atan2(velocity.y, velocity.x) + Math.PI / 2f);
//            lastAngle = angle;
//        }
//        spr.Set(new Vector2f(position.x-12, position.y-8), new Vector2f(25f, 16f), heartguy, offset, size);
//        spr.SetColor(1, 1, 1, 1);
//        spr.SetAngle(angle);
//
//        RenderPlayerHud();
//
//    }
//
//
//
//
//
//    void DrawHealthBar() {
//        // Empty heart
//        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
//        spr.Set(healthPosition, healthSize, healthBar, healthTexOffset, healthTexSize);
//
//        // Fill up according to health
//        float tempheath = health;
//        if(tempheath <= 0f)
//            tempheath = 0.01f;
//        float healthFrac = tempheath/100f;
//        spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
//        healthTex2Offset.y =  1f - healthFrac ;
//        healthTex2Size.y = healthFrac;
//        healthSize2.y = 0.14f * healthFrac;
//        spr.Set(healthPosition2, healthSize2, healthBar2, healthTex2Offset, healthTex2Size);
//    }
//
//    void RenderPlayerHud() {
//        // Black background
////        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
////        spr.Set(new Vector2f(0.05f, 0.9f), new Vector2f(0.3f, 0.05f), null, new Vector2f(), new Vector2f(1, 1));
////        spr.SetColor(new Vector4f(0, 0, 0, 0.5f));
//
//
//
////        for (int i= 0; i < lives; i++) {
////            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
////            spr.Set(new Vector2f(0.4f + 0.045f*i, 0.925f), 0.025f, heart);
////        }
//
//
//        DrawHealthBar();
//
//        if(health <= 0 && dieTime + 3000 < Ref.client.realtime) {
//            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
//            spr.Set(new Vector2f(0.1f, 0.1f), new Vector2f(0.8f, 0.8f), gameoverTex, new Vector2f(), new Vector2f(1,1));
//
//
//            retryButton.Render();
//            if(!submitting)
//                submitButton.Render();
//            else {
//                Ref.textMan.AddText(new Vector2f(0.4f, 0.155f), "Submitted.", Align.LEFT, Type.HUD);
//            }
//
//            Ref.textMan.AddText(new Vector2f(0.35f, 0.65f), "Your score: " + score, Align.CENTER, Type.HUD);
//            String text = "Not bad, my man";
//            if(score < 100) {
//                text = "Thats horrible...";
//            } else if(score < 1000) {
//                text = "Getting there.";
//            } else if(score < 10000) {
//                text = "AWWW YEEEE";
//            } else {
//                text = "Whoa.";
//            }
//            Ref.textMan.AddText(new Vector2f(0.35f, 0.58f), text, Align.CENTER, new Color(30,30,30,200), null, Type.HUD,1);
//
//            Ref.textMan.AddText(new Vector2f(0.15f, 0.245f), "Name: ", Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(0.25f, 0.245f), scorename, Align.LEFT, Type.HUD);
//            Ref.textMan.AddText(new Vector2f(0.15f, 0.30f), "Submit your score!", Align.LEFT, Type.HUD);
//
//            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
//            spr.Set(new Vector2f(0.25f, 0.25f), new Vector2f(0.35f, 0.05f), null, new Vector2f(), new Vector2f(1, 1));
//            spr.SetColor(0, 0, 0,127);
//            return;
//        }
//
//        Ref.textMan.AddText(new Vector2f(0.98f, 0.92f), "Score: " + score, Align.RIGHT, Type.HUD);
//        int sec = (int)(time/1000);
//        int min = (int)(sec/60);
//        sec -= min*60;
//        Ref.textMan.AddText(new Vector2f(0.7f, 0.92f), "Time: " + min + ":" + sec , Align.RIGHT, Type.HUD);
//    }
//
//
//
//    void RenderSpatialDebug() {
//        SpatialQuery result = Ref.spatial.Query(position.x - extent.x, position.y - extent.y, position.x + extent.x, position.y + extent.y);
//        int queryNum = result.getQueryNum();
//        Object object;
//        while((object = result.ReadNext()) != null) {
//            if(object.getClass() != Block.class)
//                continue;
//            Block block = (Block)object;
//            if(block.LastQueryNum == queryNum)
//                continue; // duplicate
//            block.LastQueryNum = queryNum;
//
//            Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.GAME);
//            spr.Set(block.getPosition(), block.getSize(), block.Texture, block.TexOffset, block.TexSize);
//            spr.SetAngle(block.getAngle());
//            //spr.Angle = block.getAngle();
//            spr.SetColor(255, 0, 0, 255);
//            //spr.Color = new Vector4f(1, 0, 0, 1f);
//        }
//    }
//
//
//byte[] EncryptData(String str) throws Exception {
//        final MessageDigest md = MessageDigest.getInstance("md5");
//        final byte[] digestOfPassword = md.digest("HG58YZ3CR9"
//                        .getBytes("utf-8"));
//        String skey = "passwordDR0wSS@P6660juht";
//        String sIV = "password";
//
//        final byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
//        for (int j = 0, k = 16; j < 8;) {
//                keyBytes[k++] = keyBytes[j++];
//        }
//
//        //final SecretKey key = new SecretKeySpec(keyBytes, "DESede");
//        final SecretKey key = new SecretKeySpec(skey.getBytes("utf-8"), "DESede");
//        final IvParameterSpec iv = new IvParameterSpec(sIV.getBytes("utf-8"));
//
//        final Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
//        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
//
//        final byte[] plainTextBytes = str.getBytes("utf-8");
//        final byte[] cipherText = cipher.doFinal(plainTextBytes);
//        // final String encodedCipherText = new sun.misc.BASE64Encoder()
//        // .encode(cipherText);
//
//        return cipherText;
//
//    }
//
//    public void SubmitScore(String name, int score) {
//        try {
//
//            String payload = getHex(EncryptData("magic" + name + "\n" + score + "\n"));
//            URL scoreUrl = new URL("http://pd-eastcoast.com/rgjscore.php?s=" + payload);
//            scoreUrl.openStream().close();
//        } catch (Exception ex) {
//            Logger.getLogger(Player.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
//
//    static final String HEXES = "0123456789ABCDEF";
//    public static String getHex( byte [] raw ) {
//    if ( raw == null ) {
//      return null;
//    }
//    final StringBuilder hex = new StringBuilder( 2 * raw.length );
//    for ( final byte b : raw ) {
//      hex.append(HEXES.charAt((b & 0xF0) >> 4))
//         .append(HEXES.charAt((b & 0x0F)));
//    }
//    return hex.toString();
//  }
//
//    void HandleKey(Key event) {
//        if(dieTime + 3000 < Ref.client.realtime && health <= 0) {
//            if(Character.isLetterOrDigit(event.Char) && scorename.length() < 23)
//                scorename += event.Char;
//            else {
//                switch(event.key) {
//                    case Keyboard.KEY_BACK:
//                        if(scorename.length() > 1)
//                            scorename = scorename.substring(0, scorename.length()-1);
//                        else if(scorename.length() == 1)
//                            scorename = "";
//                        break;
//                }
//            }
//        }
//    }
//
//}
