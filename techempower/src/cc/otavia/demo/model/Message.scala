package cc.otavia.demo.model

import cc.otavia.json.JsonSerde

case class Message(message: String) derives JsonSerde
