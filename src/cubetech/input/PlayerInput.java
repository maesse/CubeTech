package cubetech.input;

import cubetech.common.Helper;
import cubetech.common.items.Weapon;
import cubetech.net.NetBuffer;
import java.util.Arrays;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class PlayerInput {
    // not sent
    public int[] MouseDelta = new int[2];

    // this is transmitted
    public Vector2f MousePos = new Vector2f(0.5f,0.5f); // no need to send this one
    public int[] angles = new int[3]; // can be reduced to 3 shorts
    public boolean[] buttons = new boolean[30];
    public int serverTime;
    public int WheelDelta;
    public Weapon weapon;
    public boolean Mouse1;
    public boolean Mouse1Diff;
    public boolean Mouse2;
    public boolean Mouse2Diff;
    public boolean Mouse3;
    public boolean Mouse3Diff;
    public boolean Forward;
    public boolean Back;
    public boolean Left;
    public boolean Right;
    public boolean Up;
    public boolean Down;

    public static PlayerInput ReadDeltaUserCmd(NetBuffer buf, PlayerInput oldcmd) {
        PlayerInput dest = new PlayerInput();
        dest.serverTime = buf.ReadInt();
        if(buf.ReadBool()) { // Got new data
            int hags = buf.ReadInt();
            int index = 0;
            dest.Forward = getBit(hags,index++);
            dest.Back = getBit(hags,index++);
            dest.Left = getBit(hags,index++);
            dest.Right = getBit(hags,index++);
            dest.Up = getBit(hags,index++);
            dest.Down = getBit(hags,index++);
            dest.Mouse1Diff = getBit(hags,index++);
            dest.Mouse1 = getBit(hags,index++);
            dest.Mouse2Diff = getBit(hags,index++);
            dest.Mouse2 = getBit(hags,index++);
            dest.Mouse3Diff = getBit(hags,index++);
            dest.Mouse3 = getBit(hags,index++);
            boolean hasWheel = getBit(hags,index++);
            boolean sameVec = getBit(hags,index++);

            if(hasWheel)
                dest.WheelDelta = buf.ReadInt();
            if(!sameVec)
                dest.MousePos = buf.ReadVector();
            else
                dest.MousePos.set(oldcmd.MousePos);

            dest.angles[0] = buf.ReadInt();
            dest.angles[1] = buf.ReadInt();
            dest.angles[2] = buf.ReadInt();
            int buttonPack = buf.ReadInt();
            for (int i= 0; i < 30; i++) {
                dest.buttons[i] = (buttonPack & (1 << (i+1))) != 0;
            }
            dest.weapon = buf.ReadEnum(Weapon.class);
        } else { // unchanged

            dest.Forward = oldcmd.Forward;
            dest.Back = oldcmd.Back;
            dest.Left = oldcmd.Left;
            dest.Right = oldcmd.Right;
            dest.Up = oldcmd.Up;
            dest.Down = oldcmd.Down;
            dest.Mouse1Diff = oldcmd.Mouse1Diff;
            dest.Mouse2Diff = oldcmd.Mouse2Diff;
            dest.Mouse3Diff = oldcmd.Mouse3Diff;
            dest.Mouse1 = oldcmd.Mouse1;
            dest.Mouse2 = oldcmd.Mouse2;
            dest.Mouse3 = oldcmd.Mouse3;
            dest.WheelDelta = oldcmd.WheelDelta;
            dest.MousePos = oldcmd.MousePos;
            System.arraycopy(oldcmd.angles, 0, dest.angles, 0, oldcmd.angles.length);
            System.arraycopy(oldcmd.buttons, 0, dest.buttons, 0, oldcmd.buttons.length);
            dest.weapon = oldcmd.weapon;
        }

        return dest;
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof PlayerInput))
            return false;

//        return false;
        int oCode = o.hashCode();
        int thisCode = this.hashCode();
        return (oCode == thisCode );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Arrays.hashCode(this.angles);
        hash = 59 * hash + this.WheelDelta;
        hash = 59 * hash + (this.Mouse1 ? 1 : 0);
        hash = 59 * hash + (this.Mouse1Diff ? 1 : 0);
        hash = 59 * hash + (this.Mouse2 ? 1 : 0);
        hash = 59 * hash + (this.Mouse2Diff ? 1 : 0);
        hash = 59 * hash + (this.Mouse3 ? 1 : 0);
        hash = 59 * hash + (this.Mouse3Diff ? 1 : 0);
        hash = 59 * hash + (this.Forward ? 1 : 0);
        hash = 59 * hash + (this.Back ? 1 : 0);
        hash = 59 * hash + (this.Left ? 1 : 0);
        hash = 59 * hash + (this.Right ? 1 : 0);
        hash = 59 * hash + (this.Up ? 1 : 0);
        hash = 59 * hash + (this.Down ? 1 : 0);
        return hash;
    }





    private static int setBit(int val, int index, boolean value) {
        if(!value)
            return val;

        if(index >= 31) throw new RuntimeException("PlayerInput.setBit: overflow on bit " + index);

        return val | (1<<index);
    }

    private static boolean getBit(int val, int index) {
        return (val & (1<<index)) != 0;
    }

    private static boolean arrayEquals(boolean[] a, boolean[] b) {
        if(a == null || b == null) return a == b;

        for (int i= 0; i < a.length; i++) {
            if(a[i] != b[i]) return false;
        }

        return true;
    }

    public void WriteDeltaUserCmd(NetBuffer buf, PlayerInput from) {

        buf.Write(serverTime);
        
        if(this.equals(from)
                && Helper.Equals(MousePos, from.MousePos)
                && MouseDelta[0] == from.MouseDelta[0] && MouseDelta[1] == from.MouseDelta[1]
                && arrayEquals(buttons, from.buttons) && weapon == from.weapon) {
            buf.Write(false); // no change
            return;
        }
        
        buf.Write(true); // Got change

        int hags = 0;
        int index = 0;
        hags = setBit(hags, index++, Forward);
        hags = setBit(hags, index++, Back);
        hags = setBit(hags, index++, Left);
        hags = setBit(hags, index++, Right);
        hags = setBit(hags, index++, Up);
        hags = setBit(hags, index++, Down);
        hags = setBit(hags, index++, Mouse1Diff);
        hags = setBit(hags, index++, Mouse1);
        hags = setBit(hags, index++, Mouse2Diff);
        hags = setBit(hags, index++, Mouse2);
        hags = setBit(hags, index++, Mouse3Diff);
        hags = setBit(hags, index++, Mouse3);
        hags = setBit(hags, index++, WheelDelta != 0);
        boolean sameVec = Helper.Equals(MousePos, from.MousePos);
        hags = setBit(hags, index++, sameVec);

        buf.Write(hags);
        if(WheelDelta != 0)
            buf.Write(WheelDelta);

        if(!sameVec)
            buf.Write(MousePos);

        buf.Write(angles[0]);
        buf.Write(angles[1]);
        buf.Write(angles[2]);
        int buttonPack = 0;
        for (int i= 0; i < buttons.length; i++) {
            if(!buttons[i]) continue;
            buttonPack |= 1 << (i+1);
        }
        buf.Write(buttonPack);
        buf.WriteEnum(weapon);
    }

    public PlayerInput Clone() {
        PlayerInput n = new PlayerInput();
        n.MousePos = new Vector2f();
        n.MousePos.x = MousePos.x;
        n.MousePos.y = MousePos.y;
        n.MouseDelta[0] = MouseDelta[0];
        n.MouseDelta[1] = MouseDelta[1];
        System.arraycopy(angles, 0, n.angles, 0, angles.length);
        n.serverTime = serverTime;
        n.WheelDelta = WheelDelta;
        n.Mouse1 = Mouse1;
        n.Mouse1Diff = Mouse1Diff;
        n.Mouse2 = Mouse2;
        n.Mouse2Diff = Mouse2Diff;
        n.Mouse3 = Mouse3;
        n.Mouse3Diff = Mouse3Diff;
        n.Forward = Forward;
        n.Back = Back;
        n.Left = Left;
        n.Right = Right;
        n.Up = Up;
        n.Down = Down;
        System.arraycopy(buttons, 0, n.buttons, 0, buttons.length);
        n.weapon = weapon;
        return n;
    }

    public boolean isButtonDown(int i) {
        return buttons[i];
    }
}
