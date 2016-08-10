package de.aktey.akka.visualmailbox.packing

import de.aktey.akka.visualmailbox.{MetricEnvelope, VisualMailboxMetric}
import org.scalacheck.Prop._
import org.scalacheck.{Arbitrary, Gen, Properties}

object PackingCheck extends Properties("PackingCheck") {

  import Packing._

  def packUnpack[A](a: A)(implicit packer: Packer[A], unpacker: Unpacker[A]): Boolean =
    a == unpack[A](pack(a)).get

  implicit val VisualMailboxMetricGenerator: Arbitrary[VisualMailboxMetric] = Arbitrary(for {
    sender <- Arbitrary.arbString.arbitrary
    receiver <- Arbitrary.arbString.arbitrary
    receiverMailBoxSize <- Gen.choose(Int.MinValue, Int.MaxValue)
    meassureTimeMillies <- Gen.choose(Long.MinValue, Long.MaxValue)
  } yield VisualMailboxMetric(sender, receiver, receiverMailBoxSize, meassureTimeMillies))

  implicit val EnvelopeGenerator: Arbitrary[MetricEnvelope] = Arbitrary(for {
    version <- Gen.choose(Int.MinValue, Int.MaxValue)
    payload <- Gen.containerOf[Array, Byte](Gen.choose(Byte.MinValue, Byte.MaxValue))
  } yield MetricEnvelope(version, payload))

  property("int-packer") = forAll(Gen.choose(Int.MinValue, Int.MaxValue)) { a => packUnpack(a) }

  property("string-packer") = forAll { (a: String) => packUnpack(a) }

  property("envelope-packer") = forAll { a: MetricEnvelope =>
    val unpacked = unpack[MetricEnvelope](pack(a)).get
    unpacked.version == a.version &&
      unpacked.payload.toList == a.payload.toList
  }

  property("string-traversal-packer") = forAll { (a: List[String]) => packUnpack(a) }

  property("visual-mailbox-metric-packer") = forAll { (a: VisualMailboxMetric) => packUnpack(a) }

  property("visual-mailbox-metric-traversal-packer") = forAll { (a: List[VisualMailboxMetric]) => packUnpack(a) }

  property("visual-mailbox-metric-tuple2-packer") = forAll { (a: (String, Int)) => packUnpack(a) }

  property("visual-mailbox-metric-map-packer") = forAll { (a: Map[String, Int]) => packUnpack(a) }

}
