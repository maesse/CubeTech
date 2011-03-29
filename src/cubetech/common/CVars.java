package cubetech.common;

import cubetech.gfx.ResourceManager;
import cubetech.misc.FinishedUpdatingListener;
import cubetech.misc.Ref;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;

/**
 *
 * @author mads
 */
public class CVars {
    HashMap<String, CVar> vars = new HashMap<String, CVar>();
    HashMap<Integer, CVar> Intvars = new HashMap<Integer, CVar>();
    public EnumSet<CVarFlags> modifiedFlags = EnumSet.noneOf(CVarFlags.class);
    int varcount = 0;

    protected FinishedUpdatingListener updateListener = null;

    public void SetUpdateListener(FinishedUpdatingListener listener) {
        updateListener = listener;
    }

    public void RemoveUpdateListener()
    {
        updateListener = null;
    }

    public CVars() {
        // Initialize commands
        Ref.commands.AddCommand("print", new ICommand() {
            public void RunCommand(String[] args) {
                cmd_Print(args);
            }
        });
        Ref.commands.AddCommand("listcvars", new ICommand() {
            public void RunCommand(String[] args) {
                cmd_ListCVars(args);
            }
        });
        Ref.commands.AddCommand("exec", cmd_Exec);
        Ref.commands.AddCommand("savecfg", cmd_save);
        Ref.commands.AddCommand("seta", cmd_Set);
    }

    private ICommand cmd_Set = new ICommand() {
        public void RunCommand(String[] args) {
            if(args.length < 3) {
                Common.Log("usage: seta <cvar> <value>");
                return;
            }

            CVar v = Set2(args[1], args[2], false);
            if(v == null)
                return;
            if(!v.flags.contains(CVarFlags.ARCHIVE))
                v.flags.add(CVarFlags.ARCHIVE);
        }
    };
    
    private ICommand cmd_save = new ICommand() {
        public void RunCommand(String[] args) {
            Ref.common.WriteConfiguration(true);
        }
    };

    private ICommand cmd_Exec = new ICommand() {
        public void RunCommand(String[] args) {
            if(args.length != 2) {
                Common.Log("exec <filename> : execute a script");
                return;
            }

            String file = args[1];
            boolean found = ResourceManager.FileExists(file);
            if(!found) {
                if(!file.endsWith(".cfg")) {
                    file = file + ".cfg";
                    found = ResourceManager.FileExists(file);
                }
            }

            if(!found) {
                Common.Log(args[1] + " doesn't exist.");
                return;
            }
            
            try {
                StringBuilder str = new StringBuilder();
                InputStreamReader rdr = new InputStreamReader (ResourceManager.OpenFileAsInputStream(file));
                int c;
                while(( c = rdr.read()) != -1) {
                    str.append((char)c);
                }

                Ref.commands.ExecuteText(Commands.ExecType.INSERT, str.toString());
                
            } catch (IOException ex) {
                Common.LogDebug(Common.getExceptionString(ex));
            }
        }
    };

    // Used for debugging UI
    public CVar GetCVarIndex(int index) {
        return Intvars.get(index);
    }

    // Used for debugging UI
    public int GetCVarCount()
    {
        return varcount;
    }

    public String InfoString(CVarFlags flag) {
        StringBuilder str = new StringBuilder();
        for(CVar var : vars.values()) {
            if(!var.Name.isEmpty() && var.flags.contains(flag)) {
                str.append(String.format("\\%s\\%s", var.Name, var.sValue));
            }
        }
        return str.toString();
    }



    void cmd_ListCVars(String[] tokens) {
        Common.Log("CVar list:");
        for(CVar var: vars.values()) {
            Common.Log(" " + var.Name + " \t" + var.sValue);
        }
        Common.Log("----");
    }

    void cmd_Print(String[] tokens) {
        if(tokens.length != 2)
        {
            Common.Log("Usage: print <variable>");
            return;
        }

        String name = tokens[1];
        CVar cv = Find(name);
        if(cv != null)
            cv.Print();
        else
            Common.Log("CVar " + name + " doesn't exist.");
    }

    public CVar Find(String str) {
        str = str.toLowerCase();
        CVar result = vars.get(str);
        return result;
    }

    // Adds the cvars that match the searchpattern to the given arraylist
    public void AddMatchingCVarsToList(Collection<String> list, String searchPattern) {
        searchPattern = searchPattern.toLowerCase();
        for (String var: vars.keySet()) {
            if(var.startsWith(searchPattern))
                list.add(var);
        }
    }

    // Handle input from command subsystem. Returns true if
    // cvar system recogniced the command.
    public boolean HandleFromCommand(String[] tokens) {
        CVar var = Find(tokens[0]);
        if(var == null)
            return false;

        // Perform a varable print
        if(tokens.length == 1) {
            var.Print();
            return true;
        }

        // Set the cvar
        String args = Commands.Args(tokens);
        Set2(var.Name, args, false);
        return true;
    }

    // If the variable already exists, the value will not be set unless CVAR_ROM
    // The flags will be or'ed in if the variable exists.
    public CVar Get(String name, String value, EnumSet<CVarFlags> flags) {
        if(name == null || name.length() == 0 || value == null) {
            Common.Log("CVar: Get w/ null arguments");
            return null;
        }

        if(!ValidateString(name))
        {
            Common.Log("Invalid cvar name: " + name);
            name = "BADNAME";
        }
       
        name = name.toLowerCase();
        CVar nvar = Find(name);
        if(nvar != null) {
            // already exists, try to set it
            CVar var = nvar;
            value = Validate(var, value, false);

            if(var.flags.contains(CVarFlags.USER_CREATED)) {
                var.flags.remove(CVarFlags.USER_CREATED);
                var.resetString = value;

                if(flags.contains(CVarFlags.ROM))
                    var.latchedString = value;
            }
            
            var.flags.addAll(flags);

            // only allow one non-empty reset string without a warning
            if(var.resetString == null || var.resetString.length() == 0)
                var.resetString = value;
            else
                Common.LogDebug("Warning: cvar \"%s\" given initial values: \"%s\" and \"%s\"", name, var.resetString, value);

            // if we have a latched string, take that value now
            if(var.latchedString != null && !var.latchedString.isEmpty()) {
                String s = var.latchedString;
                var.latchedString = null;
                Set2(var.Name, s, true);
            }

            modifiedFlags.addAll(flags);
            return var;
        }

        //
        // allocate a new cvar
        //
        CVar cvar = new CVar();
        cvar.Name = name;
        cvar.sValue = value;
        cvar.modified = true;
        cvar.modificationCount = 1;
        try {
            cvar.fValue = Float.parseFloat(value);
        } catch (NumberFormatException ex) {}
        try {
            cvar.iValue = Integer.parseInt(value);
            cvar.isInteger = true;
        } catch (NumberFormatException ex) {}
        cvar.resetString = value;
        cvar.validate  =false;
        cvar.flags = flags;
        modifiedFlags.addAll(flags);

        vars.put(name, cvar);
        Intvars.put(varcount++, cvar);
        if(updateListener != null)
            updateListener.FinishedUpdating();
        return cvar;

    }

    // Sets value of a cvar
    public CVar Set2(String name, String value, boolean force) {
        name = name.toLowerCase();
        if(!ValidateString(name)) {
            Common.Log("WARNING: Invalid cvar name: " + name);
            name = "BADNAME";
        }

        CVar nvar = Find(name);
        if(nvar == null) {
            if(value == null)
                return null;

            // create it
            if(force)
                return Get(name, value, EnumSet.of(CVarFlags.NONE));
            else
                return Get(name, value, EnumSet.of(CVarFlags.USER_CREATED));
        }

        CVar var = nvar;
        if(value == null || value.length() == 0)
            value = var.resetString;

        value = Validate(var, value, true);

        // Latched cvar?
        if(var.flags.contains(CVarFlags.LATCH) && var.latchedString != null) {
            // if new latch is equal to current value, just clear the current latch and be done with it
            if(value.equals(var.sValue))
            {
                var.latchedString = null;
                return var;
            }

            // if the new latch is the same as the old, we're done
            if(value.equals(var.latchedString))
                return var;
        }
        else if(value.equals(var.sValue)) {
            if(force)
                var.modified = true;
            return var; // If new value is the same as the old, just return
        }

        // note what types of cvars have been modified (userinfo, archive, serverinfo, systeminfo)
        modifiedFlags.addAll(var.flags);

        if(!force) {
            if(var.flags.contains(CVarFlags.ROM)) {
                Common.Log(name + " is read only.");
                return var;
            }

            if(var.flags.contains(CVarFlags.INIT)) {
                Common.Log(name + " is write protected.");
                return var;
            }

            if(var.flags.contains(CVarFlags.LATCH)) {
                if(var.latchedString != null) {
                    if(value.equals(var.latchedString))
                        return var;
                    var.latchedString = null;
                } else if(value.equals(var.sValue))
                    return var;

                Common.Log(name + " will be changed opun restarting.");
                var.latchedString = value;
                var.modificationCount++;
                var.modified = true;
                return var;
            }
        } else {
            // We're forcing a value, so clear whatever is latched
            if(var.latchedString != null)
                var.latchedString = null;
        }

        if(value.equals(var.sValue)) {
            
            return var;
        }

        var.modificationCount++;
        var.modified = true;

        var.sValue = value;
        try {
            float val = Float.parseFloat(value);
            var.fValue = val;
        } catch (NumberFormatException e) {}
        
        try {
            var.isInteger = true;
            var.iValue = Integer.parseInt(value);
        } catch(NumberFormatException e) {
            var.isInteger = false;
        }
        if(updateListener != null)
            updateListener.FinishedUpdating();
        return var;
    }

    public static boolean ValidateString(String str) {
        if(str == null)
            return false;
        if(str.contains("\\"))
            return false;
        if(str.contains("\""))
            return false;
        if(str.contains(";"))
            return false;

        return true;
    }

    // Validates a value against a cvar,
    // returns the cleaned value
    public static String Validate(CVar var, String value, boolean warn) {
        if(!var.validate)
            return value; // doesn't need to be validated

        if(value == null || value.length() == 0)
            return value;
        
        boolean changed = false;
        float valf;
        try {
            valf = Float.parseFloat(value);
            if(var.isInteger) {
                if(valf != (int)valf) {
                    if(warn)
                        System.out.print("WARNING: cvar " + var.Name + " must be an integer");

                    valf = (int)valf;
                    changed = true;
                }
            }
        } catch (NumberFormatException e) {
            if(warn)
                System.out.print("WARNING: cvar " + var.Name + " must be numeric");
            
            valf = Float.parseFloat(var.resetString);
            changed = true;
        }

        if(valf < var.Min) {
            if(warn) {
                if(changed)
                    System.out.print(" and is");
                else
                    System.out.print("WARNING: cvar " + var.Name);

                System.out.print(" out of range (min " + var.Min + ")");
            }
            valf = var.Min;
            changed = true;
        }
        else if(valf > var.Max) {
            if(warn) {
                if(changed)
                    System.out.print(" and is");
                else
                    System.out.print("WARNING: cvar " + var.Name);

                System.out.print(" out of range (max " + var.Max + ")");
            }
            valf = var.Max;
            changed = true;
        }

        if(changed) {
            if(warn)
                Common.Log(", settings to " + value);

            return ""+valf;
        }

        return value;
    }

    void WriteCVars(StringBuilder dst) {
        for (CVar var : vars.values()) {
            if(!var.flags.contains(CVarFlags.ARCHIVE))
                continue;
            dst.append("seta ");
            dst.append(var.Name);
            dst.append(" \"");
            // Write latched string?
            if(var.latchedString != null && !var.latchedString.isEmpty()) {
                dst.append(var.latchedString);
            } else {
                dst.append(var.sValue);
            }
            dst.append("\"\r\n");
        }
        
    }
}
