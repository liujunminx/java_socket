# chatroom
Java 多人聊天室

## BIO实现
Client部分一个主类一个监听输入类，Server部分一个主类一个监听输入类。
基于BIO模型下每开启一个Client线程, Server都要创建一个线程去监听，这样很浪费资源，故Server端采用线程池创建线程监听client
client数量过多，而线程池满则会阻塞。
