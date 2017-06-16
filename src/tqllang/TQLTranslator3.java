package tqllang;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by Yas.
 */

public class TQLTranslator3
{
    public LinkedHashMap<String,String> translatedQueries;

    public TQLTranslator3()
    {
        translatedQueries = new LinkedHashMap<>();
    }

    public String translate(TQLQuery tqlQuery) throws TQLException
    {
        System.out.println("Translating...");

        // translate each collection variables
        for(CollectionVariable collectionVariable : tqlQuery.collectionVariables.values())
        {
            if(collectionVariable.isAssigned)
                translatedQueries.put(collectionVariable.name, translateCollection(collectionVariable));
        }

        String finalSQL = translateSQL(tqlQuery.finalQuery);

        finalSQL += ";";

        return finalSQL;
    }

    public String translateCollection(CollectionVariable collectionVariable) throws TQLException
    {
        // sensor variable
        if(collectionVariable.type == CollectionType.sensorVariable)
        {
            return translateSQL(((SensorCollectionVariable)collectionVariable).query);
        }
        // observation variable
        else
        {
            ObservationCollectionVariable observationVariable = (ObservationCollectionVariable) collectionVariable;

            // get the SQL of the sensor. It must have been already translated and put in the dictionary
            // TODO: do you need to check if not there?
            if(!translatedQueries.containsKey(observationVariable.sensorVariable.name))
                throw new TQLException("Sensor variable "+observationVariable.sensorVariable.name+" is probably not assigned/defined.");

            String sensorSQL = translatedQueries.get(observationVariable.sensorVariable.name);

            // form the SQL of the observation
            String observationQuery = "SELECT "+observationVariable.name+".* ";

            observationQuery += "\nFROM Observation AS "+observationVariable.name+ "\n\tINNER JOIN ( ";
            observationQuery += sensorSQL + " ) AS "+observationVariable.sensorVariable.name;
            observationQuery += " ON ("+observationVariable.name+".sen_id = "+observationVariable.sensorVariable.name+".sen_id)";

            return observationQuery;
        }
    }

    public String translateSQL(SQLQuery sqlQuery) throws TQLException
    {
        // Map entry for each collection in the "from" that contains the join tables to be done
        HashMap<String,JoinInfo> collectionsJoinMap = new HashMap<>();

        String translatedQuery = "SELECT ";

        // parse the "WHERE" clause first. this is to determine the joins needed in the "FROM"
        String whereCondition = translateWhere(sqlQuery, collectionsJoinMap);

        for(int i = 0; i < sqlQuery.attributesList.size(); i++)
        {
            // if the last attribute, don't put a comma
            if(i == sqlQuery.attributesList.size()-1)
            {
                translatedQuery += sqlQuery.attributesList.get(i);
            }
            else
            {
                translatedQuery += sqlQuery.attributesList.get(i)+",";
            }
        }

        translatedQuery += "\nFROM ";

        int i = 0;
        for(Collection collection : sqlQuery.fromCollections)
        {
            if(collection.type == CollectionType.sensorVariable || collection.type == CollectionType.observationVariable)
            {
                // sensor and observation variables must have already been translated
                // TODO: do you need to check if not??
                if(!translatedQueries.containsKey(collection.name))
                    throw new TQLException("Collection "+collection.name+" is probably not assigned/defined.");

                translatedQuery += "(\n\t"+translatedQueries.get(collection.name)+"\n\t)";
            }
            else
            {
                translatedQuery += MySQLTableMapping.getMySQLNameForType(collection.type);
            }

            // TODO: parser should enforce the alias use in the TQL query
            translatedQuery += " AS "+ collection.alias;

            // check for joins
            if(collectionsJoinMap.containsKey(collection.alias))
            {
                translatedQuery += produceJoins(collectionsJoinMap.get(collection.alias));
            }

            if(i < sqlQuery.fromCollections.size()-1)
                translatedQuery += " , ";

            i++;
        }

        // "WHERE" clause
        whereCondition = whereCondition.trim();
        if(!whereCondition.isEmpty())
            translatedQuery += "\nWHERE "+whereCondition;

        // "GROUP BY"
        if(sqlQuery.groupby != null)
        {
            sqlQuery.groupby = sqlQuery.groupby.trim();
            if(!sqlQuery.groupby.isEmpty())
            {
                translatedQuery += "\nGROUP BY "+sqlQuery.groupby;
            }
        }


        // "HAVING"
        if(sqlQuery.having != null)
        {
            sqlQuery.having = sqlQuery.having.trim();
            if(!sqlQuery.having.isEmpty())
            {
                translatedQuery += "\nHAVING "+sqlQuery.groupby;
            }
        }

        return translatedQuery;
    }

    public String translateWhere(SQLQuery query, HashMap<String, JoinInfo> collectionsJoinMap) throws TQLException
    {
        if(query.where != null && !query.where.trim().isEmpty())
        {
            String modifiedWhere = "";

            WhereScanner whereScanner = new WhereScanner(query.where);
            Token token = whereScanner.getToken();

            while(token != Token.endOfFileToken)
            {
                if(token == Token.errorToken)
                    throw new TQLException("Error reading the \"WHERE\" clause");

                // if token is identifier, then figure out the join (if you actually need a join)
                if(token == Token.identToken)
                {
                    modifiedWhere += figureOutJoins(query, collectionsJoinMap, whereScanner.tokenString);
                }
                else
                {
                    modifiedWhere += whereScanner.tokenString;
                }

                token = whereScanner.getToken();
            }

            return modifiedWhere;
        }
        else
        {
            return "";
        }
    }

    public String figureOutJoins(SQLQuery query, HashMap<String, JoinInfo> collectionsJoinMap, String attribute) throws TQLException
    {
        // there must always be at least two things ___.___
        String[] array = attribute.split("\\.");

        // first one must always be alias
        CollectionType firstCollectionType = CollectionType.noType;

        // find it in the from list and identify its type
        for(Collection collection : query.fromCollections)
        {
            if(collection.alias.equals(array[0]))
            {
                firstCollectionType = collection.type;
                break;
            }
        }

        // TODO: write something useful
        if(firstCollectionType == CollectionType.noType)
            throw new TQLException("SOME ERROR IN WHERE CLAUSE!!!");

        String firstCollectionName = CollectionTypeMapping.getNameOf(firstCollectionType);
        String firstCollectionAlias = array[0];

        // create join information for the collection if not yet created
        if(!collectionsJoinMap.containsKey(firstCollectionAlias))
        {
            collectionsJoinMap.put(firstCollectionAlias,new JoinInfo(firstCollectionName,firstCollectionAlias));
        }

        String qualifiedName = "";
        JoinInfo currentJoinInfo = collectionsJoinMap.get(firstCollectionAlias);
        JoinInfo temp;

        for(int i = 0; i < array.length-1; i++)
        {
            temp = currentJoinInfo.createJoinInfo(array[i+1]);

            if(temp.relationshipType == RelationshipType.attribute)
            {
                if(i+1 < array.length-1)
                    throw new TQLException("Attribute \""+temp.TQLTableName+"\" can't have...");

                qualifiedName = temp.alias;
            }
            else if(temp.relationshipType == RelationshipType.json)
            {
                // TODO: check syntax for json
                qualifiedName = temp.alias+"->\"$";

                // TODO: write something useful, i.e. json condition is missing the path
                if(i+1 == array.length-1)
                    throw new TQLException("Error in json condition");

                // get the path
                i = i+2;

                while(i < array.length)
                {
                    qualifiedName += "."+array[i];
                    i++;
                }

                qualifiedName += "\"";
            }
            else if(temp.relationshipType == RelationshipType.join && i+1 == array.length-1)
            {
                // TODO: dunno about this case. It's weird to have the last attribute as collection
                // TODO: the assumption is the id of the last collection to be used
                Relationship r = Relationship.getRelationship(currentJoinInfo.TQLTableType,array[i+1]);
                qualifiedName += temp.alias+"."+r.joinInformation.get(r.joinInformation.size()-1).column;
            }

            currentJoinInfo = temp;
            //temp = null;
        }

        return qualifiedName;
    }

    public String produceJoins(JoinInfo joinInfo)
    {
        String joins = "";

        if(joinInfo.children.size() == 0)
        {
            return "";
        }
        else
        {
            for(JoinInfo child : joinInfo.children)
            {
                for(Join join : child.joins)
                {
                    joins += "\nINNER JOIN "+join.tableName+" AS "+join.alias+" ON "+join.condition;
                }

                joins += produceJoins(child);
            }
        }

        return joins;
    }

}
