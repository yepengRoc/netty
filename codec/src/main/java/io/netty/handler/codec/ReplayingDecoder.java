
package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.util.Signal;
import io.netty.util.internal.StringUtil;

import java.util.List;

/**
 * A specialized variation of {@link ByteToMessageDecoder} which enables implementation
 * of a non-blocking decoder in the blocking I/O paradigm.
 * ByteToMessageDecoder的一种特殊变体，它可以在阻塞I / O范例中实现非阻塞解码器。
 * <p>
 * The biggest difference between {@link ReplayingDecoder} and
 * {@link ByteToMessageDecoder} is that {@link ReplayingDecoder} allows you to
 * implement the {@code decode()} and {@code decodeLast()} methods just like
 * all required bytes were received already, rather than checking the
 * availability of the required bytes.  For example, the following
 * {@link ByteToMessageDecoder} implementation:
 * ReplayingDecoder和ByteToMessageDecoder之间的最大区别在于，ReplayingDecoder允许您像已收到所有必需的字节一样实现decode（）
 * 和decodeLast（）方法，而不是检查所需字节的可用性。例如，以下ByteToMessageDecoder实现：
 * <pre>
 * public class IntegerHeaderFrameDecoder extends {@link ByteToMessageDecoder} {
 *
 *   {@code @Override}
 *   protected void decode({@link ChannelHandlerContext} ctx,
 *                           {@link ByteBuf} buf, List&lt;Object&gt; out) throws Exception {
 *
 *     if (buf.readableBytes() &lt; 4) {
 *        return;
 *     }
 *
 *     buf.markReaderIndex();
 *     int length = buf.readInt();
 *
 *     if (buf.readableBytes() &lt; length) {
 *        buf.resetReaderIndex();
 *        return;
 *     }
 *
 *     out.add(buf.readBytes(length));
 *   }
 * }
 * </pre>
 * is simplified like the following with {@link ReplayingDecoder}:
 * <pre>
 * public class IntegerHeaderFrameDecoder
 *      extends {@link ReplayingDecoder}&lt;{@link Void}&gt; {
 *
 *   protected void decode({@link ChannelHandlerContext} ctx,
 *                           {@link ByteBuf} buf) throws Exception {
 *
 *     out.add(buf.readBytes(buf.readInt()));
 *   }
 * }
 * </pre>
 *
 * <h3>How does this work?</h3>
 * <p>
 * {@link ReplayingDecoder} passes a specialized {@link ByteBuf}
 * implementation which throws an {@link Error} of certain type when there's not
 * enough data in the buffer.  In the {@code IntegerHeaderFrameDecoder} above,
 * you just assumed that there will be 4 or more bytes in the buffer when
 * you call {@code buf.readInt()}.  If there's really 4 bytes in the buffer,
 * it will return the integer header as you expected.  Otherwise, the
 * {@link Error} will be raised and the control will be returned to
 * {@link ReplayingDecoder}.  If {@link ReplayingDecoder} catches the
 * {@link Error}, then it will rewind the {@code readerIndex} of the buffer
 * back to the 'initial' position (i.e. the beginning of the buffer) and call
 * the {@code decode(..)} method again when more data is received into the
 * buffer.
 * ReplayingDecoder传递了一个专门的ByteBuf实现，当缓冲区中的数据不足时，该实现将引发某种类型的Error。
 * 在上面的IntegerHeaderFrameDecoder中，您仅假设调用buf.readInt（）时缓冲区中将有4个或更多字节。
 * 如果缓冲区中确实有4个字节，它将返回您期望的整数头。否则，将引发Error并将控件返回给ReplayingDecoder。
 * 如果ReplayingDecoder捕获到错误，则它将把缓冲区的readerIndex倒回到“初始”位置（即缓冲区的开头），
 * 并在缓冲区中接收到更多数据时再次调用encode（..）方法。
 * <p>
 * Please note that {@link ReplayingDecoder} always throws the same cached
 * {@link Error} instance to avoid the overhead of creating a new {@link Error}
 * and filling its stack trace for every throw.
 * 请注意，ReplayingDecoder总是抛出相同的缓存Error实例，以避免创建新Error并为每次抛出填充其堆栈跟踪的开销。
 *
 * <h3>Limitations</h3>
 * <p>
 * At the cost of the simplicity, {@link ReplayingDecoder} enforces you a few
 * limitations:
 * 以简单为代价，ReplayingDecoder强制您执行一些限制：
 * <ul>
 * <li>Some buffer operations are prohibited.</li>
 * 禁止某些缓冲区操作。
 * <li>Performance can be worse if the network is slow and the message
 *     format is complicated unlike the example above.  In this case, your
 *     decoder might have to decode the same part of the message over and over
 *     again.</li>
 *     如果网络速度慢并且消息格式复杂，则性能可能会变差，与上面的示例不同。在这种情况下，您的解码器可能不得不一遍又一遍地解码消息的相同部分。
 * <li>You must keep in mind that {@code decode(..)} method can be called many
 *     times to decode a single message.  For example, the following code will
 *     not work:
 *     您必须记住，可以多次调用encode（..）方法来解码一条消息。例如，以下代码将不起作用：
 * <pre> public class MyDecoder extends {@link ReplayingDecoder}&lt;{@link Void}&gt; {
 *
 *   private final Queue&lt;Integer&gt; values = new LinkedList&lt;Integer&gt;();
 *
 *   {@code @Override}
 *   public void decode(.., {@link ByteBuf} buf, List&lt;Object&gt; out) throws Exception {
 *
 *     // A message contains 2 integers.
 *     values.offer(buf.readInt());
 *     values.offer(buf.readInt());
 *
 *     // This assertion will fail intermittently since values.offer()
 *     // can be called more than two times!
 *     assert values.size() == 2;
 *     out.add(values.poll() + values.poll());
 *   }
 * }</pre>
 *      The correct implementation looks like the following, and you can also
 *      utilize the 'checkpoint' feature which is explained in detail in the
 *      next section.
 *      正确的实现如下所示，并且您还可以利用“检查点”功能，下一部分将对其进行详细说明。
 * <pre> public class MyDecoder extends {@link ReplayingDecoder}&lt;{@link Void}&gt; {
 *
 *   private final Queue&lt;Integer&gt; values = new LinkedList&lt;Integer&gt;();
 *
 *   {@code @Override}
 *   public void decode(.., {@link ByteBuf} buf, List&lt;Object&gt; out) throws Exception {
 *
 *     // Revert the state of the variable that might have been changed
 *     // since the last partial decode.
 *     values.clear();
 *
 *     // A message contains 2 integers.
 *     values.offer(buf.readInt());
 *     values.offer(buf.readInt());
 *
 *     // Now we know this assertion will never fail.
 *     assert values.size() == 2;
 *     out.add(values.poll() + values.poll());
 *   }
 * }</pre>
 *     </li>
 * </ul>
 *
 * <h3>Improving the performance</h3>
 * <p>
 * Fortunately, the performance of a complex decoder implementation can be
 * improved significantly with the {@code checkpoint()} method.  The
 * {@code checkpoint()} method updates the 'initial' position of the buffer so
 * that {@link ReplayingDecoder} rewinds the {@code readerIndex} of the buffer
 * to the last position where you called the {@code checkpoint()} method.
 * 幸运的是，使用checkpoint（）方法可以显着提高复杂解码器实现的性能。checkpoint（）方法更新缓冲区的“初始”位置，
 * 以便ReplayingDecoder将缓冲区的readerIndex倒退到调用checkpoint（）方法的最后位置。
 *
 * <h4>Calling {@code checkpoint(T)} with an {@link Enum}</h4>
 * <p>
 * Although you can just use {@code checkpoint()} method and manage the state
 * of the decoder by yourself, the easiest way to manage the state of the
 * decoder is to create an {@link Enum} type which represents the current state
 * of the decoder and to call {@code checkpoint(T)} method whenever the state
 * changes.  You can have as many states as you want depending on the
 * complexity of the message you want to decode:
 *尽管您可以只使用checkpoint（）方法并自己管理解码器的状态，但是管理解码器状态的最简单方法是创建一个Enum类型来表示
 * 解码器的当前状态并调用checkpoint（T）状态改变时的方法。您可以根据需要解码的消息的复杂性，根据需要设置任意多个状态：
 * <pre>
 * public enum MyDecoderState {
 *   READ_LENGTH,
 *   READ_CONTENT;
 * }
 *
 * public class IntegerHeaderFrameDecoder
 *      extends {@link ReplayingDecoder}&lt;<strong>MyDecoderState</strong>&gt; {
 *
 *   private int length;
 *
 *   public IntegerHeaderFrameDecoder() {
 *     // Set the initial state.
 *     <strong>super(MyDecoderState.READ_LENGTH);</strong>
 *   }
 *
 *   {@code @Override}
 *   protected void decode({@link ChannelHandlerContext} ctx,
 *                           {@link ByteBuf} buf, List&lt;Object&gt; out) throws Exception {
 *     switch (state()) {
 *     case READ_LENGTH:
 *       length = buf.readInt();
 *       <strong>checkpoint(MyDecoderState.READ_CONTENT);</strong>
 *     case READ_CONTENT:
 *       ByteBuf frame = buf.readBytes(length);
 *       <strong>checkpoint(MyDecoderState.READ_LENGTH);</strong>
 *       out.add(frame);
 *       break;
 *     default:
 *       throw new Error("Shouldn't reach here.");
 *     }
 *   }
 * }
 * </pre>
 *
 * <h4>Calling {@code checkpoint()} with no parameter</h4>
 * <p>
 * An alternative way to manage the decoder state is to manage it by yourself.
 * 管理解码器状态的另一种方法是自己管理。
 * <pre>
 * public class IntegerHeaderFrameDecoder
 *      extends {@link ReplayingDecoder}&lt;<strong>{@link Void}</strong>&gt; {
 *
 *   <strong>private boolean readLength;</strong>
 *   private int length;
 *
 *   {@code @Override}
 *   protected void decode({@link ChannelHandlerContext} ctx,
 *                           {@link ByteBuf} buf, List&lt;Object&gt; out) throws Exception {
 *     if (!readLength) {
 *       length = buf.readInt();
 *       <strong>readLength = true;</strong>
 *       <strong>checkpoint();</strong>
 *     }
 *
 *     if (readLength) {
 *       ByteBuf frame = buf.readBytes(length);
 *       <strong>readLength = false;</strong>
 *       <strong>checkpoint();</strong>
 *       out.add(frame);
 *     }
 *   }
 * }
 * </pre>
 *
 * <h3>Replacing a decoder with another decoder in a pipeline</h3>用流水线中的另一个解码器替换一个解码器
 * <p>
 * If you are going to write a protocol multiplexer, you will probably want to
 * replace a {@link ReplayingDecoder} (protocol detector) with another
 * {@link ReplayingDecoder}, {@link ByteToMessageDecoder} or {@link MessageToMessageDecoder}
 * (actual protocol decoder).
 * It is not possible to achieve this simply by calling
 * {@link ChannelPipeline#replace(ChannelHandler, String, ChannelHandler)}, but
 * some additional steps are required:
 * 如果要编写协议多路复用器，则可能要用另一个ReplayingDecoder，ByteToMessageDecoder或MessageToMessageDecoder（实际协议解码器）
 * 替换ReplayingDecoder（协议检测器）。不能仅通过调用ChannelPipeline.replace（ChannelHandler，String，ChannelHandler）来实现此目的，
 * 但是需要一些其他步骤：
 * <pre>
 * public class FirstDecoder extends {@link ReplayingDecoder}&lt;{@link Void}&gt; {
 *
 *     {@code @Override}
 *     protected void decode({@link ChannelHandlerContext} ctx,
 *                             {@link ByteBuf} buf, List&lt;Object&gt; out) {
 *         ...
 *         // Decode the first message
 *         Object firstMessage = ...;
 *
 *         // Add the second decoder
 *         ctx.pipeline().addLast("second", new SecondDecoder());
 *
 *         if (buf.isReadable()) {
 *             // Hand off the remaining data to the second decoder
 *             out.add(firstMessage);
 *             out.add(buf.readBytes(<b>super.actualReadableBytes()</b>));
 *         } else {
 *             // Nothing to hand off
 *             out.add(firstMessage);
 *         }
 *         // Remove the first decoder (me)
 *         ctx.pipeline().remove(this);
 *     }
 * </pre>
 * @param <S>
 *        the state type which is usually an {@link Enum}; use {@link Void} if state management is
 *        unused
 */
public abstract class ReplayingDecoder<S> extends ByteToMessageDecoder {

    static final Signal REPLAY = Signal.valueOf(ReplayingDecoder.class, "REPLAY");

    private final ReplayingDecoderByteBuf replayable = new ReplayingDecoderByteBuf();
    private S state;
    private int checkpoint = -1;

    /**
     * Creates a new instance with no initial state (i.e: {@code null}).
     */
    protected ReplayingDecoder() {
        this(null);
    }

    /**
     * Creates a new instance with the specified initial state.
     */
    protected ReplayingDecoder(S initialState) {
        state = initialState;
    }

    /**
     * Stores the internal cumulative buffer's reader position.
     */
    protected void checkpoint() {
        checkpoint = internalBuffer().readerIndex();
    }

    /**
     * Stores the internal cumulative buffer's reader position and updates
     * the current decoder state.
     */
    protected void checkpoint(S state) {
        checkpoint();
        state(state);
    }

    /**
     * Returns the current state of this decoder.
     * @return the current state of this decoder
     */
    protected S state() {
        return state;
    }

    /**
     * Sets the current state of this decoder.
     * @return the old state of this decoder
     */
    protected S state(S newState) {
        S oldState = state;
        state = newState;
        return oldState;
    }

    @Override
    final void channelInputClosed(ChannelHandlerContext ctx, List<Object> out) throws Exception {
        try {
            replayable.terminate();
            if (cumulation != null) {
                callDecode(ctx, internalBuffer(), out);
            } else {
                replayable.setCumulation(Unpooled.EMPTY_BUFFER);
            }
            decodeLast(ctx, replayable, out);
        } catch (Signal replay) {
            // Ignore
            replay.expect(REPLAY);
        }
    }

    @Override
    protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        replayable.setCumulation(in);
        try {
            while (in.isReadable()) {
                int oldReaderIndex = checkpoint = in.readerIndex();
                int outSize = out.size();

                if (outSize > 0) {
                    fireChannelRead(ctx, out, outSize);
                    out.clear();

                    // Check if this handler was removed before continuing with decoding.
                    // If it was removed, it is not safe to continue to operate on the buffer.
                    //
                    // See:
                    // - https://github.com/netty/netty/issues/4635
                    if (ctx.isRemoved()) {
                        break;
                    }
                    outSize = 0;
                }

                S oldState = state;
                int oldInputLength = in.readableBytes();
                try {
                    decodeRemovalReentryProtection(ctx, replayable, out);

                    // Check if this handler was removed before continuing the loop.
                    // If it was removed, it is not safe to continue to operate on the buffer.
                    //
                    // See https://github.com/netty/netty/issues/1664
                    if (ctx.isRemoved()) {
                        break;
                    }

                    if (outSize == out.size()) {
                        if (oldInputLength == in.readableBytes() && oldState == state) {
                            throw new DecoderException(
                                    StringUtil.simpleClassName(getClass()) + ".decode() must consume the inbound " +
                                    "data or change its state if it did not decode anything.");
                        } else {
                            // Previous data has been discarded or caused state transition.
                            // Probably it is reading on.
                            continue;
                        }
                    }
                } catch (Signal replay) {
                    replay.expect(REPLAY);

                    // Check if this handler was removed before continuing the loop.
                    // If it was removed, it is not safe to continue to operate on the buffer.
                    //
                    // See https://github.com/netty/netty/issues/1664
                    if (ctx.isRemoved()) {
                        break;
                    }

                    // Return to the checkpoint (or oldPosition) and retry.
                    int checkpoint = this.checkpoint;
                    if (checkpoint >= 0) {
                        in.readerIndex(checkpoint);
                    } else {
                        // Called by cleanup() - no need to maintain the readerIndex
                        // anymore because the buffer has been released already.
                    }
                    break;
                }

                if (oldReaderIndex == in.readerIndex() && oldState == state) {
                    throw new DecoderException(
                           StringUtil.simpleClassName(getClass()) + ".decode() method must consume the inbound data " +
                           "or change its state if it decoded something.");
                }
                if (isSingleDecode()) {
                    break;
                }
            }
        } catch (DecoderException e) {
            throw e;
        } catch (Exception cause) {
            throw new DecoderException(cause);
        }
    }
}
