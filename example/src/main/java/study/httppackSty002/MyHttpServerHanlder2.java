package study.httppackSty002;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.nio.charset.Charset;
import java.util.UUID;

public class MyHttpServerHanlder2 extends SimpleChannelInboundHandler<PersonProtocol> {
    private int count;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PersonProtocol msg) throws Exception {
        System.out.println("server recv msg len:" + msg.getLen());
        System.out.println("server recv content:" + new String(msg.getContent(), Charset.forName("UTF-8")));
        System.out.println("server count:" + count++);

        byte[] rspContent = UUID.randomUUID().toString().getBytes();
        int repLen = rspContent.length;

        PersonProtocol rspPer = new PersonProtocol();
        rspPer.setLen(repLen);
        rspPer.setContent(rspContent);

        ctx.writeAndFlush(rspPer);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}
