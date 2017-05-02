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
import java.util.Iterator;
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
    //public or private?
    protected Map<String, Integer> gameType = new HashMap<>();
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
        // System.out.println("a;sldkfja;sldkfja;sldkfj");
        String[] code = streamReader.readLine().split(" ");
        System.out.println(code[0]);

        if (code[0].equals ("CREATE")){
            createCustomGame(client, code[1]);
        }
        else if (code[0].equals("JOIN")){
            joinCustomGame(client, code[1]);
        }
        else if(code[0].equals("QUICK")){
            joinRandomGame(client);
        }
        //streamReader.close();
    }
    void joinRandomGame(Socket client) throws IOException{
        OutputStream outputToSocket = client.getOutputStream();
        PrintWriter streamWriter = new PrintWriter(outputToSocket);
       
        for (Map.Entry pair : gameType.entrySet()) {
            String key = (String)pair.getKey();
            //if game is public
            if(gameType.get(key) == 0){
                //if game is not full or in game
                if(gameStatus.get(key) == 0){
                    joinCustomGame(client, key);
                    return;
                }
            }
        }
         //couldnt find an available game, so create a public one
        streamWriter.println("NEW");
        streamWriter.flush();
        createCustomGame(client, "0");
            
    }
    void createCustomGame(Socket client, String type) throws IOException{
        //OutputStream outputToSocket = client.getOutputStream();
        //PrintWriter streamWriter = new PrintWriter(outputToSocket);
        
        codeCounter++;
        String newCode = _CODE + codeCounter;
        
        customMasterClients.put(newCode, new MasterThread(client, newCode));
        gameStatus.put(newCode, 0);     
        gameType.put(newCode, Integer.parseInt(type));
        //streamWriter.write("200 SUCCESS");
        //streamWriter.println(newCode);
        //streamWriter.flush();
        //streamWriter.close();
        
    }
    void joinCustomGame(Socket client, String code) throws IOException{
        System.out.println(isValid(code));
        OutputStream outputToSocket = client.getOutputStream();
        PrintWriter streamWriter = new PrintWriter(outputToSocket);
        if (isValid(code)){
            
           if(gameStatus.get(code) == 0){
                MasterThread master = customMasterClients.get(code);
                String clientPort = Integer.toString(master.getPort());
                String clientIP = master.getIp();
               // Socket master = customMasterClients.get(code);

<<<<<<< HEAD
            MasterThread master = customMasterClients.get(code);
            //String clientPort = Integer.toString(master.getPort());
            String clientIP = master.getIp();
           // Socket master = customMasterClients.get(code);
         
        //    String clientIP = master.getInetAddress().getHostAddress();
=======
            //    String clientIP = master.getInetAddress().getHostAddress();
>>>>>>> 1f022af90bc6a411e2da5b314f8874b8aaa00713

                String clientInfo = clientIP;
                streamWriter.println(clientInfo);
                streamWriter.flush();
                streamWriter.close();
           }else{
            streamWriter.println("FAILURE");
            if(isValid(code)){            
                if(gameStatus.get(code)==1){
                    streamWriter.println("Could not join game, the game is full.");
                }
            }
            else{
                streamWriter.println("Room does not exist.");
            }
            streamWriter.flush();
        }
        }else{
            streamWriter.println("FAILURE");
            if(isValid(code)){            
                if(gameStatus.get(code)==1){
                    streamWriter.println("Could not join game, the game is full.");
                }
            }
            else{
                streamWriter.println("Room does not exist.");
            }
            streamWriter.flush();
        }
         
    }
    
    boolean isValid(String code){
        return customMasterClients.containsKey(code);
    }
    public class MasterThread extends Thread{
        Socket master;
        BufferedReader masterReader;
        OutputStream outputToSocket;
        PrintWriter streamWriter;
        String myKey;
        public MasterThread (Socket c, String key) throws IOException{
            outputToSocket = c.getOutputStream();
            streamWriter = new PrintWriter(outputToSocket);
            master = c;
            masterReader = new BufferedReader(new InputStreamReader(master.getInputStream()));
            myKey = key;
            streamWriter.println(key);
            streamWriter.flush();
            this.start();
        }
        
        @Override
        public void run(){
            while(true){
                try {
                    // System.out.println("waiting for" + myKey);
                    //CHECK DISCONNECT HERE SOMEHOW
                    String command = masterReader.readLine();
                    if(command.equals ("DENY")){
                        gameStatus.put(myKey, 1);
                    }else if(command.equals("ACCEPT")){
                        gameStatus.put(myKey, 0);
                    }
                } catch (IOException ex) {
                    //master client closed his thing
                    customMasterClients.remove(myKey);
                    gameStatus.remove(myKey);
                    gameType.remove(myKey);
                    this.stop();
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
