package cubetech.common;

import cubetech.misc.FinishedUpdatingListener;
import cubetech.misc.Ref;
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
        
    }

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
        System.out.println("CVar list:");
        for(CVar var: vars.values()) {
            System.out.println(" " + var.Name + " \t" + var.sValue);
        }
        System.out.println("----");
    }

    void cmd_Print(String[] tokens) {
        if(tokens.length != 2)
        {
            System.out.println("Usage: print <variable>");
            return;
        }

        String name = tokens[1];
        CVar cv = Find(name);
        if(cv != null)
            cv.Print();
        else
            System.out.println("CVar " + name + " doesn't exist.");
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

    public CVar Get(String name, String value, EnumSet<CVarFlags> flags) {
        if(name == null || name.length() == 0 || value == null) {
            System.out.println("CVar: Get w/ null arguments");
            return null;
        }

        if(!ValidateString(name))
        {
            System.out.println("Invalid cvar name: " + name);
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

                if(var.flags.contains(CVarFlags.ROM))
                    var.latchedString = value;

            }
            
            var.flags.addAll(flags);

            // only allow one non-empty reset string without a warning
            if(var.resetString == null || var.resetString.length() == 0)
                var.resetString = value;

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
            System.out.println("WARNING: Invalid cvar name: " + name);
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
                System.out.println(name + " is read only.");
                return var;
            }

            if(var.flags.contains(CVarFlags.INIT)) {
                System.out.println(name + " is write protected.");
                return var;
            }

            if(var.flags.contains(CVarFlags.LATCH)) {
                if(var.latchedString != null) {
                    if(value.equals(var.latchedString))
                        return var;
                    var.latchedString = null;
                } else if(value.equals(var.sValue))
                    return var;

                System.out.println(name + " will be changed opun restarting.");
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
                System.out.println(", settings to " + value);

            return ""+valf;
        }

        return value;
    }
}
