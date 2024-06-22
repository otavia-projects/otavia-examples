package cc.otavia.demo.model

import cc.otavia.json.JsonSerde
import cc.otavia.sql.{Row, RowDecoder}

/** The model for the "fortune" database table. */
case class Fortune(id: Int, message: String) extends Row derives RowDecoder, JsonSerde
