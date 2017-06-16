package tqllang;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Yas.
 */

public class TQLScanner
{
    public int currentLine;
    public int currentCharPosition;
    public String tokenString;

    private TQLReader reader;
    private char inputChar;
    private String tokenStringBuilder;
    private HashMap<String, Token> keywordTable;
    private boolean firstRun;

    public TQLScanner(String query)
    {
        reader = new TQLReader(query);
        tokenStringBuilder = "";

        // pre-populate keyword table with keywords
        keywordTable = new HashMap<>(15);
        keywordTable.put("define", Token.defineToken);
        keywordTable.put("sensorcollection", Token.sensorToken);
        keywordTable.put("observationcollection", Token.observationToken);
        keywordTable.put("sensors_to_observations", Token.sensorToObsToken);
        keywordTable.put("select", Token.selectToken);
        keywordTable.put("as", Token.asToken);
        keywordTable.put("from", Token.fromToken);
        keywordTable.put("where", Token.whereToken);
        keywordTable.put("group by", Token.groupbyToken);
        keywordTable.put("having", Token.havingToken);

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

    public Token getToken(boolean preserveCharacter)
    {
        // don't eat spaces if you are reading as a string for "WHERE", "GROUP BY" and "HAVING"
        if(!preserveCharacter)
            eatSpaces();

        // error character, end of file characters
        if (inputChar == 0x00)
        {
            error("Error in reading");
            return Token.errorToken;
        }

        // skip the characters if it's a comment
        /*while(inputChar == '#')
        {
            skipCharacters();
            eatSpaces();
        }*/

        tokenStringBuilder = "";

        switch (inputChar)
        {
            case (char)-1:
                //System.out.println("Reached end of file");
                return Token.endOfFileToken;
            case '*':
                // eat "*"
                next();
                tokenString = "*";
                return Token.timesToken;
            case '=':
                // eat "="
                next();
                tokenString = "=";
                return Token.eqlToken;
            case ',':
                // eat ","
                next();
                tokenString = ",";
                return Token.commaToken;
            case ')':
                // eat ")"
                next();
                tokenString = ")";
                return Token.closeparenToken;
            case '(':
                // eat "("
                next();
                tokenString = "(";
                return Token.openparenToken;
            case ';':
                next();
                tokenString = ";";
                return Token.semiToken;
            default:

                // TODO: code needs to handle string quotes "...."
                if(Character.isLetter(inputChar))
                {
                    // TODO: Think
                    while (Character.isLetterOrDigit(inputChar) || inputChar == '.' || inputChar == '_' || inputChar == '*')
                    {
                        tokenStringBuilder = tokenStringBuilder+inputChar;
                        next();
                    }

                    tokenString = tokenStringBuilder;

                    // this is for group by
                    if(tokenString.equalsIgnoreCase("group"))
                    {
                        try
                        {
                            // save the state
                            int savedLine = currentLine;
                            int savedCharPosition = currentCharPosition;
                            char savedChar = inputChar;
                            String savedTokenString = tokenString;
                            String builder = "";

                            reader.markPosition();

                            eatSpaces();

                            if(inputChar == 'b' || inputChar == 'B')
                            {
                                builder += inputChar;

                                next();

                                if(inputChar == 'y' || inputChar == 'Y')
                                {
                                    builder += inputChar;

                                    next();

                                    if(Character.isWhitespace(inputChar))
                                    {
                                        tokenString = savedTokenString+" "+builder;
                                        return Token.groupbyToken;
                                    }
                                }
                            }

                            // undo the reading since it's not a "group by"
                            currentLine = savedLine;
                            currentCharPosition = savedCharPosition;
                            inputChar = savedChar;
                            reader.resetToPosition();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    if(keywordTable.containsKey(tokenStringBuilder.toLowerCase()))
                    {
                        return keywordTable.get(tokenStringBuilder.toLowerCase());
                    }
                    else
                    {
                        return Token.identToken;
                    }
                }

                if(preserveCharacter)
                {
                    tokenString = ""+inputChar;
                    next();
                    return Token.identToken;
                }

                return Token.errorToken;
        }
    }

    public boolean isKeyword(String name)
    {
        return keywordTable.containsKey(name.toLowerCase());
    }

    private void eatSpaces()
    {
        while(Character.isWhitespace(inputChar))
        {
            next();
        }
    }

    private void skipCharacters()
    {
        // keep reading characters until you hit end of line
        while(inputChar != '\n')
        {
            next();
        }

        // eat end of line
        next();
    }

    private void error(String errorMsg)
    {
        inputChar = 0x00;
        System.out.println(errorMsg);
    }
}
