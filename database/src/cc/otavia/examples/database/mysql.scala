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

import cc.otavia.core.actor.{ChannelsActor, MainActor, SocketChannelsActor}
import cc.otavia.core.stack.StackState.{FutureState, start}
import cc.otavia.core.stack.{NoticeStack, StackState}
import cc.otavia.core.system.ActorSystem
import cc.otavia.sql.Connection
import cc.otavia.sql.Connection.Connect

@main def start(url: String, username: String, password: String): Unit =
    val system = ActorSystem()
    system.buildActor(() =>
        new MainActor(Array.empty) {
            override def main0(stack: NoticeStack[MainActor.Args]): Option[StackState] = stack.state match
                case StackState.start =>
                    val connection = this.system.buildActor(() => new Connection())
                    connection.ask(Connect(url, username, password)).suspend()
                case state: FutureState[?] =>
                    if (!state.future.isSuccess)
                        state.future.causeUnsafe.printStackTrace()
                    stack.`return`()
        }
    )
