/*
 * Copyright 2022 Yan Kun <yan_kun_1992@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.otavia.examples

import cc.otavia.buffer.pool.AdaptiveBuffer
import cc.otavia.core.actor.{ChannelsActor, MainActor, SocketChannelsActor}
import cc.otavia.core.channel.message.FileReadPlan
import cc.otavia.core.channel.{Channel, ChannelAddress, ChannelHandlerContext}
import cc.otavia.core.message.*
import cc.otavia.core.stack.*
import cc.otavia.core.stack.helper.{ChannelFutureState, FutureState, StartState}
import cc.otavia.core.system.ActorSystem
import cc.otavia.handler.codec.*
import cc.otavia.handler.codec.string.LineSeparator

import java.io.File
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file.StandardOpenOption
import scala.collection.mutable.ArrayBuffer
import scala.language.unsafeNulls

object FileOps {

    def main(args: Array[String]): Unit = {
        val system = ActorSystem()

        val fileChannelActor = system.buildActor(() => new FileChannelActor(new File("build.sc")))

        system.buildActor(() =>
            new MainActor(args) {
                override def main0(stack: NoticeStack[MainActor.Args]): StackYield =
                    stack.state match
                        case _: StartState =>
                            val state = FutureState[ReadLinesReply]()
                            fileChannelActor.ask(ReadLines(), state.future)
                            stack.suspend(state)
                        case futureState: FutureState[ReadLinesReply] =>
                            for (elem <- futureState.future.getNow.lines) {
                                print(elem)
                            }
                            stack.`return`()
            }
        )

    }

    private case class ReadLinesReply(lines: Seq[String]) extends Reply
    private case class ReadLines()                        extends Ask[ReadLinesReply]

    private class FileChannelActor(file: File, charset: Charset = StandardCharsets.UTF_8)
        extends ChannelsActor[ReadLines] {

        override protected def initFileChannel(channel: Channel): Unit = {
            channel.pipeline.addFirst(new ReadLinesHandler(charset))
        }

        override def resumeAsk(stack: AskStack[ReadLines]): StackYield = {
            stack.state match {
                case _: StartState =>
                    stack.suspend(openFile(file, Seq(StandardOpenOption.READ), attrs = Seq.empty))
                case openState: ChannelFutureState if openState.id == 0 =>
                    val linesState = ChannelFutureState(1)
                    val channel    = openState.future.channel
                    channel.ask(FileReadPlan(-1, -1), linesState.future)
                    stack.suspend(linesState)
                case linesState: ChannelFutureState if linesState.id == 1 =>
                    stack.`return`(ReadLinesReply(linesState.future.getNow.asInstanceOf[Seq[String]]))
            }
        }

    }

    private class ReadLinesHandler(charset: Charset) extends ByteToMessageDecoder {

        private val lines              = ArrayBuffer.empty[String]
        private var currentMsgId: Long = -1

        override def write(ctx: ChannelHandlerContext, msg: AnyRef, msgId: Long): Unit = {
            msg match
                case fileReadPlan: FileReadPlan =>
                    ctx.read(fileReadPlan)
                    currentMsgId = msgId
                case _ =>
                    ctx.write(msg, msgId)
        }

        override protected def decode(ctx: ChannelHandlerContext, input: AdaptiveBuffer): Unit = {
            var continue = true
            while (continue) {
                val length = input.bytesBefore('\n'.toByte) + 1
                if (length != 0) {
                    lines.addOne(input.readCharSequence(length, charset).toString)
                } else continue = false
            }
        }

        override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
            val seq = lines.toSeq
            lines.clear()
            ctx.fireChannelRead(seq, currentMsgId)
        }

    }

}
