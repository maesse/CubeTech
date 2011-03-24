package cubetech.ui;

import cubetech.gfx.CubeMaterial;
import cubetech.gfx.CubeMaterial.Filtering;
import cubetech.gfx.ResourceManager;
import cubetech.misc.Ref;
import java.awt.Color;
import java.awt.Event;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class MaterialUI extends javax.swing.JPanel {
    JScrollPane imgScoller;
    private MaterialCanvas canvas = null;

    ImageIcon unlocked = new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/lock_open.png"));
    ImageIcon locked = new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/lock.png"));
    
    // View settings
    float zoomAmount = 1f;
    boolean changing = false; // prevents recursive component value changes
    boolean modified = false;

    // The material that is being viewed or changed
    CubeMaterial mat = null;
    Image img = null;

    private FileFilter materialFilter = new FileFilter() {

        @Override
        public boolean accept(File f) {
            if(f.isDirectory())
                return true;

            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');

            if (i > 0 &&  i < s.length() - 1) {
                ext = s.substring(i+1).toLowerCase();
            }

             if(ext == null || ext.isEmpty())
                return false;

            if(ext.equalsIgnoreCase("mat"))
                return true;
            return false;
        }

        @Override
        public String getDescription() {
            return "Materials (*.mat)";
        }
    };
    private FileFilter textureFilter = new FileFilter() {

        @Override
        public boolean accept(File f) {
            if(f.isDirectory())
                return true;

            String ext = null;
            String s = f.getName();
            int i = s.lastIndexOf('.');

            if (i > 0 &&  i < s.length() - 1) {
                ext = s.substring(i+1).toLowerCase();
            }

            if(ext == null || ext.isEmpty())
                return false;

            if(ext.equalsIgnoreCase("png"))
                return true;
            else if(ext.equalsIgnoreCase("jpg"))
                return true;
            return false;
        }

        @Override
        public String getDescription() {
            return "Textures (png|jpg)";
        }
    };


    /** Creates new form MaterialUI */
    public MaterialUI() {
        InputMap inputmap = this.getInputMap();
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK);
        inputmap.put(key, "save");
        key = KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK);
        inputmap.put(key, "new");
        key = KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK);
        inputmap.put(key, "open");
        key = KeyStroke.getKeyStroke(KeyEvent.VK_T, Event.CTRL_MASK);
        inputmap.put(key, "opentex");

        ActionMap map = this.getActionMap();
        Action saveShortCutAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                SaveMaterial();
            }
        };
        Action newShortCutAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                NewMaterial();
            }
        };
        Action openShortCutAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                OpenMaterial();
            }
        };
        Action opentexShortCutAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                OpenLoadTexture();
            }
        };
        map.put("save", saveShortCutAction);
        map.put("open", openShortCutAction);
        map.put("opentex", opentexShortCutAction);
        map.put("new", newShortCutAction);
        this.setActionMap(map);
        this.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputmap);
        
        initComponents();
        canvas = new MaterialCanvas(this);
        canvas.setIgnoreRepaint(true);

        imgScoller = new JScrollPane(canvas);
        jSplitPane1.setRightComponent(imgScoller);
        
        CubeMaterial mata = new CubeMaterial();
        LoadMaterial(mata);
        modified = false;

        
    }

    // Canvas callback
    public void TexOffsetChanged(int x, int y, int w, int h) {
        offsetX.setValue(x);
        offsetY.setValue(y);
        sizeX.setValue(w);
        sizeY.setValue(h);
        modified = true;
    }

    // Canvas callback
    public void updateMouseCoords(int x, int y) {
        labelCursor.setText("[x = " + x + ", y = " + y + "]");
    }

    private void LoadMaterial(CubeMaterial mate) {
        mat = mate;
        ResetUI();
        labelMaterial.setText("Unsaved");
        labelMaterial.setToolTipText(null);
        if(!mat.getName().equals("None")) {
            String shortenedPath = mat.getPath();
            if(shortenedPath.length() > 19)
                shortenedPath = mat.getName();
            labelMaterial.setText(shortenedPath);
            labelMaterial.setToolTipText(mat.getPath());
        }

        LoadTexture(mat.getTextureName());
        settingTranslucent.setSelected(mat.getTranslucent() >= 1);
        settingAdditive.setSelected(mat.getTranslucent() == 2);
        settingIgnoreZ.setSelected(mat.isIgnorez());
        if(mat.getFilter() == Filtering.LINEAR)
            settingLinear.setSelected(true);
        else
            settingPoint.setSelected(true);

        canvas.setFilter(mat.getFilter());

        if(mat.getAnimCount() > 1) {
            enableAnimation.setSelected(true);
            ToggleAnimation(true);
            animFrames.setValue(mat.getAnimCount());
        }

        animDelay.setValue(mat.getFramedelay());

        // Convert texture coords to pixel offsets
        if(img != null) {
            int texWidth = img.getWidth(null);
            int texHeight = img.getHeight(null);


            int sizX = (int) (Math.ceil(mat.getTextureSize().x * texWidth));
            int temp = (int) (Math.floor(mat.getTextureSize().x * texWidth));
            float ftemp = mat.getTextureSize().x * texWidth;
            if(Math.abs(temp - ftemp) < Math.abs(sizX - ftemp))
                sizX = temp;

            int sizY = (int) Math.ceil((mat.getTextureSize().y * texHeight));
            temp = (int) Math.floor((mat.getTextureSize().y * texHeight));
            ftemp = mat.getTextureSize().y * texHeight;
            if(Math.abs(temp - ftemp) < Math.abs(sizY - ftemp))
                sizY = temp;

            
            int offX = (int) (Math.ceil(mat.getTextureOffset().x * texWidth));
            temp = (int)Math.floor(mat.getTextureOffset().x * texWidth);
            ftemp = (mat.getTextureOffset().x * texWidth);
            if(Math.abs(temp - ftemp) < Math.abs(offX - ftemp))
                offX = temp;
            
            int offY = (int)Math.floor(mat.getTextureOffset().y * texHeight);
            temp = (int)Math.ceil(mat.getTextureOffset().y * texHeight);
            ftemp = (mat.getTextureOffset().y * texHeight);
            if(Math.abs(temp - ftemp) < Math.abs(offY - ftemp))
                offY = temp;

//            offset.y = texHeight - (canvas.getMinY() + canvas.getH());
            // Flip y
            offY = texHeight - (int) (offY + sizY);

            sizeX.setValue(sizX);
            sizeY.setValue(sizY);
            offsetX.setValue(offX);
            offsetY.setValue(offY);
            canvas.init(offX, offY, sizX, sizY);
        }

        // Load color
        Color col = new Color(mat.getColor().getRed(), mat.getColor().getGreen(), mat.getColor().getBlue(), mat.getColor().getAlpha());
        jSlider1.setValue(mat.getColor().getAlpha());
        canvas.setColor(col);
        colorPanel.setBackground(col);
    }

    private void ResetUI() {
        offsetX.setValue(0);
        offsetY.setValue(0);
        sizeX.setValue(1);
        sizeY.setValue(1);
        zoomAmount = 1f;
        canvas.setImg(null);
        colorPanel.setBackground(Color.white);
        labelZoom.setText(String.format("%d%%", (int) (zoomAmount*100)));
        labelTexture.setText("None");
        labelSize.setText("N/A");
        sizeWarning.setText("No Texture");
        jSlider1.setValue(255);
        jToggleButton1.setSelected(false);
        animFrameLabel.setText("1");
        animFrames.setValue(1);
        enableAnimation.setSelected(false);
        jButton9.setEnabled(false);
        jButton10.setEnabled(false);
        jButton11.setEnabled(false);
        animFrames.setEnabled(false);
        canvas.setAnimationOffset(0);
        canvas.setFrameCount(1);
        currentFrame = 0;
        canvas.lockCoords(false);
        animDelay.setEnabled(false);
        animDelay.setValue(200);
    }

    private void LoadTexture(String path) {
        if(path.equals("None"))
            return;
        
        try {
            // Try absolute path first
            if(ResourceManager.FileExists(path) && mat.getName().equals("None")) {
                img = Ref.ResMan.loadImage2(path);
            } else {
                // grab path from material
                String p = CubeMaterial.getPath(mat.getPath());
                String p2 = CubeMaterial.stripPath(path);
                img = Ref.ResMan.loadImage2(p + "/" + p2);
            }
            canvas.setImg(img);
            zoomAmount = 1f;
            labelZoom.setText(String.format("%d%%", (int) (zoomAmount*100)));

            String shortenedPath = path;
            if(path.length() > 19)
                shortenedPath = ".." + path.substring(path.length()-19, path.length());
            labelTexture.setText(shortenedPath);
            labelTexture.setToolTipText(path);
            int width = img.getWidth(null);
            int height = img.getHeight(null);
            labelSize.setText(String.format("%dx%d px", width, height));
            boolean npot = false;

//            if(width != ResourceManager.get2Fold(width)
//                    || height != ResourceManager.get2Fold(height))
//                npot = true;
            
            if(npot)
                sizeWarning.setText("Size is not power of 2");
            else
                sizeWarning.setText("");

            mat.setTextureName(path);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            ResetUI();
        }

    }

    private void OpenMaterial() {
        if(modified) {
            int result = JOptionPane.showConfirmDialog(this, "Save Modifications before continuing?", "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION);
            if(result == JOptionPane.CANCEL_OPTION)
                return;
            if(result == JOptionPane.YES_OPTION)
                SaveMaterial();
        }
        jFileChooser1.removeChoosableFileFilter(textureFilter);
        jFileChooser1.setFileFilter(materialFilter);
        jFileChooser1.setDialogTitle("Select material...");
        if(jFileChooser1.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = jFileChooser1.getSelectedFile();
            try {
                // TODO: Ask for saving
                CubeMaterial newmat = CubeMaterial.Load(file.getPath(), false);
                if(newmat != null)
                    LoadMaterial(newmat);
                modified = false;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Open Error", JOptionPane.ERROR_MESSAGE);
            }

        }
    }

    private void OpenLoadTexture() {
        jFileChooser1.removeChoosableFileFilter(materialFilter);
        jFileChooser1.setFileFilter(textureFilter);
        jFileChooser1.setDialogTitle("Select texture...");
        if(jFileChooser1.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = jFileChooser1.getSelectedFile();
            LoadTexture(file.getPath());
            if(img != null) {
                // Reset texcoords
                int texWidth = img.getWidth(null);
                int texHeight = img.getHeight(null);

                canvas.init(0,0,texWidth,texHeight);
            }
            modified = true;
        }
    }

    private void SaveMaterialAs() {
        jFileChooser1.removeChoosableFileFilter(textureFilter);
        jFileChooser1.setFileFilter(materialFilter);
        jFileChooser1.setDialogTitle("Save material....");
        if(jFileChooser1.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = jFileChooser1.getSelectedFile();
            String path = file.getPath();
            if(!materialFilter.accept(file)) {
                path += ".mat";
            }
            mat.setPath(path);
            setMaterialValues();

            try {
                mat.Save(path);
                modified = false;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void SaveMaterial() {
        if(mat.getName().equals("None"))
        {
            SaveMaterialAs();
            return;
        }
        setMaterialValues();
        try {
            mat.Save(mat.getPath());
            modified = false;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void NewMaterial() {
        if(modified) {
            int result = JOptionPane.showConfirmDialog(this, "Save Modifications before continuing?", "Save Changes?", JOptionPane.YES_NO_CANCEL_OPTION);
            if(result == JOptionPane.CANCEL_OPTION)
                return;
            if(result == JOptionPane.YES_OPTION)
                SaveMaterial();
        }
        CubeMaterial mata = new CubeMaterial();
        LoadMaterial(mata);
        modified = false;
    }

    int currentFrame = 0;

    private int getFrameCount() {
        return (Integer)animFrames.getValue();
    }

    private void NextFrame() {
        int next = currentFrame+1;
        if(next >= getFrameCount())
            next = 0;
        SetFrame(next);
    }

    private void PreviousFrame() {
        int prev = currentFrame-1;
        if(prev < 0)
            prev = getFrameCount()-1;
        SetFrame(prev);
    }

    private void SetFrame(int frame) {
        currentFrame = frame;
        animFrameLabel.setText(""+(frame+1));
        canvas.setAnimationOffset(frame);
    }

    private void ToggleAnimation(boolean newval) {
        // Let the canvas know that we wan't to see a grid of the frames
        if(newval) {
            // Enabling, ensure frames spinner is set
            if(getFrameCount() < 2)
                animFrames.setValue(2);
            canvas.setFrameCount(getFrameCount());
        } else {
            // disabling
            SetFrame(0);
            canvas.setFrameCount(0);
        }

        jButton9.setEnabled(newval);
        jButton10.setEnabled(newval);
        jButton11.setEnabled(newval);
        animFrames.setEnabled(newval);
        animDelay.setEnabled(newval);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        filterGroup1 = new javax.swing.ButtonGroup();
        jFileChooser1 = new javax.swing.JFileChooser();
        jColorChooser1 = new javax.swing.JColorChooser();
        jToolBar1 = new javax.swing.JToolBar();
        jButton1 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jCheckBox1 = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        labelZoom = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        labelCursor = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        settingTranslucent = new javax.swing.JCheckBox();
        settingAdditive = new javax.swing.JCheckBox();
        settingIgnoreZ = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        settingPoint = new javax.swing.JRadioButton();
        settingLinear = new javax.swing.JRadioButton();
        jLabel5 = new javax.swing.JLabel();
        colorPanel = new javax.swing.JPanel();
        jButton5 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jSlider1 = new javax.swing.JSlider();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        offsetX = new javax.swing.JSpinner();
        offsetY = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        sizeX = new javax.swing.JSpinner();
        sizeY = new javax.swing.JSpinner();
        jToggleButton1 = new javax.swing.JToggleButton();
        jToggleButton2 = new javax.swing.JToggleButton();
        jPanel6 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        labelTexture = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        labelSize = new javax.swing.JLabel();
        sizeWarning = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();
        labelMaterial = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        enableAnimation = new javax.swing.JCheckBox();
        jLabel15 = new javax.swing.JLabel();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jButton11 = new javax.swing.JButton();
        animFrameLabel = new javax.swing.JLabel();
        animFrames = new javax.swing.JSpinner();
        jLabel13 = new javax.swing.JLabel();
        animDelay = new javax.swing.JSpinner();
        jLabel14 = new javax.swing.JLabel();

        jFileChooser1.setCurrentDirectory(null);
        jFileChooser1.setDialogTitle("Open Material");

        jToolBar1.setBackground(java.awt.SystemColor.controlHighlight);
        jToolBar1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        jButton1.setBackground(java.awt.SystemColor.controlHighlight);
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/layer_create.png"))); // NOI18N
        jButton1.setToolTipText("Create new material (Ctrl+N)");
        jButton1.setBorder(null);
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setMaximumSize(new java.awt.Dimension(39, 39));
        jButton1.setMinimumSize(new java.awt.Dimension(39, 39));
        jButton1.setPreferredSize(new java.awt.Dimension(39, 39));
        jButton1.setVerifyInputWhenFocusTarget(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton1);

        jButton6.setBackground(java.awt.SystemColor.controlHighlight);
        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/layer_open.png"))); // NOI18N
        jButton6.setToolTipText("Open material... (Ctrl+O)");
        jButton6.setFocusable(false);
        jButton6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton6.setVerifyInputWhenFocusTarget(false);
        jButton6.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton6);

        jButton7.setBackground(java.awt.SystemColor.controlHighlight);
        jButton7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/layer_save.png"))); // NOI18N
        jButton7.setToolTipText("Save material (Ctrl+S)");
        jButton7.setFocusable(false);
        jButton7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton7.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton7);

        jButton8.setBackground(java.awt.SystemColor.controlHighlight);
        jButton8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/layer_saveas.png"))); // NOI18N
        jButton8.setToolTipText("Save material as...");
        jButton8.setFocusable(false);
        jButton8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton8.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton8ActionPerformed(evt);
            }
        });
        jToolBar1.add(jButton8);

        jSplitPane1.setDividerLocation(260);
        jSplitPane1.setPreferredSize(new java.awt.Dimension(325, 400));

        jPanel2.setPreferredSize(new java.awt.Dimension(230, 775));

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Viewer"));

        jCheckBox1.setSelected(true);
        jCheckBox1.setText("Checkered Background");
        jCheckBox1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jCheckBox1StateChanged(evt);
            }
        });
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jLabel1.setText("Zoom:");

        labelZoom.setText("100%");

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/magnifier_zoom_in.png"))); // NOI18N
        jButton2.setToolTipText("Zoom In");
        jButton2.setBorder(null);
        jButton2.setPreferredSize(new java.awt.Dimension(24, 24));
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/magnifier_zoom_out.png"))); // NOI18N
        jButton3.setToolTipText("Zoom Out");
        jButton3.setBorder(null);
        jButton3.setPreferredSize(new java.awt.Dimension(24, 24));
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel2.setText("Cursor:");

        labelCursor.setText("N/A");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBox1)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(labelZoom)
                                .addGap(18, 18, 18)
                                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(labelCursor))))
                .addContainerGap(39, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jCheckBox1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(labelZoom, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jButton3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(11, 11, 11)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(labelCursor))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Edit"));

        settingTranslucent.setSelected(true);
        settingTranslucent.setText("Translucent");
        settingTranslucent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingTranslucentActionPerformed(evt);
            }
        });

        settingAdditive.setText("Additive");

        settingIgnoreZ.setText("Ignore-Z");

        jLabel3.setText("Filtering:");

        filterGroup1.add(settingPoint);
        settingPoint.setSelected(true);
        settingPoint.setText("Point");
        settingPoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingPointActionPerformed(evt);
            }
        });

        filterGroup1.add(settingLinear);
        settingLinear.setText("Linear");
        settingLinear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingLinearActionPerformed(evt);
            }
        });

        jLabel5.setText("Color:");

        javax.swing.GroupLayout colorPanelLayout = new javax.swing.GroupLayout(colorPanel);
        colorPanel.setLayout(colorPanelLayout);
        colorPanelLayout.setHorizontalGroup(
            colorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 48, Short.MAX_VALUE)
        );
        colorPanelLayout.setVerticalGroup(
            colorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 23, Short.MAX_VALUE)
        );

        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/layer_rgb.png"))); // NOI18N
        jButton5.setToolTipText("Pick Color");
        jButton5.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jLabel7.setText("Alpha:");

        jSlider1.setMaximum(255);
        jSlider1.setToolTipText("255 is fully opaque");
        jSlider1.setValue(255);
        jSlider1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSlider1StateChanged(evt);
            }
        });

        jLabel10.setText("Texture Coordinates:");

        jLabel11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/transform_layer.png"))); // NOI18N
        jLabel11.setText("Offset");

        offsetX.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                offsetXStateChanged(evt);
            }
        });

        offsetY.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                offsetYStateChanged(evt);
            }
        });

        jLabel12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/layer_to_image_size.png"))); // NOI18N
        jLabel12.setText("Size");

        sizeX.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sizeXStateChanged(evt);
            }
        });

        sizeY.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sizeYStateChanged(evt);
            }
        });

        jToggleButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/zoom_layer.png"))); // NOI18N
        jToggleButton1.setText("Zoom to Coords");
        jToggleButton1.setFocusable(false);
        jToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton1ActionPerformed(evt);
            }
        });

        jToggleButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/lock_open.png"))); // NOI18N
        jToggleButton2.setToolTipText("Lock texture coordinates");
        jToggleButton2.setFocusable(false);
        jToggleButton2.setPreferredSize(new java.awt.Dimension(24, 24));
        jToggleButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel11)
                            .addComponent(jLabel12))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(sizeX)
                            .addComponent(offsetX, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(sizeY)
                            .addComponent(offsetY, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(settingIgnoreZ)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(settingTranslucent)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(settingAdditive))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jToggleButton2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jToggleButton1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(settingPoint))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel5))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(jSlider1, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel9))
                            .addGroup(jPanel5Layout.createSequentialGroup()
                                .addComponent(colorPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButton5)
                                    .addComponent(settingLinear))))))
                .addContainerGap(24, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(settingTranslucent)
                    .addComponent(settingAdditive))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(settingIgnoreZ)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(settingPoint)
                        .addComponent(jLabel3))
                    .addComponent(settingLinear))
                .addGap(4, 4, 4)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(colorPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 23, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
                    .addComponent(jSlider1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToggleButton2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(11, 11, 11)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(offsetX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(offsetY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sizeX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sizeY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToggleButton1)
                .addContainerGap())
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Information"));

        jLabel4.setText("Texture:");

        labelTexture.setText("N/A");

        jLabel6.setText("Size:");

        labelSize.setText("N/A");

        sizeWarning.setForeground(new java.awt.Color(153, 0, 0));
        sizeWarning.setText("jLabel8");

        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/layer_import.png"))); // NOI18N
        jButton4.setText("Load Texture...");
        jButton4.setToolTipText("Select texture for this material (Ctrl+T)");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jLabel8.setText("Material:");

        labelMaterial.setText("Unsaved");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(labelMaterial, javax.swing.GroupLayout.DEFAULT_SIZE, 136, Short.MAX_VALUE))
                    .addComponent(jButton4)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel6)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelSize)
                            .addComponent(sizeWarning)
                            .addComponent(labelTexture, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(labelMaterial))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(labelTexture))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(labelSize))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sizeWarning)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton4)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Animation"));

        enableAnimation.setText("Enable Animation");
        enableAnimation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableAnimationActionPerformed(evt);
            }
        });

        jLabel15.setText("Frames:");

        jButton9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/control_start.png"))); // NOI18N
        jButton9.setEnabled(false);
        jButton9.setPreferredSize(new java.awt.Dimension(24, 24));
        jButton9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton9ActionPerformed(evt);
            }
        });

        jButton10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/control_stop.png"))); // NOI18N
        jButton10.setEnabled(false);
        jButton10.setPreferredSize(new java.awt.Dimension(24, 24));
        jButton10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton10ActionPerformed(evt);
            }
        });

        jButton11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/cubetech/data/control_end.png"))); // NOI18N
        jButton11.setEnabled(false);
        jButton11.setPreferredSize(new java.awt.Dimension(24, 24));
        jButton11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton11ActionPerformed(evt);
            }
        });

        animFrameLabel.setForeground(new java.awt.Color(102, 102, 102));
        animFrameLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        animFrameLabel.setText("1");
        animFrameLabel.setToolTipText("Current Frame");
        animFrameLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        animFrames.setEnabled(false);
        animFrames.setPreferredSize(new java.awt.Dimension(40, 20));
        animFrames.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                animFramesStateChanged(evt);
            }
        });

        jLabel13.setText("FrameDelay");

        animDelay.setEnabled(false);

        jLabel14.setText("ms");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(enableAnimation)
                    .addComponent(animFrameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButton9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addGap(18, 18, 18)
                        .addComponent(animDelay, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel15)
                        .addGap(18, 18, 18)
                        .addComponent(animFrames, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel14)
                .addGap(79, 79, 79))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(enableAnimation)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(animFrameLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(animFrames, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(animDelay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jScrollPane1.setViewportView(jPanel2);

        jSplitPane1.setLeftComponent(jScrollPane1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 668, Short.MAX_VALUE)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, 668, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 778, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        NewMaterial();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        zoomAmount *= 2f;
        if(zoomAmount > 16f)
            zoomAmount = 16f;
        labelZoom.setText(String.format("%d%%", (int)(zoomAmount*100)));
        canvas.setZoom(zoomAmount);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        zoomAmount *= 0.5f;
        if(zoomAmount < 0.0625f)
            zoomAmount = 0.0625f;
        labelZoom.setText(String.format("%d%%", (int)(zoomAmount*100)));
        canvas.setZoom(zoomAmount);
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jCheckBox1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jCheckBox1StateChanged
        

    }//GEN-LAST:event_jCheckBox1StateChanged

    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        canvas.enableAlphaBackground(jCheckBox1.isSelected());
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void settingTranslucentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingTranslucentActionPerformed
        settingAdditive.setEnabled(settingTranslucent.isSelected());
        if(!settingTranslucent.isSelected()) // no translucent disables additive
            settingAdditive.setSelected(false);
        modified = true;
    }//GEN-LAST:event_settingTranslucentActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        OpenLoadTexture();

    }//GEN-LAST:event_jButton4ActionPerformed

    private void settingPointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingPointActionPerformed
        if(canvas.setFilter(settingPoint.isSelected()?Filtering.POINT:Filtering.LINEAR))
            modified = true;
    }//GEN-LAST:event_settingPointActionPerformed

    private void settingLinearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingLinearActionPerformed
        if(canvas.setFilter(settingLinear.isSelected()?Filtering.LINEAR:Filtering.POINT))
            modified = true;
    }//GEN-LAST:event_settingLinearActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        
        java.awt.Color col = JColorChooser.showDialog(this, "Select default Color", new Color(mat.getColor().getRed(), mat.getColor().getGreen(), mat.getColor().getBlue(), mat.getColor().getAlpha()));
        if(col == null)
            return;
        org.lwjgl.util.Color matColor = new org.lwjgl.util.Color(col.getRed(), col.getGreen(), col.getBlue(), jSlider1.getValue());
        col = new Color(col.getRed(), col.getGreen(), col.getBlue(), jSlider1.getValue());
        mat.setColor(matColor);
        canvas.setColor(col);
        colorPanel.setBackground(col);
        modified = true;
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jSlider1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSlider1StateChanged
        mat.getColor().setAlpha(jSlider1.getValue());
        canvas.setColor(new Color(mat.getColor().getRed(), mat.getColor().getGreen(), mat.getColor().getBlue(), jSlider1.getValue()));
        jLabel9.setText(""+jSlider1.getValue());
        modified = true;
    }//GEN-LAST:event_jSlider1StateChanged

    private void offsetXStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_offsetXStateChanged
        if(changing)
            return;
        changing = true;
        int old = canvas.getMinX();
        int newval = (Integer)offsetX.getValue();
        if(newval < 0)
            offsetX.setValue(old);
        else if(!canvas.setX(newval))
            offsetX.setValue(old);
        else
            modified = true;
        
        changing = false;
    }//GEN-LAST:event_offsetXStateChanged

    private void offsetYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_offsetYStateChanged
        if(changing)
            return;
        changing = true;
        int old = canvas.getMinY();
        int newval = (Integer)offsetY.getValue();
        if(newval < 0)
            offsetY.setValue(old);
        else if(!canvas.setY(newval))
            offsetY.setValue(old);
        else
            modified = true;

        changing = false;
    }//GEN-LAST:event_offsetYStateChanged

    private void sizeXStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sizeXStateChanged
        if(changing)
            return;
        changing = true;
        int old = canvas.getW();
        int newval = (Integer)sizeX.getValue();
        if(newval <= 0)
            sizeX.setValue(old);
        else if(!canvas.setW(newval))
            sizeX.setValue(old);
        else
            modified = true;
        changing = false;
    }//GEN-LAST:event_sizeXStateChanged
    
    private void sizeYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sizeYStateChanged
        if(changing)
            return;
        changing = true;
        int old = canvas.getH();
        int newval = (Integer)sizeY.getValue();
        if(newval <= 0)
            sizeY.setValue(old);
        else if(!canvas.setH(newval))
            sizeY.setValue(old);
        else
            modified = true;
        changing = false;
    }//GEN-LAST:event_sizeYStateChanged

    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
        canvas.zoomToCoords(jToggleButton1.isSelected());

//        if(!jToggleButton1.isSelected())
//            SetFrame(0);
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        SaveMaterial();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        OpenMaterial();
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton8ActionPerformed
        SaveMaterialAs();
    }//GEN-LAST:event_jButton8ActionPerformed

    private void enableAnimationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableAnimationActionPerformed
        // Enable/Disable animation
        boolean value = enableAnimation.isSelected();
        ToggleAnimation(value);
        modified = true;
    }//GEN-LAST:event_enableAnimationActionPerformed

    private void jButton9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton9ActionPerformed
        PreviousFrame();
    }//GEN-LAST:event_jButton9ActionPerformed

    private void jButton10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton10ActionPerformed
        SetFrame(0);
    }//GEN-LAST:event_jButton10ActionPerformed

    private void jButton11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton11ActionPerformed
        NextFrame();
    }//GEN-LAST:event_jButton11ActionPerformed

    private void animFramesStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_animFramesStateChanged
        if(changing)
            return;
        changing = true;

        int newval = (Integer)animFrames.getValue();
        if(newval <= 0)
            animFrames.setValue(newval+1);
        else
            modified = true;

        newval = (Integer)animFrames.getValue();
        canvas.setFrameCount(newval);

        if(currentFrame >= newval)
            SetFrame(newval-1);

        changing = false;
    }//GEN-LAST:event_animFramesStateChanged

    private void jToggleButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton2ActionPerformed
        // Texture lock
        if(jToggleButton2.isSelected()) {
            jToggleButton2.setIcon(locked);
            offsetX.setEnabled(false);
            offsetY.setEnabled(false);
            sizeX.setEnabled(false);
            sizeY.setEnabled(false);
            canvas.lockCoords(true);
        } else {
            jToggleButton2.setIcon(unlocked);
            offsetX.setEnabled(true);
            offsetY.setEnabled(true);
            sizeX.setEnabled(true);
            sizeY.setEnabled(true);
            canvas.lockCoords(false);
        }
    }//GEN-LAST:event_jToggleButton2ActionPerformed

    private void setMaterialValues() {
        mat.setFilter(settingLinear.isSelected()?Filtering.LINEAR:Filtering.POINT);
        mat.setIgnorez(settingIgnoreZ.isSelected());
        mat.setTranslucent(settingTranslucent.isSelected()?settingAdditive.isSelected()?2:1:0);
        mat.setColor(new org.lwjgl.util.Color(mat.getColor().getRed(), mat.getColor().getGreen(), mat.getColor().getBlue(), jSlider1.getValue()));

        // Calculate final texture offsets
        int texWidth = img.getWidth(null);
        int texHeight = img.getHeight(null);

        Vector2f offset = new Vector2f();
        offset.x = canvas.getMinX();
        offset.y = texHeight - (canvas.getMinY() + canvas.getH());
        Vector2f size = new Vector2f();
        size.x = canvas.getW();
        size.y = canvas.getH();

        offset.x /= texWidth;
        offset.y /= texHeight;
        size.x /= texWidth;
        size.y /= texHeight;

        mat.setTextureOffset(offset);
        mat.setTextureSize(size);
        String shortenedPath = mat.getPath();
        if(shortenedPath.length() > 19)
            shortenedPath = mat.getName();
        labelMaterial.setText(shortenedPath);
        labelMaterial.setToolTipText(mat.getPath());

        // anim
        mat.setAnimCount((Integer)animFrames.getValue());
        mat.setFramedelay((Integer)animDelay.getValue());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner animDelay;
    private javax.swing.JLabel animFrameLabel;
    private javax.swing.JSpinner animFrames;
    private javax.swing.JPanel colorPanel;
    private javax.swing.JCheckBox enableAnimation;
    private javax.swing.ButtonGroup filterGroup1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton11;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JColorChooser jColorChooser1;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSlider jSlider1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JToggleButton jToggleButton1;
    private javax.swing.JToggleButton jToggleButton2;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JLabel labelCursor;
    private javax.swing.JLabel labelMaterial;
    private javax.swing.JLabel labelSize;
    private javax.swing.JLabel labelTexture;
    private javax.swing.JLabel labelZoom;
    private javax.swing.JSpinner offsetX;
    private javax.swing.JSpinner offsetY;
    private javax.swing.JCheckBox settingAdditive;
    private javax.swing.JCheckBox settingIgnoreZ;
    private javax.swing.JRadioButton settingLinear;
    private javax.swing.JRadioButton settingPoint;
    private javax.swing.JCheckBox settingTranslucent;
    private javax.swing.JLabel sizeWarning;
    private javax.swing.JSpinner sizeX;
    private javax.swing.JSpinner sizeY;
    // End of variables declaration//GEN-END:variables

}
