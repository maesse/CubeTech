/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.ui;

import cubetech.gfx.BlendComposite;
import cubetech.gfx.CubeMaterial.Filtering;
import cubetech.gfx.ResourceManager;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class MaterialCanvas extends JPanel implements MouseListener, MouseMotionListener
{
    Image currentImg = null;
    float zoomAmount = 1f;
    // current mouse cursor in image pixels
    int mouse_x = 0;
    int mouse_y = 0;
    boolean drawAlphaBG = true;
    MaterialUI parent = null;
    Color color = new Color(255,255,255,255);
    Filtering filer = Filtering.POINT;
    BufferedImage composite = null;
    boolean dirty = true;
    boolean zoomtocoords = false;

    Point dragStart = null;
    Point dragEnd = null;

    int width, height;

    int minx, miny, w, h;

    int realWidth, realHeight;

    int animationOffset = 0;
    int framecount = 0;

    boolean coordLock = false;

    public void lockCoords(boolean value) {
        coordLock = value;
        repaint();
    }

    public void setFrameCount(int value) {
        framecount = value;
        repaint();
    }

    public void setAnimationOffset(int value) {
        animationOffset = value;
        repaint();
    }

    public void setDragStart(Point point) {
        dragStart = point;
    }

    public void setDragEnd(Point point) {
        dragEnd = point;
    }

    public boolean setW(int v) {
        if( minx + v > realWidth)
            return false;
        w = v;
        if(zoomtocoords) // viewport size change when zoomed to coords
            setZoom(zoomAmount);
        repaint();
        return true;
    }
    public boolean setH(int v) {
        if( miny + v > realHeight)
            return false;
        h = v;
        if(zoomtocoords) // viewport size change when zoomed to coords
            setZoom(zoomAmount);
        repaint();
        return true;
    }
    public boolean setX(int v) {
        if( v + w > realWidth)
            return false;
        minx = v;
        repaint();
        return true;
    }
    public boolean setY(int v) {
        if( v + h > realHeight)
            return false;
        miny = v;
        repaint();
        return true;
    }

    public int getW() {
        return w;
    }
    public int getH() {
        return h;
    }
    public int getMinX() {
        return minx;
    }
    public int getMinY() {
        return miny;
    }

    public MaterialCanvas(MaterialUI pat) {
        parent = pat;
        addMouseListener(this);
        addMouseMotionListener(this);

    }

    private void DoComposite() {
        // so hacky
        composite = new BufferedImage(currentImg.getWidth(null), currentImg.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D comp = (Graphics2D)composite.getGraphics();
        comp.drawImage(currentImg, 0, 0, null);
        comp.setComposite(BlendComposite.Multiply);
        comp.setColor(color);
        comp.fillRect(0, 0, currentImg.getWidth(null), currentImg.getHeight(null));
        dirty = false;
    }

    public void setColor(Color color) {
        this.color = color;
        dirty = true;
        repaint();
    }

    public boolean setFilter(Filtering filter) {
        boolean changed = filter != filer;
        this.filer = filter;
        repaint();
        return changed;
    }

    public void setZoom(float zoom) {
        zoomAmount = zoom;

        if(currentImg == null)
            return;

        if(zoomtocoords) {
            width = (int)(zoomAmount * w);
            height = (int)(zoomAmount * h);
        } else {
            width = (int)(zoomAmount * currentImg.getWidth(null));
            height = (int)(zoomAmount * currentImg.getHeight(null));
        }

        realWidth = width;
        realHeight = height;
        //if(!zoomtocoords)
            realWidth = currentImg.getWidth(null);
        //if(!zoomtocoords)
            realHeight = currentImg.getHeight(null);

        setSize(width, height);
        setPreferredSize(getSize());
//            invalidate();
    }

    public void enableAlphaBackground(boolean value) {
        drawAlphaBG = value;
        repaint();
    }

    public void setImg(Image img) {
        zoomAmount = 1f;
        
        currentImg = img;

        if(img == null) {
            setSize(100, 100);
            setPreferredSize(getSize());
            zoomAmount = 1f;
            dirty = true;
            dragEnd = dragStart = null;
            zoomtocoords = false;
            invalidate();
            return;
        }
        width = (int)(zoomAmount * currentImg.getWidth(null));
        height = (int)(zoomAmount * currentImg.getHeight(null));
        // When not zooming to coords, we want the actual texture
        // width and height, not the npot transformed width/height
        realWidth = width;
        realHeight = height;
        //if(!zoomtocoords)
            realWidth = currentImg.getWidth(null);
        //if(!zoomtocoords)
            realHeight = currentImg.getHeight(null);
        setSize(width, height);
        setPreferredSize(getSize());
        dirty = true;
//            composite = new BufferedImage(currentImg.getWidth(null), currentImg.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        invalidate();
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if(currentImg == null)
            return;

        if(dirty)
            DoComposite();

        Graphics2D g2 = (Graphics2D)g;

        if(drawAlphaBG)
            DrawAlphaBackground(g, width, height);
//            g.fillRect(0, 0, 100, 100);




//            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        if(filer == Filtering.LINEAR)
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        else
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        g2.setComposite(BlendComposite.Normal.derive(color.getAlpha()/255f));

        if(zoomtocoords) {
            int animMinx = minx;
            int aminMiny = miny;

            if(animationOffset != 0) {
                int extraWidth = (animationOffset) * w;

                int maxprRow = 0;
                if(w != 0)
                    maxprRow = (currentImg.getWidth(null) - minx) / w;
                animMinx += extraWidth;
                while(animMinx + w > currentImg.getWidth(null)) {
                    animMinx -= maxprRow * w;
                    //animMinx += 10;
                    aminMiny += h;
                }
//                if(aminMiny + h > currentImg.getHeight(null))
//                    continue; // cannot be seen
            }

            if(aminMiny + h <= currentImg.getHeight(null))
                g2.drawImage(composite, 0, 0, (int)(zoomAmount * w), (int)(zoomAmount * h), animMinx, aminMiny, animMinx+w, aminMiny+h, null);
        } else
            g2.drawImage(composite, 0, 0, (int)(zoomAmount * currentImg.getWidth(null)), (int)(zoomAmount * currentImg.getHeight(null)), null);
        g2.setComposite(BlendComposite.Normal);

        if(dragStart != null && dragEnd != null && !zoomtocoords) {
            g2.setColor(Color.black);
            int count = framecount;
            if(count <= 0)
                count = 1;

            int maxprRow = 0;
            if(w != 0)
                maxprRow = (currentImg.getWidth(null) - minx) / w;
            
            for (int i= 0; i < count; i++) {
                int extraWidth = (i) * w;
                int animMinx = minx;
                int aminMiny = miny;

                animMinx += extraWidth;
//                if(animMinx + w > currentImg.getWidth(null) && aminMiny + h < currentImg.getHeight(null)) {
//
//                    int line = (animMinx + w) - currentImg.getWidth(null);
//                    int lensave = line;
//                    lensave = lensave % (currentImg.getWidth(null) - minx);
////                    lensave++;
//                    line /= currentImg.getWidth(null) - minx;
//                    line++;
//                    aminMiny += h * line;
//                    animMinx = minx + lensave;
//                }
                while(animMinx + w > currentImg.getWidth(null)) {
                    animMinx -= maxprRow * w;
                    //animMinx += 10;
                    aminMiny += h;
                }
                if(aminMiny + h > currentImg.getHeight(null))
                    continue; // cannot be seen
                g2.drawRect(Math.max(0, (int) (zoomAmount * animMinx)), Math.max(0,(int) (zoomAmount * aminMiny)), Math.min((int) (zoomAmount * w), width), Math.min((int) (zoomAmount * h),height));
            }

            int animMinx = minx;
            int aminMiny = miny;

            if(animationOffset != 0) {
                int extraWidth = (animationOffset) * w;

                maxprRow = 0;
                if(w != 0)
                    maxprRow = (currentImg.getWidth(null) - minx) / w;
                animMinx += extraWidth;
                while(animMinx + w > currentImg.getWidth(null)) {
                    animMinx -= maxprRow * w;
                    //animMinx += 10;
                    aminMiny += h;
                }
//                if(aminMiny + h > currentImg.getHeight(null))
//                    continue; // cannot be seen
            }
            
            g2.setColor(Color.white);
            if(coordLock)
                g2.setColor(Color.LIGHT_GRAY);
            if(aminMiny + h <= currentImg.getHeight(null))
                g2.drawRect((int) (zoomAmount * animMinx), (int) (zoomAmount * aminMiny), (int) (zoomAmount * w), (int) (zoomAmount * h));
        }

        //System.out.println(color.getAlpha());


//            g.drawImage(getGraphics()., 0, 0, null);
    }

    private void updateCoords() {
        if(dragStart != null && dragEnd != null && currentImg != null) {
            if(dragStart.x > width)
                dragStart.x = width;
            if(dragStart.y > height)
                dragStart.y = height;
            if(dragEnd.x > width)
                dragEnd.x = width;
            if(dragEnd.y > height)
                dragEnd.y = height;


            // Figure out mins and size
            if(dragStart.x < dragEnd.x)
            {
                minx = dragStart.x;
                w = dragEnd.x - minx;
            } else
            {
                minx = dragEnd.x;
                w = dragStart.x - minx;
            }
            if(dragStart.y < dragEnd.y) {
                miny = dragStart.y;
                h = dragEnd.y - miny;
            } else {
                miny = dragEnd.y;
                h = dragStart.y - miny;
            }



            // Make sure offsets doesn't go out of bounds
            if(w == 0)
                w = 1;
            if(h == 0)
                h = 1;
            if(minx >= realWidth)
                minx = realWidth-1;
            else if(minx + w > realWidth)
                w = realWidth - minx;
            if(miny >= realHeight)
                miny = realHeight-1;
            else if(miny + h > realHeight)
                h = realHeight - miny;
            if(w == 0) {
                w = 1;
                
            }
            if(h == 0) {
                h = 1;
                
            }

            parent.TexOffsetChanged(minx, miny, w, h);
        }
    }

    private void DrawAlphaBackground(Graphics g, int width, int height) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        Color lightGray = new Color(204, 204, 204);
        g.setColor(lightGray);


        int blockSpacing = 8;
        boolean topaint = true;
        int nHeight = height/blockSpacing;
        if(height%blockSpacing > 0)
            nHeight++;
        int nWidth = width/blockSpacing;
        if(width % blockSpacing > 0)
            nWidth++;
        for (int y= 0; y < nHeight; y++) {
            topaint = (y%2 == 0);
            for (int x= 0; x < nWidth; x++) {
                if(topaint) {
                    int overflowx = Math.max(0, (x*blockSpacing + blockSpacing) - width);
                    int overflowy = Math.max(0, (y*blockSpacing + blockSpacing) - height);
                    g.fillRect(x*blockSpacing, y*blockSpacing, blockSpacing-overflowx, blockSpacing-overflowy);
                }

                topaint = !topaint;
            }
        }
    }



    public void mouseDragged(MouseEvent e) {
        if(zoomtocoords || coordLock)
            return;
        int newx, newy;
        newx = (int)(e.getPoint().x / zoomAmount);
        newy = (int) (e.getPoint().y / zoomAmount);

        // Don't let it get negatiev
        newx = Math.max(0, newx);
        newy = Math.max(0, newy);

        dragEnd = new Point(newx, newy);
        updateCoords();
        repaint();
    }

    public void mouseMoved(MouseEvent e) {
        if(currentImg == null)
            return;

        mouse_x = (int)(e.getPoint().x / zoomAmount);
        mouse_y = (int)(e.getPoint().y / zoomAmount);
        parent.updateMouseCoords(mouse_x, mouse_y);
    }

    public void mouseClicked(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {
        if(zoomtocoords || coordLock)
            return;
        dragStart = new Point((int)(e.getPoint().x / zoomAmount), (int) (e.getPoint().y / zoomAmount));
        dragEnd = new Point((int)(e.getPoint().x / zoomAmount), (int) (e.getPoint().y / zoomAmount));
        updateCoords();
    }

    public void mouseReleased(MouseEvent e) {
        if(zoomtocoords || coordLock)
            return;
        if(dragStart != null) {
            int newx = (int)(e.getPoint().x / zoomAmount);
            int newy = (int) (e.getPoint().y / zoomAmount);

            newx = Math.max(0, newx);
            newy = Math.max(0, newy);

            dragEnd = new Point(newx, newy);
            updateCoords();
            repaint();
        }
    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }


    public void zoomToCoords(boolean enable) {
        // When true, only show the part in the texcoord rectangle
        zoomtocoords = enable;

        setZoom(zoomAmount);
    }

    public void init(int nminx, int nminy, int nw, int nh) {
        // just needs to init dragStart ang dragEnd
        setZoom(1);
        dragStart = new Point(nminx, nminy);
        dragEnd = new Point(nminx+nw, nminy+nh);
        updateCoords();
        repaint();
    }
}
