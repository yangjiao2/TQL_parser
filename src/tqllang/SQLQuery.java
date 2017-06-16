package tqllang;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yas.
 */

public class SQLQuery
{
    public String command;

    public List<Collection> fromCollections;

    // TODO: no parsing is done for these clauses yet.
    // TODO: Translator should do one more parsing step. Currently, the translator parses "where" for a simplified "where" clause.
    public List<String> attributesList;         // TODO: better to model as an object, i.e. parse it
    public String where;                        // TODO: better to model as an object, i.e. parse it
    public String groupby;                      // TODO: better to model as an object, i.e. parse it
    public String having;                       // TODO: better to model as an object, i.e. parse it

    public SQLQuery()
    {
        command = "";
        fromCollections = new ArrayList<>();
        attributesList = new ArrayList<>();
        where = "";
        groupby = "";
        having = "";
    }

    public boolean aliasIsUsed(String alias)
    {
        for(Collection collection : fromCollections)
        {
            if(collection.alias.equals(alias))
                return true;
        }

        return false;
    }
}


