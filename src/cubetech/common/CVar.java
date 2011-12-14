/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cubetech.common;

import cubetech.misc.Ref;
import java.util.EnumSet;

/**
 *
 * @author mads
 */
public class CVar {
    public String Name;
    public String resetString;
    public EnumSet<CVarFlags> flags = EnumSet.of(CVarFlags.NONE);
    public boolean modified;
    public int modificationCount;
    public Float fValue;
    public int iValue;
    public String sValue;
    public String latchedString;
    public boolean validate;
    public boolean isInteger;
    public float Min, Max; // Min/max values

    public void Print() {
        System.out.print(Name + " is " + sValue);
        if(flags.contains(CVarFlags.ROM)) {
            if(sValue.equals(resetString))
                System.out.print(", the default");
            else
                System.out.print(" default: " + resetString);
        }

        System.out.println("");
        if(latchedString != null)
            System.out.println(" latched: " + latchedString);
    }

    public void set(String string) {
        Ref.cvars.Set2(Name, string, true);
    }

    /**
     * Converts this cvars iValue to a boolean.
     * Everything except 0 is true.
     */
    public boolean isTrue() {
        return iValue != 0;
    }

    /**
     * Sets the latched string and clears the modified flag
     */
    public void applyLatched() {
        if(latchedString == null) return;
        set(latchedString);
        modified = false;
    }
}
