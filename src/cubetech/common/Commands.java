package cubetech.common;

import cubetech.misc.Ref;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
/**
 * Requires common and cvars
 * @author mads
 */
public final class Commands {
    private static final int BUFFER_SIZE = 1024*4; // 4k
    private static final int MAX_LINE_SIZE = 1024; // 1k
    private StringBuilder cmd_text = new StringBuilder(BUFFER_SIZE);
    private HashMap<String, ICommand> registeredCommands = new HashMap<String, ICommand>();
    private HashMap<String, String> aliases = new HashMap<String, String>();
    private int wait = 0;
    static ArrayList<String> tempTokens = new ArrayList<String>();

    public Commands() {
        AddCommand("listcmds", new ICommand() {
            public void RunCommand(String[] args) {
                cmd_ListCmds(args);
            }
        });
        AddCommand("wait", new ICommand() {
            public void RunCommand(String[] args) {
                if(args.length == 2) {
                    try {
                        wait = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        wait = 1;
                    }
                } else
                    wait = 1;
            }
        });
        AddCommand("crash", new ICommand() {
            public void RunCommand(String[] args) {
                String str = Args(args);
                Ref.common.Error(Common.ErrorCode.FATAL, str);
            }
        });
        AddCommand("gc", new ICommand() {
            public void RunCommand(String[] args) {
                System.gc();
            }
        });

        AddCommand("alias", new ICommand() {
            public void RunCommand(String[] args) {
                // Usage
                if(args.length < 2) {
                    Common.Log("usage: alias <name> [command]");
                    return;
                }

                // Print
                if(args.length == 2) {
                    if(aliases.containsKey(args[1])) {
                        Common.Log(String.format("alias \"%s\" = \"%s\"", args[1], aliases.get(args[1])));
                    } else
                        Common.Log(String.format("alias \"%s\" doesn't exist", args[1]));
                    return;
                }

                // Set alias

                String cmd = args[2];
                if(cmd.isEmpty())
                    aliases.remove(args[1]);
                else
                    aliases.put(args[1], cmd);
            }
        });

    }

    /**
     * NOW: Instant execute, dont add to buffer
     * INSERT: Adds command text immediately after the current command
     * APPEND: Append to end of command buffer, doesn't add \n
     */
    public enum ExecType {
        NOW, // Instant execute, dont add to buffer
        INSERT, // Adds command text immediately after the current command
        APPEND // Append to end of command buffer
    }

    public void ExecuteText(ExecType when, String text) {
        switch(when) {
            case APPEND:
                AddText(text);
                break;
            case INSERT:
                InsertText(text);
                break;
            case NOW:
                if(text == null || text.length() == 0)
                    Execute();
                else
                    ExecuteString(text);
                break;
        }
    }

    // Adds the cvars that match the searchpattern to the given arraylist
    public void AddMatchingCommandsToList(Collection<String> list, String searchPattern) {
        searchPattern = searchPattern.toLowerCase();
        for (String var: registeredCommands.keySet()) {
            if(var.toLowerCase().startsWith(searchPattern))
                list.add(var);
        }
    }

    public void Execute() {
        while(cmd_text.length() > 0) {
            if(wait > 0) {
                wait--;
                break;
            }

            int quotes = 0;
            int i;
            for (i = 0; i < cmd_text.length(); i++) {
                char c = cmd_text.charAt(i);
                if(c == '"')
                    quotes++;
                if((quotes&1)!=1 && c == ';')
                    break;  // dont break if inside a quoted string
                if(c == '\n' || c == '\r')
                    break;
            }

            // Cap size
            if(i >= MAX_LINE_SIZE)
                i = MAX_LINE_SIZE - 1;
            // extract line from command buffer
            String line = cmd_text.substring(0, i);

            // delete the text from the command buffer and move remaining commands down
            // this is necessary because commands (exec) can insert data at the
            // beginning of the text buffer
            i++;
            cmd_text.delete(0, i);

            ExecuteString(line);
        }
    }

    // Adds command text at the end of the buffer, does NOT add a final \n
    public void AddText(String text) {
        if(cmd_text.length() + text.length() >= BUFFER_SIZE) {
            Common.Log("AddText(): Command buffer overflow");
            return;
        }
        cmd_text.append(text);
    }

    // Adds command text immediately after the current command
    // Adds a \n to the text
    private void InsertText(String text) {
        if(cmd_text.length() + text.length() + 1 >= BUFFER_SIZE) {
            Common.Log("InsertText(): Command buffer overflow");
            return;
        }

        cmd_text.insert(0, '\n');
        cmd_text.insert(0, text);
    }



    public void AddCommand(String name, ICommand cmd) {
        name = name.toLowerCase();
        if(registeredCommands.containsKey(name)) {
            Common.LogDebug("AddCommand: " + name + " is already defined.");
            //return;
            RemoveCommand(name);
        }

        registeredCommands.put(name, cmd);
    }

    public void RemoveCommand(String cmd) {
        cmd = cmd.toLowerCase();
        if(!registeredCommands.containsKey(cmd))
            Common.LogDebug("RemoveCommand: " + cmd + " didn't exist.");
        registeredCommands.remove(cmd);    
    }

    public void cmd_ListCmds(String[] tokens) {
        Common.Log("Command listing:");
        for(String str : registeredCommands.keySet()) {
            Common.Log(" " + str);
        }
        Common.Log("----");
    }

    private void ExecuteString(String str) {
        String[] tokens = TokenizeString(str, false);

        // no tokens
        if(tokens == null || tokens.length == 0)
            return;

        // Check if we have a command registered by this name
        String cmd = tokens[0].toLowerCase();
        if(registeredCommands.containsKey(cmd)) {
            ICommand command = registeredCommands.get(cmd);
            if(command != null) { // command can be null but still exist
                command.RunCommand(tokens);
                return;
            }
        }

        // Check for cvars with this name
        if(Ref.cvars.HandleFromCommand(tokens))
            return;

        // Check client game commands
        if(Ref.common.cl_running.iValue == 1 && Ref.cgame != null && Ref.cgame.GameCommands(tokens))
            return;

        // Check aliases
        if(aliases.containsKey(cmd)) {
            ExecuteText(ExecType.INSERT, aliases.get(cmd) + "\n");
            return;
        }

//        if(Ref.common.sv_running.iValue == 1 && Ref.game.GameCommand(tokens))
//            return;

        // send it as a server command if we are connected
        // this will usually result in a chat message
        if(Ref.client != null) {
            Ref.client.clc.ForwardCommandToServer(str, tokens);
        }
    }

    // Returns  a single string containing argv(1) to argv(argc()-1)
    public static String Args(String[] tokens) {
        return ArgsFrom(tokens, 1);
    }

    // Returns  a single string containing argv(arg) to argv(argc()-1)
    public static String ArgsFrom(String[] tokens, int arg) {
        StringBuilder str = new StringBuilder();
        for (int i= arg; i < tokens.length; i++) {
            str.append(tokens[i]);
            if(i < tokens.length-1)
                str.append(' ');
        }
        return str.toString().trim();
    }
    
    public static String[] trimTokens(String[] tokens) {
        int count = 0;
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].replace('\n', ' ').trim();
            if(!tokens[i].isEmpty()) count++;
        }
        if(count != tokens.length) {
            String[] ntokens = new String[count];
            int n = 0;
            for (int i = 0; i < tokens.length; i++) {
                if(tokens[i].isEmpty()) continue;
                ntokens[n++] = tokens[i];
            }
            tokens = ntokens;
        }
        return tokens;
    }

    // Takes a string and splits it up
    public static String[] TokenizeString(String str, boolean ignoreQuotes) {
//        byte[] data;
//        try {
//            data = str.getBytes("UTF-8");
//        } catch (UnsupportedEncodingException ex) {
//            Ref.common.Error(Common.ErrorCode.FATAL, "Current JVM does not support UTF-8: " + Common.getExceptionString(ex));
//            return null;
//        }
        
        String text_out = "";

        if(str == null || str.length() == 0)
            return null;

        tempTokens.clear();
        int offset = 0;
        int len = str.length();
        while(true) {
            if(tempTokens.size() == 1024)
            {
                String[] dst = new String[1024];
                return tempTokens.toArray(dst);
            }
            text_out = "";
            
            while(true) {
                // skip whitespace
                char c = 0;
                while(offset < len && ((Character.isWhitespace(c = str.charAt(offset)) && c != '\n') || Character.isIdentifierIgnorable(c)))
                    offset++;

                if(offset >= len)
                {
                    String[] dst = new String[tempTokens.size()];
                    return tempTokens.toArray(dst);
                }

                // skip // comments
                if(c == '/' && offset < len-1 && str.charAt(offset+1) == '/') {
                    String[] dst = new String[tempTokens.size()];
                    return tempTokens.toArray(dst);
                }

                // skip /* */ comments
                if(c == '/' && offset < len-1 && str.charAt(offset+1) == '*') {
                    while(offset < len-1 && ((c = str.charAt(offset)) != '*' || str.charAt(offset+1) != '/'))
                        offset++;
                    if(offset >= len)
                    {
                        String[] dst = new String[tempTokens.size()];
                        return tempTokens.toArray(dst);
                    }
                    offset += 2;
                } else
                    break;
            }

            char c = str.charAt(offset);

            // Handle quoted string
            if(!ignoreQuotes && c == '"') {
                offset++;
                while(offset < len && (c = str.charAt(offset)) != '"') {
                    text_out += c;
                    offset++;
                }
                tempTokens.add(text_out);

                if(offset >= len) {
                    String[] dst = new String[tempTokens.size()];
                    return tempTokens.toArray(dst);
                }

                offset++;
                continue;
            }

            while(offset < len &&  !Character.isIdentifierIgnorable((c = str.charAt(offset))) && (!Character.isWhitespace(c) || c == '\n')) {
                if(!ignoreQuotes && c == '"')
                    break;

                if(c == '/' && offset < len-1 && str.charAt(offset+1) == '/')
                    break;

                if(c == '/' && offset < len-1 && str.charAt(offset+1) == '*')
                    break;

                text_out += c;
                offset++;
                if(c == '\n') break;
            }

            tempTokens.add(text_out);
            if(offset >= len)
            {
                String[] dst = new String[tempTokens.size()];
                return tempTokens.toArray(dst);
            }
        }
    }
}
