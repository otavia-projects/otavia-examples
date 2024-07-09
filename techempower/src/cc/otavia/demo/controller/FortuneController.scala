package cc.otavia.demo.controller

import cc.otavia.core.actor.{MessageOf, StateActor}
import cc.otavia.core.address.Address
import cc.otavia.core.stack.helper.{FutureState, StartState}
import cc.otavia.core.stack.{AskStack, StackState, StackYield}
import cc.otavia.demo.controller.FortuneController.*
import cc.otavia.demo.model.Fortune
import cc.otavia.demo.view.Fortunes
import cc.otavia.http.server.{HttpRequest, HttpResponse}
import cc.otavia.sql.Statement.PrepareQuery
import cc.otavia.sql.{Connection, RowSet}

class FortuneController extends StateActor[FortuneRequest] {

    private var connection: Address[MessageOf[Connection]] = _

    override protected def afterMount(): Unit = connection = autowire[Connection]()

    //  Test 4: Fortunes
    override protected def resumeAsk(stack: AskStack[FortuneRequest]): StackYield = {
        stack.state match
            case _: StartState =>
                val state = FutureState[RowSet[Fortune]]()
                connection.ask(PrepareQuery.fetchAll[Fortune](SELECT_FORTUNE), state.future)
                stack.suspend(state)
            case state: FutureState[RowSet[Fortune]] =>
                val fortunes = Fortune(0, "Additional fortune added at request time.") +: state.future.getNow.rows
                val html     = Fortunes.view(fortunes.sortBy(_.message))
                val response = HttpResponse.builder.setContent(html).build()
                stack.`return`(response)
    }

}

object FortuneController {

    class FortuneRequest extends HttpRequest[Nothing, HttpResponse[String]]

    private val SELECT_FORTUNE = "SELECT id, message from FORTUNE"

}
