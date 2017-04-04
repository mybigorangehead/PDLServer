/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pdlserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author yayang
 */
public class PDLServer {
    protected int listenPort = 3000;
    protected Map<String, MasterThread> customMasterClients = new HashMap <>();
    
    protected Map<String, Integer> gameStatus = new HashMap <>();
    
    //ALLOWS 9 MAX
    String _CODE = "000";
    int codeCounter = 0;
    public static void main(String[] args) 
    {
      //  customMasterClients = new HashMap <String, Socket>();
        PDLServer server = new PDLServer();
        server.acceptConnections();
    
    }
    
    
    public void acceptConnections() 
    {
        try {
           ServerSocket server = new ServerSocket(listenPort); //BindException if it cannot listen on this port
           Socket incomingConnection = null;
           
           while (true) {
                incomingConnection = server.accept();
                System.out.println("Port "+incomingConnection.getPort());
                handleConnection(incomingConnection);
                //incomingConnection.close();
            }
        } catch (BindException e) {
                System.out.println("Unable to bind to port " + listenPort);
        } catch (IOException e) {
                System.out.println("Unable to instantiate a ServerSocket on port: " + listenPort);
        }
        
    }
    
    //200 for quickplay, DO AFTER CUSTOM
    //210 for custom play
    //240 ROOMCODE for custom join
    public void handleConnection(Socket client) throws IOException
    {
        InputStream inputFromSocket = client.getInputStream();
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputFromSocket));
        System.out.println("a;sldkfja;sldkfja;sldkfj");
        String[] code = streamReader.readLine().split(" ");
        System.out.println(code[0]);
        if (code[0].equals ("210")){
            
            createCustomGame(client);
        }
        else if (code[0].equals("240")){
            joinCustomGame(client, code[1]);
        }
        streamReader.close();
    }
    void createCustomGame(Socket client) throws IOException{
        OutputStream outputToSocket = client.getOutputStream();
        PrintWriter streamWriter = new PrintWriter(outputToSocket);
        
        codeCounter++;
        String newCode = _CODE + codeCounter;
        
        customMasterClients.put(newCode, new MasterThread(client, newCode));
        
        //streamWriter.write("200 SUCCESS");
        streamWriter.println(newCode);
        streamWriter.flush();
        streamWriter.close();
        
    }
    void joinCustomGame(Socket client, String code) throws IOException{
        if (isValid(code)){
            OutputStream outputToSocket = client.getOutputStream();
            PrintWriter streamWriter = new PrintWriter(outputToSocket);
            
            MasterThread master = customMasterClients.get(code);
            String clientPort = Integer.toString(master.getPort());
            String clientIP = master.getIp();

            String clientInfo = "300 " + clientPort + " " + clientIP;
            
            streamWriter.write(clientInfo);
            streamWriter.flush();
            streamWriter.close();
        }
        
    }
    
    boolean isValid(String code){
        return customMasterClients.containsKey(code);
    }
    public class MasterThread extends Thread{
        Socket master;
        BufferedReader masterReader;
        String myKey;
        public MasterThread (Socket c, String key) throws IOException{
            master = c;
            masterReader = new BufferedReader(new InputStreamReader(master.getInputStream()));
            myKey = key;
            this.start();
        }
        
        @Override
        public void run(){
            while(true){
                try {
                    
                    //on disconnect
                    if(masterReader.readLine() == null){
                        customMasterClients.remove(myKey);
          
                        //remove  this master from map
                        //end this thread
                        
                    }else if(masterReader.readLine().equals ("INGAME")){
                        gameStatus.put(myKey, 1);
                    }else if(masterReader.readLine().equals("OUTGAME")){
                        gameStatus.put(myKey, 0);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(PDLServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        public int getPort(){
            return master.getPort();
        }
        public String getIp(){
            return master.getInetAddress().getHostAddress();
        }
    }   
}
