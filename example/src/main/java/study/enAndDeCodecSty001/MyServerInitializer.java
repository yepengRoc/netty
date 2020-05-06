package study.enAndDeCodecSty001;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class MyServerInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel sc) throws Exception {
        ChannelPipeline cp = sc.pipeline();
        cp.addLast(new MyLongToByteEncodec());
        cp.addLast(new MyByteToLongDecodec());
        cp.addLast(new MyServerHandler());
    }
}
