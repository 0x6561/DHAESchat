
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

//stuff for crypto 
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.security.interfaces.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;
import com.sun.crypto.provider.SunJCE;

/*
 * This class creates either a client/server 
 * socket which runs in its own thread. 
 * traffic passing through is encrypts using AES 
 */
public class DHAESSocketThread implements Runnable
{
  //for I/O
  private static final boolean DBG = true;
  //private static final int SLEEPTIME = 5;
  // Socket Timeout for chat session
  private static final int SOTIMEOUT = 100;


  private boolean socketSetup;
  ServerSocket serverSock;
  private Socket sock;
  private String address;
  private int port;
  private boolean isServer;
  private ObjectInputStream in = null;
  private ObjectOutputStream out = null;
  protected BlockingQueue<Object> inQueue;
  protected BlockingQueue<Object> outQueue;
  private boolean done;
  private String myName;
  private String penPal;

  // for encryption
  private AlgorithmParameterGenerator paramGen;
  private AlgorithmParameters params;
  private DHParameterSpec dhParamSpec;
  private KeyPairGenerator serverKpairGen;
  private  KeyPairGenerator clientKpairGen;
  private  KeyPair clientKpair;
  private KeyPair serverKpair;
  private KeyAgreement serverKeyAgree;
  private  KeyAgreement clientKeyAgree;
  private byte[] serverPubKeyEnc;
  private  byte[] clientSharedSecret;
  private  int clientLen;
  private KeyFactory serverKeyFac;
  private  KeyFactory clientKeyFac;
  private X509EncodedKeySpec x509KeySpec; 
  private  PublicKey serverPubKey;
  private PublicKey clientPubKey;
  private  byte[] clientPubKeyEnc;
  private  byte[] dhShared;
  private CryptAES cryptAES;
  private boolean isCryptSetup;

  /*
   * Constructor for Server Socket
   * @param p, port number (int)
   * @param inbq, BlockingQueue (input)
   * @param outbq, BlockingQueue (output)
   * @param n, String name
   */
  DHAESSocketThread(int p, BlockingQueue inbq, BlockingQueue outbq, String n)
  {
    this.port = p;
    this.inQueue = inbq;
    this.outQueue = outbq;
    this.isServer = true;
    this.socketSetup = false;
    this.myName = n;
    this.isCryptSetup = false;
  }//close SocketThread Constructor

  /*
   * Constructor for Client Socket
   * @param p, port number (int)
   * @param ip, String ip/address of server
   * @param inbq, BlockingQueue (input)
   * @param outbq, BlockingQueue (output)
   * @param n, String name
   */
  DHAESSocketThread(int p, String ip, BlockingQueue inbq, BlockingQueue outbq, String n)
  {
    this.port = p;
    this.address = ip;
    this.inQueue = inbq;
    this.outQueue = outbq;
    this.isServer = false;
    this.socketSetup = false;
    this.myName = n;
    this.isCryptSetup = false;
  }//close SocketThread Constructor

  /**
   * setup listening socket
   * @param int p : port to listen on
   * @return socketSetup (boolean)
   */
  public boolean setupSock()
  { 
    try
    {
      if(isServer)
      {
        serverSock = new ServerSocket(port);
        if(DBG){System.out.println("Server listening on port " + port);}
        sock = serverSock.accept();
        //sock.setTcpNoDelay(false);
        //sock.setSoTimeout(10);
        //while(!sock.isConnected()){/*wait to connect*/}
      }
      else
      {
        sock = new Socket(address, port);
        //while(!sock.isConnected()){/*wait to connect*/}
      }

      while(!sock.isConnected()){/*wait to connect*/}
      if(DBG){System.out.println("raw socket connected : " + sock.toString());}

      out = new ObjectOutputStream(sock.getOutputStream());
      in = new ObjectInputStream(sock.getInputStream());

      if(sock.isConnected()){socketSetup = true;}


      if(isServer)
      {
        prepDH();
        completeDH();
        if(DBG){System.out.println("SERVER DHAES setup DONE ");}
      }
      else
      {
        setupDHAES();
        if(DBG){System.out.println("CLIENT DHAES setup DONE ");}
      }
    }//close try
    catch(Exception e)
    {
      System.out.println(e.getMessage());
      System.out.println(e.getCause());
      e.printStackTrace();
    }//close catch
    return socketSetup;
  }//close setup method

  //@Overide
  public void run()
  {
    try
    {
      setupSock();
      sendAESCrypt(myName);
      penPal = readAESCrypt();
      if(penPal.equals("me")){penPal = "them";}


        sock.setTcpNoDelay(false);
        sock.setSoTimeout(SOTIMEOUT);

      while (true)
      {
        if(DBG){System.out.print("X");}

        boolean outGoing = false;
        boolean inComing = false;
        Object inObj = null;

        try
        {
        if((inObj = in.readObject()) != null){inComing = true;}
        }
        catch(java.net.SocketTimeoutException ste){}

        if(DBG){System.out.print("R");}

        if(outQueue.size() > 0){outGoing = true;}

        if(DBG){System.out.print("Y");}

        if(outGoing)
        {
        if(DBG){System.out.print("w");}
          LinkedList<Object> outMsgList = new LinkedList<Object>();
          outQueue.drainTo(outMsgList);
          Iterator<Object> itr = outMsgList.iterator();  
          while(itr.hasNext())
          {  
            String outTxt = itr.next().toString(); 
            sendAESCrypt(outTxt);
            if(DBG){System.out.println("wrote to socket : " + outTxt);}
          }//close inner while  
        }

        if(DBG){System.out.print("Z");}

        if(inComing)
        {
        if(DBG){System.out.print("r");}
          String inStr = cryptAES.decrypt(inObj.toString());
          inQueue.put(penPal + ": " + inStr + "\n");
          if(DBG){System.out.println("socket read : " + inStr);}
        }
        if(DBG){System.out.print("Z");}
      }//close while

        //if(DBG){System.out.print("E");}
    }//close try
    catch(Exception e)
    {
      System.out.println(e.getMessage());
      System.out.println(e.getCause());
      e.printStackTrace();
    }//close catch
  }//close run

  /*
   * Server side DH setup
   */
  public void prepDH()
  { 
    try
    {
      /* 
       * A central authority generates parameters 
       * and gives them to the two entities seeking 
       * to generate a secret key. The parameters 
       * are a prime p, a base g, and optionally 
       * the length in bits of the private value, l (letter l).
       */
      //Creat Diffie-Hellman parameters 
      paramGen = AlgorithmParameterGenerator.getInstance("DH");
      paramGen.init(2048);
      params = paramGen.generateParameters();
      dhParamSpec = (DHParameterSpec)params.getParameterSpec
        (DHParameterSpec.class);

      serverKpairGen = KeyPairGenerator.getInstance("DH");
      serverKpairGen.initialize(dhParamSpec);
      serverKpair = serverKpairGen.generateKeyPair();
      serverKeyAgree = KeyAgreement.getInstance("DH");
      serverKeyAgree.init(serverKpair.getPrivate());

      // server encodes its public key, and sends it over to client.
      serverPubKeyEnc = serverKpair.getPublic().getEncoded();
    }
    catch(Exception e)
    {
      System.out.println(e.getMessage());
      System.out.println(e.getCause());
      e.printStackTrace();
    }//close catch
  }//close prepDH

  /*
   * complete Server side DH process
   * @return boolean, is Diffie-Helman setup done
   */
  public boolean completeDH()
  {
    try
    {
      send(serverPubKeyEnc);// <-- socket write
      if(DBG){System.out.println("Server has sent its PUBLIC key");}

      /*
       * Server uses Client's public key for the first (and only) phase
       * of its version of the DH protocol. Before the Server can do so,
       * it has to instantiate a DH public key
       * from the Client's encoded key material.
       */
      serverKeyFac = KeyFactory.getInstance("DH");
      x509KeySpec = 
        new X509EncodedKeySpec((byte[])read());// <-- socket read
      if(DBG){System.out.println("Server has received CLIENTS PUBLIC key");}
      clientPubKey = serverKeyFac.generatePublic(x509KeySpec);
      serverKeyAgree.doPhase(clientPubKey, true);

      /*
       * At this point, both the Server and Client have 
       * completed the DH key agreement protocol.
       * Both can generate the (same) shared secret.
       */

      //use shared secret as an AES key
      byte[] dhShared = serverKeyAgree.generateSecret();
      int dhSharedLen = dhShared.length;
      if(DBG){System.out.println("SERVER creating SHARED secret");}
      //now use dhShared to create AES Cipher
      cryptAES = new CryptAES(dhShared);
      isCryptSetup = true;
      if(DBG){System.out.println("SERVER creating AES CIPHER");}
    }
    catch(Exception e)
    {
      System.out.println(e.getMessage());
      System.out.println(e.getCause());
      e.printStackTrace();
    }//close catch
    finally
    {
      return isCryptSetup;
    }
  }//close completeDH

  /*
   * Setup DH (Client version)
   * @return boolean, is Diffie-Helman setup done
   */
  public boolean setupDHAES()
  {
    try
    {
      clientKeyFac = KeyFactory.getInstance("DH");
      x509KeySpec = new X509EncodedKeySpec
        ((byte[])read()); // <-- Socket read here
      if(DBG){System.out.println("CLIENT read KEYSPEC parameters from server");}

      serverPubKey = clientKeyFac.generatePublic(x509KeySpec);

      dhParamSpec = ((DHPublicKey)serverPubKey).getParams();

      // Client creates its own DH key pair using same params
      clientKpairGen = KeyPairGenerator.getInstance("DH");
      // client uses same spec as server
      clientKpairGen.initialize(dhParamSpec);
      clientKpair = clientKpairGen.generateKeyPair();
      if(DBG){System.out.println("CLIENT creates it's public/private keypair");}

      // Client creates and initializes its DH KeyAgreement object
      clientKeyAgree = KeyAgreement.getInstance("DH");
      clientKeyAgree.init(clientKpair.getPrivate());

      // Client encodes its public key, and sends it to Server.
      clientPubKeyEnc = clientKpair.getPublic().getEncoded();
      //send client's public key to server
      send(clientPubKeyEnc); // <-- writing to socket here
      if(DBG){System.out.println("CLIENT sent it's public key");}

      /*
       * Client uses Server's public key for the first (and only) phase
       * of its version of the DH protocol.
       */
      clientKeyAgree.doPhase(serverPubKey, true);

      /*
         At this point, both the Server and Client have 
         completed the DH key agreement protocol.
         Both can generate the SAME shared secret.
         */
      clientSharedSecret = clientKeyAgree.generateSecret();
      clientLen = clientSharedSecret.length;

      //meke AES key
      clientKeyAgree.doPhase(serverPubKey, true);
      // use the shared secret as a seed to generate 256 bit aes key
      dhShared = clientKeyAgree.generateSecret();
      if(DBG){System.out.println("CLIENT created shared secret");}
      //now use dhShared to create AES CipherS

      /*
       * encrypt using DES in ECB mode
       */
      cryptAES = new CryptAES(dhShared);
      isCryptSetup = true;
      if(DBG){System.out.println("CLIENT created AES CIPHER");}
    }
    catch(Exception ioex)
    {
      System.out.println(ioex.getMessage());
      System.out.println(ioex.getCause());
      ioex.printStackTrace();
    }//close catch
    finally
    {
      return isCryptSetup;
    }
  }// close setupDHAES

  /*
   * Method to send data (unencrypted) 
   * @param objOut, outgoing data
   */
  public void send(Object objOut)
  {
    try
    {
      System.out.println("server sending : " + objOut);
      out.writeObject(objOut);
      out.flush();
    }//close try
    catch(IOException ioex)
    {
      System.out.println(ioex.getMessage());
      System.out.println(ioex.getCause());
      ioex.printStackTrace();
    }//close catch
  }//close send method


  /*
   * Method to read data (unencrypted) 
   * @return Object, incoming data
   */
  public Object read()
  {
    Object objIn = null;
    try
    {
      objIn = in.readObject();
    }//close try
    catch(IOException ioex)
    {
      System.out.println(ioex.getMessage());
      System.out.println(ioex.getCause());
      ioex.printStackTrace();
    }//close catch
    catch(ClassNotFoundException cnfex)
    {
      System.out.println(cnfex.getMessage());
      System.out.println(cnfex.getCause());
    }
    return objIn;
  }//close read method

  /*
   * Method to send data (encrypted) 
   * @param outStr, outgoing data
   */
  public void sendAESCrypt(String outStr)
  {
    try
    {
      String cryptedOutStr = cryptAES.crypt(outStr);
      out.writeObject(cryptedOutStr);
      out.flush();
      if(DBG){System.out.println("socket write : " + outStr);}
    }//close try
    catch(Exception ioex)
    {
      System.out.println(ioex.getMessage());
      System.out.println(ioex.getCause());
      ioex.printStackTrace();
    }//close catch
  }//close send method

  /*
   * Method to read data (encrypted) 
   * @return String 
   */
  public String readAESCrypt()
  {
    String plainText = "";
    try
    {
      Object objIn = in.readObject();
      if(DBG){System.out.println("socket read : " + objIn.toString());}
      String cryptedInStr = (String) objIn;
      plainText = cryptAES.decrypt(cryptedInStr);
    }//close try
    catch(Exception ioex)
    {
      System.out.println(ioex.getMessage());
      System.out.println(ioex.getCause());
      ioex.printStackTrace();
    }//close catch
    return plainText ;
  }//close read method

}//close DHAESSocketThread class 
