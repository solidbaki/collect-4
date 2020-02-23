import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.util.Base64;
public class AsymmetricCryptography {

    public static String printBytes(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X:", b));
        }
        return sb.toString();
    }

    String msg = "This is SE375 SYSTEM PROGRAMMING.";
    byte[] plain_text;
    byte[] encrypted_text;
    byte[] decrypted_text;
    Cipher cipher;
    KeyFactory kf;
    PublicKey publicKey;
    PrivateKey privateKey;



    public void CreatePrivateKey(){


        try {
            byte[] keyBytes = Files.readAllBytes(new File("private.key").toPath());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            privateKey = kf.generatePrivate(spec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public byte[] CreatePublicKey(){
        byte[] keyBytes2=null;
        try {
            keyBytes2 = Files.readAllBytes(new File("public.key").toPath());
            X509EncodedKeySpec spec2 = new X509EncodedKeySpec(keyBytes2);
            publicKey = kf.generatePublic(spec2);

        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return keyBytes2;


    }

    public void EncryptMsg(String msg){
        try {
            plain_text = msg.getBytes("UTF-8");

            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            // STEP 4. Perform the operation
            encrypted_text = cipher.doFinal(plain_text);
            System.out.println("Encrypted data:" + printBytes(encrypted_text));
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


    }

    public String DecryptMsg(byte [] encrypted_text){

        try {
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            decrypted_text = cipher.doFinal(encrypted_text);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }



        return new String(decrypted_text);
    }
    public void keyGen(){
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(1024);
            KeyPair pair = keyGen.generateKeyPair();

            PublicKey publicKey = pair.getPublic();
            PrivateKey privateKey = pair.getPrivate();


            FileOutputStream fos;
// Store public key
            byte[] key = publicKey.getEncoded();
            fos = new FileOutputStream(new File("public.key"));
            fos.write(key);

            fos.flush(); fos.close();
// Store private key
            fos = new FileOutputStream(new File("private.key"));
            byte[] key2 = privateKey.getEncoded();
            fos.write(key2);
            fos.flush(); fos.close();

            kf = KeyFactory.getInstance("RSA");
            cipher = Cipher.getInstance("RSA");

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

    }
}