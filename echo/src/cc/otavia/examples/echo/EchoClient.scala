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

package cc.otavia.examples.echo

import cc.otavia.buffer.pool.AdaptiveBuffer
import cc.otavia.core.actor.ChannelsActor.ChannelEstablished
import cc.otavia.core.actor.SocketChannelsActor.{Connect, ConnectChannel}
import cc.otavia.core.actor.{ChannelsActor, MainActor, SocketChannelsActor}
import cc.otavia.core.address.Address
import cc.otavia.core.channel.*
import cc.otavia.core.message.{Ask, Reply}
import cc.otavia.core.stack.helper.{ChannelFutureState, FutureState, StartState}
import cc.otavia.core.stack.{AskStack, ChannelFuture, NoticeStack, StackYield}
import cc.otavia.core.system.ActorSystem
import cc.otavia.handler.codec.MessageToByteEncoder

import java.net.{InetAddress, InetSocketAddress}
import java.nio.charset.{Charset, StandardCharsets}
import scala.language.unsafeNulls

object EchoClient {

    def main(args: Array[String]): Unit = {
        val system = ActorSystem()
        system.buildActor(() => new Main(args))
    }

    private class Main(args: Array[String]) extends MainActor(args) {

        private var clientActor: Address[ConnectChannel | Echo] = _

        override def main0(stack: NoticeStack[MainActor.Args]): StackYield =
            stack.state match
                case _: StartState =>
                    clientActor = system.buildActor(() => new ClientActor())
                    val state = FutureState[ChannelEstablished](1)
                    clientActor.ask(ConnectChannel(new InetSocketAddress("localhost", 8080), None), state.future)
                    stack.suspend(state)
                case state: FutureState[ChannelEstablished] if state.id == 1 =>
                    state.future.cause match
                        case Some(value) =>
                            value.printStackTrace()
                            stack.`return`()
                        case None =>
                            println("connected")
                            val state = FutureState[EchoReply](2)
                            clientActor.ask(Echo("hello otavia!"), state.future)
                            stack.suspend(state)
                case state: FutureState[EchoReply] if state.id == 2 =>
                    if (state.future.isSuccess) {
                        println(s"get echo reply: ${state.future.getNow.answer}")
                    }
                    stack.`return`()

    }

    private class ClientActor extends SocketChannelsActor[Echo | ConnectChannel] {

        private var channel: ChannelAddress = _

        override def handler: Option[ChannelInitializer[? <: Channel]] = Some(
          new ChannelInitializer[Channel] {
              override protected def initChannel(ch: Channel): Unit = ch.pipeline.addFirst(new ClientHandler())
          }
        )

        override def resumeAsk(stack: AskStack[Echo | ConnectChannel]): StackYield = {
            stack match
                case s: AskStack[?] if s.ask.isInstanceOf[Connect] =>
                    connect(s.asInstanceOf[AskStack[Connect]])
                case s: AskStack[?] if s.ask.isInstanceOf[Echo] => echo(s.asInstanceOf[AskStack[Echo]])
        }

        private def echo(stack: AskStack[Echo]): StackYield = {
            stack.state match
                case _: StartState =>
                    val state = ChannelFutureState()
                    channel.ask(stack.ask, state.future)
                    stack.suspend(state)
                case state: ChannelFutureState =>
                    val answer = state.future.getNow.asInstanceOf[String]
                    stack.`return`(EchoReply(answer))
        }

        override protected def afterConnected(channel: ChannelAddress): Unit = this.channel = channel

    }

    private case class Echo(question: String)    extends Ask[EchoReply]
    private case class EchoReply(answer: String) extends Reply

    private class ClientHandler extends MessageToByteEncoder {

        override protected def encode(
            ctx: ChannelHandlerContext,
            output: AdaptiveBuffer,
            msg: AnyRef,
            msgId: Long
        ): Unit =
            output.writeCharSequence(msg.asInstanceOf[Echo].question)

        override def channelRead(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {
            val buffer = ctx.inboundAdaptiveBuffer
            val sc     = buffer.readCharSequence(buffer.readableBytes, StandardCharsets.UTF_8)
            ctx.fireChannelRead(sc.toString)
        }

    }

}
