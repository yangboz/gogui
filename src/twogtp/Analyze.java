//-----------------------------------------------------------------------------
// $Id$
// $Source$
//-----------------------------------------------------------------------------

package twogtp;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import go.*;
import utils.*;

//-----------------------------------------------------------------------------

public class Analyze
{
    public Analyze(String filename) throws Exception
    {
        File file = new File(filename);
        readFile(file);
        File htmlFile =
            new File(FileUtils.replaceExtension(file, "dat", "html"));
        File dataFile =
            new File(FileUtils.replaceExtension(file, "dat", "summary.dat"));
        if (htmlFile.exists())
            throw new Exception("File " + htmlFile + " exists");
        if (dataFile.exists())
            throw new Exception("File " + dataFile + " exists");
        calcStatistics();
        writeHtml(htmlFile);
        writeData(dataFile);
    }

    private int m_duplicates;

    private int m_errors;

    private int m_games;

    private int m_gamesUsed;

    private int m_lineNumber;

    private String m_black = "Black";

    private String m_white = "White";

    private String m_blackCommand = "";

    private String m_whiteCommand = "";

    private String m_size = "";

    private String m_komi = "";

    private String m_date = "";

    private String m_host = "";

    private Vector m_entries = new Vector(128, 128);

    private Statistics m_length = new Statistics();

    private Statistics m_unknownBlack = new Statistics();

    private Statistics m_unknownWhite = new Statistics();

    private Statistics m_winBlack = new Statistics();

    private Statistics m_winWhite = new Statistics();

    private Statistics m_cpuBlack = new Statistics();

    private Statistics m_cpuWhite = new Statistics();

    private Histogram m_histoBlack = new Histogram(-400, 400, 10);

    private Histogram m_histoWhite = new Histogram(-400, 400, 10);

    private void calcStatistics()
    {
        for (int i = 0; i < m_entries.size(); ++i)
        {
            Entry e = (Entry)m_entries.get(i);
            ++m_games;
            if (e.m_error)
            {
                ++m_errors;
                continue;
            }
            if (! e.m_duplicate.equals("")
                && ! e.m_duplicate.equals("-"))
            {
                ++m_duplicates;
                continue;
            }
            ++m_gamesUsed;
            parseResult(e.m_resultBlack, m_histoBlack, m_winBlack,
                        m_unknownBlack);
            parseResult(e.m_resultWhite, m_histoWhite, m_winWhite,
                        m_unknownWhite);
            m_cpuBlack.addValue(e.m_cpuBlack);
            m_cpuWhite.addValue(e.m_cpuWhite);
            m_length.addValue(e.m_length);
        }
    }

    private String getCommentValue(String comment, String key)
    {
        assert(comment.startsWith(key));
        return comment.substring(key.length()).trim();
    }

    private void handleComment(String comment)
    {
        comment = comment.trim();
        if (comment.startsWith("Black:"))
            m_black = getCommentValue(comment, "Black:");
        else if (comment.startsWith("White:"))
            m_white = getCommentValue(comment, "White:");
        else if (comment.startsWith("BlackCommand:"))
            m_blackCommand = getCommentValue(comment, "BlackCommand:");
        else if (comment.startsWith("WhiteCommand:"))
            m_whiteCommand = getCommentValue(comment, "WhiteCommand:");
        else if (comment.startsWith("Size:"))
            m_size = getCommentValue(comment, "Size:");
        else if (comment.startsWith("Komi:"))
            m_komi = getCommentValue(comment, "Komi:");
        else if (comment.startsWith("Date:"))
            m_date = getCommentValue(comment, "Date:");
        else if (comment.startsWith("Host:"))
            m_host = getCommentValue(comment, "Host:");
    }

    private void handleLine(String line) throws Exception
    {
        line = line.trim();
        if (line.startsWith("#"))
        {
            handleComment(line.substring(1));
            return;
        }
        String[] array = line.split("\t");
        if (array.length < 9 || array.length > 10)
            throwException("Wrong file format");
        try
        {
            int gameIndex = Integer.parseInt(array[0]);
            String resultBlack = array[1];
            String resultWhite = array[2];
            boolean alternated = (Integer.parseInt(array[3]) != 0);
            String duplicate = array[4];
            int length = Integer.parseInt(array[5]);
            float cpuBlack = Float.parseFloat(array[6]);
            float cpuWhite = Float.parseFloat(array[7]);
            boolean error = (Integer.parseInt(array[8]) != 0);
            String errorMessage = "";
            if (array.length == 10)
                errorMessage = array[9];
            m_entries.add(new Entry(gameIndex, resultBlack, resultWhite,
                                    alternated, duplicate, length,
                                    cpuBlack, cpuWhite, error, errorMessage));

        }
        catch (NumberFormatException e)
        {
            throwException("Wrong file format");
        }
    }

    private void parseResult(String result, Statistics statResult,
                             Statistics statWin, Statistics statUnknown)
    {
        boolean isValid = false;
        double r = 0;
        String s = result.trim();
        try
        {
            if (! s.equals("?"))
            {
                if (s.indexOf("B+") >= 0)
                    r = Double.parseDouble(s.substring(2));
                else if (s.indexOf("W+") >= 0)
                    r = - Double.parseDouble(s.substring(2));
                isValid = true;
            }
        }
        catch (NumberFormatException e)
        {
        }
        if (isValid)
        {
            statResult.addValue(r);
            statWin.addValue(r > 0 ? 1 : 0);
        }
        statUnknown.addValue(isValid ? 0 : 1);
    }

    private void readFile(File file) throws Exception
    {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        m_lineNumber = 0;
        String line = null;
        while ((line = reader.readLine()) != null)
        {
            ++m_lineNumber;
            handleLine(line);
        }
        reader.close();
    }

    private void throwException(String message) throws Exception
    {
        throw new Exception("Line " + m_lineNumber + ": " + message);
    }

    private void writeHtml(File file) throws Exception
    {
        String prefix = "game";
        if (file.toString().endsWith(".html"))
        {
            String filename = file.toString();
            prefix = filename.substring(0, filename.length() - 5);
        }
        PrintStream out = new PrintStream(new FileOutputStream(file));
        NumberFormat format = NumberFormat.getInstance(new Locale("C"));
        format.setMaximumFractionDigits(1);
        out.print("<html>\n" +
                  "<head>\n" +
                  "<title>" + m_black + " - " + m_white + "</title>\n" +
                  "</head>\n" +
                  "<body bgcolor=\"white\" text=\"black\" link=\"#0000ee\"" +
                  " vlink=\"#551a8b\">\n" +
                  "<h1>" + m_black + " - " + m_white + "</h1>\n" +
                  "<hr>\n" +
                  "<small>\n" +
                  "<table>\n" +
                  "<tr><th align=\"left\">Black:</th><td align=\"left\">"
                  + m_black + "</td></tr>\n" +
                  "<tr><th align=\"left\">White:</th><td align=\"left\">"
                  + m_white + "</td></tr>\n" +
                  "<tr><th align=\"left\">Size:</th><td align=\"left\">"
                  + m_size + "</td></tr>\n" +
                  "<tr><th align=\"left\">Komi:</th><td align=\"left\">"
                  + m_komi + "</td></tr>\n" +
                  "<tr><th align=\"left\">Date:</th><td align=\"left\">"
                  + m_date + "</td></tr>\n" +
                  "<tr><th align=\"left\">Host:</th><td align=\"left\">"
                  + m_host + "</td></tr>\n" +
                  "<tr><th align=\"left\">Black command:</th>"
                  + "<td align=\"left\"><tt>"
                  + m_blackCommand + "</tt></td></tr>\n" +
                  "<tr><th align=\"left\">White command:</th>"
                  + "<td align=\"left\"><tt>"
                  + m_whiteCommand + "</tt></td></tr>\n" +
                  "<tr><th align=\"left\">Games:</th><td align=\"left\">"
                  + m_games + "</td></tr>\n" +
                  "<tr><th align=\"left\">Errors:</th><td align=\"left\">"
                  + m_errors + "</td></tr>\n" +
                  "<tr><th align=\"left\">Duplicates:</th><td align=\"left\">"
                  + m_duplicates + "</td></tr>\n" +
                  "<tr><th align=\"left\">Games used:</th><td align=\"left\">"
                  + m_gamesUsed + "</td></tr>\n" +
                  "<tr><th align=\"left\">Game length:</th>"
                  + "<td align=\"left\">"
                  + format.format(m_length.getMean()) + " (&plusmn;"
                  + format.format(m_length.getErrorMean())
                  + ")</td></tr>\n" +
                  "<tr><th align=\"left\">CpuTime Black:</th>"
                  + "<td align=\"left\">"
                  + format.format(m_cpuBlack.getMean()) + " (&plusmn;"
                  + format.format(m_cpuBlack.getErrorMean())
                  + ")</td></tr>\n" +
                  "<tr><th align=\"left\">CpuTime White:</th>"
                  + "<td align=\"left\">"
                  + format.format(m_cpuWhite.getMean()) + " (&plusmn;"
                  + format.format(m_cpuWhite.getErrorMean())
                  + ")</td></tr>\n" +
                  "</table>\n" +
                  "</small>\n" +
                  "<hr>\n");
        out.print("<h2>Result [" + m_black + "]</h2>\n" +
                  "<p>\n" +
                  "<table>\n" +
                  "<tr><th align=\"left\">Black score["
                  + m_black + "]:</th><td align=\"left\">"
                  + format.format(m_histoBlack.getMean()) + " (&plusmn;"
                  + format.format(m_histoBlack.getErrorMean())
                  + ")</td></tr>\n" +
                  "<tr><th align=\"left\">Black wins["
                  + m_black + "]:</th><td align=\"left\">"
                  + format.format(m_winBlack.getMean() * 100) + "% (&plusmn;"
                  + format.format(m_winBlack.getErrorMean() * 100)
                  + ")</td></tr>\n" +
                  "<tr><th align=\"left\">Unknown["
                  + m_black + "]:</th><td align=\"left\">"
                  + format.format(m_unknownBlack.getMean() * 100) + "%"
                  + "</td></tr>\n" +
                  "</table>\n" +
                  "</p>\n");
        m_histoBlack.printHtml(out);
        out.print("<hr>\n" +
                  "<h2>Result [" + m_white + "]</h2>\n" +
                  "<p>\n" +
                  "<table>\n" +
                  "<tr><th align=\"left\">Black score["
                  + m_white + "]:</th><td align=\"left\">"
                  + format.format(m_histoWhite.getMean()) + " (&plusmn;"
                  + format.format(m_histoWhite.getErrorMean())
                  + ")</td></tr>\n" +
                  "<tr><th align=\"left\">Black wins["
                  + m_white + "]:</th><td align=\"left\">"
                  + format.format(m_winWhite.getMean() * 100) + "% (&plusmn;"
                  + format.format(m_winWhite.getErrorMean() * 100)
                  + ")</td></tr>\n" +
                  "<tr><th align=\"left\">Unknown["
                  + m_white + "]:</th><td align=\"left\">"
                  + format.format(m_unknownWhite.getMean() * 100) + "%"
                  + "</td></tr>\n" +
                  "</table>\n" +
                  "</p>\n");
        m_histoWhite.printHtml(out);
        out.print("<hr>\n");
        out.print("<table border=\"1\">\n" +
                  "<thead>\n" +
                  "<th>Game</th>\n" +
                  "<th>Result [" + m_black + "]</th>\n" +
                  "<th>Result [" + m_white + "]</th>\n" +
                  "<th>Colors exchanged</th>\n" +
                  "<th>Duplicate</th>\n" +
                  "<th>Length</th>\n" +
                  "<th>CpuTime Black</th>\n" +
                  "<th>CpuTime White</th>\n" +
                  "<th>Error</th>\n" +
                  "<th>Error Message</th>\n" +
                  "</thead>\n");
        for (int i = 0; i < m_entries.size(); ++i)
        {
            Entry e = (Entry)m_entries.get(i);
            String name = prefix + "-" + e.m_gameIndex + ".sgf";
            out.print("<tr align=\"center\">" +
                      "<td><a href=\"" + name + "\">" + name + "</a></td>\n" +
                      "<td>" + e.m_resultBlack + "</td>" +
                      "<td>" + e.m_resultWhite + "</td>" +
                      "<td>" + (e.m_alternated ? "1" : "0") + "</td>" +
                      "<td>" + e.m_duplicate + "</td>" +
                      "<td>" + e.m_length + "</td>" +
                      "<td>" + e.m_cpuBlack + "</td>" +
                      "<td>" + e.m_cpuWhite + "</td>" +
                      "<td>" + (e.m_error ? "1" : "") + "</td>" +
                      "<td>" + e.m_errorMessage + "</td>" +
                      "</tr>\n");
        }
        out.print("</table>\n" +
                  "<hr>\n");
        out.print("</body>\n" +
                  "</html>\n");
        out.close();
    }

    private void writeData(File file) throws Exception
    {
        PrintStream out = new PrintStream(new FileOutputStream(file));
        NumberFormat format1 = NumberFormat.getInstance(new Locale("C"));
        format1.setMaximumFractionDigits(1);
        NumberFormat format2 = NumberFormat.getInstance(new Locale("C"));
        format2.setMaximumFractionDigits(2);
        out.print("# Games\tErr\tDup\tUsed\tResB\tErrResB\tWinB\tErrWinB\t"
                  + "UnknB\tResB\tErrResB\tWinB\tErrWinB\tUnknB\n" +
                  m_games + "\t" + m_errors + "\t" + m_duplicates + "\t"
                  + m_gamesUsed
                  + "\t" + format1.format(m_histoBlack.getMean())
                  + "\t" + format1.format(m_histoBlack.getErrorMean())
                  + "\t" + format2.format(m_winBlack.getMean())
                  + "\t" + format2.format(m_winBlack.getErrorMean())
                  + "\t" + format2.format(m_unknownBlack.getMean())
                  + "\t" + format1.format(m_histoWhite.getMean())
                  + "\t" + format1.format(m_histoWhite.getErrorMean())
                  + "\t" + format2.format(m_winWhite.getMean())
                  + "\t" + format2.format(m_winWhite.getErrorMean())
                  + "\t" + format2.format(m_unknownWhite.getMean()) + "\n");
        out.close();
    }
}

class Entry
{
    public int m_gameIndex;

    public String m_resultBlack;

    public String m_resultWhite;

    public boolean m_alternated;

    public String m_duplicate;

    public int m_length;

    public float m_cpuBlack;

    public float m_cpuWhite;

    public boolean m_error;

    public String m_errorMessage;

    public Entry(int gameIndex, String resultBlack, String resultWhite,
                 boolean alternated, String duplicate, int length,
                 float cpuBlack, float cpuWhite, boolean error,
                 String errorMessage)
    {
        m_gameIndex = gameIndex;
        m_resultBlack = resultBlack;
        m_resultWhite = resultWhite;
        m_alternated = alternated;
        m_duplicate = (duplicate.equals("-") ? "" : duplicate);
        m_length = length;
        m_cpuBlack = cpuBlack;
        m_cpuWhite = cpuWhite;
        m_error = error;
        m_errorMessage = errorMessage;
    }
}

class Statistics
{
    public Statistics()
    {
    }

    public void addValue(double value)
    {
        m_sum += value;
        m_sumSq += (value * value);
        ++m_count;
    }

    public int getCount()
    {
        return m_count;
    }

    public double getDeviation()
    {
        return Math.sqrt(getVariance());
    }

    public double getErrorMean()
    {
        if (m_count == 0)
            return 0;
        return getDeviation() / Math.sqrt(m_count);
    }

    public double getMean()
    {
        if (m_count == 0)
            return 0;
        return m_sum / m_count;
    }

    public double getVariance()
    {
        if (m_count == 0)
            return 0;
        double mean = getMean();
        return m_sumSq / m_count - mean * mean;
    }

    private int m_count;

    private double m_sum;

    private double m_sumSq;
}

class Histogram
    extends Statistics
{
    public Histogram(double min, double max, double step)
    {
        m_min = min;
        m_max = max;
        m_step = step;
        m_size = (int)((max - min) / step) + 1;
        m_array = new int[m_size];
    }

    public void addValue(double value)
    {
        super.addValue(value);
        int i = (int)((value - m_min) / m_step);
        ++m_array[i];
    }

    public void printHtml(PrintStream out)
    {
        out.print("<p>\n" +
                  "<small>\n" +
                  "<table cellspacing=\"1\" cellpadding=\"0\">\n");
        int min;
        for (min = 0; min < m_size - 1 && m_array[min] == 0; ++min);
        int max;
        for (max = m_size - 1; max > 0 && m_array[max] == 0; --max);
        for (int i = min; i <= max; ++i)
        {
            final int scale = 630;
            int width = m_array[i] * scale / getCount();
            out.print("<tr><td align=\"right\">" + (m_min + i * m_step)
                      + "</td><td><table cellspacing=\"0\"" +
                      " cellpadding=\"0\" width=\"" + scale + "\"><tr>" +
                      "<td bgcolor=\"blue\" width=\"" + width +
                      "\"></td>" + "<td bgcolor=\"#cccccc\" width=\""
                      + (scale - width) + "\">"
                      + m_array[i] + "</td></tr></table></td></tr>\n");
        }
        out.print("</small>" +
                  "</table>\n" +
                  "</p>\n");
    }

    private int m_size;

    private double m_min;

    private double m_max;

    private double m_step;

    private int[] m_array;
}

//-----------------------------------------------------------------------------
