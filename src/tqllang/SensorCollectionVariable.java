package tqllang;

/**
 * Yas
 */

public class SensorCollectionVariable extends CollectionVariable
{
    public SQLQuery query;

    public SensorCollectionVariable(String name)
    {
        super(name, CollectionType.sensorVariable);
    }

    public Collection createCollection()
    {
        SensorCollectionVariable collection = new SensorCollectionVariable(this.name);
        collection.query = this.query;
        collection.isAssigned = this.isAssigned;

        return collection;
    }
}
