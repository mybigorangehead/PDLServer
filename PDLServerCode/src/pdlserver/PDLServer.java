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

/**
 *
 * @author yayang
 */
public class PDLServer {
    protected int listenPort = 3000;
    protected Map<String, Socket> customMasterClients = new HashMap <>();
    
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
    
    //250 for quickplay, DO AFTER CUSTOM
    //200 for custom play
    //210 ROOMCODE for custom join
    public void handleConnection(Socket client) throws IOException
    {
        
        InputStream inputFromSocket = client.getInputStream();
        BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputFromSocket));
        
        String[] code = streamReader.readLine().split(" ");
        if (code[0].equals ("200")){
           // Map<>
            createCustomGame(client);
        }
        else if(code[0].equals("210")){
            joinCustomGame(client, code[1]);
        }
    }
    void createCustomGame(Socket client) throws IOException{
        OutputStream outputToSocket = client.getOutputStream();
        PrintWriter streamWriter = new PrintWriter(outputToSocket);
        
        codeCounter++;
        String newCode = _CODE + codeCounter;
        customMasterClients.put(newCode, client);
        streamWriter.write("400 SUCCESS");
        streamWriter.flush();
    }
    void joinCustomGame(Socket client, String code){
        if (isValid(code)){
            String clientPort = Integer.toString(client.getPort());
            //String clientIP = client.getRemoteAddr();
            //String clientInfo = clientPort + " " + clientIP;
            
            //customMasterClients.put(clientPortInfo, client);
            

            //cient needs master clients ip, master clients port
            //send those as string
            //JOSH will have client expect one string
            
            //master client needds same thing from clien 
       }
        
    }
    
    boolean isValid(String code){
        return customMasterClients.containsKey(code);
    }
    
    
}
