package study.enAndDeCodecSty001;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class MyClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline cp = ch.pipeline();
       cp.addLast(new MyLongToByteEncodec());
        /*  cp.addLast(new MyByteToLongDecodec());*/

        cp.addLast(new MyByteToLongDecodec());

//       cp.addLast(new ReplayingDecoder<Void>() {
//            @Override
//            protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
//                out.add(in.readLong());
//            }
//        });
        cp.addLast(new MyClientHandler());
    }
}
