package study.httppackSty002;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class MyPersonDecodec extends ReplayingDecoder<Void> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int len = in.readInt();
        byte[] content = new byte[len];//in.readBytes()

        in.readBytes(content);

        PersonProtocol personProtocol = new PersonProtocol();
        personProtocol.setLen(len);
        personProtocol.setContent(content);

        out.add(personProtocol);
    }
}
