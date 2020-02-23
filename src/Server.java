import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.net.ServerSocket;


/*
 * A chat server that delivers public and private messages.
 */
public class Server {

    // The server socket.
    private static ServerSocket serverSocket = null;
    // The client socket.
    private static Socket clientSocket = null;

    // This chat server can accept up to maxClientsCount clients' connections.
    private static final int maxClientsCount = 100;
    private static final clientThread[] threads = new clientThread[maxClientsCount];
    public static ArrayList<GameRoom> GameRoomList = new ArrayList<GameRoom>();
    public static ArrayList<clientThread> JoinRoomList = new ArrayList<clientThread>();
    public static byte[] publicKeyByte;
    static AsymmetricCryptography cryptography;


    public static void main(String args[]) {

        // The default port number.
        int portNumber = 2222;

        System.out.println("Usage: java MultiThreadChatServer <portNumber>\n"+ "Now using port number=" + portNumber);


        /*
         * Open a server socket on the portNumber (default 2222). Note that we can
         * not choose a port less than 1023 if we are not privileged users (root).
         */
        try {
            serverSocket = new ServerSocket(portNumber);
            cryptography= new AsymmetricCryptography();
            Database database=new Database();
            database.DatabaseSetup();
            cryptography.keyGen();
            cryptography.CreatePrivateKey();
            publicKeyByte=cryptography.CreatePublicKey();


        } catch (IOException e) {
            System.out.println(e);
        }

        /*
         * Create a client socket for each connection and pass it to a new client
         * thread.
         */
        while (true) {


            try {

                if(GameRoomList.size()!=0){
                    System.out.println("bingo "+GameRoomList.get(0).getBingoCount()+" clientsize "+ GameRoomList.get(0).getGameRoomClients().size()+" leader "+ GameRoomList.get(0).getLeaderboardList().size()+" isstarted "+GameRoomList.get(0).isStarted()
                            +" bot "+ GameRoomList.get(0).getBotCount());



                }

                clientSocket = serverSocket.accept();
                int i = 0;
                for (i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == null) {
                        (threads[i] = new clientThread(clientSocket, threads)).start();

                        break;
                    }
                }
                if (i == maxClientsCount) {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public static void UpdateRoomList()
    {
        for (int k = 0 ; k<Server.JoinRoomList.size();k++){
            Server.JoinRoomList.get(k).os.println("Game Rooms Updated#######");
            for(int l = 0 ; l < Server.GameRoomList.size() ; l++)
            {
                Server.JoinRoomList.get(k).os.println(l+1 +". " + Server.GameRoomList.get(l).getRoomName() + ((Server.GameRoomList.get(l).isPublic())?" (+)":" (-)") + ((Server.GameRoomList.get(l).isStarted())?" (The game already started)":""));

            }
        }
    }
    public static clientThread[] getThreads() {
        return threads;
    }

}

/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, ask the client's name, informs all the
 * clients connected to the server about the fact that a new client has joined
 * the chat room, and as long as it receive data, echos that data back to all
 * other clients. When a client leaves the chat room this thread informs also
 * all the clients about that and terminates.
 */
class clientThread extends Thread {

    private boolean InRoom=false;
    private GameRoom currentGameRoom = null;
    private boolean actionPerformed=false;
    private int selectedCard=-1;
    private int point=0;
    private boolean isSaidBingo=false;
    private boolean isBot=false;


    private ArrayList<Integer> ownedCards = new ArrayList<Integer>();
    private DataInputStream is = null;
    protected PrintStream os = null;
    DataOutputStream dOut = null;
    private Socket clientSocket = null;
    private final clientThread[] threads;
    private int maxClientsCount;
    private String name;

    public clientThread(Socket clientSocket, clientThread[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.length;
    }
    public clientThread(ArrayList<Integer> OwnedCards, boolean isBot ,clientThread[] threads) {
        setOwnedCards(OwnedCards);
        setBot(isBot);
        this.threads = threads;
    }

    public boolean isBot() {
        return isBot;
    }

    public void setBot(boolean isBot) {
        this.isBot = isBot;
    }

    public boolean isSaidBingo() {
        return isSaidBingo;
    }

    public void setSaidBingo(boolean isSaidBingo) {
        this.isSaidBingo = isSaidBingo;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public int getSelectedCard() {
        return selectedCard;
    }

    public void setSelectedCard(int selectedCard) {
        this.selectedCard = selectedCard;
    }

    public boolean isActionPerformed() {
        return actionPerformed;
    }

    public void setActionPerformed(boolean actionPerformed) {
        this.actionPerformed = actionPerformed;
    }

    public ArrayList<Integer> getOwnedCards() {
        return ownedCards;
    }

    public void setOwnedCards(ArrayList<Integer> ownedCards) {
        this.ownedCards = ownedCards;
    }

    public GameRoom getCurrentGameRoom() {
        return currentGameRoom;
    }

    public void setCurrentGameRoom(GameRoom currentGameRoom) {
        this.currentGameRoom = currentGameRoom;
    }

    public String getPlayerName() {
        return name;
    }
    public boolean isInRoom() {
        return InRoom;
    }

    public void setInRoom(boolean inRoom) {
        InRoom = inRoom;
    }



    public void run() {
        int maxClientsCount = this.maxClientsCount;
        clientThread[] threads = this.threads;



        try {
            /*
             * Create input and output streams for this client.
             */
            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());
            dOut = new DataOutputStream(clientSocket.getOutputStream());


            dOut.writeInt(Server.publicKeyByte.length); // write length of the message
            dOut.write(Server.publicKeyByte);           // write the message



            boolean login=true;
            boolean register=true;
            boolean logined=false;
            String userName="";
            while (!logined){

                os.println("Enter /login for login , /register for register");

                String line = DecryptProcess();
                if(line.startsWith("/login")){

                    os.println("Please enter your user name:");
                    login = true;

                    while (login){
                         userName=DecryptProcess();

                        if(userName!=null){
                            os.println("Please enter your password:");

                            while (login){

                                String passWord = DecryptProcess();

                                if(passWord!=null){
                                    //////
                                    if(Database.login(userName,passWord)&&!AlreadyJoin(userName)){
                                        os.println("Welcome "+ userName);
                                        login=false;
                                        logined=true;
                                    }
                                    else {
                                        os.println("Invalid username or password");
                                        login=false;

                                    }
                                }

                            }

                        }




                    }

                }
                else  if(line.startsWith("/register")){

                    os.println("Please enter your user name:");
                    register = true;

                    while (register){
                         userName=DecryptProcess();

                        if(userName!=null){
                            os.println("Please enter your password:");

                            while (register){

                                String passWord = DecryptProcess();

                                if(passWord!=null){
                                    //////
                                    if(Database.register(userName,passWord)){
                                        os.println("You successfully registered");
                                        register=false;

                                    }
                                    else {
                                        os.println("This username already exist");
                                        register=false;

                                    }

                                }

                            }

                        }




                    }

                }





            }

            name=userName;


            os.println(name
                    + ", you joined chat room.\nTo leave enter /quit in a new line , To create a game room command /create");
            for (int i = 0; i < maxClientsCount; i++) {
                if (threads[i] != null && threads[i] != this && !threads[i].isInRoom()) {

                    threads[i].os.println("*** A new user " + name
                            + " entered the chat room !!! ***");


                }
            }



            while (true) {
                boolean cont=true;
                String line = DecryptProcess();







                if (line.startsWith("/quit")) {
                    if(isInRoom()){
                        for(int i = 0 ; i<currentGameRoom.getLeaderboardList().size() ; i++ )
                        {
                            if(currentGameRoom.getLeaderboardList().get(i).equals(this)){
                                currentGameRoom.getLeaderboardList().remove(i);
                            }
                        }
                        for(int i = 0 ; i<currentGameRoom.getGameRoomClients().size() ; i++ )
                        {

                            if(currentGameRoom.getGameRoomClients().get(i).equals(this))
                            {
                                if(currentGameRoom.getAdmin().equals(this)&& currentGameRoom.getLeaderboardList().size() > 1){

                                    for(int j=0;j<currentGameRoom.getLeaderboardList().size();j++){

                                        if(!currentGameRoom.getLeaderboardList().get(j).equals(this)){
                                            currentGameRoom.setAdmin(currentGameRoom.getLeaderboardList().get(j));
                                            currentGameRoom.getLeaderboardList().get(j).os.println("You have become admin of the game room ");
                                            break;
                                        }

                                    }



                                }


                                if(currentGameRoom.isStarted()){

                                    clientThread tempClient =new clientThread(getOwnedCards(),true,Server.getThreads());
                                    for(int j=0;j<currentGameRoom.getGameRoomClients().size();j++){

                                        if(currentGameRoom.getGameRoomClients().get(j).equals(this)){

                                            currentGameRoom.getGameRoomClients().set(j, tempClient);
                                            currentGameRoom.setBotCount(currentGameRoom.getBotCount()+1);
                                        }

                                    }

                                }

                                else {
                                    currentGameRoom.getGameRoomClients().remove(i);
                                }


                                if(currentGameRoom.getLeaderboardList().size()==0)
                                {
                                    for(int j = 0 ; j<Server.GameRoomList.size();j++)
                                    {
                                        if(Server.GameRoomList.get(j).equals(currentGameRoom))
                                        {

                                            Server.GameRoomList.remove(j);


                                            Server.UpdateRoomList();
                                            break;

                                        }
                                    }
                                }


                            }
                            else
                            {
                                if(!currentGameRoom.getGameRoomClients().get(i).isBot)
                                {
                                    currentGameRoom.getGameRoomClients().get(i).os.println("*** The user " + name + " left the game room !!! ***");
                                }




                            }
                        }
                        for (int j = 0; j < maxClientsCount; j++) {
                            if (threads[j] != null && threads[j] != this && !threads[j].isInRoom()) {

                                threads[j].os.println("*** A new user " + name
                                        + " entered the chat room !!! ***");


                            }
                        }
                        os.println("You left the game room and entered the lobby");
                        setInRoom(false);
                        setCurrentGameRoom(null);
                        setPoint(0);
                    }
                    else
                    {
                        break;
                    }

                }


                else if (line.startsWith("/create")&&!isInRoom()) {
                    boolean isPublic=true;
                    os.println("Is the game room public or private? /+ for public , /- for private,/back for the return to lobby ");

                    while( cont)
                    {
                        String line2 = DecryptProcess();
                        if(line2.startsWith("/+"))
                        {
                            isPublic = true;
                            os.println("Enter the game room name : ");
                            break;
                        }
                        else if(line2.startsWith("/-"))
                        {
                            isPublic = false;
                            os.println("Enter the game room name : ");
                            break;
                        }
                        else if(line2.startsWith("/back")){
                            cont=false;
                        }
                        else if(line2 != null)
                        {
                            os.println("Invalid command");
                        }


                    }

                    while(cont)
                    {
                        String line2 = DecryptProcess();
                        if(line2.startsWith("/back")){
                            cont=false;
                            break;
                        }
                        else if(line2 != null)
                        {

                            if(isPublic)
                            {

                                Server.GameRoomList.add(new GameRoom(line2,isPublic,this));
                                for (int i = 0; i < maxClientsCount; i++) {
                                    if (threads[i] != null && threads[i] != this  && !threads[i].isInRoom()) {
                                        threads[i].os.println("*** The user " + name
                                                + " created a public game room: " +line2+ " ***");
                                    }
                                }
                                setCurrentGameRoom(Server.GameRoomList.get(Server.GameRoomList.size()-1));
                                Server.GameRoomList.get(Server.GameRoomList.size()-1).getGameRoomClients().add(this);
                                setInRoom(true);
                                os.println("Successfully created a game room.");
                                currentGameRoom.getLeaderboardList().add(this);
                                currentGameRoom.start();

                                break;
                            }
                            else
                            {
                                os.println("Enter the password of game room : ");
                                while(true)
                                {
                                    String line3 = DecryptProcess();
                                    if (line3.startsWith("/back")){
                                        cont=false;
                                        break;
                                    }
                                    else if(line3 != null)
                                    {
                                        Server.GameRoomList.add(new GameRoom(line2,line3,isPublic,this));
                                        for (int i = 0; i < maxClientsCount; i++) {
                                            if (threads[i] != null && threads[i] != this  && !threads[i].isInRoom()) {
                                                threads[i].os.println("*** The user " + name
                                                        + " created a private game room: " +line2+ " ***");
                                            }
                                        }
                                        setCurrentGameRoom(Server.GameRoomList.get(Server.GameRoomList.size()-1));
                                        Server.GameRoomList.get(Server.GameRoomList.size()-1).getGameRoomClients().add(this);
                                        setInRoom(true);
                                        os.println("Successfully created a game room.");
                                        currentGameRoom.getLeaderboardList().add(this);
                                        currentGameRoom.start();
                                        break;
                                    }
                                }
                                break;
                            }
                        }

                    }

                    if(!cont){
                        os.println("You returned to the lobby");
                    }
                    else
                    {
                        Server.UpdateRoomList();
                    }

                }
                else if(line.startsWith("/join")&&!isInRoom()){
                    Server.JoinRoomList.add(this);
                    for(int i = 0 ; i < Server.GameRoomList.size() ; i++)
                    {
                        os.println(i+1 +". " + Server.GameRoomList.get(i).getRoomName() + ((Server.GameRoomList.get(i).isPublic())?" (+)":" (-)") + ((Server.GameRoomList.get(i).isStarted())?" (The game already started)":""));
                    }
                    os.println("Enter the index of game room that you want to join: ");

                    while(cont)
                    {
                        String line2 = DecryptProcess();
                        if(line2.startsWith("/back")){
                            cont=false;
                            break;
                        }
                        else if(line2.chars().allMatch( Character::isDigit )&& !line2.isEmpty() && line2 != null && Integer.parseInt(line2) >= 1 && Integer.parseInt(line2) <= Server.GameRoomList.size())
                        {

                            if(!Server.GameRoomList.get(Integer.parseInt(line2)-1).isPublic())
                            {
                                if(Server.GameRoomList.get(Integer.parseInt(line2)-1).isStarted())
                                {
                                    os.println("The Game Room is started already... , Please choose another room to join");
                                }
                                else{
                                    os.println("Enter the password of game room: ");
                                    while(cont)
                                    {
                                        String line3 = DecryptProcess();
                                        if (line3.startsWith("/back")){
                                            cont=false;
                                            break;
                                        }
                                        else if(line3 != null && line3.equalsIgnoreCase(Server.GameRoomList.get(Integer.parseInt(line2)-1).getPassword()) )
                                        {
                                            os.println("You successfully entered the game room");
                                            setCurrentGameRoom(Server.GameRoomList.get(Integer.parseInt(line2)-1));
                                            Server.GameRoomList.get(Integer.parseInt(line2)-1).getGameRoomClients().add(this);
                                            setInRoom(true);
                                            currentGameRoom.getLeaderboardList().add(this);
                                            break;
                                        }
                                        else if(line3 != null)
                                        {
                                            os.println("You entered wrong password");
                                        }
                                    }
                                }

                            }
                            else {
                                if(Server.GameRoomList.get(Integer.parseInt(line2)-1).isStarted())
                                {
                                    os.println("The Game Room is started already... , Please choose another room to join");
                                }
                                else{
                                    os.println("You successfully entered the game room");
                                    setCurrentGameRoom(Server.GameRoomList.get(Integer.parseInt(line2)-1));
                                    Server.GameRoomList.get(Integer.parseInt(line2)-1).getGameRoomClients().add(this);
                                    setInRoom(true);
                                    currentGameRoom.getLeaderboardList().add(this);
                                }

                            }




                            if(cont&&!Server.GameRoomList.get(Integer.parseInt(line2)-1).isStarted()){
                                for(int i=0; i<Server.GameRoomList.get(Integer.parseInt(line2)-1).getGameRoomClients().size();i++){
                                    if (Server.GameRoomList.get(Integer.parseInt(line2)-1).getGameRoomClients().get(i) != null && Server.GameRoomList.get(Integer.parseInt(line2)-1).getGameRoomClients().get(i) != this) {
                                        Server.GameRoomList.get(Integer.parseInt(line2)-1).getGameRoomClients().get(i).os.println("*** The user " + name + " entered the game room !!! ***");
                                    }
                                }

                                os.println("Player List of the Game Room " + Server.GameRoomList.get(Integer.parseInt(line2)-1).getRoomName());
                                for(int i=0; i<Server.GameRoomList.get(Integer.parseInt(line2)-1).getGameRoomClients().size();i++){
                                    os.println(i+1 +". " +Server.GameRoomList.get(Integer.parseInt(line2)-1).getGameRoomClients().get(i).getPlayerName());
                                }
                                break;
                            }




                        }
                        else
                        {
                            os.println("Invalid command");
                        }
                    }
                    if(!cont){
                        os.println("You returned to the lobby");
                    }

                    for (int i = 0 ; i<Server.JoinRoomList.size();i++){
                        if(Server.JoinRoomList.get(i).equals(this)){
                            Server.JoinRoomList.remove(i);
                        }
                    }

                }

                else if(line.startsWith("/start")&&isInRoom())
                {

                    if(currentGameRoom.getAdmin()==this&&!currentGameRoom.isStarted()&&currentGameRoom.getGameRoomClients().size()>=4)
                    {

                        currentGameRoom.AddCardstoDeck();
                        currentGameRoom.ShuffleCards();
                        currentGameRoom.setStarted(true);
                        Server.UpdateRoomList();


                    }

                    else {

                        os.println("There are not enough players in game room to start. At least 4 players needed to start. ");

                    }
                }

                else if(line.startsWith("/showcards")&&isInRoom()&&currentGameRoom.isStarted())
                {
                    getOwnedCards().forEach(card -> os.println(card));

                }
                //Game Inputs
                else if(isInRoom()&&currentGameRoom.isStarted()){

                    if(line.startsWith("bingo")){
                        //first bingo
                        if(!currentGameRoom.isBingo()){

                            for(int i=0;i<ownedCards.size();i++){
                                int temp =ownedCards.get(i);
                                int tempCount=0;
                                for(int j=0;j<ownedCards.size();j++){
                                    if(temp==ownedCards.get(j)){
                                        tempCount++;
                                    }
                                }
                                if(tempCount==ownedCards.size()){
                                    currentGameRoom.setBingo(true);
                                    setPoint(getPoint() + currentGameRoom.getGameRoomClients().size());
                                    setSaidBingo(true);
                                    currentGameRoom.setWinner(this);
                                    currentGameRoom.setBingoCount(1);
                                    os.println("BINGO!!! You get " + currentGameRoom.getGameRoomClients().size() + " points.");
                                    for(int k = 0 ; k < currentGameRoom.getGameRoomClients().size();k++)
                                    {
                                        if(currentGameRoom.getGameRoomClients().get(k) != this && !currentGameRoom.getGameRoomClients().get(k).isBot)
                                        {
                                            currentGameRoom.getGameRoomClients().get(k).os.println(getPlayerName() + " said BINGO!!!!");
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        //other players bingo
                        else if(currentGameRoom.isBingo()){
                            setSaidBingo(true);

                            setPoint(getPoint() + currentGameRoom.getGameRoomClients().size()-currentGameRoom.getBingoCount());
                            os.println("BINGO!!! You get " + (currentGameRoom.getGameRoomClients().size()-currentGameRoom.getBingoCount()) + " points.");
                            currentGameRoom.setBingoCount(currentGameRoom.getBingoCount()+1);

                        }
                    }

                    else if(line.chars().allMatch( Character::isDigit )&& line!= null && !line.isEmpty() &&Integer.parseInt(line)>0 &&Integer.parseInt(line)<=currentGameRoom.getGameRoomClients().size()){

                        for(int i=0;i<getOwnedCards().size();i++){

                            if(Integer.parseInt(line)==getOwnedCards().get(i)){

                                setSelectedCard(Integer.parseInt(line));
                                setActionPerformed(true);
                                os.println("You Selected "+ line);

                                break;
                            }

                        }
                        if(getSelectedCard()== -1){
                            os.println("Invalid Command");
                        }

                    }

                    else{
                        os.println("Invalid Command");
                    }


                }








                if(!isInRoom()){
                    for (int i = 0; i < maxClientsCount; i++) {
                        if (threads[i] != null && !"/create".equals(line) && !"/join".equals(line) && !"/quit".equals(line) && !threads[i].isInRoom()) {
                            threads[i].os.println("<" + name  +">" + line);
                        }
                    }
                }

            }
            for (int i = 0; i < maxClientsCount; i++) {
                if (threads[i] != null && threads[i] != this  && !threads[i].isInRoom()) {
                    threads[i].os.println("*** The user " + name
                            + " is leaving the chat room !!! ***");
                }
            }
            os.println("*** Bye " + name + " ***");

            /*
             * Clean up. Set the current thread variable to null so that a new client
             * could be accepted by the server.
             *
             */



            for (int i = 0; i < maxClientsCount; i++) {
                if (threads[i] == this) {
                    threads[i] = null;
                }
            }

            /*
             * Close the output stream, close the input stream, close the socket.
             */
            is.close();
            os.close();
            clientSocket.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean AlreadyJoin(String name){
        for(int i =0;i<maxClientsCount;i++){

            if(threads[i]!=null&&threads[i].getPlayerName()!=null&&threads[i].getPlayerName().equals(name)){
                return true;
            }

        }
        return false;
    }

    public void RandomCardSelector(){
        Random rnd =new Random();
        int temp = rnd.nextInt(getOwnedCards().size());
        selectedCard=getOwnedCards().get(temp);
    }

    public String DecryptProcess(){
        String line=null;
        try {

            int length = is.readInt();                    // read length of incoming message
            if(length>0) {
                byte[] message = new byte[length];
                is.readFully(message, 0, message.length); // read the message

                line = Server.cryptography.DecryptMsg(message);


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }




}

class GameRoom extends Thread
{
    private String name;
    private String password;
    private boolean isPublic;
    private boolean isBingo=false;
    private int bingoCount=0;
    private clientThread winner;
    private boolean isStarted = false;
    private clientThread Admin;
    private ArrayList<clientThread> GameRoomClients = new ArrayList<clientThread>();
    private ArrayList<clientThread> LeaderboardList = new ArrayList<clientThread>();
    private ArrayList<Integer> GameRoomCards = new ArrayList<Integer>();
    private int botCount=0;


    public int getBotCount() {
        return botCount;
    }

    public void setBotCount(int botCount) {
        this.botCount = botCount;
    }

    public ArrayList<clientThread> getLeaderboardList() {
        return LeaderboardList;
    }

    public void setLeaderboardList(ArrayList<clientThread> leaderboardList) {
        LeaderboardList = leaderboardList;
    }

    public clientThread getWinner() {
        return winner;
    }

    public void setWinner(clientThread winner) {
        this.winner = winner;
    }

    public int getBingoCount() {
        return bingoCount;
    }

    public void setBingoCount(int bingoCount) {
        this.bingoCount = bingoCount;
    }

    public boolean isBingo() {
        return isBingo;
    }

    public void setBingo(boolean isBingo) {
        this.isBingo = isBingo;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void setStarted(boolean isStarted) {
        this.isStarted = isStarted;
    }

    public clientThread getAdmin() {
        return Admin;
    }

    public void setAdmin(clientThread admin) {
        Admin = admin;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String getPassword() {
        return password;
    }

    public String getRoomName() {
        return name;
    }

    public ArrayList<clientThread> getGameRoomClients() {
        return GameRoomClients;
    }

    public void setGameRoomClients(ArrayList<clientThread> gameRoomClients) {
        GameRoomClients = gameRoomClients;
    }

    public GameRoom(String name , boolean isPublic,clientThread Admin)
    {
        this.name = name;
        this.isPublic = isPublic;
        this.Admin=Admin;
    }

    public GameRoom(String name , String password , boolean isPublic,clientThread Admin)
    {
        this.name = name;
        this.password = password;
        this.isPublic = isPublic;
        this.Admin=Admin;
    }

    public void AddCardstoDeck ()
    {
        GameRoomCards.clear();
        for(int i = 1 ; i<=GameRoomClients.size();i++)
        {

            for(int j = 1 ; j<=4;j++)
            {
                GameRoomCards.add(j);
            }
        }
    }

    public void ShuffleCards ()
    {
        Random rand = new Random();


        for(int i = 0 ; i<GameRoomClients.size();i++)
        {
            GameRoomClients.get(i).getOwnedCards().clear();
            ArrayList<Integer> PlayerCards = new ArrayList<Integer>();
            for(int j = 0 ; j<4;j++)
            {
                int temp = rand.nextInt(GameRoomCards.size());
                PlayerCards.add(GameRoomCards.get(temp));
                GameRoomCards.remove(temp);

            }
            GameRoomClients.get(i).setOwnedCards(PlayerCards);
        }

    }

    public void run()
    {
        //Game Room Created
        while(true)
        {

            if(getGameRoomClients().size()==10)
            {

                AddCardstoDeck();
                ShuffleCards();
                setStarted(true);
                Server.UpdateRoomList();
            }
            System.out.print("");
            if(isStarted)
            {

                for(int i=0;i<getGameRoomClients().size();i++){

                    if(!getGameRoomClients().get(i).isBot()){
                        getGameRoomClients().get(i).os.println("The game started ");
                    }
                }

                ///Game Begin
                while(isStarted)
                {


                    for(int i =0;i<getGameRoomClients().size();i++){
                        int k=i;
                        if(!getGameRoomClients().get(i).isBot()){

                            getGameRoomClients().get(i).os.println("Current Cards: ");
                            getGameRoomClients().get(i).getOwnedCards().forEach(card -> getGameRoomClients().get(k).os.println(card));
                            getGameRoomClients().get(i).os.println("Select A Card From Your Deck To Send Next Player: ");
                        }

                    }
                    ///Round Begin
                    while(isStarted)
                    {

                        int actionCount=0;
                        for(int i =0;i<getGameRoomClients().size();i++){
                            if(getGameRoomClients().get(i).isActionPerformed()){
                                actionCount++;
                            }
                        }
                        if(actionCount==getGameRoomClients().size()-getBotCount()){

                            for(int i=0;i<getGameRoomClients().size();i++){

                                if(getGameRoomClients().get(i).isBot()){
                                    getGameRoomClients().get(i).RandomCardSelector();
                                }
                            }

                            RoundProcess();
                            ResetRound();

                            for(int i=0;i<getGameRoomClients().size();i++){

                                if(!getGameRoomClients().get(i).isBot()){
                                    getGameRoomClients().get(i).os.println("End of Round: ");
                                }
                            }

                            break;
                        }
                        System.out.print("");
                        if(bingoCount==getGameRoomClients().size()-getBotCount()){

                            isStarted=false;
                            break;
                        }
                    }


                }
                //End of Game

                for(int i=getGameRoomClients().size()-1;i>=0;i--){

                    if(getGameRoomClients().get(i).isBot()){
                        getGameRoomClients().remove(i);
                    }
                }

                setBotCount(0);

                for(int i =0;i<getGameRoomClients().size();i++){

                    if(!getGameRoomClients().get(i).isBot()){
                        getGameRoomClients().get(i).os.println("Winner: " + winner.getPlayerName());
                    }

                }
                isStarted=false;
                ResetRound();
                Sort();
                DisplayLeaderboard();
                Server.UpdateRoomList();


                try {
                    sleep(3000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }
    }

    void RoundProcess(){
        for(int i =0;i<GameRoomClients.size();i++){
            if(GameRoomClients.size()-1==i){
                GameRoomClients.get(0).getOwnedCards().add(GameRoomClients.get(i).getSelectedCard());
            }
            else{
                GameRoomClients.get(i+1).getOwnedCards().add(GameRoomClients.get(i).getSelectedCard());
            }
            ///Remove Selected Card From Deck
            for(int j =0;j<GameRoomClients.get(i).getOwnedCards().size();j++){
                if(GameRoomClients.get(i).getSelectedCard()==GameRoomClients.get(i).getOwnedCards().get(j)){
                    GameRoomClients.get(i).getOwnedCards().remove(j);
                    break;
                }
            }
        }
    }
    void ResetRound(){

        for(int i =0;i<GameRoomClients.size();i++){
            GameRoomClients.get(i).setActionPerformed(false);
            GameRoomClients.get(i).setSelectedCard(-1);
            GameRoomClients.get(i).setSaidBingo(false);

        }
        setBingoCount(0);
        setBingo(false);
        setWinner(null);

    }

    void Sort()
    {

        for (int i = 0 ; i < LeaderboardList.size() ; i++)
        {
            for (int j = 0 ; j < LeaderboardList.size()-1 ; j++)
            {
                if(LeaderboardList.get(j).getPoint() < LeaderboardList.get(j+1).getPoint())
                {
                    clientThread tempClient;
                    tempClient = LeaderboardList.get(j);
                    LeaderboardList.set(j, LeaderboardList.get(j+1));
                    LeaderboardList.set(j+1,tempClient);

                }
            }
        }
    }

    void DisplayLeaderboard()
    {

        for(int i = 0 ; i < getLeaderboardList().size() ; i++)
        {

            getLeaderboardList().get(i).os.println("Leaderboard List");
            for(int j = 0 ; j < getLeaderboardList().size() ; j++)
            {
                getLeaderboardList().get(i).os.println(j+1 + ". " + LeaderboardList.get(j).getPlayerName() + ": " + LeaderboardList.get(j).getPoint());
            }
        }
    }




}