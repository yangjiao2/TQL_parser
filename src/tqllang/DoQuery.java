package tqllang;
import java.sql.*;
/**
 * Created by john on 2017/5/22.
 */
public class DoQuery {
    public ResultSet res;
    public Connection connect;
    public DoQuery(){
        res=null;
    }
    public String DNS = "ec2-59-39-237-173.us-west-2.compute.amazonaws.com";

    public void Connect() {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connect = DriverManager.getConnection("jdbc:mysql://172.31.24.9:3306/tqldb?autoReconnect=true&useSSL=false", "groupuser", "223pass");
            System.out.println(connect);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        if (connect != null) {
            System.out.println("SUCCESS!!!! You made it, take control     your database now!");
        } else {
            System.out.println("FAILURE! Failed to make connection!");
        }

    }

    public void Execute(String q){
        try {
            PreparedStatement ps = connect.prepareStatement(q);
            res = ps.executeQuery();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    public void getResult(){
        try{
            if(res!=null){
                while(res.next()){
                    System.out.println("obs_id: "+ res.getString(1));
                    System.out.println("sen_id: "+res.getString(2));
                    System.out.println("timestamp: "+res.getString(3));
                    //  System.out.println("payload: "+res.getObject(4));
                    System.out.println("type: "+res.getString(5));
                }
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

}
