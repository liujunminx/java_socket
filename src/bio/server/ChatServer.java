package bio.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * BIO 聊天服务器
 */
public class ChatServer {
    private int DEFAULT_PORT = 8888;
    private final String QUIT = "quit";

    private ExecutorService executorService;
    private ServerSocket serverSocket;
    private Map<Integer, Writer> connectedClients;

    public ChatServer(){
        executorService = Executors.newFixedThreadPool(10);
        connectedClients = new HashMap<>();
    }

    /**
     * 添加客户端
     * @param socket
     * @throws IOException
     */
    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null){
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            connectedClients.put(port, writer);
            System.out.println("client[" + port +"]connected server");
        }
    }

    /**
     * 客户端关闭
     * @param socket
     * @throws IOException
     */
    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket != null){
            int port = socket.getPort();
            if (connectedClients.containsKey(port)){
                connectedClients.get(port).close();
            }
            connectedClients.remove(port);
            System.out.println("client[" + port +"]connect finished");
        }
    }

    /**
     * 转发消息
     * @param socket
     * @param fwdMsg
     * @throws IOException
     */
    public synchronized void forwardMessage(Socket socket, String fwdMsg) throws IOException {
        for (Integer id: connectedClients.keySet()){
            if (!id.equals(socket.getPort())){
                Writer writer = connectedClients.get(id);
                writer.write(fwdMsg);
            }
        }
    }

    public boolean readyToQuit(String msg){
        return QUIT.equals(msg);
    }

    private synchronized void close(){
        try {
            if (serverSocket != null){
                serverSocket.close();
                System.out.println("serverSocket closed");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(){
        try {
            // 登台监听端口
            serverSocket = new ServerSocket(DEFAULT_PORT);
            System.out.println("server start, listen: " + DEFAULT_PORT + "...");

            while (true){
                // 等待客户端连接
                Socket socket = serverSocket.accept();
                // 不用没进来一个用户都要单独创建一个线程，浪费资源
                // new Thread(new ChatHandler(this, socket)).start();
                executorService.execute(new ChatHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            close();
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }
}
