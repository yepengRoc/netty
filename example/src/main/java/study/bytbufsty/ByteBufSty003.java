package study.bytbufsty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

import java.util.Iterator;

public class ByteBufSty003 {

    public static void main(String[] args) {
        //复合缓冲区。http header body content
        CompositeByteBuf compositeByteBuf = Unpooled.compositeBuffer();

        ByteBuf heapByteBuf = Unpooled.buffer(10);
        ByteBuf directByteBuf = Unpooled.directBuffer(8);

        compositeByteBuf.addComponent(heapByteBuf);
        compositeByteBuf.addComponent(directByteBuf);

        Iterator iterator = compositeByteBuf.iterator();
        while(iterator.hasNext()){
            System.out.println(iterator.next());
        }

    }
}
