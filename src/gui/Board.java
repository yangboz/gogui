//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.print.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import go.*;
import utils.GuiUtils;

//-----------------------------------------------------------------------------

class BoardLabel
    extends JLabel
{
    public BoardLabel(String text)
    {
        super(text, JLabel.CENTER);
        setForeground(java.awt.Color.DARK_GRAY);
    }
}

public class Board
    extends JPanel
    implements FocusListener, Printable
{
    public interface Listener
    {
        void fieldClicked(go.Point p, boolean modifiedSelect);
    }

    public Board(go.Board board)
    {
        m_board = board;
        setPreferredFieldSize();
        URL url = getClass().getClassLoader().getResource("images/wood.png");
        if (url != null)
            m_image = new ImageIcon(url);
        initSize(m_board.getSize());
        setFocusable(true);
        addFocusListener(this);
        setFocusPoint(m_focusPoint);
    }

    public void clearAll()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            clearInfluence(p);
            setFieldBackground(p, null);
        }
        clearAllCrossHair();
        clearAllMarkup();
        clearAllSelect();
        clearAllStrings();
        clearAllTerritory();
        drawLastMove();
        if (m_variationShown)
        {
            updateFromGoBoard();
            m_variationShown = false;
        }
    }

    public void clearAllCrossHair()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setCrossHair(m_board.getPoint(i), false);
    }

    public void clearAllMarkup()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setMarkup(m_board.getPoint(i), false);
    }

    public void clearAllSelect()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setSelect(m_board.getPoint(i), false);
    }

    public void clearAllStrings()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setString(m_board.getPoint(i), "");
    }

    public void clearAllTerritory()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
            setTerritory(m_board.getPoint(i), go.Color.EMPTY);
    }

    public void clearInfluence(go.Point p)
    {
        getField(p).clearInfluence();
    }

    public void focusGained(FocusEvent event)
    {
        setFocusPoint(m_focusPoint);
    }

    public void focusLost(FocusEvent event)
    {
    }

    public void fieldClicked(go.Point p, boolean modifiedSelect)
    {
        if (m_listener != null)
            m_listener.fieldClicked(p, modifiedSelect);
    }

    public go.Board getBoard()
    {
        return m_board;
    }

    public boolean[][] getMarkups()
    {
        int size = m_board.getSize();
        boolean[][] result = new boolean[size][size];
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point point = m_board.getPoint(i);
            result[point.getX()][point.getY()] = getField(point).getMarkup();
        }
        return result;
    }

    public Dimension getPreferredFieldSize()
    {
        return m_preferredFieldSize;
    }

    public boolean[][] getSelects()
    {
        int size = m_board.getSize();
        boolean[][] result = new boolean[size][size];
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point point = m_board.getPoint(i);
            result[point.getX()][point.getY()] = getField(point).getSelect();
        }
        return result;
    }

    public boolean getShowCursor()
    {
        return m_showCursor;
    }

    public String[][] getStrings()
    {
        int size = m_board.getSize();
        String[][] result = new String[size][size];
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point point = m_board.getPoint(i);
            result[point.getX()][point.getY()] = getField(point).getString();
        }
        return result;
    }

    public void initSize(int size)
    {
        m_board.initSize(size);
        m_field = new Field[size][size];
        removeAll();
        setLayout(new GridLayout(size + 2, size + 2));
        setOpaque(false);
        addColumnLabels(size);
        for (int y = size - 1; y >= 0; --y)
        {
            String text = Integer.toString(y + 1);
            add(new BoardLabel(text));
            for (int x = 0; x < size; ++x)
            {
                go.Point p = m_board.getPoint(x, y);
                Field field = new Field(this, p);
                add(field);
                m_field[x][y] = field;
                KeyListener keyListener = new KeyAdapter()
                    {
                        public void keyPressed(KeyEvent event)
                        {
                            Board.this.keyPressed(event);
                        }
                    };
                field.addKeyListener(keyListener);
            }
            add(new BoardLabel(text));
        }
        addColumnLabels(size);
        m_focusPoint = new go.Point(size / 2, size / 2);
        m_lastMove = null;
        revalidate();
        repaint();
    }

    public void paintComponent(Graphics graphics)
    {
        Graphics2D graphics2D = (Graphics2D)graphics;
        if (graphics2D != null)
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
        Dimension dimension = getSize();
        if (m_image != null)
            graphics.drawImage(m_image.getImage(), 0, 0, dimension.width,
                               dimension.height, null);
        else
        {
            graphics.setColor(java.awt.Color.YELLOW.darker());
            graphics.fillRect(0, 0, dimension.width, dimension.height);
        }
        int size = m_board.getSize();
        graphics.setColor(java.awt.Color.darkGray);
        for (int y = 0; y < size; ++y)
        {
            java.awt.Point left = getScreenLocation(0, y);
            java.awt.Point right = getScreenLocation(size - 1, y);
            graphics.drawLine(left.x, left.y, right.x, right.y);
        }
        for (int x = 0; x < size; ++x)
        {
            java.awt.Point top = getScreenLocation(x, 0);
            java.awt.Point bottom = getScreenLocation(x, size - 1);
            graphics.drawLine(top.x, top.y, bottom.x, bottom.y);
        }
        int r = dimension.width / (size + 2) / 10;
        for (int x = 0; x < size; ++x)
            if (m_board.isHandicapLine(x))
                for (int y = 0; y < size; ++y)
                    if (m_board.isHandicapLine(y))
                    {
                        java.awt.Point point = getScreenLocation(x, y);
                        graphics.fillOval(point.x - r, point.y - r,
                                          2 * r + 1, 2 * r + 1);
                    }
    }

    public int print(Graphics g, PageFormat format, int page)
        throws PrinterException
    {
        if (page >= 1)
        {
            return Printable.NO_SUCH_PAGE;
        }
        double width = getSize().width;
        double height = getSize().height;
        double pageWidth = format.getImageableWidth();
        double pageHeight = format.getImageableHeight();
        double scale = 1;
        if (width >= pageWidth)
            scale = pageWidth / width;
        double xSpace = (pageWidth - width * scale) / 2;
        double ySpace = (pageHeight - height * scale) / 2;
        Graphics2D g2d = (Graphics2D)g;
        g2d.translate(format.getImageableX() + xSpace,
                      format.getImageableY() + ySpace);
        g2d.scale(scale, scale);
        print(g2d);
        return Printable.PAGE_EXISTS;
    }

    public void resetBoard()
    {
        if (! m_needsReset)
            return;
        clearAll();
        m_needsReset = false;
    }

    public void scoreBegin(go.Point[] isDeadStone)
    {
        m_board.scoreBegin(isDeadStone);
        if (isDeadStone != null)
            for (int i = 0; i < isDeadStone.length; ++i)
                setCrossHair(isDeadStone[i], true);
        calcScore();
    }

    public void scoreSetDead(go.Point p)
    {
        go.Color c = m_board.getColor(p);
        if (c == go.Color.EMPTY)
            return;
        Vector stones = new Vector(m_board.getNumberPoints());
        m_board.getStones(p, c, stones);
        boolean dead = ! m_board.scoreGetDead((go.Point)(stones.get(0)));
        for (int i = 0; i < stones.size(); ++i)
        {
            go.Point stone = (go.Point)stones.get(i);
            m_board.scoreSetDead(stone, dead);
            setCrossHair(stone, dead);
        }
        calcScore();
    }

    public void setFocus()
    {
        int moveNumber = m_board.getMoveNumber();
        if (moveNumber > 0)
        {
            Move m = m_board.getMove(moveNumber - 1);
            go.Point lastMove = m.getPoint();
            if (lastMove != null && m.getColor() != go.Color.EMPTY)
                setFocusPoint(lastMove);
        }
        else if (m_board.getInternalNumberMoves() == 0)
        {
            int size = m_board.getSize();
            if (size > 0)
                setFocusPoint(m_board.getPoint(size / 2, size / 2));
        }
    }

    public void setFocusPoint(go.Point point)
    {
        if (! m_board.contains(point))
            return;
        getField(point).requestFocusInWindow();
        m_focusPoint.set(point.getX(), point.getY());
    }

    public void setFieldBackground(go.Point p, java.awt.Color color)
    {
        getField(p).setFieldBackground(color);
        m_needsReset = true;
    }

    public void setCrossHair(go.Point p, boolean crossHair)
    {
        getField(p).setCrossHair(crossHair);
        m_needsReset = true;
    }

    public void setInfluence(go.Point p, double value)
    {
        getField(p).setInfluence(value);
        m_needsReset = true;
    }

    public void setListener(Listener l)
    {
        m_listener = l;
    }

    public void setMarkup(go.Point p, boolean markup)
    {
        getField(p).setMarkup(markup);
        m_needsReset = true;
    }

    public void setShowCursor(boolean showCursor)
    {
        m_showCursor = showCursor;
    }

    public void setShowLastMove(boolean showLastMove)
    {
        clearLastMove();
        m_showLastMove = showLastMove;
        drawLastMove();
    }

    public void setSelect(go.Point p, boolean select)
    {
        getField(p).setSelect(select);
        m_needsReset = true;
    }

    public void setString(go.Point p, String s)
    {
        getField(p).setString(s);
        m_needsReset = true;
    }

    public void setTerritory(go.Point p, go.Color color)
    {
        getField(p).setTerritory(color);
        m_needsReset = true;
    }

    public void showBWBoard(String[][] board)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            String s = board[p.getX()][p.getY()].toLowerCase();
            if (s.equals("b") || s.equals("black"))
                setTerritory(p, go.Color.BLACK);
            else if (s.equals("w") || s.equals("white"))
                setTerritory(p, go.Color.WHITE);
            else
                setTerritory(p, go.Color.EMPTY);
        }
    }

    public void showColorBoard(String[][] board)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            String s = board[p.getX()][p.getY()];
            java.awt.Color c = null;
            if (s.equals("blue"))
                c = java.awt.Color.blue;
            else if (s.equals("cyan"))
                c = java.awt.Color.cyan;
            else if (s.equals("green"))
                c = java.awt.Color.green;
            else if (s.equals("gray"))
                c = java.awt.Color.lightGray;
            else if (s.equals("magenta"))
                c = java.awt.Color.magenta;
            else if (s.equals("pink"))
                c = java.awt.Color.pink;
            else if (s.equals("red"))
                c = java.awt.Color.red;
            else if (s.equals("yellow"))
                c = java.awt.Color.yellow;
            setFieldBackground(p, c);
        }
    }

    public void showDoubleBoard(double[][] board, double scale)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            double d = board[p.getX()][p.getY()] * scale;
            setInfluence(p, d);
        }
    }

    public void showPointList(go.Point pointList[])
    {
        clearAllMarkup();
        for (int i = 0; i < pointList.length; ++i)
        {
            go.Point p = pointList[i];
            if (p != null)
                setMarkup(p, true);
        }
    }

    public void showPointStringList(Vector pointList, Vector stringList)
    {
        clearAllStrings();
        for (int i = 0; i < pointList.size(); ++i)
        {
            go.Point point = (go.Point)pointList.get(i);
            String string = (String)stringList.get(i);
            if (point != null)
                setString(point, string);
        }
    }

    public void showStringBoard(String[][] board)
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            setString(p, board[p.getX()][p.getY()]);
        }
    }

    public void showVariation(go.Point[] variation, go.Color toMove)
    {
        clearAllStrings();
        for (int i = 0; i < variation.length; ++i)
        {
            go.Point point = variation[i];
            if (point != null)
            {
                setColor(point, toMove);
                setString(point, Integer.toString(i + 1));
            }
            toMove = toMove.otherColor();
        }
        m_variationShown = true;
    }

    public void updateFromGoBoard()
    {
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point point = m_board.getPoint(i);
            Field field = getField(point);
            go.Color color = m_board.getColor(point);
            go.Color oldColor = field.getColor();
            if (color != oldColor)
            {
                setColor(point, color);
                field.repaint();
            }
        }
        drawLastMove();
    }

    private boolean m_needsReset;

    private boolean m_showCursor = true;

    private boolean m_showLastMove = true;

    private boolean m_variationShown;

    private go.Point m_focusPoint;

    private go.Point m_lastMove;

    private Dimension m_preferredFieldSize;

    private go.Board m_board;

    private ImageIcon m_image;

    private Field m_field[][];

    private Listener m_listener;

    private void addColumnLabels(int size)
    {
        add(Box.createRigidArea(new Dimension(1, 1)));
        char c = 'A';
        for (int x = 0; x < size; ++x)
        {
            add(new BoardLabel(new Character(c).toString()));
            ++c;
            if (c == 'I')
                ++c;
        }
        add(Box.createRigidArea(new Dimension(1, 1)));
    }

    private void calcScore()
    {
        m_board.calcScore();
        for (int i = 0; i < m_board.getNumberPoints(); ++i)
        {
            go.Point p = m_board.getPoint(i);
            go.Color c = m_board.getScore(p);
            if (c == go.Color.BLACK)
                setInfluence(p, 1.0);
            else if (c == go.Color.WHITE)
                setInfluence(p, -1.0);
            else
                setInfluence(p, 0);
        }
    }

    private void clearLastMove()
    {
        if (m_lastMove != null)
        {
            Field field = getField(m_lastMove);
            field.setLastMoveMarker(false);
            field.repaint();
            m_lastMove = null;
        }
    }

    private void drawLastMove()
    {
        if (m_showLastMove)
            clearLastMove();
        int moveNumber = m_board.getMoveNumber();
        if (moveNumber > 0)
        {
            Move m = m_board.getMove(moveNumber - 1);
            go.Point lastMove = m.getPoint();
            if (lastMove != null && m.getColor() != go.Color.EMPTY)
            {
                if (m_showLastMove)
                {
                    Field field = getField(lastMove);
                    field.setLastMoveMarker(true);
                    field.repaint();
                    m_lastMove = lastMove;
                }
            }
        }
        setFocus();
    }

    private Field getField(go.Point p)
    {
        assert(p != null);
        return m_field[p.getX()][p.getY()];
    }

    private void setColor(go.Point p, go.Color color)
    {
        getField(p).setColor(color);
    }

    private java.awt.Point getScreenLocation(int x, int y)
    {
        Dimension dimension = getSize();
        int size = m_board.getSize();
        int dx = dimension.width / (size + 2);
        int dy = dimension.height / (size + 2);
        return new java.awt.Point(dx / 2 + (x + 1) * dx,
                                  dy / 2 + (size - y) * dy);
    }

    private void keyPressed(KeyEvent event)
    {
        int code = event.getKeyCode();
        int modifiers = event.getModifiers();
        int size = m_board.getSize();
        if ((modifiers & ActionEvent.CTRL_MASK) != 0)
            return;
        boolean shiftModifier = ((modifiers & ActionEvent.SHIFT_MASK) != 0);
        if (code == KeyEvent.VK_DOWN)
        {
            if (shiftModifier)
                do
                    m_focusPoint.down();
                while (! m_board.isHandicapLine(m_focusPoint.getY())
                       && ! m_board.isEdgeLine(m_focusPoint.getY()));
            else
                m_focusPoint.down();
        }
        else if (code == KeyEvent.VK_UP)
        {
            if (shiftModifier)
                do
                    m_focusPoint.up(size);
                while (! m_board.isHandicapLine(m_focusPoint.getY())
                       && ! m_board.isEdgeLine(m_focusPoint.getY()));
            else
                m_focusPoint.up(size);
        }
        else if (code == KeyEvent.VK_LEFT)
        {
            if (shiftModifier)
                do
                    m_focusPoint.left();
                while (! m_board.isHandicapLine(m_focusPoint.getX())
                       && ! m_board.isEdgeLine(m_focusPoint.getX()));
            else
                m_focusPoint.left();
        }
        else if (code == KeyEvent.VK_RIGHT)
        {
            if (shiftModifier)
                do
                    m_focusPoint.right(size);
                while (! m_board.isHandicapLine(m_focusPoint.getX())
                       && ! m_board.isEdgeLine(m_focusPoint.getX()));
            else
                m_focusPoint.right(size);
        }
        setFocusPoint(m_focusPoint);
    }

    private void setPreferredFieldSize()
    {
        int size = (int)((double)GuiUtils.getDefaultMonoFontSize() * 2.5);
        if (size % 2 == 0)
            ++size;
        m_preferredFieldSize = new Dimension(size, size);
    }
}

//-----------------------------------------------------------------------------
