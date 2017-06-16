package tqllang;

/**
 * Created Yas
 */

public class JoinTable
{
    public String table;
    public String alias;
    public String column;

    public JoinTable(String table, String alias, String column)
    {
        this.table = table;
        this.alias = alias;
        this.column = column;
    }
}
