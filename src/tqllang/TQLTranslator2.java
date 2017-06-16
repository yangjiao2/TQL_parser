package tqllang;

import javax.management.relation.Relation;
import javax.xml.stream.Location;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by john on 2017/5/25.
 */
public class TQLTranslator2 {
    HashMap<String,String> TempMap;
    public LinkedHashMap<String,String> translatedQueries;

    public TQLTranslator2()
    {
        translatedQueries = new LinkedHashMap<>();
        TempMap=new HashMap<>();
        TempMap.put("User","Users");
        TempMap.put("Group","Groups");
    }

    public String translate(TQLQuery tqlQuery) throws TQLException
    {
        // translate each collection variables
        for(CollectionVariable collectionVariable : tqlQuery.collectionVariables.values())
        {
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
            String sensorSQL = translatedQueries.get(observationVariable.sensorVariable.name);

            // form the SQL of the observation
            String observationQuery = "SELECT "+observationVariable.name+".* ";

            observationQuery += "FROM Observation AS "+observationVariable.name+ " ,( ";
            observationQuery += sensorSQL + " ) AS "+observationVariable.sensorVariable.name;
            observationQuery += " where "+observationVariable.name+".sen_id = "+observationVariable.sensorVariable.name+".sen_id";

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
            HashSet<String> addedCollections =new  HashSet<String>();

            // check for joins
           for(String collections: collectionsJoinMap.keySet())// if(collectionsJoinMap.containsKey(collection.alias))
            {
                for(JoinTable joinTable : collectionsJoinMap.get(collections).joinTables)
                {
                    if(!addedCollections.contains(joinTable.table)){
                        translatedQuery += ","+joinTable.table;
                        addedCollections.add(joinTable.table);
                    }
                    whereCondition += " and "+collections+"."+joinTable.column+"="+joinTable.table+"."+joinTable.column;
                }
            }

            if(i < sqlQuery.fromCollections.size()-1)
                translatedQuery += " , ";

            i++;
        }

        // "where" clause
        if(!whereCondition.isEmpty())
            translatedQuery += " WHERE "+whereCondition;
        //"Group" clause
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
    //    String firstCollectionAlias = array[0];

        // TODO: should you check if the table is not mentioned in the from clause and add it?

        Relationship relationship;
        String qualifiedName="";
        //  TODO: reverse the loop
        for(int i = 0; i < array.length-1; i++)
        {
            // the first one and last one need to be dealt with differently
            if(i == 0)
            {
                relationship = Relationship.getRelationship(firstCollectionName,array[1]);
            }
          else
         {
                relationship = Relationship.getRelationship(array[i],array[i+1]);
         }
            qualifiedName = array[i];
            if(TempMap.containsKey(qualifiedName)){
                qualifiedName=TempMap.get(qualifiedName);
            }
            if(relationship.type == RelationshipType.join)
            {
                // TODO:
                if(i+1==array.length-1){
                    throw new TQLException("No attributes found in where");
                }
                if(!collectionsJoinMap.containsKey(array[i])){
                        collectionsJoinMap.put(qualifiedName,new JoinTables());//array[i]
                }

                for(int j=0;j<relationship.joinInformation.size();j++)
                {
                    JoinTable jT =relationship.joinInformation.get(j);
                    qualifiedName += "_"+jT.table;
                    if(j<1){
                        String key=array[i];
                        if(TempMap.containsKey(key)){
                            key=TempMap.get(key);
                        }
                        collectionsJoinMap.get(key).addJoinTable(jT.table,qualifiedName,jT.column);
                    }else{
                         JoinTable last_jT=relationship.joinInformation.get(j-1);
                         collectionsJoinMap.put(last_jT.table,new JoinTables());
                         collectionsJoinMap.get(last_jT.table).addJoinTable(jT.table,qualifiedName,jT.column);
                    }

                    //firstCollectionAlias
                }
            }
            else if(relationship.type == RelationshipType.attribute)
            {
                qualifiedName += "."+array[i+1];
            }
            else if(relationship.type == RelationshipType.json)
            {
                // TODO: check syntax for json

                qualifiedName += "."+array[i+1]+"->\"$";

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
                break;
            }

            // change the attribute name to its type for next iteration
            array[i+1] = relationship.fieldType;

        }

        return qualifiedName;
    }

 /*   public Relationship getRelationship(String s1, String s2)
    {
        Relationship relationship = new Relationship();
        relationship.fieldType = s2;

        // TODO: ifs
        if(s1.equals("Sensor") || s1.equals("SensorCollection"))
        {
            if(s2.equals("location"))
            {
                relationship.fieldType = "Location";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Location","","loc_id"));
            }
            else if(s2.equals("type"))
            {
                relationship.fieldType = "SensorType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("SensorType","","sen_type_id"));
            }
            else if(s2.equals("platform"))
            {
                relationship.fieldType = "Platform";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Platform","","pltfm_id"));
            }
            else if(s2.equals("user"))
            {
                relationship.fieldType = "User";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Users","","user_id"));
            }
            else if(s2.equals("coverageRooms"))
            {
                relationship.fieldType = "Infrastructure";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Sen_Infr","","sen_id"));
                relationship.joinInformation.add(new JoinTable("Infrastructure","","infr_id"));
            }
            else if(!s2.equals("id") && !s2.equals("description") && !s2.equals("name") && !s2.equals("mac") && !s2.equals("IP") && !s2.equals("port"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equals("Observation") || s1.equals("ObservationCollection"))
        {
            if(s2.equals("sensor"))
            {
                relationship.fieldType = "Sensor";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Sensor","","sen_id"));
            }
            else if(s2.equals("type"))
            {
                relationship.fieldType = "ObservationType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("ObservationType","","obs_type_id"));
            }
            else if(s2.equals("payload"))
            {
                relationship.type = RelationshipType.json;
            }
            else if(!s2.equals("id") && !s2.equals("timestamp"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equals("Group"))
        {
            if(!s2.equals("id") && !s2.equals("description") && !s2.equals("name"))
            {
                throwUnrecognizedException(s2);
            }

            relationship.type = RelationshipType.attribute;
        }
        else if(s1.equals("User"))
        {
            if(s2.equals("groups"))
            {
                relationship.fieldType = "Group";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Grp_User","","user_id"));
                relationship.joinInformation.add(new JoinTable("Groups","","group_id"));
            }
            else if(!s2.equals("email") && !s2.equals("name"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equals("Location"))
        {
            if(!s2.equals("x") && !s2.equals("y") && !s2.equals("z"))
            {
                throwUnrecognizedException(s2);
            }

            relationship.type = RelationshipType.attribute;
        }
        else if(s1.equals("Region"))
        {
            if(s2.equals("geometry"))
            {
                relationship.fieldType = "Location";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Reg_Loc","","reg_id"));
                relationship.joinInformation.add(new JoinTable("Location","","loc_id"));
            }
            else if(!s2.equals("id") && !s2.equals("name") && !s2.equals("floor"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equals("InfrastructureType"))
        {
            if(!s2.equals("id") && !s2.equals("description") && !s2.equals("name"))
            {
                throwUnrecognizedException(s2);
            }

            relationship.type = RelationshipType.attribute;
        }
        else if(s1.equals("Infrastructure"))
        {
            if(s2.equals("type"))
            {
                relationship.fieldType = "InfrastructureType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("InfrastructureType","","infr_type_id"));
            }
            else if(s2.equals("region"))
            {
                relationship.fieldType = "Region";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Region","","reg_id"));
            }
            else if(!s2.equals("id") && !s2.equals("name"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equals("PlatformType"))
        {
            if(!s2.equals("id") && !s2.equals("name") && !s2.equals("description"))
            {
                throwUnrecognizedException(s2);
            }

            relationship.type = RelationshipType.attribute;
        }
        else if(s1.equals("Platform"))
        {
            if(s2.equals("type"))
            {
                relationship.fieldType = "PlatformType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("PlatformType","","pltfm_type_id"));
            }
            else if(s2.equals("location"))
            {
                relationship.fieldType = "Location";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Location","","loc_id"));
            }
            else if(s2.equals("owner"))
            {
                relationship.fieldType = "User";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("Users","","user_id"));
            }
            else if(!s2.equals("id") && !s2.equals("name") && !s2.equals("description"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equals("SensorType"))
        {
            if(s2.equals("payloadSchema"))
            {
                relationship.type = RelationshipType.json;
            }
            else if(!s2.equals("id") && !s2.equals("name") && !s2.equals("description") && !s2.equals("mobility"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }

        }
        else if(s1.equals("ObservationType"))
        {
            if(s2.equalsIgnoreCase("payloadSchema"))
            {
                relationship.type = RelationshipType.json;
            }
            else if(!s2.equals("id") && !s2.equals("name") && !s2.equals("description"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equals("SemanticObservationType"))
        {
            if(s2.equals("payloadSchema"))
            {
                relationship.type = RelationshipType.json;
            }
            else if(!s2.equals("id") && !s2.equals("name") && !s2.equals("description"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equals("VirtualSensorType"))
        {
            if(s2.equals("observationType"))
            {
                relationship.fieldType = "ObservationType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("ObservationType","","obs_type_id"));
            }
            else if(s2.equals("semanticObservationType"))
            {
                relationship.fieldType = "SemanticObservationType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("SemanticObservationType","","so_type_id"));
            }
            else if(!s2.equals("id") && !s2.equals("name") && !s2.equals("description"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equals("VirtualSensor"))
        {
            if(s2.equals("type"))
            {
                relationship.fieldType = "VirtualSensorType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("VirtualSensorType","","vs_type_id"));
            }
            else if(!s2.equals("id") && !s2.equals("name") && !s2.equals("description") && !s2.equals("sourceFileLocation") && !s2.equals("compiledCodeLocation") && !s2.equals("language") && !s2.equals("projectName"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }
        }
        else if(s1.equals("SemanticObservation"))
        {
            if(s2.equals("virtualsensor"))
            {
                relationship.fieldType = "VirtualSensor";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("VirtualSensor","","vs_id"));
            }
            else if(s2.equals("type"))
            {
                relationship.fieldType = "SemanticObservationType";
                relationship.type = RelationshipType.join;
                relationship.joinInformation.add(new JoinTable("SemanticObservationType","","so_type_id"));
            }
            else if(s2.equals("payload"))
            {
                relationship.type = RelationshipType.json;
            }
            else if(!s2.equals("id") && !s2.equals("timestamp"))
            {
                throwUnrecognizedException(s2);
            }
            else
            {
                relationship.type = RelationshipType.attribute;
            }
        }
        else
        {
            throwUnrecognizedException(s1);
        }

        return relationship;
    }

        return relationship;
    }*/

}
