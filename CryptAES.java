//package net.0x6561.dhaesmsgr;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.util.*;

/*
 * This class is a wrapper class for constructing a 
 * AES Cipher
 */
public class CryptAES
{
  private static final boolean DBG = false;
  private String KEY_FILE = "aesKey256.dat";
  Cipher aesCipher;
  Key aesKey;
  SecretKeySpec skeySpec;

  /**
   * Class Contructor
   * This version to be used with a 
   * pre-shared key as a file on disk 
   *
   */
  public CryptAES()
  {
    try
    {
      FileInputStream in = new FileInputStream(KEY_FILE);
      int keySize = in.available();
      byte[] keyAsBytes = new byte[keySize];
      in.read(keyAsBytes);
      in.close();

      aesCipher = Cipher.getInstance("AES");
      skeySpec = new SecretKeySpec(keyAsBytes, "AES");
    }//close try
    catch(NoSuchAlgorithmException nsaex)
    {
      nsaex.printStackTrace();
    }
    catch(NoSuchPaddingException nspex)
    {
      nspex.printStackTrace();
    }
    catch(FileNotFoundException fnfex)
    {
      fnfex.printStackTrace();
    }
    catch(IOException ioex)
    {
      ioex.printStackTrace();
    }

  }//close Crypt

  /**
   * Class Contructor
   * This version requires a key as parameter, 
   * Uses output of Diffie-helman
   * @param byte[], Diffie Helman shared secret
   */
  public CryptAES(byte[] k)
  {
    try
    {
      aesCipher = Cipher.getInstance("AES");
      //SecretKeySpec(byte[] key, int offset, int len, String algorithm)
      //Constructs a secret key from the given 
      //byte array, using the first len bytes of key, 
      //starting at offset inclusive. + 
      skeySpec = new SecretKeySpec(k,0,16,"AES");
      byte[] skstr = skeySpec.getEncoded();
      String keySpecStr = Base64.getEncoder().encodeToString(skstr);
    }//close try
    catch(NoSuchAlgorithmException nsaex)
    {
      nsaex.printStackTrace();
    }
    catch(NoSuchPaddingException nspex)
    {
      nspex.printStackTrace();
    }
  }//close Crypt

  /*
   *Method to encrypt a String using AES
   @param plainText, String to be encrypted
   @return String, ciphertext 
  */
  public String crypt(String plainText)
  {
    String cipherTextAsStr = ""; 
    try
    {
      //setup for ENCRYPTION
      aesCipher.init(Cipher.ENCRYPT_MODE, skeySpec);
      byte[] plainTxtAsBytes = plainText.getBytes();
      byte[] cipherTxtAsBytes = aesCipher.doFinal(plainTxtAsBytes);
      // encode byte[] as Base64...

      cipherTextAsStr = Base64.getEncoder().encodeToString(cipherTxtAsBytes);
    }//close try
    catch(IllegalBlockSizeException ibsex)
    {
      ibsex.printStackTrace();
    }
    finally
    {
      return cipherTextAsStr;
    }//close finally
  }//close crypt

  /*
   *Method to encrypt a String using AES
   @param plainTxtAsBytes, byte[] to be encrypted
   @return String, ciphertext 
  */
  public String crypt(byte[] plainTxtAsBytes)
  {
    String cipherTextAsStr = ""; 
    try
    {
      //setup for ENCRYPTION
      aesCipher.init(Cipher.ENCRYPT_MODE, skeySpec);
      byte[] cipherTxtAsBytes = aesCipher.doFinal(plainTxtAsBytes);
      // encode byte[] as Base64...

      cipherTextAsStr = Base64.getEncoder().encodeToString(cipherTxtAsBytes);
    }//close try
    catch(IllegalBlockSizeException ibsex)
    {
      ibsex.printStackTrace();
    }
    finally
    {
      return cipherTextAsStr;
    }//close finally
  }//close crypt


  /*
   *Method to decrypt a String encrypted using AES
   @param cipherText, String to be encrypted
   @return String, plaintext 
  */
  public String decrypt(String cipherText)
  {
    String decryptedTxtStr = "";
    try
    {

      System.out.println("DECRYPTING = " + cipherText);

      //setup for DECRYPTION
      aesCipher.init(Cipher.DECRYPT_MODE, skeySpec);
      byte[] cipherTxtAsBytes = Base64.getDecoder().decode(cipherText);
      byte[] decipheredTxtBytes = aesCipher.doFinal(cipherTxtAsBytes);
      decryptedTxtStr = new String(decipheredTxtBytes, "UTF8");

      System.out.println("DECRYPTION DONE : decryptedTxtStr = "+ decryptedTxtStr);

    }//close try
    catch(InvalidKeyException ikex)
    {
      ikex.printStackTrace();
    }
    catch(IllegalBlockSizeException ibsex)
    {
      ibsex.printStackTrace();
    }
    catch(UnsupportedEncodingException ueex)
    {
      ueex.printStackTrace();
    }
    finally
    {
      return decryptedTxtStr;
    }//close finally
  }//close decrypt


  /*
   *Method to decrypt a String encrypted using AES
   @param cipherTxtAsBytes, byte[] to be encrypted
   @return String, plaintext 
  */
  public String decrypt(byte[] cipherTxtAsBytes)
  {
    String decryptedTxtStr = "";
    try
    {

      if(DBG){System.out.println("DECRYPTING = " + cipherTxtAsBytes.toString());}

      //setup for DECRYPTION
      aesCipher.init(Cipher.DECRYPT_MODE, skeySpec);
      byte[] decipheredTxtBytes = aesCipher.doFinal(cipherTxtAsBytes);
      decryptedTxtStr = new String(decipheredTxtBytes, "UTF8");

      if(DBG){System.out.println("DECRYPTION DONE : decryptedTxtStr = "+ decryptedTxtStr);}

    }//close try
    catch(InvalidKeyException ikex)
    {
      ikex.printStackTrace();
    }
    catch(IllegalBlockSizeException ibsex)
    {
      ibsex.printStackTrace();
    }
    catch(UnsupportedEncodingException ueex)
    {
      ueex.printStackTrace();
    }
    finally
    {
      return decryptedTxtStr;
    }//close finally
  }//close decrypt
}//close CryptAES class
