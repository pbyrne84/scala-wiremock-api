package uk.org.devthings.scala.wiremockapi

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Microsecond, Second, Span}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

/**
  * By default the interval is 150 milliseconds which can really add up in tests depending on how they are written. I
  * have seen tests go from 10 minutes to 10 seconds. Also worth checking if the Execution Context is not the wrong type
  * for the type of operation. A good way to see if things are blocking is to look at top or equivalent when running
  * tests. CPU should hammer if things are going as fast as possible.
  */
trait ImpatientPatience { self: ScalaFutures =>

  // This will just thread with no bounds, not limited to cpu cores. Cached/Custom fixed thread pools
  // can/should be used for blocking operations. In this case for tests we will just use it for everything
  // as unlikely to cause any problems except faster.
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  final implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(1, Second), interval = Span(1, Microsecond))

}
