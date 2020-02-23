import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Random;

public class Database {
    // Database credentials
    static final String userName="root";
    static final String password="se375123";
    static final String databaseName="";
    static final String url="jdbc:mysql://localhost:3306/"+databaseName+"?useUnicode=true&useLegacyDatetimeCode=false&serverTimezone=Turkey";
    static Connection connection = null;




    public static boolean login(String playerName, String playerPassword) throws SQLException,ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection(url,userName,password);

        String sqlQueryForName = String.format("SELECT * from collect_four.players WHERE player_name='%s'",playerName);
        String sqlQueryForPassword = String.format("SELECT player_password from collect_four.players WHERE player_name='%s'",playerName);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlQueryForName);
        ResultSet resultSet = preparedStatement.executeQuery();

        PreparedStatement preparedStatement1 = connection.prepareStatement(sqlQueryForPassword);
        ResultSet resultSet1 = preparedStatement1.executeQuery();
        resultSet1.next();

        if (resultSet.next() == true) {
            System.out.println(playerName + " found in database");
            String sqlQueryForSalt = String.format("SELECT player_salt from collect_four.players WHERE player_name='%s'",playerName);
            PreparedStatement preparedStatement2 = connection.prepareStatement(sqlQueryForSalt);
            ResultSet resultSet2 = preparedStatement2.executeQuery();
            resultSet2.next();
            String salt = resultSet2.getString("player_salt");

            try {
                if (hashPassword(playerPassword,salt).equals(resultSet1.getString(1))) {
                    System.out.println("Password found for " + playerName + ", welcome");
                    return true;
                } else {
                    return false;
                }
            } catch (NoSuchAlgorithmException nsae) {
                nsae.printStackTrace();
            }

        } else {
            System.out.println(playerName + " does not exist.");
            return false;
        }
        return false;
    }


    public static boolean register(String playerName, String playerPassword) throws SQLException,ClassNotFoundException{
        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection(url,userName,password);

        String sqlQuery = String.format("SELECT * FROM collect_four.players WHERE player_name='%s'",playerName);
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery(sqlQuery);
        String salt = generateSalt();

        if (result.next() == false){
            try {
                String storedPassword = hashPassword(playerPassword,salt);
                sqlQuery = String.format("INSERT INTO collect_four.players (player_name,player_salt,player_password) VALUES (\"%s\",\"%s\",\"%s\")",playerName,salt,storedPassword);
                statement.executeUpdate(sqlQuery);
            }   catch (NoSuchAlgorithmException nsae) {
                nsae.printStackTrace();
            }


            return true;
        } else {
            System.out.println("We have " + playerName + " already.");
            return false;
        }
        //return false;
    }

    //Hash function will be used in login() and register().
    public static String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] mergedPassword = new byte[password.getBytes().length + salt.getBytes().length];
        System.arraycopy(password.getBytes(),0, mergedPassword,0, password.getBytes().length);
        System.arraycopy(salt.getBytes(),0,mergedPassword,password.getBytes().length,salt.getBytes().length);
        messageDigest.update(mergedPassword);
        mergedPassword = messageDigest.digest();
        StringBuffer stringBuffer = new StringBuffer();
        for (byte currentByte : mergedPassword) {
            stringBuffer.append(Integer.toHexString(currentByte & 0xff));
        }
        return stringBuffer.toString();
    }

    // Generates a random string consists of 6 characters.
    public static String generateSalt(){
        Random random = new Random();
        String salt = "";
        for (int i=0; i<=5; i++){
            char c = (char) (random.nextInt(25)+'a');
            salt+=c;
        }
        return salt;
    }
    public static void DatabaseSetup(){
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(url,userName,password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}