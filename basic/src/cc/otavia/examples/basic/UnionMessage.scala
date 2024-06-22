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

import cc.otavia.core.actor.{MessageOf, StateActor}
import cc.otavia.core.address.Address
import cc.otavia.core.message.{Ask, Notice, Reply}
import cc.otavia.core.stack.helper.{FutureState, StartState}
import cc.otavia.core.stack.{AskStack, NoticeStack, StackState}
import cc.otavia.core.system.ActorSystem
import cc.otavia.core.stack.StackYield

object UnionMessage {

    def main(args: Array[String]): Unit = {
        val system    = ActorSystem()
        val pongActor = system.buildActor(() => new PongActor())
        val pingActor = system.buildActor(() => new PingActor(pongActor))
        pingActor.notice(Start(true))
        pingActor.notice(Start(false))
    }

    private case class Pong()                 extends Reply
    private case class World()                extends Reply
    private case class Ping()                 extends Ask[Pong]
    private case class Hello()                extends Ask[World]
    private case class Start(toggle: Boolean) extends Notice

    private class PingActor(val pongActor: Address[MessageOf[PongActor]]) extends StateActor[Start] {

        override def resumeNotice(stack: NoticeStack[Start]): StackYield = handleStart(stack)

        private def handleStart(stack: NoticeStack[Start]): StackYield = {
            stack.state match
                case _: StartState =>
                    if (stack.notice.toggle) {
                        val state = FutureState[World](0)
                        pongActor.ask(Hello(), state.future)
                        stack.suspend(state)
                    } else {
                        val state = FutureState[Pong](1)
                        pongActor.ask(Ping(), state.future)
                        stack.suspend(state)
                    }
                case state: FutureState[World] if state.id == 0 =>
                    val world = state.future.getNow
                    println(s"get world ${world}")
                    stack.`return`()
                case state: FutureState[Pong] if state.id == 1 =>
                    val pong = state.future.getNow
                    println(s"get pong ${pong}")
                    stack.`return`()
        }

    }

    private class PongActor extends StateActor[Ping | Hello] {

        override def resumeAsk(stack: AskStack[Ping | Hello]): StackYield = stack match
            case s: AskStack[Ping] if s.ask.isInstanceOf[Ping]   => handlePing(s)
            case s: AskStack[Hello] if s.ask.isInstanceOf[Hello] => handleHello(s)

        private def handlePing(stack: AskStack[Ping]): StackYield = {
            stack.`return`(Pong())
        }

        private def handleHello(stack: AskStack[Hello]): StackYield = {
            stack.`return`(World())
        }

    }

}
