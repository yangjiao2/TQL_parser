package tqllang;

/**
 * Yas
 */

public class Collection
{
    public String name;
    public String alias;
    public CollectionType type;

    public Collection(String name, CollectionType type)
    {
        this(name,"",type);
    }

    public Collection(String name, String alias, CollectionType type)
    {
        this.name = name;
        this.alias = alias;
        this.type = type;
    }

}
