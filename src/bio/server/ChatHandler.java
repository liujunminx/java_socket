package bio.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ChatHandler implements Runnable{
    private ChatServer chatServer;
    private Socket socket;

    public ChatHandler(ChatServer chatServer, Socket socket) {
        this.chatServer = chatServer;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            chatServer.addClient(socket);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            String msg = null;
            while ((msg = reader.readLine()) != null){
                if (chatServer.readyToQuit(msg)){
                    break;
                }
                String fwdMsg = "client[" + socket.getPort() + "]: " + msg + "\n";
                System.out.println(fwdMsg);
                chatServer.forwardMessage(socket, fwdMsg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                System.out.println("client[" + socket.getPort() +"] closed");
                chatServer.removeClient(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
