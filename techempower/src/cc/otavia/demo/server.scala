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
            val singleQueryRequestFactory = new HttpRequestFactory() {
                override def createHttpRequest(): HttpRequest[?, ?] = new SingleQueryRequest()
            }
            val multipleQueryRequestFactory = new HttpRequestFactory() {
                override def createHttpRequest(): HttpRequest[?, ?] = new MultipleQueryRequest()
            }
            val updateRequestFactory = new HttpRequestFactory() {
                override def createHttpRequest(): HttpRequest[?, ?] = new UpdateRequest()
            }
            val fortuneRequestFactory = new HttpRequestFactory {
                override def createHttpRequest(): HttpRequest[?, ?] = new FortuneRequest()
            }
            val fortunesResponseSerde = new HttpResponseSerde[String](StringSerde.utf8, MediaType.TEXT_HTML_UTF8)
            val worldResponseSerde    = new HttpResponseSerde[World](summon[JsonSerde[World]], MediaType.APP_JSON)
            val worldsResponseSerde =
                new HttpResponseSerde[Seq[World]](JsonSerde.derived[Seq[World]], MediaType.APP_JSON)
            val dbController      = autowire[DBController]()
            val fortuneController = autowire[FortuneController]()
            val routers = Seq(
              constant[Array[Byte]](GET, "/plaintext", "Hello, World!".getBytes(UTF_8), BytesSerde, TEXT_PLAIN_UTF8),
              constant[Message](GET, "/json", Message("Hello, World!"), summon[JsonSerde[Message]], APP_JSON),
              get("/db", dbController, singleQueryRequestFactory, worldResponseSerde),
              get("/queries", dbController, multipleQueryRequestFactory, worldsResponseSerde),
              get("/updates", dbController, updateRequestFactory, worldsResponseSerde),
              get("/fortunes", fortuneController, fortuneRequestFactory, fortunesResponseSerde)
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
