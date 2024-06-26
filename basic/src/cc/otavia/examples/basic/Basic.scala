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

package cc.otavia.examples.basic

import cc.otavia.core.actor.*
import cc.otavia.core.address.Address
import cc.otavia.core.message.{Ask, Notice, Reply}
import cc.otavia.core.stack.helper.{FutureState, StartState}
import cc.otavia.core.stack.{AskStack, MessageFuture, NoticeStack, StackState}
import cc.otavia.core.system.{ActorSystem, ActorThread}
import cc.otavia.core.timer.TimeoutTrigger
import cc.otavia.core.stack.StackYield

object Basic {

    // -XX:NewRatio=1 -XX:SurvivorRatio=8
    def main(args: Array[String]): Unit = {
        val system = ActorSystem()
        val start  = System.currentTimeMillis()
        for (id <- 0 until 200_000) {
            val pongActor = system.buildActor[PongActor](() => new PongActor())
            val pingActor = system.buildActor[PingActor](() => new PingActor(pongActor))
            for (idx <- 0 until 1_000) pingActor.notice(Start(idx))
        }
        val end = System.currentTimeMillis()

        println(s"main exit with ${end - start}")
    }

    private case class Start(id: Int) extends Notice
    private case class Ping()         extends Ask[Pong]
    private case class Pong()         extends Reply

    private class PingActor(val pongActor: Address[Ping]) extends StateActor[Start] {

        override def resumeNotice(stack: NoticeStack[Start]): StackYield =
            stack.state match {
                case _: StartState =>
                    val state = FutureState[Pong]()
                    pongActor.ask(Ping(), state.future)
                    stack.suspend(state)
                case state: FutureState[?] if state.id == 0 =>
                    val pong = state.future.asInstanceOf[MessageFuture[Pong]].getNow
                    stack.`return`()
            }

    }

    private class PongActor extends StateActor[Ping] {

        override def resumeAsk(stack: AskStack[Ping]): StackYield = {
            stack.`return`(Pong())
        }

    }

}
