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

package cc.otavia.examples.timer

import cc.otavia.core.actor.{ChannelsActor, MainActor, SocketChannelsActor}
import cc.otavia.core.message.TimeoutReply
import cc.otavia.core.stack.helper.*
import cc.otavia.core.stack.{NoticeStack, StackYield}
import cc.otavia.core.system.ActorSystem

object TimeoutInStack {

    def main(args: Array[String]): Unit = {
        val system = ActorSystem()
        system.buildActor(() => new TimeoutInStackActor(args))
    }

    private class TimeoutInStackActor(args: Array[String]) extends MainActor(args) {
        override def main0(stack: NoticeStack[MainActor.Args]): StackYield = {
            stack.state match
                case _: StartState =>
                    val state = FutureState[TimeoutReply]()
                    timer.sleepStack(state.future, 2000)
                    stack.suspend(state)
                case state: FutureState[?] =>
                    println(s"timeout event reach: ${state.future.getNow}")
                    stack.`return`()
        }

    }

}
