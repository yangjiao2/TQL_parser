package tqllang;

import java.util.ArrayList;

/**
 * Created by Yas
 */

public class JoinInfo
{
    public String TQLTableName;
    public String TQLTableType;
    public String alias;
    public RelationshipType relationshipType;
    public ArrayList<JoinInfo> children;
    public ArrayList<Join> joins;

    public JoinInfo(String TQLTableType, String alias, RelationshipType type)
    {
        this.TQLTableName = "";
        this.TQLTableType = TQLTableType;
        this.alias = alias;
        this.relationshipType = type;
        children = new ArrayList<>();
        joins = new ArrayList<>();
    }

    public JoinInfo()
    {
        this("",  "", RelationshipType.noRelationship);
    }

    public JoinInfo(String TQLTableType, String alias)
    {
        this(TQLTableType, alias, RelationshipType.noRelationship);
    }

    public JoinInfo createJoinInfo(String TQLAttribute) throws TQLException
    {
        JoinInfo joinInfo = new JoinInfo();

        // get the relationship
        Relationship relationship = Relationship.getRelationship(this.TQLTableType,TQLAttribute);
        joinInfo.relationshipType = relationship.type;

        // check if the join already exists
        if(relationship.type == RelationshipType.join)
        {
            for(JoinInfo child : children)
            {
                if(child.TQLTableName.equals(TQLAttribute))
                {
                    return child;
                }
            }

            // add join info to children if not existing
            joinInfo.TQLTableName = TQLAttribute;
            joinInfo.TQLTableType = relationship.fieldType;
            Join join;

            String tempAlias1 = this.alias;

            // TODO: this is for many to many relationship
            for(JoinTable joinTable : relationship.joinInformation)
            {
                join = new Join();
                join.tableName = joinTable.table;
                join.alias = tempAlias1+"_"+join.tableName;
                join.condition = tempAlias1+"."+joinTable.column+" = "+join.alias+"."+joinTable.column;

                joinInfo.joins.add(join);

                tempAlias1 = join.alias;
            }

            joinInfo.alias = joinInfo.joins.get(joinInfo.joins.size()-1).alias;
            /*
            String rTable = relationship.joinInformation.get(0).table;
            String rColumn = relationship.joinInformation.get(0).column;

            joinInfo.tableName = rTable;
            joinInfo.alias = this.alias+"_"+TQLAttribute;
            joinInfo.condition = this.alias+"."+rColumn+" = "+joinInfo.alias+"."+rColumn;

            // TODO: this is for many to many relationship
            if(relationship.joinInformation.size() > 1)
            {
                JoinInfo jInfo = new JoinInfo();

                rTable = relationship.joinInformation.get(1).table;
                rColumn = relationship.joinInformation.get(1).column;

                jInfo.TQLTableName = "";
                jInfo.tableName = rTable;
                jInfo.alias = joinInfo.alias+"_"+TQLAttribute;
                jInfo.condition = joinInfo.alias+"."+rColumn+" = "+jInfo.alias+"."+rColumn;
            }*/

            this.children.add(joinInfo);

        }
        else
        {
            joinInfo.alias = this.alias+"."+relationship.fieldType;
        }

        return joinInfo;

    }
}
