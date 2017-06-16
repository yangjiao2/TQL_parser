package tqllang;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main
{

    public static void main(String[] args)
    {
        String query = "";
        DoQuery db;
        db = new DoQuery();
        db.Connect();
        System.out.println("db");
        System.out.println(db);
        db.Execute("show tables;");
        try
        {

            Scanner input = new Scanner(new FileInputStream("example3"));

            while(input.hasNextLine())
            {
                query += input.nextLine()+"\n";
            }

            TQLParser tqlParser = new TQLParser(query);
            //TQLTranslator translator = new TQLTranslator();
            TQLTranslator3 translator = new TQLTranslator3();

            try
            {
                tqlParser.parse();
                System.out.println(translator.translate(tqlParser.tqlQuery));
            }
            catch(TQLException e)
            {
                System.out.println(e.getMessage());
            }
        }
        catch (FileNotFoundException e)
        {
            System.out.println(e.getMessage());
        }
    }
}