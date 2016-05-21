package de.aktey.akka.visualmailbox

/**
  * Created by ruben on 21.05.16.
  */
case class VisualMailboxMetric(sender: String,
                               receiver: String,
                               receiverMailBoxSize: Int,
                               meassureTimeMillies: Long = System.currentTimeMillis())
