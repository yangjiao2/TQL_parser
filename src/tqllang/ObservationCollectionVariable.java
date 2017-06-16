package tqllang;

/**
 * Yas
 */

public class ObservationCollectionVariable extends CollectionVariable
{
    public SensorCollectionVariable sensorVariable;

    public ObservationCollectionVariable(String name)
    {
        super(name, CollectionType.observationVariable);
    }

    public Collection createCollection()
    {
        ObservationCollectionVariable collection = new ObservationCollectionVariable(this.name);
        collection.sensorVariable = this.sensorVariable;
        collection.isAssigned = this.isAssigned;

        return collection;
    }
}
