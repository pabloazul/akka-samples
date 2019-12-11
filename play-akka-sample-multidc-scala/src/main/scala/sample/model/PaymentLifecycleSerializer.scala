package sample.model

import java.io.NotSerializableException

import akka.serialization.BaseSerializer
import akka.serialization.SerializerWithStringManifest
import sample.model.protobuf.LifecycleTrackingMessages
import scala.collection.JavaConverters._

class PaymentLifecycleSerializer(val system: akka.actor.ExtendedActorSystem)
  extends SerializerWithStringManifest with BaseSerializer {

  override def manifest(o: AnyRef): String = o match {
    case _ ⇒
      throw new IllegalArgumentException(s"Can't serialize object of type ${o.getClass} in [${getClass.getName}]")
  }

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case _ ⇒
      throw new IllegalArgumentException(s"Cannot serialize object of type [${o.getClass.getName}]")
  }
  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = manifest match {

    case _ ⇒
      throw new NotSerializableException(
        s"Unimplemented deserialization of message with manifest [$manifest] in [${getClass.getName}]")
  }

}

