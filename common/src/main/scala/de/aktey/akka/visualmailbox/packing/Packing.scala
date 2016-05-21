package de.aktey.akka.visualmailbox.packing

import java.nio.charset.Charset

import de.aktey.akka.visualmailbox.VisualMailboxMetric

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

  implicit object StringPacker extends Packer[String] {
    override def pack(a: String): Array[Byte] = {
      val packed = a.getBytes(charSet)
      val size = packed.size
      Array((size >> 24).toByte, (size >> 16).toByte, (size >> 8).toByte, size.toByte) ++ packed
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
      t.foldLeft(s"${t.size}:".getBytes(charSet)) {
        (bytes, elem) => bytes ++ Packing.pack(elem)
      }
  }
}

trait Unpackers {
  def charSet: Charset

  implicit object StringUnpacker extends Unpacker[String] {
    override def unpack(bytes: Array[Byte]): Try[(String, Array[Byte])] = Try {
      val sizeBytes = bytes.take(4)
      val size = (sizeBytes(0) << 24) + (sizeBytes(1) << 16) + (sizeBytes(2) << 8) + sizeBytes(3)
      val strBytes = bytes.slice(4, 4 + size)
      (new String(strBytes, charSet), bytes.drop(4 + size))
    }
  }

  implicit def traversableUnpacker[A, T](implicit ev: T <:< Traversable[A], bf: CanBuildFrom[List[A], A, T], aunpacker: Unpacker[A]) = new Unpacker[T] {
    override def unpack(bytes: Array[Byte]): Try[(T, Array[Byte])] = Try {
      new String(bytes, charSet).splitColon
    } flatMap { case (nr, code) =>
      unpackRec(bf(), nr.toInt, code.getBytes(charSet))
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

}
