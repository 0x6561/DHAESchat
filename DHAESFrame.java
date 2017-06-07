
import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * GUI for chat/msg program, uses DHAESSocketThread in 
 * background 
 */
public class DHAESFrame extends JFrame 
{
  private static final boolean DBG = false;

  //frame dimensions before setup
  private static final int SETUP_FRAME_WIDTH = 400;
  private static final int SETUP_FRAME_HEIGHT = 200;

  //frame dimensions after setup
  private static final int FRAME_WIDTH = 600;
  private static final int FRAME_HEIGHT = 800;

  private JTextField userNameTxt;
  private JTextField ipTxt;
  private JTextField portTxt;
  JRadioButton svrOption;
  JRadioButton cltOption;
  JTextArea plainTextArea;
  JTextArea msgTextArea;
  JLabel statusL;

  private String myName = "me";
  private String address;
  private int port;
  private boolean connected;
  private boolean serverDone;

  private DHAESSocketThread socket = null;
  private DHAESWorker worker = null;
  protected BlockingQueue inbq;
  protected BlockingQueue outbq;

  private CardLayout cardLayout;
  private JPanel cardsPanel;
  private JPanel setupPanel;
  private JPanel svrSetupPanel;
  private JPanel cltSetupPanel;
  private JPanel messagePanel;

  private Color foreground = Color.white;
  private Color background = Color.black;

  /**
   * ServerFrame Constructor
  */
  public DHAESFrame()
  {
    connected = false;
    serverDone = false;
    plainTextArea = new JTextArea("waiting..");
    msgTextArea = new JTextArea("Enter Message..");
    inbq = new LinkedBlockingQueue<Object>();
    outbq = new LinkedBlockingQueue<Object>();
    cardLayout = new CardLayout();
    cardsPanel = new JPanel(cardLayout);
    setupPanel = createSetupPanel();
    svrSetupPanel = createSvrSetupPanel();
    cltSetupPanel = createCltSetupPanel();
    messagePanel = createMessagePanel();
    cardsPanel.add(setupPanel);
    cardsPanel.add(svrSetupPanel);
    cardsPanel.add(cltSetupPanel);
    cardsPanel.add(messagePanel);
    add(cardsPanel);

    setSize(SETUP_FRAME_WIDTH, SETUP_FRAME_HEIGHT);
    setBackground(Color.black);
    setForeground(Color.white);
    cardLayout.first(cardsPanel);
  }//close MsgSetUpFrame constructor

  public static void main(String[] args) 
  {
    DHAESFrame ssf = new DHAESFrame();
    ssf.setVisible(true);
  }//close main

  private JPanel createSetupPanel() 
  {
    //panel for labels 
    JPanel labelPanel = new JPanel();
    labelPanel.setBackground(background);
    labelPanel.setForeground(foreground);
    labelPanel.setLayout(new GridLayout(2,1));
    JLabel userNL = new JLabel("Enter desired username : ");
    userNL.setForeground(foreground);
    userNL.setBackground(background);
    userNameTxt = new JTextField("me");
    userNameTxt.setForeground(foreground);
    userNameTxt.setBackground(background);
    JLabel choiceL = new JLabel(" Run as ");
    choiceL.setBackground(background);
    choiceL.setForeground(foreground);
    labelPanel.add(userNL);
    labelPanel.add(userNameTxt);
    labelPanel.add(choiceL);

    //panel for user options 
    JPanel radioOptPanel = new JPanel();
    radioOptPanel.setBackground( background);
    radioOptPanel.setForeground( foreground);
    radioOptPanel.setLayout(new GridLayout(2,1));
    svrOption = new JRadioButton("Server", true);
    svrOption.setBackground(background);
    svrOption.setForeground(foreground);
    cltOption = new JRadioButton("Client");
    cltOption.setBackground(background);
    cltOption.setForeground(foreground);
    ButtonGroup optGroup = new ButtonGroup();

    optGroup.add(svrOption);
    optGroup.add(cltOption);

    radioOptPanel.add(svrOption);
    radioOptPanel.add(cltOption);

    //Buttons
    JPanel buttonPanel = new JPanel();
    buttonPanel.setBackground( background);
    buttonPanel.setForeground( foreground);
    buttonPanel.setLayout(new GridLayout(1,2));
    buttonPanel.setBorder(BorderFactory.createLineBorder(foreground));
    JButton nextButton = new JButton("Next");
    ActionListener nextListener = new NextListener();
    nextButton.addActionListener(nextListener);
    JButton cancelButton = new JButton("Cancel");
    ActionListener cancelListener = new CancelListener();
    cancelButton.addActionListener(cancelListener);
    buttonPanel.add(nextButton);
    buttonPanel.add(cancelButton);

    //panel to hold the other panels
    JPanel topPanel = new JPanel();
    topPanel.setBackground( background);
    topPanel.setForeground( foreground);
    topPanel.setLayout(new GridLayout(3,1));
    topPanel.add(labelPanel);
    topPanel.add(radioOptPanel);
    topPanel.add(buttonPanel);
    topPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    return topPanel;
  }//close createComponents methods

  private JPanel createSvrSetupPanel() 
  {
    //panel for labels 
    JPanel labelPanel = new JPanel();
    labelPanel.setBackground( background);
    labelPanel.setForeground( foreground);
    labelPanel.setLayout(new GridLayout(2,1));
    JLabel portL = new JLabel(" Port Number: ");
    portL.setBackground( background);
    portL.setForeground( foreground);
    labelPanel.add(portL);

    //panel of user entry fields
    JPanel fieldPanel = new JPanel();
    fieldPanel.setBackground( background);
    fieldPanel.setForeground( foreground);
    fieldPanel.setLayout(new GridLayout(2,1));
    portTxt = new JTextField("1234");
    fieldPanel.add(portTxt);

    //panel for labels/textfields
    JPanel labelFieldPanel = new JPanel();
    labelFieldPanel.setBackground( background);
    labelFieldPanel.setForeground( foreground);
    labelFieldPanel.setLayout(new GridLayout(1,2));
    labelFieldPanel.add(labelPanel);
    labelFieldPanel.add(fieldPanel);

    //Buttons
    JPanel buttonPanel = new JPanel();
    buttonPanel.setBackground( background);
    buttonPanel.setForeground( foreground);
    buttonPanel.setLayout(new GridLayout(1,2));
    buttonPanel.setBorder(BorderFactory.createLineBorder(foreground));
    JButton svrSetupButton = new JButton("Connect");
    ActionListener svrSetupListener = new SvrSetUpListener();
    svrSetupButton.addActionListener(svrSetupListener);
    JButton cancelButton = new JButton("Cancel");
    ActionListener cancelListener = new CancelListener();
    cancelButton.addActionListener(cancelListener);
    buttonPanel.add(svrSetupButton);
    buttonPanel.add(cancelButton);

    //panel to hold the other panels
    JPanel topPanel = new JPanel();
    topPanel.setBackground( background);
    topPanel.setForeground( foreground);
    topPanel.setLayout(new GridLayout(3,1));
    topPanel.add(labelFieldPanel);
    topPanel.add(buttonPanel);
    topPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    return topPanel;
  }//close createComponents methods

  private JPanel createCltSetupPanel() 
  {
    //panel for labels 
    JPanel labelPanel = new JPanel();
    labelPanel.setBackground(background);
    labelPanel.setForeground(foreground);
    labelPanel.setLayout(new GridLayout(2,1));
    JLabel ipL = new JLabel(" IP Address : ");
    ipL.setBackground(background);
    ipL.setForeground(foreground);
    JLabel portL = new JLabel(" Port Number: ");
    portL.setBackground(background);
    portL.setForeground(foreground);
    labelPanel.add(ipL);
    labelPanel.add(portL);

    //panel of user entry fields
    JPanel fieldPanel = new JPanel();
    fieldPanel.setBackground(background);
    fieldPanel.setForeground(foreground);
    fieldPanel.setLayout(new GridLayout(2,1));
    ipTxt = new JTextField("127.0.0.1");
    ipTxt.setBackground(background);
    ipTxt.setForeground(foreground);
    portTxt = new JTextField("1234");
    portTxt.setBackground(background);
    portTxt.setForeground(foreground);
    fieldPanel.add(ipTxt);
    fieldPanel.add(portTxt);

    //panel for labels/textfields
    JPanel labelFieldPanel = new JPanel();
    labelFieldPanel.setLayout(new GridLayout(1,2));
    labelFieldPanel.setBorder(BorderFactory.createLineBorder(Color.white));
    labelFieldPanel.add(labelPanel);
    labelFieldPanel.add(fieldPanel);

    //Buttons
    JPanel buttonPanel = new JPanel();
    buttonPanel.setBackground(background);
    buttonPanel.setForeground(foreground);
    buttonPanel.setLayout(new GridLayout(1,2));
    buttonPanel.setBorder(BorderFactory.createLineBorder(Color.white));
    JButton cltSetupButton = new JButton("Connect");
    ActionListener cltSetupListener = new CltSetUpListener();
    cltSetupButton.addActionListener(cltSetupListener);
    JButton cancelButton = new JButton("Cancel");
    ActionListener cancelListener = new CancelListener();
    cancelButton.addActionListener(cancelListener);
    buttonPanel.add(cltSetupButton);
    buttonPanel.add(cancelButton);

    //panel to hold the other panels
    JPanel topPanel = new JPanel();
    topPanel.setBackground(background);
    topPanel.setForeground(foreground);
    topPanel.setLayout(new GridLayout(3,1));
    topPanel.add(labelFieldPanel);
    topPanel.add(buttonPanel);
    topPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    return topPanel;
  }

  private JPanel createMessagePanel() 
  {
    //panel for labels/textfields/butons
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());
    mainPanel.setBackground(background);
    mainPanel.setForeground(foreground);

    statusL = new JLabel("STATUS: ");
    statusL.setBackground(background);
    statusL.setForeground(foreground);
    JLabel plainLbl = new JLabel("Incoming Text");
    plainLbl.setBackground(background);
    plainLbl.setForeground(foreground);

    //panel for labels
    JPanel labelPanel = new JPanel();
    labelPanel.setLayout(new GridLayout(2,1));
    labelPanel.setBackground(background);
    labelPanel.setForeground(foreground);
    JPanel txtL = new JPanel();
    txtL.setLayout(new GridLayout(1,2));
    txtL.setBackground(background);
    txtL.setForeground(foreground);
    txtL.add(plainLbl);
    labelPanel.add(statusL);
    labelPanel.add(txtL);

    // TextArea for dialogue
    plainTextArea.setLineWrap(true);
    plainTextArea.setBackground(background);
    plainTextArea.setForeground(foreground);
    JScrollPane plaintxtScroller = new JScrollPane(plainTextArea);

    // TextArea for outgoing message
    msgTextArea.setLineWrap(true);
    msgTextArea.setBackground(background);
    msgTextArea.setForeground(foreground);
    JScrollPane msgtxtScroller = new JScrollPane(msgTextArea);
    
    //panel with scrollable area for textAreas
    JPanel textAreasPanel = new JPanel();
    textAreasPanel.setBackground(background);
    textAreasPanel.setForeground(foreground);
    textAreasPanel.setLayout(new GridLayout(2,1));
    textAreasPanel.add(plaintxtScroller);
    textAreasPanel.add(msgtxtScroller);
    textAreasPanel.setSize(700,500);

    JButton sndMsgButton = new JButton("Send Message");
    ActionListener sndMsgListener = new SndMsgListener();
    sndMsgButton.addActionListener(sndMsgListener);

    JButton quitButton = new JButton("Quit");
    ActionListener quitListener = new QuitListener();
    quitButton.addActionListener(quitListener);

    //panel for buttons
    JPanel buttonPanel = new JPanel();
    buttonPanel.setBackground(background);
    buttonPanel.setForeground(foreground);
    buttonPanel.setLayout(new GridLayout(1,2));
    buttonPanel.add(sndMsgButton);
    buttonPanel.add(quitButton);

    mainPanel.add(labelPanel, BorderLayout.NORTH);
    mainPanel.add(textAreasPanel, BorderLayout.CENTER);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    return mainPanel;
  }//close createComponents methods

  /**
   * what to do after user picks client/server 
   */
  public class NextListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      try
      {
        myName = userNameTxt.getText();
        if(svrOption.isSelected())
        {
          if(DBG){System.out.println("Server Option Selected");}
          cardLayout.next(cardsPanel); 
        }
        else //user chose client
        {
          if(DBG){System.out.println("Client Option Selected");}
         cardLayout.next(cardsPanel);//flip to client card 
         cardLayout.next(cardsPanel);//flip to client card 
        }
      }
      catch(Exception e)
      { 
        System.out.println(e);
        e.printStackTrace();
      }//close catch
    }//close actionPerformed method
  }//close NextListener class

  /**
   * server setup button 
   */
  public class SvrSetUpListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      try
      {
        if ((portTxt.getText().equals("")))
        {
          JOptionPane.showMessageDialog(null,"Port required ","Connection Error",JOptionPane.ERROR_MESSAGE);
        }//close if 
        else
        {
          //read what user entered
          port = Integer.parseInt(portTxt.getText());

          //setup DHAESSocketThread
          socket = new DHAESSocketThread(port, inbq, outbq, myName);
          Thread socketThread = new Thread(socket);
          socketThread.start();

          // setup SwingWorker 
          DHAESWorker swingWorker = new DHAESWorker(plainTextArea, statusL, inbq);

          swingWorker.execute();//runs code in doInBackground
          if(DBG){System.out.println("server started..");}

          //adjust gui
          setSize(FRAME_WIDTH, FRAME_HEIGHT);
          cardLayout.last(cardsPanel);
          revalidate();

        }//close else
      }//close try
      catch(Exception e)
      { 
        System.out.println(e);
        e.printStackTrace();
      }//close catch
    }//close actionPerformed method
  }//close SetUpListener class

  /*
   * Listener for client setup button
   */
  public class CltSetUpListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      try
      {
        if((ipTxt.getText().equals("")) || (portTxt.getText().equals("")))
        {
          JOptionPane.showMessageDialog(null,"IP/Port required ","Connection Error",JOptionPane.ERROR_MESSAGE);
        }//close if 
        else
        {
          //read what user entered
          address = ipTxt.getText();
          port = Integer.parseInt(portTxt.getText());

          //setup DHAESSocketThread
          socket = new DHAESSocketThread(port, address, inbq, outbq, myName);
          Thread socketThread = new Thread(socket);
          socketThread.start();

          // setup Client
          DHAESWorker  swingWorker = new DHAESWorker(plainTextArea,  statusL, inbq);
          swingWorker.execute();
          if(DBG){System.out.println("client started..");}

          //adjust gui
          setSize(FRAME_WIDTH, FRAME_HEIGHT);
          cardLayout.last(cardsPanel);
          revalidate();

        }//close else
      }//close try
      catch(Exception e)
      { 
        System.out.println(e);
        e.printStackTrace();
      }//close catch
    }//close actionPerformed method
  }//close SetUpListener class

  public class CancelListener implements ActionListener
  {
    public void actionPerformed(ActionEvent event)
    {
      //what to do on button click 
      System.exit(0);
    }//close actionPerformed method
  }//close ClickListener class


  /*
   * Listener for send button
   */
  class SndMsgListener implements ActionListener 
  {
    public void actionPerformed(ActionEvent event)
    {
      try
      {
      //insert event here			
      String msg = msgTextArea.getText();
      outbq.put(msg);
      if(DBG){System.out.println("Sending: " + msg + " Queue size " + outbq.size());}
      msgTextArea.setText("");
      plainTextArea.append(myName +": " +msg + "\n");

      }
      catch(Exception e)
      {
        System.out.println(e);
        e.printStackTrace();
      }
    }
  } //end SndMsgListener

  //listener for quit button
  class QuitListener implements ActionListener 
  {
    public void actionPerformed(ActionEvent event)
    {
      //insert event here			
      if(DBG){System.out.println("Quit Button Clicked");}
      //socket.quit();
      dispose();
      System.exit(0);
      //server.quit();
    }
  } //end sendListener

}//close ServerFrame class
