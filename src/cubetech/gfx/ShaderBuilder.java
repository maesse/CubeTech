/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.gfx;

import cubetech.misc.Ref;
import java.util.HashMap;

/**
 *
 * @author mads
 */
public class ShaderBuilder {
    private String name;
    private HashMap<String, Integer> attributes = new HashMap<String, Integer>();
    private String[] textureUniforms = new String[8];
    private boolean used = false;

    public ShaderBuilder(String name) {
        this.name = name;
    }

    public Shader createShader() throws Exception {
        if(used) throw new IllegalAccessException("Can't reuse the builder, sorry");
        used = true;
        return new Shader(this);
    }

    public ShaderBuilder setAttribute(String name, int index) {
        attributes.put(name, index);
        return this;
    }

    public ShaderBuilder mapTextureUniform(String name, int index) {
        textureUniforms[index] = name;
        return this;
    }

    public HashMap<String, Integer> getAttributes() {
        return attributes;
    }

    public String[] getTextureUniforms() {
        return textureUniforms;
    }

    public String getName() {
        return name;
    }
}
