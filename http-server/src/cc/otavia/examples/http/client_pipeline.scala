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

package cc.otavia.examples.http

import cc.otavia.buffer.pool.AdaptiveBuffer
import cc.otavia.core.actor.{MainActor, SocketChannelsActor}
import cc.otavia.core.channel.{Channel, ChannelHandlerAdapter, ChannelHandlerContext}
import cc.otavia.core.message.Notice
import cc.otavia.core.stack.helper.{ChannelFutureState, FutureState, StartState}
import cc.otavia.core.stack.{NoticeStack, StackState}
import cc.otavia.handler.codec.ByteToByteCodec

import java.net.{InetSocketAddress, SocketAddress}
import java.nio.ByteBuffer
import java.nio.channels.{SelectionKey, Selector, SocketChannel}

@main def client_pipeline(): Unit = {
    val request = """GET /json HTTP/1.1
                    |Host: server
                    |User-Agent: Mozilla/5.0 (X11; Linux x86_64) Gecko/20130501 Firefox/30.0 AppleWebKit/600.00 Chrome/30.0.0000.0 Trident/10.0 Safari/600.00
                    |Cookie: uid=12345678901234567890; __utma=1.1234567890.1234567890.1234567890.1234567890.12; wd=2560x1600
                    |Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
                    |Accept-Language: en-US,en;q=0.5
                    |Connection: keep-alive
                    |
                    |GET /scale_message?length=1 HTTP/1.1
                    |Content-Type: application/json
                    |User-Agent: PostmanRuntime/7.35.0
                    |Accept: */*
                    |Postman-Token: 8be05336-7951-4e06-8af7-1c65c1867575
                    |Host: localhost:80
                    |Accept-Encoding: gzip, deflate, br
                    |Connection: keep-alive
                    |Content-Length: 27
                    |
                    |{"name": "Tom ", "age": 30}GET /plaintext HTTP/1.1
                    |Host: server
                    |User-Agent: Mozilla/5.0 (X11; Linux x86_64) Gecko/20130501 Firefox/30.0 AppleWebKit/600.00 Chrome/30.0.0000.0 Trident/10.0 Safari/600.00
                    |Cookie: uid=12345678901234567890; __utma=1.1234567890.1234567890.1234567890.1234567890.12; wd=2560x1600
                    |Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
                    |Accept-Language: en-US,en;q=0.5
                    |Connection: keep-alive
                    |
                    |""".stripMargin * 1500000
    val selector = Selector.open()
    val channel  = SocketChannel.open()
    channel.connect(new InetSocketAddress("localhost", 80))
    channel.configureBlocking(false)

    channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE)
    val data = ByteBuffer.wrap(request.getBytes())
    val recv = ByteBuffer.allocateDirect(409600)

    var continue = true
    while (true) {
        selector.select()
        val iter = selector.selectedKeys().iterator()
        while (iter.hasNext) {
            val key = iter.next()
            iter.remove()
            var op = 0
            if (key.isReadable) {
                val read = channel.read(recv)
                println(s"read ${read}")
                op = read
                recv.clear()
            }
            if (key.isWritable) {
                val write = channel.write(data)
                if (data.remaining() == 0) key.interestOps(SelectionKey.OP_READ)
                println(s"write ${write}")
                op += write
            }
            continue = op != 0 && data.remaining() == 0
        }
    }

    Thread.sleep(100000)
    // channel.close()
}
