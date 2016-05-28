package de.aktey.akka.visualmailbox

import java.nio.charset.StandardCharsets

/**
  * Created by ruben on 18.05.16.
  */
package object packing extends Packers with Unpackers {

  val charSet = StandardCharsets.UTF_8

  implicit class ExtStr(val s: String) extends AnyVal {
    def splitColon: (String, String) = {
      val (pr, su) = s.splitAt(s.indexOf(':'))
      (pr, su.substring(1))
    }
  }

}
