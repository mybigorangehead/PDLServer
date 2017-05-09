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
    // game status show if the game is available
    protected Map<String, Integer> gameStatus = new HashMap <>();
    // game type is either public or private
    protected Map<String, Integer> gameType = new HashMap<>();

    // generate 4-digigt room code (only 9 rooms aare available now)
    String _CODE = "000";
    int codeCounter = 0;
    public static void main(String[] args) 
    {
        PDLServer server = new PDLServer();
        server.acceptConnections();
    }
    
    // accept connections
    public void acceptConnections() 
    {
        try {
           ServerSocket server = new ServerSocket(listenPort); 
           Socket incomingConnection = null;
           
           // always accept connections
           while (true) {
                incomingConnection = server.accept();
                System.out.println("Port "+incomingConnection.getPort());
                handleConnection(incomingConnection);
            }
        // BindException if it cannot listen on this port
        } catch (BindException e) {
                System.out.println("Unable to bind to port " + listenPort);
        } catch (IOException e) {
                System.out.println("Unable to instantiate a ServerSocket on port: " + listenPort);
        }
    }
    
    // handle connection
    public void handleConnection(Socket client) throws IOException
    {
        InputStream inputFromSocket = client.getInputStream();
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputFromSocket));
        
        // check connection request type
        String[] code = streamReader.readLine().split(" ");
        System.out.println(code[0]);
        
        // if request is create, call createCustomGame
        if (code[0].equals ("CREATE")){
            createCustomGame(client, code[1]);
        } // if request is join, call joinCustomGame
        else if (code[0].equals("JOIN")){
            joinCustomGame(client, code[1]);
        }// if request is create, call createCustomGame
        else if(code[0].equals("QUICK")){
            joinRandomGame(client);
        }    
    }
    
    void joinRandomGame(Socket client) throws IOException{
        OutputStream outputToSocket = client.getOutputStream();
        PrintWriter streamWriter = new PrintWriter(outputToSocket);
       
        for (Map.Entry pair : gameType.entrySet()) {
            String key = (String)pair.getKey();
            
            // if game type is public
            if(gameType.get(key) == 0){
                // if game is not full or in game
                if(gameStatus.get(key) == 0){
                    joinCustomGame(client, key);
                    return;
                }
            }
        }
        // if couldnt find an available game, create a public one
        streamWriter.println("NEW");
        streamWriter.flush();
        createCustomGame(client, "0");
    }
    
    void createCustomGame(Socket client, String type) throws IOException{
        // generate room code
        codeCounter++;
        String newCode = _CODE + codeCounter;
        
        customMasterClients.put(newCode, new MasterThread(client, newCode));
        gameStatus.put(newCode, 0);     
        gameType.put(newCode, Integer.parseInt(type));
        
    }
    
    void joinCustomGame(Socket client, String code) throws IOException{
        System.out.println(isValid(code));
        OutputStream outputToSocket = client.getOutputStream();
        PrintWriter streamWriter = new PrintWriter(outputToSocket);
        if (isValid(code)){
           // if game is available
           if(gameStatus.get(code) == 0){
                MasterThread master = customMasterClients.get(code);
                String clientPort = Integer.toString(master.getPort());
                String clientIP = master.getIp();

                String clientInfo = clientIP;
                streamWriter.println(clientInfo);
                streamWriter.flush();
                streamWriter.close();
           }else{
            streamWriter.println("FAILURE");
            
            // if the game is not available
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
    
    // check if the code is valid
    boolean isValid(String code){
        return customMasterClients.containsKey(code);
    }
    // allow multiple users
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
                    String command = masterReader.readLine();
                    if(command.equals ("DENY")){
                        gameStatus.put(myKey, 1);
                    }else if(command.equals("ACCEPT")){
                        gameStatus.put(myKey, 0);
                    }
                } catch (IOException ex) {
                    // if master client closed her page
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
