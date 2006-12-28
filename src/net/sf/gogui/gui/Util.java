//----------------------------------------------------------------------------
// $Id$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.Component;
import javax.swing.JOptionPane;
import net.sf.gogui.go.Board;
import net.sf.gogui.go.Komi;
import net.sf.gogui.gtp.GtpError;
import net.sf.gogui.util.StringUtil;

/** Static utility functions. */
public final class Util
{
    /** Set komi.
        Sends the komi command if the CommandThread is not null and
        it supports the command.
        Errors are shown to the user.
    */
    public static void sendKomi(Component parent, Komi komi,
                                String name, GuiGtpClient gtp)
    {
        if (gtp == null || komi == null)
            return;
        try
        {
            if (gtp.isCommandSupported("komi"))
                gtp.send("komi " + komi);
        }
        catch (GtpError e)
        {
            showError(parent, name, e);
        }
    }

    /** Set rules using the scoring_system command.
        Sends the scoring_system command if rules are not
        go.Board.RULES_UNKNOWN, the CommandThread is not null and
        it supports the command.
        Errors are ignored.
    */
    public static void sendRules(int rules, GuiGtpClient gtp)
    {
        if (gtp == null
            || rules == Board.RULES_UNKNOWN
            || ! gtp.isCommandSupported("scoring_system"))
            return;
        try
        {
            String s =
                (rules == Board.RULES_JAPANESE ? "territory" : "area");
            gtp.send("scoring_system " + s);
        }
        catch (GtpError e)
        {
        }
    }

    public static void showError(Component parent, String name,
                                 GtpError error)
    {        
        String message = error.getMessage().trim();
        if (message.length() == 0)
            message = "Command failed";
        else
            message = StringUtil.capitalize(message);
        String title = "Error";
        if (name != null)
            title = title + " - " + name;
        JOptionPane.showMessageDialog(parent, message, title,
                                      JOptionPane.ERROR_MESSAGE);
    }

    /** Make constructor unavailable; class is for namespace only. */
    private Util()
    {
    }
}

