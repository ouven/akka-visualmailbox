package de.aktey.akka.visualmailbox.packing

import de.aktey.akka.visualmailbox.VisualMailboxMetric
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen, Properties}

object PackingCheck extends Properties("PackingCheck") {

  import Packing._

  def packUnpack[A](a: A)(implicit packer: Packer[A], unpacker: Unpacker[A]): Boolean =
    a == unpack[A](pack(a)).get

  implicit val VisualMailboxMetricGenerator: Arbitrary[VisualMailboxMetric] = Arbitrary(for {
    sender <- Gen.alphaStr
    receiver <- Gen.alphaStr
    receiverMailBoxSize <- Gen.choose(Int.MinValue, Int.MaxValue)
    meassureTimeMillies <- Gen.choose(Long.MinValue, Long.MaxValue)
  } yield VisualMailboxMetric(sender, receiver, receiverMailBoxSize, meassureTimeMillies))

  property("string-packer") = forAll(Gen.alphaStr) { (a: String) => packUnpack(a) }

  property("string-traversal-packer") = forAll(Gen.listOfN(100, Gen.alphaStr)) { (a: List[String]) => packUnpack(a) }

  property("visual-mailbox-metric-packer") = forAll { (a: VisualMailboxMetric) => packUnpack(a) }

  property("visual-mailbox-metric-traversal-packer") = forAll { (a: List[VisualMailboxMetric]) => packUnpack(a) }

}
