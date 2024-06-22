package cc.otavia.demo.view

import cc.otavia.demo.model.Fortune
import scalatags.Text
import scalatags.Text.all.*

object Fortunes {
    def view(fortunes: Seq[Fortune]): String = {
        "<!DOCTYPE html>" + html(
          head(tag("title")("Fortunes")),
          body(
            table(
              tr(
                th("id"),
                th("message")
              ),
              fortunes.map(fortune => tr(td(fortune.id), td(fortune.message)))
            )
          )
        )
    }

}
