package cubetech.input;

import cubetech.common.Helper;
import cubetech.net.NetBuffer;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class PlayerInput {
    public Vector2f MousePos = new Vector2f(0.5f,0.5f);
    public Vector2f MouseDelta = new Vector2f();
    public int serverTime;
    public int WheelDelta;
    public boolean Mouse1;
    public boolean Mouse1Diff;
    public boolean Mouse2;
    public boolean Mouse2Diff;
    public boolean Mouse3;
    public boolean Mouse3Diff;
    public boolean Up;
    public boolean Down;
    public boolean Left;
    public boolean Right;
    public boolean Jump;

    public static PlayerInput ReadDeltaUserCmd(NetBuffer buf, PlayerInput oldcmd) {
        PlayerInput dest = new PlayerInput();
        dest.serverTime = buf.ReadInt();
        if(buf.ReadBool()) { // Got new data
            int hags = buf.ReadInt();
            int index = 0;
            dest.Up = getBit(hags,index++);
            dest.Down = getBit(hags,index++);
            dest.Left = getBit(hags,index++);
            dest.Right = getBit(hags,index++);
            dest.Jump = getBit(hags,index++);
            dest.Mouse1Diff = getBit(hags,index++);
            dest.Mouse1 = getBit(hags,index++);
            dest.Mouse2Diff = getBit(hags,index++);
            dest.Mouse2 = getBit(hags,index++);
            dest.Mouse3Diff = getBit(hags,index++);
            dest.Mouse3 = getBit(hags,index++);
            boolean hasWheel = getBit(hags,index++);
            boolean sameVec = getBit(hags,index++);
//            dest.Up = buf.ReadBool();
//            dest.Down = buf.ReadBool();
//            dest.Left = buf.ReadBool();
//            dest.Right = buf.ReadBool();
//            dest.Jump = buf.ReadBool();
//            dest.Mouse1Diff = buf.ReadBool();
//            if(dest.Mouse1Diff)
//                dest.Mouse1 = buf.ReadBool();
//            dest.Mouse2Diff = buf.ReadBool();
//            if(dest.Mouse2Diff)
//                dest.Mouse2 = buf.ReadBool();
//            dest.Mouse3Diff = buf.ReadBool();
//            if(dest.Mouse3Diff)
//                dest.Mouse3 = buf.ReadBool();
//            boolean hasWheel = buf.ReadBool();
            if(hasWheel)
                dest.WheelDelta = buf.ReadInt();
            if(!sameVec)
                dest.MousePos = buf.ReadVector();
            else
                dest.MousePos.set(oldcmd.MousePos);
        } else { // unchanged
            dest.Up = oldcmd.Up;
            dest.Down = oldcmd.Down;
            dest.Left = oldcmd.Left;
            dest.Right = oldcmd.Right;
            dest.Jump = oldcmd.Jump;
            dest.Mouse1Diff = oldcmd.Mouse1Diff;
            dest.Mouse2Diff = oldcmd.Mouse2Diff;
            dest.Mouse3Diff = oldcmd.Mouse3Diff;
            dest.Mouse1 = oldcmd.Mouse1;
            dest.Mouse2 = oldcmd.Mouse2;
            dest.Mouse3 = oldcmd.Mouse3;
            dest.WheelDelta = oldcmd.WheelDelta;
            dest.MousePos = oldcmd.MousePos;
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
        hash = 41 * hash + this.WheelDelta;
        hash = 41 * hash + (this.Mouse1 ? 1 : 0);
        hash = 41 * hash + (this.Mouse1Diff ? 1 : 0);
        hash = 41 * hash + (this.Mouse2 ? 1 : 0);
        hash = 41 * hash + (this.Mouse2Diff ? 1 : 0);
        hash = 41 * hash + (this.Mouse3 ? 1 : 0);
        hash = 41 * hash + (this.Mouse3Diff ? 1 : 0);
        hash = 41 * hash + (this.Up ? 1 : 0);
        hash = 41 * hash + (this.Down ? 1 : 0);
        hash = 41 * hash + (this.Left ? 1 : 0);
        hash = 41 * hash + (this.Right ? 1 : 0);
        hash = 41 * hash + (this.Jump ? 1 : 0);
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

    public void WriteDeltaUserCmd(NetBuffer buf, PlayerInput from) {

        buf.Write(serverTime);
        
        if(this.equals(from)
                && Helper.Equals(MousePos, from.MousePos)
                && Helper.Equals(MouseDelta, from.MouseDelta)) {
            buf.Write(false); // no change
            return;
        }
        
        buf.Write(true); // Got change

        int hags = 0;
        int index = 0;
        hags = setBit(hags, index++, Up);
        hags = setBit(hags, index++, Down);
        hags = setBit(hags, index++, Left);
        hags = setBit(hags, index++, Right);
        hags = setBit(hags, index++, Jump);
        hags = setBit(hags, index++, Mouse1Diff);
        hags = setBit(hags, index++, Mouse1);
        hags = setBit(hags, index++, Mouse2Diff);
        hags = setBit(hags, index++, Mouse2);
        hags = setBit(hags, index++, Mouse3Diff);
        hags = setBit(hags, index++, Mouse3);
        hags = setBit(hags, index++, WheelDelta != 0);
        boolean sameVec = Helper.Equals(MousePos, from.MousePos);
        hags = setBit(hags, index++, sameVec);

//        buf.Write(Up);
//        buf.Write(Down);
//        buf.Write(Left);
//        buf.Write(Right);
//        buf.Write(Jump);
//        buf.Write(Mouse1Diff);
//        if(Mouse1Diff)
//            buf.Write(Mouse1);
//        buf.Write(Mouse2Diff);
//        if(Mouse2Diff)
//            buf.Write(Mouse2);
//        buf.Write(Mouse3Diff);
//        if(Mouse3Diff)
//            buf.Write(Mouse3);
//        buf.Write(WheelDelta != 0);
        buf.Write(hags);
        if(WheelDelta != 0)
            buf.Write(WheelDelta);

        if(!sameVec)
            buf.Write(MousePos);
    }

    public PlayerInput Clone() {
        PlayerInput n = new PlayerInput();
        n.MousePos = new Vector2f();
        n.MousePos.x = MousePos.x;
        n.MousePos.y = MousePos.y;
        n.MouseDelta = new Vector2f();
        n.MouseDelta.x = MouseDelta.x;
        n.MouseDelta.y = MouseDelta.y;
        n.serverTime = serverTime;
        n.WheelDelta = WheelDelta;
        n.Mouse1 = Mouse1;
        n.Mouse1Diff = Mouse1Diff;
        n.Mouse2 = Mouse2;
        n.Mouse2Diff = Mouse2Diff;
        n.Mouse3 = Mouse3;
        n.Mouse3Diff = Mouse3Diff;
        n.Up = Up;
        n.Down = Down;
        n.Left = Left;
        n.Right = Right;
        n.Jump = Jump;
        return n;
    }
}
