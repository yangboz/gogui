//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.io.File;
import java.util.Vector;
import javax.swing.JMenu;

//----------------------------------------------------------------------------

/** Menu for recent files.
    Automatically assigns short, but unique labels.
*/
public class RecentFileMenu
{
    public interface Callback
    {
        void fileSelected(String label, File file);
    }

    public RecentFileMenu(String label, File file, Callback callback)
    {
        assert(callback != null);
        m_callback = callback;
        RecentMenu.Callback recentCallback = new RecentMenu.Callback()
        {
            public void itemSelected(String label, String value)
            {
                m_callback.fileSelected(label, new File(value));
            }
        };
        m_menu = new RecentMenu(label, file, recentCallback);
    }

    public void add(File file)
    {
        String name = file.getName();
        m_menu.add(name, file.toString());
        m_sameName.clear();
        for (int i = 0; i < getCount(); ++i)
            if (getName(i).equals(name))
                m_sameName.add(getValue(i));
        int n = 0;
        while (true)
        {
            boolean samePrefix = true;
            char c = file.toString().charAt(n);
            for (int i = 0; i < m_sameName.size(); ++i)
            {
                String sameName = (String)m_sameName.get(i);
                if (sameName.length() < n || sameName.charAt(n) != c)
                {
                    samePrefix = false;
                    break;
                }
            }
            if (! samePrefix)
                break;
            ++n;
        }
        for (int i = 0; i < getCount(); ++i)
            if (getName(i).equals(name))
                m_menu.setLabel(i, getValue(i).substring(n));
    }

    /** Don't modify the items in this menu! */
    public JMenu getMenu()
    {
        return m_menu.getMenu();
    }

    private Callback m_callback;

    private RecentMenu m_menu;

    /** Vector<String> */
    private Vector m_sameName = new Vector();

    private int getCount()
    {
        return m_menu.getCount();
    }

    private File getFile(int i)
    {
        return new File(getValue(i));
    }

    private String getValue(int i)
    {
        return m_menu.getValue(i);
    }

    private String getName(int i)
    {
        return getFile(i).getName();
    }
}

//----------------------------------------------------------------------------
