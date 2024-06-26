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

import cc.otavia.core.actor.*
import cc.otavia.core.address.Address
import cc.otavia.core.message.{Ask, Notice, Reply, TimeoutReply}
import cc.otavia.core.stack.helper.*
import cc.otavia.core.stack.{AskStack, NoticeStack, StackYield}
import cc.otavia.core.system.ActorSystem

object AskTimeout {

    def main(args: Array[String]): Unit = {
        val system    = ActorSystem()
        val pongActor = system.buildActor(() => new PongActor())
        val pingActor = system.buildActor(() => new PingActor(pongActor))
        pingActor.notice(Start())
    }

    private case class Start() extends Notice
    private case class Pong()  extends Reply
    private case class Ping()  extends Ask[Pong]

    private class PingActor(val pongActor: Address[MessageOf[PongActor]]) extends StateActor[Start] {

        override def resumeNotice(stack: NoticeStack[Start]): StackYield = {
            stack.state match
                case _: StartState =>
                    val state = FutureState[Pong]()
                    pongActor.ask(Ping(), state.future, 1000) // max timeout time: 1000 millis
                    stack.suspend(state)
                case state: FutureState[Pong] =>
                    val timeout = state.future.isTimeout
                    println(s"timeout ${timeout}")
                    stack.`return`()
        }

    }

    private class PongActor extends StateActor[Ping] {

        override def resumeAsk(stack: AskStack[Ping]): StackYield = {
            stack.state match
                case _: StartState =>
                    val state = FutureState[TimeoutReply]()
                    timer.sleepStack(state.future, 2 * 1000) // sleep the current stack 2 seconds
                    stack.suspend(state)
                case _: FutureState[?] =>
                    stack.`return`(Pong())
        }

    }

}
