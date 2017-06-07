
import java.util.*;
import java.util.concurrent.BlockingQueue;
import javax.swing.*;

/* ServerThread
 * Helper class, runs socket in separate thread
 */
public class DHAESWorker extends SwingWorker<Void, Object> 
{
  //for I/O
  private static final boolean DBG = true;
  private static final int SLEEPTIME = 5;

  protected BlockingQueue<Object> inQueue;
  protected BlockingQueue<Object> outQueue;
  private boolean done;

  // for gui
  JTextArea plainTxtA;
  JLabel statusL;

  /**
   * Class Constructor
   * @param JTextArea p
   * @param JTextArea c
   * @param JLabel sl
   */
  public DHAESWorker(JTextArea p,  JLabel sl, BlockingQueue inbq)
  {
    plainTxtA = p;
    statusL = sl;
    inQueue = inbq;
    done = false;
  }//close constructor

  /**
   * doInBackground method, once server is setup used to 
   * monitor socket and update frame
   */
  @Override
  protected Void doInBackground()
  {
    try
    { 
      while(!done)
      {
        // only read if underlying LinkedBlockingQueue has data
        if(inQueue.size() > 0)
        { 
          LinkedList<Object> msgList = new LinkedList<Object>();
          inQueue.drainTo(msgList);
          Iterator<Object> itr = msgList.iterator();  
          while(itr.hasNext())
          {  
            publish(itr.next());  
          }//close inner while  
        }//close if
        else
        {
          Thread.sleep(SLEEPTIME);
        }
      }//close while
    }//close try
    catch(Exception e)
    {
      System.out.println(e.getMessage());
      System.out.println(e.getCause());
      e.printStackTrace();
    }//close catch
    return null;// ?
  }//close doInBackground method

  /*
   * Updates GUI
   * process messages received from the servers doInBackground method
   * when invoking the publish() method
   */
  protected void process(java.util.List<Object> msgs)
  {
    for (Object msg: msgs) 
    {
      if(msg instanceof String)
      {
        String strMsg = (String)msg;
        //statusL.setText(strMsg);
        plainTxtA.append(strMsg+"\n");
      }//close else if
    }//close for
  }//close process method

  public void quit()
  {
    /*TODO */
    // properly exit
    System.exit(0);
  }//close quit method
}//close Server class
