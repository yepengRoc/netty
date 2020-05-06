package study.httppackSty002;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.Charset;

public class MyHttpClientHandler2 extends SimpleChannelInboundHandler<ByteBuf> {

    private int count;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
           int len =  msg.readInt();
           byte[] content = new byte[len];
            msg.readBytes(content);
            System.out.println("clent recv len:" + len);
            System.out.println("client recv data:" + new String(content,Charset.forName("UTF-8")));
            System.out.println("client count:" + count++);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        super.channelActive(ctx);
        for(int i=0;i < 10;i++){
            PersonProtocol personProtocol = new PersonProtocol();
            byte[] buffer = "client send msg".getBytes(Charset.forName("UTF-8"));

            personProtocol.setLen(buffer.length);
            personProtocol.setContent(buffer);
            ctx.writeAndFlush(personProtocol);
        }

    }
}
