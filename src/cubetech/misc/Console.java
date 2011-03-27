package cubetech.misc;

import cubetech.common.CVar;
import cubetech.common.CVarFlags;
import cubetech.common.Commands;
import cubetech.common.Common;
import cubetech.common.ICommand;
import cubetech.gfx.CubeTexture;
import cubetech.gfx.Sprite;
import cubetech.gfx.SpriteManager;
import cubetech.gfx.SpriteManager.Type;
import cubetech.gfx.TextManager;
import cubetech.input.Binds;
import cubetech.input.Input;
import cubetech.input.KeyEventListener;
import cubetech.input.KeyEvent;
import cubetech.input.Key;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author mads
 */
public class Console implements KeyEventListener, LogEventListener {
    private static final int PAGE_SCROLL_LINES = 10; // how many lines to scroll on pgup/down
    String cmdLine = ""; // Text field
    CVar con_cmdprefix; // Command prefix
    CVar con_scale;
    int currentLine = 0; // The lowest line on the screen
    
    ArrayList<String> logList = new ArrayList<String>(100); // console log
    ArrayList<String> commandLog = new ArrayList<String>(); // commands the user has entered
    private int currentCommand; // currenly selected command, for scrolling command log
    private boolean isScrollingCommands = false; // True if scrolled up in the console log
    private int nConsecutiveTabs = 0; // At 2*tab, do autocompletion
    PrintStream stdout; // Original System.out stream
    CubeTexture scrollArrowTex = Ref.ResMan.LoadTexture("data/arrow.png");



    public Console() {
        Ref.Input.AddKeyEventListener(this, Input.KEYCATCH_CONSOLE);
        Log.AddLogListener(this);
        con_cmdprefix = Ref.cvars.Get("con_cmdprefix", "] ", EnumSet.of(CVarFlags.NONE));
        con_scale = Ref.cvars.Get("con_scale", "1", EnumSet.of(CVarFlags.NONE));
        Ref.commands.AddCommand("console", cmd_console);
    }

    private ICommand cmd_console = new ICommand() {
        public void RunCommand(String[] args) {
            Ref.Console.ToggleVisible();
        }
    };

    public void ToggleVisible() {
        int catcher = Ref.Input.GetKeyCatcher();
        
        Ref.Input.SetKeyCatcher(catcher ^ Input.KEYCATCH_CONSOLE);
    }

    public void Close() {
        Ref.Input.SetKeyCatcher(Ref.Input.GetKeyCatcher() & ~Input.KEYCATCH_CONSOLE);
    }

    public void Log(String str) {
        boolean scrolling = isScrolling();
        logList.add(str);
        if(!scrolling)
            ScrollToEnd();
    }

    // Executes a command and prints it to the console
    public void ExecuteAndLog(String str, boolean force) {
        if(str.trim().length() == 0) {
            if(force) // force out an empty line
                Common.Log(con_cmdprefix.sValue);
            return; // Ignore empty line
        }

        Common.Log(con_cmdprefix.sValue + str);
        Ref.commands.ExecuteText(Commands.ExecType.INSERT, str);
        if(!commandLog.isEmpty() && commandLog.get(commandLog.size()-1).equalsIgnoreCase(str))
            return; // same as last line, don't add to commandlog
        commandLog.add(str);
    }

    
    public void KeyPressed(KeyEvent evt) {
        Key key = (Key)evt.getSource();

        // Ignore key-up events
        if(!key.Pressed)
            return;

        // Console not visible
        if((Ref.Input.GetKeyCatcher() & Input.KEYCATCH_CONSOLE) == 0)
            return;

        // Handle key
        switch(key.key) {
            case Keyboard.KEY_BACK:
                if(cmdLine.length() > 0)
                    cmdLine = cmdLine.substring(0, cmdLine.length()-1);
                nConsecutiveTabs = 0;
                break;
            case Keyboard.KEY_RETURN:
                ScrollToEnd();
                isScrollingCommands = false;
                String command = cmdLine;
                cmdLine = "";
                ExecuteAndLog(command, true);
                break;
            case Keyboard.KEY_PRIOR: // pageup
                ScrollUp(PAGE_SCROLL_LINES);
                break;
            case Keyboard.KEY_NEXT: // pagedown
                ScrollDown(PAGE_SCROLL_LINES);
                break;
            case Binds.KEY_MWHEELDOWN:
                ScrollDown(2);
                break;
            case Binds.KEY_MWHEELUP:
                ScrollUp(2);
                break;
            case Keyboard.KEY_HOME:
                ScrollToBegin();
                break;
            case Keyboard.KEY_END:
                ScrollToEnd();
                break;
            case Keyboard.KEY_UP:
                SelectPreviousCommand();
                break;
            case Keyboard.KEY_DOWN:
                SelectNextCommand();
                break;
            case Keyboard.KEY_TAB:
                nConsecutiveTabs++;
                if(nConsecutiveTabs == 2) {
                    nConsecutiveTabs = 0;
                    TabCompletion();
                }
                break;
            // Add to commandline
            default:
                if(key.Char != Keyboard.CHAR_NONE) {
                    cmdLine += key.Char;
                    nConsecutiveTabs = 0;
                    isScrollingCommands = false;
                }
                break;
        }
    }

    // tab-tab...
    private void TabCompletion() {
        if(cmdLine.isEmpty()) {
            // Just use the default list commands
            Ref.commands.ExecuteText(Commands.ExecType.NOW, "listcmds");
            Ref.commands.ExecuteText(Commands.ExecType.NOW, "listcvars");
            return;
        }
        // Mode 1   : Tab completion when still writing the first word,
        //            this checks for cvars and commands. If there is 1 result
        //            it gets set, else the results are printed
        int index = cmdLine.indexOf(' ');
        if(index == -1) {
            String cmd = cmdLine;
            if(!cmd.isEmpty()) {
                ArrayList<String> list = new ArrayList<String>();
                Ref.cvars.AddMatchingCVarsToList(list, cmd);
                Ref.commands.AddMatchingCommandsToList(list, cmd);

                if(list.isEmpty())
                    return; // no results

                // if only one result, replace the commandline with it
                if(list.size() == 1) {
                    cmdLine = list.get(0) + " ";
                    return;
                }

                // Else sort and print to console
                Collections.sort(list);
                StringBuilder buf = new StringBuilder();
                for (String str : list) {
                    buf.append(str);
                    buf.append("  ");
                }
                Common.Log(buf.toString());
            }
            return;
        }

        // Mode 2   : When first word is complete, check is word is a
        //            cvar, and print the value if it is.
        String[] tokens = Commands.TokenizeString(cmdLine, false);
        if(tokens.length == 1) {
            CVar var = Ref.cvars.Find(tokens[0]);
            if(var != null) {
                var.Print();
            }
        }
        
    }

    // Scrolls the command log.
    // If there is text in the command field, that has been edited,
    // it will be placed in the command log
    private void SelectPreviousCommand() {
        nConsecutiveTabs = 0;
        
        if(commandLog.isEmpty())
            return;
        
        if(!isScrollingCommands) {
            isScrollingCommands = true;
            currentCommand = commandLog.size();
            if(!cmdLine.isEmpty())
                commandLog.add(cmdLine);
        }
        currentCommand--;
        if(currentCommand < 0)
            currentCommand = 0;
        cmdLine = commandLog.get(currentCommand);
    }

    // Selects the next command
    private void SelectNextCommand() {
        nConsecutiveTabs = 0;
        
        if(commandLog.isEmpty())
            return;
        
        if(!isScrollingCommands)
            return; // Already scrolled to the bottom

        currentCommand++;
        if(currentCommand >= commandLog.size()) {
            //currentCommand = commandLog.size()-1;
            cmdLine = "";
            isScrollingCommands = false;
        } else
            cmdLine = commandLog.get(currentCommand);
    }

    // Returns true if the console isn't scrolled to the bottom
    private boolean isScrolling() {
        return currentLine != logList.size();
    }

    // Scrolls the console log a page up
    private void ScrollUp(int lines) {
        currentLine -= lines;
        // Just scroll up so we can see the whole top page
        int topline = Math.min(MaxVisibleLines()-1, logList.size());
        if(currentLine < topline)
            currentLine = topline;
        nConsecutiveTabs = 0;
    }

    // Scrolls the console log a page down
    private void ScrollDown(int lines) {
        currentLine += lines;
        if(currentLine > logList.size())
            ScrollToEnd();
        nConsecutiveTabs = 0;
    }

    private void ScrollToEnd() {
        currentLine = logList.size();
        nConsecutiveTabs = 0;
    }

    private void ScrollToBegin() {
        currentLine = Math.min(MaxVisibleLines()-1, logList.size());
        nConsecutiveTabs = 0;
    }

    private int MaxVisibleLines() {
        int charHeight = (int) (Ref.textMan.GetCharHeight() * con_scale.fValue);
        return Ref.glRef.currentMode.getHeight()/charHeight;
    }

    public void Render() {
        if((Ref.Input.GetKeyCatcher() & Input.KEYCATCH_CONSOLE) == 0)
            return;

        float charHeight = Ref.textMan.GetCharHeight()*con_scale.fValue;

        // Background
        Sprite spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(new Vector2f(), new Vector2f(Ref.glRef.GetResolution()), null,null,null);
        spr.SetColor(0,0,0,127);

        // Commandline background
        spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(new Vector2f(2, Ref.glRef.GetResolution().y-charHeight), new Vector2f(Ref.glRef.GetResolution().x,charHeight), null, null, null);
        spr.SetColor(30,30,30,255);

        spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
        spr.Set(new Vector2f(0.0025f, 0.054f), new Vector2f(Ref.glRef.GetResolution().x, 0.005f), null, null, null);
        spr.SetColor(255,255,255,30);

        // Commandline
        String commandline = con_cmdprefix.sValue + cmdLine;
        if(((Ref.client.realtime >> 8) & 0x1) == 1) // MAGIC! well..not really :(
            commandline += "_";
        Ref.textMan.AddText(new Vector2f(0,Ref.glRef.GetResolution().y-charHeight), commandline, TextManager.Align.LEFT, Type.HUD, con_scale.fValue);

        // Log
        
        int maxLines = MaxVisibleLines();
        int linesDrawn = 0;
        for (int i = currentLine-1; i >= 0; i--) {

            Vector2f drawSize = Ref.textMan.GetStringSize(logList.get(i), null, null, con_scale.fValue, Type.HUD);
            int lines = (int)Math.ceil(drawSize.y / charHeight);
            
            Ref.textMan.AddText(new Vector2f(0, Ref.glRef.GetResolution().y-(linesDrawn+lines+1)*charHeight), logList.get(i), TextManager.Align.LEFT, Type.HUD, con_scale.fValue);
            linesDrawn += lines;
//            System.err.println(""+drawSize.y );
            if(linesDrawn >= maxLines)
                break; // Can't see the rest
        }

        if(isScrolling()) {
            // Draw scroll arrow
            spr = Ref.SpriteMan.GetSprite(SpriteManager.Type.HUD);
            spr.Set(new Vector2f(Ref.glRef.GetResolution().x - 70, 25), new Vector2f(64,64), scrollArrowTex, new Vector2f(), new Vector2f(1, 1));
            spr.SetColor(255, 255, 255, (int)(((1 + (float)Math.sin(Ref.client.realtime / 200f)) * 0.4f)*255f));
        }
    }

    public void HandleLogLine(String str) {
        Log(str);
    }




}
