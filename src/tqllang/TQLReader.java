package tqllang;

import java.io.*;

/**
 * Created by Yas.
 */

public class TQLReader
{
    private Reader reader;

    public TQLReader(String query)
    {
        reader = new StringReader(query);
    }

    public char getCharacter() throws IOException
    {
        return (char) reader.read();
    }

    public void markPosition() throws IOException
    {
        reader.mark(100);
    }

    public void resetToPosition() throws IOException
    {
        reader.reset();
    }
}
