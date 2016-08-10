package de.aktey.akka.visualmailbox.packing

import java.nio.charset.Charset

import de.aktey.akka.visualmailbox.{MetricEnvelope, VisualMailboxMetric}

import scala.annotation.tailrec
import scala.collection.generic.CanBuildFrom
import scala.collection.immutable._
import scala.collection.mutable
import scala.util.{Failure, Success, Try}


trait Packer[-A] {
  def pack(a: A): Array[Byte]
}

trait Unpacker[A] {
  def unpack(b: Array[Byte]): Try[(A, Array[Byte])]
}

object Packing {
  def pack[A: Packer](a: A): Array[Byte] = implicitly[Packer[A]].pack(a)

  def unpack[A: Unpacker](b: Array[Byte]): Try[A] = implicitly[Unpacker[A]].unpack(b).map(_._1)

  def iunpack[A: Unpacker](b: Array[Byte]): Try[(A, Array[Byte])] = implicitly[Unpacker[A]].unpack(b)
}

trait Packers {
  def charSet: Charset

  implicit object IntPacker extends Packer[Int] {
    override def pack(a: Int): Array[Byte] = {
      Array((a >>> 24).toByte, (a >>> 16).toByte, (a >>> 8).toByte, a.toByte)
    }
  }

  implicit object StringPacker extends Packer[String] {
    override def pack(a: String): Array[Byte] = {
      val packed = a.getBytes(charSet)
      Packing.pack(packed.size) ++ packed
    }
  }

  implicit object EnvelopPacker extends Packer[MetricEnvelope] {
    override def pack(a: MetricEnvelope): Array[Byte] = {
      Packing.pack(a.version) ++ a.payload
    }
  }

  implicit object VisualMailboxMetricPacker extends Packer[VisualMailboxMetric] {
    override def pack(a: VisualMailboxMetric): Array[Byte] =
      Packing.pack(a.receiver) ++
        Packing.pack(a.sender) ++
        Packing.pack(a.receiverMailBoxSize.toString) ++
        Packing.pack(a.meassureTimeMillies.toString)
  }

  implicit def traversablePacker[A, T](implicit ev: T <:< Traversable[A], apacker: Packer[A]): Packer[T] = new Packer[T] {
    override def pack(t: T): Array[Byte] =
      t.foldLeft(Packing.pack(t.size)) {
        (bytes, elem) => bytes ++ Packing.pack(elem)
      }
  }

  implicit def mapPacker[A, B](implicit tpacker: Packer[(A, B)]): Packer[Map[A, B]] = new Packer[Map[A, B]] {
    override def pack(t: Map[A, B]): Array[Byte] =
      t.foldLeft(Packing.pack(t.size)) {
        (bytes, elem) => bytes ++ tpacker.pack(elem)
      }
  }

  implicit def tuple2Packer[A, B](implicit apacker: Packer[A], bpacker: Packer[B]): Packer[(A, B)] = new Packer[(A, B)] {
    override def pack(t: (A, B)): Array[Byte] = apacker.pack(t._1) ++ bpacker.pack(t._2)
  }
}

trait Unpackers {
  def charSet: Charset

  implicit object IntUnpacker extends Unpacker[Int] {
    override def unpack(bytes: Array[Byte]): Try[(Int, Array[Byte])] = Try {
      val intBytes = bytes.take(4)
      val i = (intBytes(0) << 24) | (intBytes(1) << 24 >>> 8) | (intBytes(2) << 24 >>> 16) | (intBytes(3) << 24 >>> 24)
      (i, bytes.drop(4))
    }
  }

  implicit object StringUnpacker extends Unpacker[String] {
    override def unpack(bytes: Array[Byte]): Try[(String, Array[Byte])] =
      Packing.iunpack[Int](bytes).flatMap {
        case (size, rest) => Try {
          val strBytes = rest.take(size)
          (new String(strBytes, charSet), rest.drop(size))
        }
      }
  }

  implicit object EnvelopeUnpacker extends Unpacker[MetricEnvelope] {
    override def unpack(bytes: Array[Byte]): Try[(MetricEnvelope, Array[Byte])] =
      Packing.iunpack[Int](bytes).map {
        case (version, rest) => (MetricEnvelope(version, rest), Array.empty[Byte])
      }
  }

  implicit def traversableUnpacker[A, T](implicit ev: T <:< Traversable[A], bf: CanBuildFrom[List[A], A, T], aunpacker: Unpacker[A]) = new Unpacker[T] {
    override def unpack(bytes: Array[Byte]): Try[(T, Array[Byte])] =
      Packing.iunpack[Int](bytes).flatMap {
        case (size, rest) => unpackRec(bf(), size, rest)
      }

    @tailrec
    private def unpackRec(b: mutable.Builder[A, T], n: Int, bytes: Array[Byte]): Try[(T, Array[Byte])] = {
      n match {
        case 0 =>
          Success((b.result(), bytes))
        case _ =>
          Packing.iunpack[A](bytes) match {
            case Failure(f) => Failure(f)
            case Success((elem, rest)) => unpackRec(b += elem, n - 1, rest)
          }
      }
    }
  }

  implicit object VisualMailboxMetricUnpacker extends Unpacker[VisualMailboxMetric] {
    override def unpack(bytes: Array[Byte]): Try[(VisualMailboxMetric, Array[Byte])] = for {
      (receiver, r1) <- Packing.iunpack[String](bytes)
      (sender, r2) <- Packing.iunpack[String](r1)
      (receiverMailBoxSize, r3) <- Packing.iunpack[String](r2)
      (meassureTimeMillies, r) <- Packing.iunpack[String](r3)
    } yield (VisualMailboxMetric(sender, receiver, receiverMailBoxSize.toInt, meassureTimeMillies.toLong), r)
  }

  implicit def mapUnpacker[A, B](implicit ev: Unpacker[Traversable[(A, B)]], tunpacker: Unpacker[(A, B)]) = new Unpacker[Map[A, B]] {
    override def unpack(bytes: Array[Byte]): Try[(Map[A, B], Array[Byte])] =
      for ((t, rest) <- Packing.iunpack[Traversable[(A, B)]](bytes)) yield (t.toMap, rest)
  }

  implicit def tuple2Unpacker[A, B](implicit aunpacker: Unpacker[A], bunpacker: Unpacker[B]): Unpacker[(A, B)] = new Unpacker[(A, B)] {
    override def unpack(bytes: Array[Byte]): Try[((A, B), Array[Byte])] = for {
      (a, r1) <- Packing.iunpack[A](bytes)
      (b, r2) <- Packing.iunpack[B](r1)
    } yield ((a, b), r2)
  }
}
