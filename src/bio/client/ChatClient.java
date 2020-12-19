package bio.client;

import java.io.*;
import java.net.Socket;

public class ChatClient {
    private final String DEFAULT_SERVER_HOST = "127.0.0.1";
    private final int DEFAULT_SERVER_PORT = 8888;
    private final String QUIT = "quit";

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public void sendMessage(String msg) throws IOException {
        if (!socket.isOutputShutdown()){
            writer.write(msg + "\n");
            writer.flush();
        }
    }

    public String receive() throws IOException {
        String str = null;
        if (!socket.isInputShutdown()){
            str = reader.readLine();
        }
        return str;
    }

    public boolean readyToQuit(String str){
        return str.equals(QUIT);
    }

    public void close(){
        try {
            if (writer != null){
                System.out.println("server closed");
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(){
        try {
            socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            new Thread(new UserInputHandler(this)).start();
            String str = null;
            while ((str = receive()) != null){
                System.out.println(str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close();
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
}
