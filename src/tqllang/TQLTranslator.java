package tqllang;

import javax.management.relation.Relation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by Yas.
 */
public class TQLTranslator
{
    public LinkedHashMap<String,String> translatedQueries;

    public TQLTranslator()
    {
        translatedQueries = new LinkedHashMap<>();
    }

    public String translate(TQLQuery tqlQuery) throws TQLException
    {
        // translate each collection variables
        for(CollectionVariable collectionVariable : tqlQuery.collectionVariables.values())
        {
            translatedQueries.put(collectionVariable.name, translateCollection(collectionVariable));
        }

        String finalSQL = translateSQL(tqlQuery.finalQuery);

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
            String sensorSQL = translatedQueries.get(observationVariable.sensorVariable.name);

            // form the SQL of the observation
            String observationQuery = "SELECT "+observationVariable.name+".* ";

            observationQuery += "FROM Observation AS "+observationVariable.name+ " JOIN ( ";
            observationQuery += sensorSQL + " ) AS "+observationVariable.sensorVariable.name;
            observationQuery += " ON ("+observationVariable.name+".sen_id = "+observationVariable.sensorVariable.name+".sen_id)";

            return observationQuery;
        }
    }

    public String translateSQL(SQLQuery sqlQuery) throws TQLException
    {
        // Map entry for each collection in the "from" that contains the join tables to be done
        HashMap<String,JoinTables> collectionsJoinMap = new HashMap<>();

        String translatedQuery = "SELECT ";

        // parse the "where" clause first.
        // this method will add tables to the "fromCollection" if necessary
        String whereCondition = translateWhere(sqlQuery, collectionsJoinMap);

        //TODO: attributes
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


        translatedQuery += " FROM ";

        // "fromCollection" might have been modified by "translateWhere" method
        // (the tables required for the join to solve the "." issue
        // expanding the tables in the "from"
        int i = 0;
        for(Collection collection : sqlQuery.fromCollections)
        {
            if(collection.type == CollectionType.sensorVariable || collection.type == CollectionType.observationVariable)
            {
                // sensor and observation variables must have already been translated
                // TODO: do you need to check if not??
                translatedQuery += "( "+translatedQueries.get(collection.name)+" )";
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
                for(JoinTable joinTable : collectionsJoinMap.get(collection.alias).joinTables)
                {
                    translatedQuery += " INNER JOIN "+joinTable.table+" AS "+joinTable.alias+" USING ("+joinTable.column+")";
                }
            }


            if(i < sqlQuery.fromCollections.size()-1)
                translatedQuery += " , ";

            i++;
        }

        // "where" clause
        if(!whereCondition.isEmpty())
            translatedQuery += " WHERE "+whereCondition;

        return translatedQuery;
    }

    public String translateWhere(SQLQuery query, HashMap<String, JoinTables> collectionsJoinMap) throws TQLException
    {
        if(query.where != null && !query.where.isEmpty())
        {
            String modifiedWhere = "";

            WhereScanner whereScanner = new WhereScanner(query.where);
            Token token = whereScanner.getToken();

            while(token != Token.endOfFileToken)
            {
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

    public String figureOutJoins(SQLQuery query, HashMap<String, JoinTables> collectionsJoinMap, String attribute) throws TQLException
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

        // TODO: should you check if the table is not mentioned in the from clause and add it?

        Relationship relationship;
        String qualifiedName = array[0];

        for(int i = 0; i < array.length-1; i++)
        {
            // the first one and last one need to be dealt with differently
            if(i == 0)
            {
                relationship = getRelationship(firstCollectionName,array[1]);
            }
            else
            {
                relationship = getRelationship(array[i],array[i+1]);
            }

            if(relationship.type == RelationshipType.join)
            {
                // TODO:
                if(!collectionsJoinMap.containsKey(firstCollectionAlias))
                    collectionsJoinMap.put(firstCollectionAlias,new JoinTables());

                for(JoinTable jT : relationship.joinInformation)
                {
                    qualifiedName += "_"+jT.table;
                    collectionsJoinMap.get(firstCollectionAlias).addJoinTable(jT.table,qualifiedName,jT.column);
                }
            }
            else if(relationship.type == RelationshipType.attribute)
            {
                qualifiedName += "."+array[i+1];
            }
            else if(relationship.type == RelationshipType.json)
            {
                // TODO: check syntax for json
            }

            // change the attribute name to its type for next iteration
            array[i+1] = relationship.fieldType;

        }

        return qualifiedName;
    }

    public Relationship getRelationship(String s1, String s2)
    {
        Relationship relationship = new Relationship();

        // TODO: ifs
        if(s1.equals("Sensor") || s1.equals("SensorCollection"))
        {
            if(s2.equals("Location"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Location","","loc_id"));
            }
            else if(s2.equals("Type"))
            {
                relationship.fieldType = "SensorType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("SensorType","","sen_type_id"));
            }
            else if(s2.equals("Platform"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Platform","","pltfm_id"));
            }
            else if(s2.equals("User"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Users","","user_id"));
            }
            else if(s2.equals("CoverageRooms"))
            {
                relationship.fieldType = "Infrastructure";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Sen_Infr","","sen_id"));
                relationship.joinInformation.add(new JoinTable("Infrastructure","","infr_id"));
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equals("Observation") || s1.equals("ObservationCollection"))
        {
            if(s2.equals("Sensor"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Sensor","","sen_id"));
            }
            else if(s2.equalsIgnoreCase("Type"))
            {
                relationship.fieldType = "ObservationType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("ObservationType","","obs_type_id"));
            }
            else if(s2.equals("payload"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.json;
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equalsIgnoreCase("Group"))
        {
            relationship.fieldType = s2;
            relationship.type = RelationshipType.attribute;
        }
        else if(s1.equalsIgnoreCase("User"))
        {
            if(s2.equalsIgnoreCase("Groups"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Grp_User","","user_id"));
                relationship.joinInformation.add(new JoinTable("Groups","","group_id"));
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equalsIgnoreCase("Location"))
        {
            relationship.fieldType = s2;
            relationship.type = RelationshipType.attribute;
        }
        else if(s1.equalsIgnoreCase("Region"))
        {
            if(s2.equalsIgnoreCase("geometry"))
            {
                relationship.fieldType = "Location";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Reg_Loc","","reg_id"));
                relationship.joinInformation.add(new JoinTable("Location","","loc_id"));
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equalsIgnoreCase("InfrastructureType"))
        {
            relationship.fieldType = s2;
            relationship.type = RelationshipType.attribute;
        }
        else if(s1.equalsIgnoreCase("Infrastructure"))
        {
            if(s2.equalsIgnoreCase("type"))
            {
                relationship.fieldType = "InfrastructureType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("InfrastructureType","","infr_type_id"));
            }
            else if(s2.equalsIgnoreCase("Region"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Region","","reg_id"));
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equalsIgnoreCase("PlatformType"))
        {
            relationship.fieldType = s2;
            relationship.type = RelationshipType.attribute;
        }
        else if(s1.equalsIgnoreCase("Platform"))
        {
            if(s2.equalsIgnoreCase("type"))
            {
                relationship.fieldType = "PlatformType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("PlatformType","","pltfm_type_id"));
            }
            else if(s2.equalsIgnoreCase("Location"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Location","","loc_id"));
            }
            else if(s2.equalsIgnoreCase("owner"))
            {
                relationship.fieldType = "User";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Users","","user_id"));
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equalsIgnoreCase("SensorType"))
        {
            if(s2.equalsIgnoreCase("payloadSchema"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.json;
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }

        }
        else if(s1.equalsIgnoreCase("ObservationType"))
        {
            if(s2.equalsIgnoreCase("payloadSchema"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.json;
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equalsIgnoreCase("SemanticObservationType"))
        {
            if(s2.equalsIgnoreCase("payloadSchema"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.json;
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equalsIgnoreCase("VirtualSensorType"))
        {
            if(s2.equalsIgnoreCase("observationType"))
            {
                relationship.fieldType = "ObservationType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("ObservationType","","obs_type_id"));
            }
            else if(s2.equalsIgnoreCase("semanticObservationType"))
            {
                relationship.fieldType = "SemanticObservationType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("SemanticObservationType","","so_type_id"));
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equalsIgnoreCase("VirtualSensor"))
        {
            if(s2.equalsIgnoreCase("type"))
            {
                relationship.fieldType = "VirtualSensorType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("VirtualSensorType","","vs_type_id"));
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equalsIgnoreCase("SemanticObservation"))
        {
            if(s2.equalsIgnoreCase("virtualsensor"))
            {
                relationship.fieldType = "VirtualSensor";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("VirtualSensor","","vs_id"));
            }
            else if(s2.equalsIgnoreCase("type"))
            {
                relationship.fieldType = "SemanticObservationType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("SemanticObservationType","","so_type_id"));
            }
            else if(s2.equalsIgnoreCase("payload"))
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.json;
            }
            else
            {
                relationship.fieldType = s2;
                relationship.type = RelationshipType.attribute;
            }
        }

        return relationship;
    }

}
