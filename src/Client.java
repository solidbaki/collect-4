import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Scanner;

public class Client implements Runnable {

    // The client socket
    private static Socket clientSocket = null;
    // The output stream
    private static PrintStream os = null;
    // The input stream
    private static DataInputStream is = null;
    private static  DataOutputStream dOut=null;

    private static BufferedReader inputLine = null;
    private static boolean closed = false;

    KeyFactory kf;
    static Cipher cipher;
    static byte[] plain_text;
    static PublicKey publicKey;
    static byte[] encrypted_text;


    public static void main(String[] args) {
        Scanner scan=new Scanner(System.in);
        System.out.println("Please enter the ip of the server");
        String host = scan.nextLine();
        // The default port.
        int portNumber = 2222;
        // The default host.

        try {
            cipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        if (args.length < 2) {
            System.out
                    .println("Usage: java MultiThreadChatClient <host> <portNumber>\n"
                            + "Now using host=" + host + ", portNumber=" + portNumber);
        } else {
            host = args[0];
            portNumber = Integer.valueOf(args[1]).intValue();
        }

        /*
         * Open a socket on a given host and port. Open input and output streams.
         */
        try {
            clientSocket = new Socket(host, portNumber);
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            os = new PrintStream(clientSocket.getOutputStream());
            is = new DataInputStream(clientSocket.getInputStream());
            dOut = new DataOutputStream(clientSocket.getOutputStream());


        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + host);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to the host "
                    + host);
        }

        /*
         * If everything has been initialized then we want to write some data to the
         * socket we have opened a connection to on the port portNumber.
         */
        if (clientSocket != null && os != null && is != null) {
            try {

                /* Create a thread to read from the server. */
                new Thread(new Client()).start();
                while (!closed) {
                    byte[] tempEncryptedMsg=EncryptMsg(inputLine.readLine().trim());

                    dOut.writeInt(tempEncryptedMsg.length);
                    dOut.write(tempEncryptedMsg);
                }
                /*
                 * Close the output stream, close the input stream, close the socket.
                 */
                os.close();
                is.close();
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("IOException:  " + e);
            }
        }
    }

    /*
     * Create a thread to read from the server.

     */
    public void run() {
        /*
         * Keep on reading from the socket till we receive "Bye" from the
         * server. Once we received that then we want to break.
         */

        boolean asd = true;
        String responseLine;
        try {

            while(asd)
            {
                int length = is.readInt();                    // read length of incoming message
                if(length>0) {
                    byte[] message = new byte[length];
                    is.readFully(message, 0, message.length); // read the message


                    kf = KeyFactory.getInstance("RSA");
                    X509EncodedKeySpec spec2 = new X509EncodedKeySpec(message);
                    publicKey = kf.generatePublic(spec2);





                    asd=false;
                }
            }

            while ((responseLine = is.readLine()) != null) {



                System.out.println(responseLine);
                if (responseLine.indexOf("*** Bye") != -1)
                    break;
            }
            closed = true;
        } catch (IOException e) {
            System.err.println("IOException:  " + e);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static byte[] EncryptMsg(String msg){
        try {
            plain_text = msg.getBytes("UTF-8");

            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            // STEP 4. Perform the operation
            encrypted_text = cipher.doFinal(plain_text);

            String msgBase64 = Base64.getEncoder().encodeToString(encrypted_text);


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return encrypted_text;


    }
    public static String printBytes(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X:", b));
        }
        return sb.toString();
    }
}