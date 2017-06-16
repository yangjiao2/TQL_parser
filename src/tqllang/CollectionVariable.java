package tqllang;

/**
 * Created Yas
 */

public class CollectionVariable extends Collection
{
    public boolean isAssigned;

    public CollectionVariable(String name, CollectionType type)
    {
        super(name, type);
    }

    public Collection createCollection()
    {
        CollectionVariable collection = new CollectionVariable(this.name,this.type);
        collection.isAssigned = this.isAssigned;

        return collection;
    }
}
