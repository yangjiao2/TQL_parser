package tqllang;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Yas
 */

public class WhereScanner
{
    private TQLReader reader;
    private char inputChar;
    public String tokenString;
    private String tokenStringBuilder;
    private HashMap<String, Token> keywordTable;
    private boolean firstRun;
    private int currentLine;
    private int currentCharPosition;

    public WhereScanner(String whereClause)
    {
        reader = new TQLReader(whereClause);
        tokenStringBuilder = "";

        // pre-populate keyword table with keywords
        keywordTable = new HashMap<>(15);
        keywordTable.put("and", Token.andToken);
        keywordTable.put("or", Token.orToken);
        keywordTable.put("in", Token.inToken);

        firstRun = true;
        currentLine = 1;
        currentCharPosition = 1;
        next();
    }

    private void next()
    {
        // don't check if it's the first run
        // don't get any character if
        if(!firstRun)
        {
            if (inputChar == 0x00 || inputChar == 0xff || inputChar == (char)-1)
                return;
        }

        try
        {
            inputChar = reader.getCharacter();
        }
        catch(IOException e)
        {
            inputChar = 0x00;
            return;
        }

        currentCharPosition += 1;

        if(inputChar == '\n')
        {
            currentLine += 1;
            currentCharPosition = 0;
        }
    }

    public Token getToken() throws TQLException
    {
        // error character, end of file characters
        if (inputChar == 0x00)
        {
            error("Error in reading");
            return Token.errorToken;
        }

        tokenStringBuilder = "";

        if(inputChar == (char)-1)
        {
            //System.out.println("Reached end of file");
            return Token.endOfFileToken;
        }
        else if(inputChar == '\"')
        {
            tokenStringBuilder = tokenStringBuilder+inputChar;
            next();

            // keep reading until you find "
            while(inputChar != '\"')
            {
                // Exception when you reach end of file or there is an error in reading
                if(inputChar == 0x00 || inputChar == 0xff || inputChar == (char)-1)
                {
                    throw new TQLException("Syntax error near a String");
                }

                tokenStringBuilder = tokenStringBuilder+inputChar;
                next();
            }

            tokenStringBuilder = tokenStringBuilder+inputChar;
            tokenString = tokenStringBuilder;

            next();

            return Token.stringToken;
        }
        else if(inputChar == '\'')
        {
            tokenStringBuilder = tokenStringBuilder+inputChar;
            next();

            // keep reading until you find '
            while(inputChar != '\'')
            {
                // Exception when you reach end of file or there is an error in reading
                if(inputChar == 0x00 || inputChar == 0xff || inputChar == (char)-1)
                {
                    throw new TQLException("Syntax error near a String");
                }

                tokenStringBuilder = tokenStringBuilder+inputChar;
                next();
            }

            tokenStringBuilder = tokenStringBuilder+inputChar;
            tokenString = tokenStringBuilder;

            next();

            return Token.stringToken;

        }
        else if(Character.isLetter(inputChar))
        {
            // this is an identifier
            // TODO: Think
            while (Character.isLetterOrDigit(inputChar) || inputChar == '.' || inputChar == '_')
            {
                tokenStringBuilder = tokenStringBuilder+inputChar;
                next();
            }

            tokenString = tokenStringBuilder;

            if(keywordTable.containsKey(tokenStringBuilder.toLowerCase()))
            {
                return keywordTable.get(tokenStringBuilder.toLowerCase());
            }
            else
            {
                return Token.identToken;
            }

        }
        else
        {
            // just return the character as is
            tokenString = inputChar+"";
            next();
            return Token.characterToken;
        }
    }

    private void error(String errorMsg)
    {
        inputChar = 0x00;
        System.out.println(errorMsg);
    }
}
