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
import cc.otavia.core.actor.{ChannelsActor, MainActor, MessageOf, SocketChannelsActor}
import cc.otavia.core.address.Address
import cc.otavia.core.stack.helper.{FutureState, StartState}
import cc.otavia.core.stack.{NoticeStack, StackYield}
import cc.otavia.core.system.ActorSystem
import cc.otavia.sql.Statement.ModifyRows
import cc.otavia.sql.{Authentication, Connection, Statement}

@main def mysql(url: String, username: String, password: String): Unit =
    val sys = ActorSystem()
    sys.buildActor(() =>
        new MainActor() {
            private var connection: Address[MessageOf[Connection]] = _
            override def main0(stack: NoticeStack[MainActor.Args]): StackYield = stack.state match
                case _: StartState =>
                    connection = this.system.buildActor(() => new Connection())
                    val state = FutureState[ChannelEstablished](0)
                    connection.ask(Authentication(url, username, password), state.future)
                    stack.suspend(state)
                case state: FutureState[?] =>
                    val newState = FutureState[ModifyRows](1)
                    connection.ask(
                      Statement.ExecuteUpdate("insert into test(id, double_id, str_id) values (1, 2, '1')"),
                      newState.future
                    )
                    stack.suspend(newState)
                case state: FutureState[ModifyRows] if state.id == 1 =>
                    stack.`return`()
        }
    )
