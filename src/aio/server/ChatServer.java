package aio.server;

import sun.awt.CharsetString;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private static final String QUIT = "quit";
    private static final int BUFFER = 1024;
    private static final int THREADPOOL_SIZE = 8;

    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverChannel;
    private List<ClientHandler> connectedClients;
    private Charset charset = Charset.forName("UTF-8");
    private int port;

    public ChatServer(){
        this(DEFAULT_PORT);
    }

    public ChatServer(int port){
        this.port = port;
        this.connectedClients = new ArrayList<>();
    }

    private boolean readyToQuit(String msg){
        return QUIT.equalsIgnoreCase(msg);
    }

    private void close(Closeable closeable){
        if(closeable != null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class AcceptHandler  implements CompletionHandler<AsynchronousSocketChannel , Object> {

        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            // 等待下一个客户端的连接
            if(serverChannel.isOpen()){
                serverChannel.accept(null , this);
            }

            if(clientChannel != null && clientChannel.isOpen()){
                // 为每一个用户分配一个handler，并且这个handler也相当于用户本身
                ClientHandler handler = new ClientHandler(clientChannel);

                //将用户添加到在线用户列表
                addClient(handler);

                // 创建缓冲区
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER);

                /**
                 * 第一个buffer，是要写入的缓冲区。
                 * 第二个buffer，是当read完成后，
                 * 此时buffer是有数据的，
                 * 将这个buffer做为attachment。
                 * */
                // 读取客户端发送的消息
                clientChannel.read(buffer , buffer, handler);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("客户端连接失败："+ exc);
        }
    }

    private class ClientHandler implements CompletionHandler<Integer , Object>{

        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            ByteBuffer buffer = (ByteBuffer) attachment;
            // 读取客户端发送的消息完成，attachment不会为null
            if(buffer != null){
                // 客户端异常
                if(result <= 0){
                    // 将客户移除出在线客户列表
                    removeClient(this);
                } else{
                    // 读模式
                    buffer.flip();
                    String msg = receive(buffer);
                    String clientName = getClientName(clientChannel);
                    String fwdMsg = clientName + ":" + msg;
                    System.out.println(fwdMsg);
                    // 转发消息给其他用户
                    forwardMessage(clientChannel , fwdMsg);
                    // 写模式
                    buffer.clear();
                    if (readyToQuit(msg)){
                        // 用户退出
                        removeClient(this);
                    } else{
                        // 继续读取客户端发送的消息（一波接一波的感觉）
                        clientChannel.read(buffer , buffer , this);
                    }
                }
            }
        }
        @Override
        public void failed(Throwable exc, Object attachment) {
            // 先简单处理为客户端异常，移除该客户即可
            removeClient(this);
        }
    }

    private String getClientName(AsynchronousSocketChannel clientChannel) {
        String clientName = null;
        try {
            clientName =  "客户端["+((InetSocketAddress) clientChannel.getRemoteAddress()).getPort()+"]";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return clientName;
    }

    private String receive(ByteBuffer buffer) {
        return String.valueOf(charset.decode(buffer));
    }

    private synchronized void forwardMessage(AsynchronousSocketChannel clientChannel, String fwdMsg) {
        ByteBuffer buffer = null;
        for(ClientHandler clientHandler : connectedClients){
            // 转发给其他的用户
            if(clientHandler.clientChannel != clientChannel){
                buffer = charset.encode(fwdMsg);
                clientHandler.clientChannel.write(buffer , null , clientHandler);
                buffer.clear();
            }
        }
    }

    private synchronized void addClient(ClientHandler clientHandler) {
        // 将连接成功的用户上线
        connectedClients.add(clientHandler);
        System.out.println(getClientName(clientHandler.clientChannel)+"连接成功");
    }

    private synchronized void removeClient(ClientHandler clientHandler) {
        // 移除用户
        connectedClients.remove(clientHandler);
        System.out.println(getClientName(clientHandler.clientChannel)+"已退出");
        // 关闭资源
        close(clientHandler.clientChannel);
    }

    private void start(){
        try {
            // 创建线程池
            ExecutorService executorService = Executors.newFixedThreadPool(THREADPOOL_SIZE);
            // 创建ChannelGroup
            channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            // 打开管道 , 并且让管道加入我们创建的ChannelGroup
            serverChannel = AsynchronousServerSocketChannel.open(channelGroup);
            // 绑定、监听端口
            serverChannel.bind(new InetSocketAddress(LOCALHOST , port));
            System.out.println("启动服务器，监听端口："+ port);

            while(true){
                // 异步调用， 我们不需要传给回调函数其他信息，所以 attachment为null
                serverChannel.accept(null , new AcceptHandler());
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally{
            close(serverChannel);
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }
}