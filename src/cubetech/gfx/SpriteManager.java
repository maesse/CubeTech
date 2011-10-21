package cubetech.gfx;

import cubetech.common.Common;
import cubetech.gfx.VBO.BufferTarget;
import cubetech.gfx.VBO.Usage;

import cubetech.misc.Ref;
import java.nio.ByteBuffer;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;

/**
 *
 * @author mads
 */
public class SpriteManager {
    private static final int MAX_SPRITES = 10000;
    private static final Comparator<Sprite> DEPTHCOMPARER = new Comparator<Sprite>() {
        public int compare(Sprite o1, Sprite o2) {
            if(o1.getDepth() > o2.getDepth())
                return 1;
            else if(o1.getDepth() < o2.getDepth())
                return -1;
            else if(o1.Texture == o2.Texture)
                return 0;
            else if(o2.Texture == null)
                return 1;
            else if(o1.Texture == null)
                return -1;
            else if(o1.Texture.GetID() > o2.Texture.GetID())
                return 1;
            else if(o1.Texture.GetID() < o2.Texture.GetID())
                return -1;
            else
                return 0;
        }
    };

    public Sprite[] Sprites = new Sprite[MAX_SPRITES];
    public int SpriteOffset = 0;

    public int[] NormalSprites = new int[MAX_SPRITES];
    public ArrayList<Sprite> sortSprites = new ArrayList<Sprite>(100);
    public int NormalSpriteOffset = 0;

    public int[] HUDSprites = new int[MAX_SPRITES];
    public int HUDSpriteOffset = 0;

    private VBO vertexBuffer;
    private VBO indexBuffer;

    private int vboNormalOffset = 0;

    public enum Type {
        GAME,
        HUD,
    }

    public SpriteManager() {
        for (int i= 0; i < Sprites.length; i++) {
            Sprites[i] = new Sprite();
        }
    }

    public Sprite GetSprite(Type type) {
        switch(type) {
            case GAME:
                if(SpriteOffset >= Sprites.length) {
                    System.err.print("SpriteManager: Sprite overflow");
                    SpriteOffset = 0;
                }
                NormalSprites[NormalSpriteOffset++] = SpriteOffset;
                Sprites[SpriteOffset].SetDepth(100);
                Sprites[SpriteOffset].invalid = true;
                return Sprites[SpriteOffset++];
            case HUD:
                if(SpriteOffset >= Sprites.length) {
                    System.err.print("SpriteManager: Sprite overflow");
                    SpriteOffset = 0;
                }
                HUDSprites[HUDSpriteOffset++] = SpriteOffset;
                Sprites[SpriteOffset].SetDepth(10);
                Sprites[SpriteOffset].invalid = true;
                return Sprites[SpriteOffset++];
            default:
                return null; // unknown type
        }
    }

    // The index buffer never changes, so just have it built once
    private void BuildIndexBuffer() {
        if(indexBuffer != null) return; // already created
        indexBuffer = new VBO(MAX_SPRITES*6*4, BufferTarget.Index);
        ByteBuffer buf = indexBuffer.map();
        for (int i= 0; i < MAX_SPRITES; i++) {
            buf.putInt((0) + i * 4);
            buf.putInt((1) + i * 4);
            buf.putInt((2) + i * 4);
            buf.putInt((0) + i * 4);
            buf.putInt((2) + i * 4);
            buf.putInt((3) + i * 4);
        }

        // Let openGL digest them
        buf.flip();
        indexBuffer.unmap();
    }

    private void DrawNormalFixedFunction() {
        if(NormalSpriteOffset == 0)
            return;

        CubeTexture tex = null;
        sortSprites.clear();
        
        for (int i= 0; i < NormalSpriteOffset; i++) {
            Sprite spr = Sprites[NormalSprites[i]];
            if(spr.invalid) {
                Common.LogDebug("Invalid sprite");
                spr.invalid = false;
            }
            // Does it need alpha sorting?
            if(spr.sort) {
                sortSprites.add(spr);
            } else {
                // Got texture change?
                if(spr.Texture != tex) {
                    tex = spr.Texture;
                    tex.Bind();
                }
                spr.FillBuffer(null); // update internal buffer
                spr.DrawFromBuffer();
            }
        }

        Collections.sort(sortSprites, DEPTHCOMPARER);
        
        for (int i= 0; i < sortSprites.size(); i++) {
            Sprite spr = sortSprites.get(i);
            // Got texture change?
            
            if(spr.Texture != tex) {
                tex = spr.Texture;
                if(tex != null)
                    tex.Bind();
                else
                    Ref.ResMan.SetWhiteTexture();
            }
            
            spr.FillBuffer(null);
            spr.DrawFromBuffer();
        }
    }

    private void initBuffers() {
        if(vertexBuffer == null) {
            vertexBuffer = new VBO(MAX_SPRITES*8*4, BufferTarget.Vertex, Usage.STREAM);
        }
        if(indexBuffer == null) {
            BuildIndexBuffer();
        }
    }
    
    public void DrawNormal() {
        // Oldschool
        boolean useVBO = Ref.glRef.isShadersSupported();
        if(!useVBO) {
            DrawNormalFixedFunction();
            return;
        }

        // Newschool
        initBuffers();
        
        if(NormalSpriteOffset == 0)
            return;

        // nSprites * Sprite.Stride * 4 verts pr sprite
        vertexBuffer.discard();
        ByteBuffer vboBuffer = vertexBuffer.map(NormalSpriteOffset*Sprite.STRIDE*4);
        ArrayList<SimpleEntry<Integer, CubeTexture>> textureChanges = new ArrayList<SimpleEntry<Integer, CubeTexture>>();
        CubeTexture tex = null;
        int spriteIndex = 0;

        sortSprites.clear();
        for (int i= 0; i < NormalSpriteOffset; i++) {
            Sprite spr = Sprites[NormalSprites[i]];
            if(spr.invalid) {
                Common.LogDebug("Invalid sprite");
                spr.invalid = false;
            }
            // Does it need alpha sorting?
            if(spr.sort) {
                sortSprites.add(spr);
            } else {
                // Got texture change?
                if(spr.Texture != tex) {
                    tex = spr.Texture;
                    textureChanges.add(new SimpleEntry<Integer, CubeTexture>(spriteIndex*6, tex));
                }
                spr.FillBuffer(vboBuffer); // fill VBO
                spriteIndex++;
            }

            
//            spr.Draw();
        }
//        textureChanges.add(new SimpleEntry<Integer, CubeTexture>(spriteIndex*6, null));

        // Sort alpha sprites
        Collections.sort(sortSprites, DEPTHCOMPARER);
        for (int i= 0; i < sortSprites.size(); i++) {
            Sprite spr = sortSprites.get(i);
            //System.out.println(spr.getDepth() + ":" + spr.Texture.GetID());
            // Got texture change?
            if(spr.Texture != tex) {
                tex = spr.Texture;
                textureChanges.add(new SimpleEntry<Integer, CubeTexture>(spriteIndex*6, tex));
            }
            spr.FillBuffer(vboBuffer); // fill VBO
            spriteIndex++;
        }
        textureChanges.add(new SimpleEntry<Integer, CubeTexture>(spriteIndex*6, null));
        //System.out.println("-----------");
        // Fill the OpenGL buffer
        vboBuffer.flip();
        vertexBuffer.unmap();

        // Ensure our buffer is bound
        //Ref.glRef.bindVBO(BufferTarget.Vertex, vertexVBOId);
        indexBuffer.bind();
        
        int stride = Sprite.STRIDE;
//        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        ARBVertexShader.glEnableVertexAttribArrayARB(0); // position
        ARBVertexShader.glVertexAttribPointerARB(0, 3, GL11.GL_FLOAT, false, stride, 0);

//
//        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        ARBVertexShader.glEnableVertexAttribArrayARB(1); // color
        ARBVertexShader.glVertexAttribPointerARB(1, 4, GL11.GL_UNSIGNED_BYTE, true, stride, 3*4);

//        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        ARBVertexShader.glEnableVertexAttribArrayARB(2); // coords
        ARBVertexShader.glVertexAttribPointerARB(2, 2, GL11.GL_FLOAT, false, stride, 4*4);
        GLRef.checkError();

        
        CubeTexture currentTex = null;
        int offset = 0;
        int last = 0;
        for (int i= 0; i < textureChanges.size(); i++) {
            SimpleEntry<Integer, CubeTexture> entry = textureChanges.get(i);
            int end = entry.getKey(); // end indice of this call

            // indices to draw
            int lenght = (end-last);

            if(lenght > 0) {
                int callStart = (last/6)*stride;
                int callEnd = (end/6)*stride;
                int callLenght = lenght;
                
//                callEnd =  stride;
//                callLenght = 1;

                GL12.glDrawRangeElements(GL11.GL_TRIANGLES, callStart, callEnd-1, callLenght, GL11.GL_UNSIGNED_INT, offset);
                GLRef.checkError();
                //GL12.glDrawRangeElements(GL11.GL_TRIANGLES, callStart, callEnd, callLenght, GL11.GL_UNSIGNED_INT, offset);
            }

            // Prepare for next call
            last = end; // used for lenght
            offset += lenght*4; // calculate offset
            currentTex = entry.getValue(); // get next texture
            
            // Set texture for next call
            if(currentTex != null)
                currentTex.Bind();
            else
                Ref.ResMan.SetWhiteTexture();
        }

        // Clean up
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        vertexBuffer.unbind();
        indexBuffer.unbind();
        GLRef.checkError();
    }

    // use vbo for HUD sprites
    int[] events = new int[1000];
    private void drawHUDVBO() {
        initBuffers();
        if(HUDSpriteOffset == 0) return;
        // render info
        int nEvents = 0;

        vertexBuffer.discard();
        ByteBuffer buffer = vertexBuffer.map(HUDSpriteOffset*Sprite.STRIDE*4);
        CubeTexture tex = Ref.ResMan.getWhiteTexture();
        CubeTexture nulltex = tex;
        for (int i= 0; i < HUDSpriteOffset; i++) {
            int index = HUDSprites[i];
            Sprite spr = Sprites[index];
            if(spr.invalid) {
                Common.LogDebug("Invalid sprite");
                spr.invalid = false;
            }

            boolean special = spr.special != 0;
            boolean texturechange = !special && (spr.Texture == null ? (tex != nulltex):(spr.Texture != tex));
            if(!special) spr.FillBuffer(buffer);
            if(texturechange) {
                tex = spr.Texture;
            }

            // register events that require a new drawcall
            if(special || texturechange) events[nEvents++] = i;
            if(nEvents >= events.length) {
                // resize event array
                int[] newevents = new int[events.length*2];
                System.arraycopy(events, 0, newevents, 0, events.length);
                events = newevents;
            }
        }
        vertexBuffer.unmap();


        // Now set up the render state
        indexBuffer.bind();

        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_POSITION); // position
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_POSITION, 2, GL11.GL_FLOAT, false, Sprite.STRIDE, 0);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COLOR); // color
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COLOR, 4, GL11.GL_UNSIGNED_BYTE, true, Sprite.STRIDE, 2*4);
        ARBVertexShader.glEnableVertexAttribArrayARB(Shader.INDICE_COORDS); // coords
        ARBVertexShader.glVertexAttribPointerARB(Shader.INDICE_COORDS, 1, GL11.GL_FLOAT, false, Sprite.STRIDE, 3*4);

        
        tex = nulltex;
        tex.Bind();
        int offset = 0;
        for (int i= 0; i <= nEvents; i++) {
            // Go over the events, doing batched renders
            int batchStart = i == 0?0:events[i-1];
            int batchNext = i == nEvents?HUDSpriteOffset:events[i];
            int batchLenght = batchNext - batchStart;
            if(batchLenght > 0) {
                // Draw
                GL12.glDrawRangeElements(GL11.GL_TRIANGLES,batchStart*4,(batchNext*4)-1,6*batchLenght,GL11.GL_UNSIGNED_INT, offset);
                offset += batchLenght*6*4;
            }

            // Handle event for next batch
            if(i != nEvents) {
                Sprite spr = Sprites[HUDSprites[events[i]]];
                boolean special = spr.special != 0;
                boolean texturechange = !special && (spr.Texture == null ? (tex != nulltex):(spr.Texture != tex));
                if(special) {
                    spr.doSpecial();
                } else if(texturechange) {
                    tex = spr.Texture;
                    if(tex == null) nulltex.Bind();
                    else tex.Bind();
                }
            }
        }

        // Clean up
        GL20.glDisableVertexAttribArray(Shader.INDICE_POSITION);
        GL20.glDisableVertexAttribArray(Shader.INDICE_COLOR);
        GL20.glDisableVertexAttribArray(Shader.INDICE_COORDS);
        vertexBuffer.unbind();
        indexBuffer.unbind();
    }

    public void DrawHUD() {
        if(HUDSpriteOffset == 0) return;
        
        Ref.glRef.PushShader(Ref.glRef.getShader("sprite"));
        if(Ref.glRef.isShadersSupported()) {
            drawHUDVBO();
        } else {
            CubeTexture tex = Ref.ResMan.getWhiteTexture();
            tex.Bind();
            for (int i= 0; i < HUDSpriteOffset; i++) {
                int index = HUDSprites[i];
                Sprite spr = Sprites[index];
                if(spr.invalid) {
                    Common.LogDebug("Invalid sprite");
                    spr.invalid = false;
                }
                if(spr.Texture != tex) {
                    tex = spr.Texture;
                    if(tex != null)
                        tex.Bind();
                    else
                        Ref.ResMan.SetWhiteTexture();
                }

                Sprites[index].DrawFromBuffer();
            }
        }
        Ref.glRef.PopShader();
    }

    public void Reset() {
        SpriteOffset = 0;
        HUDSpriteOffset = 0;
        NormalSpriteOffset = 0;
    }
}
