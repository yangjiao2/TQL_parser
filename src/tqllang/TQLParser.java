package tqllang;

/**
 * Created by Yas.
 */

public class TQLParser
{
    public TQLScanner scanner;
    public Token token;
    public TQLQuery tqlQuery;

    public TQLParser(String query)
    {
        scanner = new TQLScanner(query);
        token = scanner.getToken(false);
        tqlQuery = new TQLQuery();
    }

    public void eatToken(boolean preserveCharacter)
    {
        token = scanner.getToken(preserveCharacter);
    }

    public void parse() throws TQLException
    {
        System.out.println("Parsing...");

        // keep parsing until you reach the final query indicated by encountering "SELECT"
        while(token != Token.selectToken)
        {
            if(token == Token.defineToken)
            {
                // parse define statement
                define();
            }
            else if(token == Token.identToken)
            {
                // parse assignment statement
                assign();
            }
            else
            {
                // error
                throw new TQLException("Unexpected statement! Line: "+scanner.currentLine);
            }

            if(token != Token.semiToken)
            {
                // error. statement not ending with ";"
                throw new TQLException("Missing \";\" at end of statement. Line: "+scanner.currentLine);
            }
            else
            {
                // eat ";"
                eatToken(false);
            }

            if(token == Token.endOfFileToken)
            {
                throw new TQLException("Missing a final query.");
            }

            if(token == Token.errorToken)
            {
                throw new TQLException("Unexpected error while parsing around line: "+scanner.currentLine);
            }
        }

        // the "if" condition here looks like it is not required. But we will just keep it!!
        // parse the final query
        if(token == Token.selectToken)
        {
            selectStatement(tqlQuery.finalQuery);

            // TODO: check for ";" to mark the end of the whole query
            if(token != Token.semiToken)
                throw new TQLException("Missing \";\" at the final query. Line: "+scanner.currentLine);
        }
        else
        {
            // error
            throw new TQLException("Missing final query.");
        }

        //System.out.println("Done parsing!");
    }

    public void define() throws TQLException
    {
        // eat "DEFINE"
        eatToken(false);

        // identify the type of the variable
        if(token == Token.sensorToken || token == Token.observationToken)
        {
            Token varType = token;

            // eat SensorCollection/ObservationCollection keyword
            eatToken(false);

            if(token == Token.identToken)
            {
                // try to add the variable our defined variables list
                addCollectionVariable(scanner.tokenString, varType);

                // eat the identifier
                eatToken(false);

                while(token == Token.commaToken)
                {
                    // eat the ","
                    eatToken(false);

                    // try to add the variable our defined variables list
                    addCollectionVariable(scanner.tokenString, varType);

                    // eat the identifier
                    eatToken(false);
                }
            }
            else
            {
                // error
                throw new TQLException("Variable name is missing. Line: "+scanner.currentLine);
            }
        }
        else
        {
            // error
            throw new TQLException("The type of variable is missing or not recognized. Line: "+scanner.currentLine);
        }
    }

    public void assign() throws TQLException
    {
        // store the variable name of the collection
        String variableName = scanner.tokenString;

        if(!tqlQuery.collectionVariables.containsKey(variableName))
            throw new TQLException("Variable "+variableName+" is not defined. Line: "+scanner.currentLine);

        if(tqlQuery.collectionVariables.get(variableName).isAssigned)
            throw new TQLException("Variable "+variableName+" is already assigned. Line: "+scanner.currentLine);

        // eat the identifier
        eatToken(false);

        if(token == Token.eqlToken)
        {
            // eat "="
            eatToken(false);

            if(token == Token.selectToken)
            {
                SQLQuery query = new SQLQuery();

                // first check the variable is of type SensorCollection
                if(tqlQuery.collectionVariables.get(variableName).type != CollectionType.sensorVariable)
                {
                    throw new TQLException("Cannot assign SQL to non-Sensor variable "+variableName+". Line: "+scanner.currentLine);
                }

                // assign this query to the sensor variable
                ((SensorCollectionVariable)tqlQuery.collectionVariables.get(variableName)).query = query;

                // parse the select statement and put it in the query of the sensor variable
                selectStatement(query);

            }
            else if(token == Token.sensorToObsToken)
            {
                // eat the keyword
                eatToken(false);

                if(token == Token.openparenToken)
                {
                    // eat "("
                    eatToken(false);

                    if(token == Token.identToken)
                    {
                        // the scanner.identifier is the sensor variable to be assigned to the observation
                        if(tqlQuery.collectionVariables.containsKey(scanner.tokenString))
                        {
                            // first check the variable is of type ObservationCollection
                            if(tqlQuery.collectionVariables.get(variableName).type != CollectionType.observationVariable)
                            {
                                throw new TQLException("Variable "+variableName+" is not of type ObservationCollection. Line: "+scanner.currentLine);
                            }

                            // then check the variable inside is of type SensorCollection
                            if(tqlQuery.collectionVariables.get(scanner.tokenString).type != CollectionType.sensorVariable)
                            {
                                throw new TQLException("Variable "+scanner.tokenString+" is not of type SensorCollection. Line: "+scanner.currentLine);
                            }

                            // print a warning if the sensor variable is not assigned
                            if(!tqlQuery.collectionVariables.get(scanner.tokenString).isAssigned)
                                System.out.println("WARNING: sensor variable "+scanner.tokenString+" is not assigned. Line: "+scanner.currentLine);

                            ((ObservationCollectionVariable)tqlQuery.collectionVariables.get(variableName)).sensorVariable = (SensorCollectionVariable) tqlQuery.collectionVariables.get(scanner.tokenString);
                        }
                        else
                        {
                            throw new TQLException("Sensor variable "+scanner.tokenString+" is not defined. Line: "+scanner.currentLine);
                        }

                        // eat the ident.
                        eatToken(false);

                        if(token == Token.closeparenToken)
                        {
                            // eat the ")"
                            eatToken(false);
                        }
                        else
                        {
                            throw new TQLException("Missing \")\" for sensor_to_observations at "+variableName+". Line: "+scanner.currentLine);
                        }
                    }
                    else
                    {
                        // error
                        throw new TQLException("Error with sensor variable "+scanner.tokenString+" assigned to observation "+variableName+". Line: "+scanner.currentLine);
                    }
                }
                else
                {
                    // error
                    throw new TQLException("Syntax error for sensor_to_observations at variable "+variableName+". Line: "+scanner.currentLine);
                }
            }
            else
            {
                // error
                throw new TQLException("Unexpected expression for variable assignment for "+variableName+". Line: "+scanner.currentLine);
            }
        }
        else
        {
            // error
            throw new TQLException("Missing \"=\" after variable "+variableName+". Line: "+scanner.currentLine);
        }

        // by now, assignment should have been successful
        tqlQuery.collectionVariables.get(variableName).isAssigned = true;
    }

    public void selectStatement(SQLQuery query) throws TQLException
    {
        // eat "select"
        eatToken(false);

        // select
        select(query);

        // from
        if(token == Token.fromToken)
        {
            // eat "from"
            eatToken(false);

            from(query);
        }
        else
        {
            // error
            throw new TQLException("Missing \"FROM\" statement. Line: "+scanner.currentLine);
        }

        // where
        if(token == Token.whereToken)
        {
            // eat "where"
            eatToken(true);

            where(query);
        }

        // group by
        if(token == Token.groupbyToken)
        {
            // eat "group by"
            eatToken(true);

            groupby(query);
        }

        // having
        if(token == Token.havingToken)
        {
            // eat "having"
            eatToken(true);

            having(query);
        }
    }

    public void select(SQLQuery query) throws TQLException
    {
        // TODO: aggregate functions
        String attributesString = "";

        if(token == Token.fromToken)
            throw new TQLException("Expecting attributes in \"SELECT\". Line: "+scanner.currentLine);

        // you have to hit "FROM"
        while(token != Token.fromToken)
        {
            if(token == Token.endOfFileToken || token == Token.errorToken)
                throw new TQLException("Syntax error in \"SELECT\". Line: "+scanner.currentLine);

            attributesString += scanner.tokenString;
            eatToken(false);
        }

        query.attributesList.add(attributesString);
        // expecting attributes
        /*if(token == Token.timesToken)
        {
            query.attributesList.add("*");

            // eat "*"
            eatToken(false);
        }
        else
        {
            if(token == Token.identToken)
            {
                // TODO: check for duplicate attributes?
                query.attributesList.add(scanner.tokenString);

                // eat the ident.
                eatToken(false);

                while(token == Token.commaToken)
                {
                    // TODO: check for duplicate attributes?
                    query.attributesList.add(scanner.tokenString);

                    // eat the ident.
                    eatToken(false);
                }
            }
            else
            {
                // error
                throw new TQLException("Unexpected attributes in \"SELECT\". Line: "+scanner.currentLine);
            }
        }*/

    }

    public void from(SQLQuery query) throws TQLException
    {
        if(token == Token.identToken)
        {
            addTableToQuery(query);

            while(token == Token.commaToken)
            {
                // eat ","
                eatToken(false);

                if(token == Token.identToken)
                {
                    // do some logic
                    addTableToQuery(query);
                }
                else
                {
                    // error
                    throw new TQLException("Unexpected identifier in \"FROM\". Line: "+scanner.currentLine);
                }
            }
        }
        else
        {
            // error
            throw new TQLException("Unexpected collections in \"FROM\". Line: "+scanner.currentLine);
        }
    }

    public void where(SQLQuery query) throws TQLException
    {
        // TODO: fix this for keywords after where clause
        while(token != Token.semiToken && !scanner.isKeyword(scanner.tokenString))
        {
            if(token == Token.endOfFileToken || token == Token.errorToken)
                throw new TQLException("Syntax error at \"WHERE\". Line: "+scanner.currentLine);

            query.where += scanner.tokenString;
            eatToken(true);
        }

        // check if query.where is empty
        if(query.where.trim().isEmpty())
        {
            throw new TQLException("Syntax error at \"WHERE\". Line: "+scanner.currentLine);
        }
    }

    public void groupby(SQLQuery query) throws TQLException
    {
        // TODO: fix this for keywords after groupby clause
        while(token != Token.semiToken && !scanner.isKeyword(scanner.tokenString))
        {
            if(token == Token.endOfFileToken || token == Token.errorToken)
                throw new TQLException("Syntax error at \"GROUP BY\". Line: "+scanner.currentLine);

            query.groupby += scanner.tokenString;
            eatToken(true);
        }

        // check if query.where is empty
        if(query.groupby.trim().isEmpty())
        {
            throw new TQLException("Syntax error at \"GROUP BY\". Line: "+scanner.currentLine);
        }
    }

    public void having(SQLQuery query) throws TQLException
    {
        // TODO: fix this for keywords after having clause
        while(token != Token.semiToken && !scanner.isKeyword(scanner.tokenString))
        {
            if(token == Token.endOfFileToken || token == Token.errorToken)
                throw new TQLException("Syntax error at \"HAVING\". Line: "+scanner.currentLine);

            query.having += scanner.tokenString;
            eatToken(true);
        }

        // check if query.where is empty
        if(query.having.trim().isEmpty())
        {
            throw new TQLException("Syntax error at \"HAVING\". Line: "+scanner.currentLine);
        }
    }

    public void addCollectionVariable(String variableName, Token variableType) throws TQLException
    {
        if(variableType == Token.sensorToken)
        {
            if(!tqlQuery.collectionVariables.containsKey(variableName))
            {
                tqlQuery.collectionVariables.put(variableName, new SensorCollectionVariable(variableName));
            }
            else
                throw new TQLException("Sensor variable "+variableName+" is already defined. Line: "+scanner.currentLine);
        }
        else
        {
            if(!tqlQuery.collectionVariables.containsKey(variableName))
            {
                tqlQuery.collectionVariables.put(variableName, new ObservationCollectionVariable(variableName));
            }
            else
                throw new TQLException("Observation variable "+variableName+" is already defined. Line: "+scanner.currentLine);
        }

    }

    public void addTableToQuery(SQLQuery query) throws TQLException
    {
        // do some logic
        String collectionName = scanner.tokenString;
        String aliasName = "";

        // eat the collection name
        eatToken(false);

        if(token == Token.asToken)
        {
            // eat "AS"
            eatToken(false);
        }

        // expecting an alias
        if(token == Token.identToken)
        {
            aliasName = scanner.tokenString;
            eatToken(false);
        }
        else
            throw new TQLException("Missing alias name for collection "+collectionName+". Line: "+scanner.currentLine);

        // TODO: check if the alias is already used
        if(query.aliasIsUsed(aliasName))
        {
            throw new TQLException("Alias "+aliasName+" is already used. Line: "+scanner.currentLine);
        }

        // TODO: should you check if the alias is already used somewhere outside this query?

        // collection in the "from" clause should be either collection variable or system-defined table
        if(tqlQuery.collectionVariables.containsKey(collectionName))
        {
            // TODO: give warning if a variable is not assigned
            Collection collection;
            CollectionVariable collectionVariable = tqlQuery.collectionVariables.get(collectionName);

            if(!collectionVariable.isAssigned)
                System.out.println("WARNING: collection variable "+collectionName+" is not assigned. Line: "+scanner.currentLine);

            collection = collectionVariable.createCollection();
            collection.alias = aliasName;
            query.fromCollections.add(collection);
        }
        else
        {
            // get the type of the collection
            CollectionType collectionType = CollectionTypeMapping.getTypeOf(collectionName);

            if(collectionType == CollectionType.noType)
                throw new TQLException("Could not recognize collection "+collectionName+". Line: "+scanner.currentLine);

            query.fromCollections.add(new Collection(collectionName, aliasName, collectionType));
        }
    }
}
