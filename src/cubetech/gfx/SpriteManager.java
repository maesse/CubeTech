package cubetech.gfx;

import cubetech.common.Common;
import cubetech.gfx.GLRef.BufferTarget;
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
    
    private int vertexVBOId = -1;
    private int indexVBOId = -1;

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
        ByteBuffer buf = Ref.glRef.mapVBO(BufferTarget.Index, indexVBOId, MAX_SPRITES*6*4);
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
        Ref.glRef.unmapVBO(BufferTarget.Index, true);
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
    
    public void DrawNormal() {
        boolean useVBO = Ref.glRef.isShadersSupported();
        if(!useVBO) {
            DrawNormalFixedFunction();
            return;
        }
        if(vertexVBOId == -1) {
            vertexVBOId = Ref.glRef.createVBOid();
            Ref.glRef.sizeVBO(BufferTarget.Vertex, vertexVBOId, MAX_SPRITES*8*4);
            return;
        } else if(indexVBOId == -1) {
            indexVBOId = Ref.glRef.createVBOid();
            Ref.glRef.sizeVBO(BufferTarget.Index, indexVBOId, MAX_SPRITES*6);
            BuildIndexBuffer();
            return;
        }
        
        if(NormalSpriteOffset == 0)
            return;

        // nSprites * Sprite.Stride * 4 verts pr sprite
        ByteBuffer vboBuffer = Ref.glRef.mapVBO(BufferTarget.Vertex, vertexVBOId, NormalSpriteOffset*Sprite.STRIDE*4);
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
        Ref.glRef.unmapVBO(BufferTarget.Vertex, false);

        // Ensure our buffer is bound
        //Ref.glRef.bindVBO(BufferTarget.Vertex, vertexVBOId);
        Ref.glRef.bindVBO(BufferTarget.Index, indexVBOId);
        
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
//        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
//        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
//        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL20.glDisableVertexAttribArray(0);
        GL20.glDisableVertexAttribArray(1);
        GL20.glDisableVertexAttribArray(2);
        Ref.glRef.unbindVBO(BufferTarget.Vertex);
        Ref.glRef.unbindVBO(BufferTarget.Index);
        GLRef.checkError();
    }

    public void DrawHUD() {
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
                
            Sprites[index].Draw();
        }
    }

    public void Reset() {
        SpriteOffset = 0;
        HUDSpriteOffset = 0;
        NormalSpriteOffset = 0;
    }
}
