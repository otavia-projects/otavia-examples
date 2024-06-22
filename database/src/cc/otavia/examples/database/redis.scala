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

package cc.otavia.examples.database

import cc.otavia.core.actor.ChannelsActor.ChannelEstablished
import cc.otavia.core.actor.SocketChannelsActor.ConnectChannel
import cc.otavia.core.actor.{MainActor, MessageOf}
import cc.otavia.core.address.Address
import cc.otavia.core.stack.helper.{FutureState, StartState}
import cc.otavia.core.stack.{NoticeStack, StackState, StackYield}
import cc.otavia.core.system.ActorSystem
import cc.otavia.redis.Client
import cc.otavia.redis.cmd.{Auth, OK}

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import scala.language.unsafeNulls

private class MainRedis(host: String, port: Int, password: String, database: Int)
    extends MainActor(Array(host, port.toString, password, database.toString)) {

    private var client: Address[MessageOf[Client]] = _

    override def main0(stack: NoticeStack[MainActor.Args]): StackYield =
        stack.state match
            case _: StartState =>
                client = system.buildActor(() => new Client())
                val state = FutureState[ChannelEstablished]()
                client.ask(ConnectChannel(new InetSocketAddress(host, port), None), state.future)
                stack.suspend(state)
            case state: FutureState[ChannelEstablished] if state.id == 0 =>
                logger.info("redis connected")
                val state = FutureState[OK]()
                client.ask(Auth(password), state.future)
                stack.suspend(state)
            case state: FutureState[OK] =>
                if (state.future.isFailed) state.future.causeUnsafe.printStackTrace()
                stack.`return`()

}

@main def redis(host: String, port: Int, password: String, database: Int): Unit =
    val system = ActorSystem()
    system.buildActor(() => new MainRedis(host, port, password, database))
