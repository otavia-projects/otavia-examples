package cc.otavia.demo

import cc.otavia.buffer.Buffer
import cc.otavia.core.actor.ChannelsActor.{Bind, ChannelEstablished}
import cc.otavia.core.actor.MainActor
import cc.otavia.core.slf4a.LoggerFactory
import cc.otavia.core.stack.helper.{FutureState, StartState}
import cc.otavia.core.stack.{NoticeStack, StackYield}
import cc.otavia.core.system.ActorSystem
import cc.otavia.demo.controller.*
import cc.otavia.demo.controller.DBController.*
import cc.otavia.demo.controller.FortuneController.*
import cc.otavia.demo.model.{Message, World}
import cc.otavia.http.HttpMethod.*
import cc.otavia.http.MediaType
import cc.otavia.http.MediaType.*
import cc.otavia.http.server.*
import cc.otavia.http.server.Router.*
import cc.otavia.json.JsonSerde
import cc.otavia.serde.helper.{BytesSerde, StringSerde}
import cc.otavia.sql.Connection

import java.nio.charset.StandardCharsets.*

private class ServerMain(val port: Int = 8080) extends MainActor(Array.empty) {

    override def main0(stack: NoticeStack[MainActor.Args]): StackYield = stack.state match
        case _: StartState =>
            val worldResponseSerde  = HttpResponseSerde.json(summon[JsonSerde[World]])
            val worldsResponseSerde = HttpResponseSerde.json(JsonSerde.derived[Seq[World]])

            val dbController      = autowire[DBController]()
            val fortuneController = autowire[FortuneController]()

            val routers = Seq(
              // Test 6: plaintext
              constant[Array[Byte]](GET, "/plaintext", "Hello, World!".getBytes(UTF_8), BytesSerde, TEXT_PLAIN_UTF8),
              // Test 1: JSON serialization
              constant[Message](GET, "/json", Message("Hello, World!"), summon[JsonSerde[Message]], APP_JSON),
              get("/db", dbController, () => new SingleQueryRequest(), worldResponseSerde),
              get("/queries", dbController, () => new MultipleQueryRequest(), worldsResponseSerde),
              get("/updates", dbController, () => new UpdateRequest(), worldsResponseSerde),
              get("/fortunes", fortuneController, () => new FortuneRequest(), HttpResponseSerde.stringHtml)
            )
            val server = system.buildActor(() => new HttpServer(system.actorWorkerSize, routers))
            val state  = FutureState[ChannelEstablished]()
            server.ask(Bind(port), state.future)
            stack.suspend(state)
        case state: FutureState[ChannelEstablished] =>
            if (state.future.isFailed) state.future.causeUnsafe.printStackTrace()
            logger.info(s"http server bind port $port success")
            stack.`return`()

}

@main def startup(url: String, user: String, password: String, poolSize: Int = 8): Unit =
    val system = ActorSystem()
    val logger = LoggerFactory.getLogger("server", system)
    logger.info("starting http server")
    system.buildActor(() => new Connection(url, user, password), global = true, num = poolSize)
    system.buildActor(() => new DBController(), global = true, num = system.actorWorkerSize)
    system.buildActor(() => new FortuneController(), global = true, num = system.actorWorkerSize)
    system.buildActor(() => new ServerMain())
