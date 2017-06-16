package tqllang;

import java.util.LinkedHashMap;

/**
 * Yas
 */

public class TQLQuery
{
    public LinkedHashMap<String, CollectionVariable> collectionVariables;
    public SQLQuery finalQuery;

    public TQLQuery()
    {
        collectionVariables = new LinkedHashMap<>();
        finalQuery = new SQLQuery();
    }
}
