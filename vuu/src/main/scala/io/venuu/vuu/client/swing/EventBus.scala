/**
 * Copyright Whitebox Software Ltd. 2014
 * All Rights Reserved.

 * Created by chris on 02/01/15.

 */
package io.venuu.vuu.client.swing

import java.util.concurrent.CopyOnWriteArrayList

class EventBus[T] {

  private val callbacks = new CopyOnWriteArrayList[T => Unit ]()

  import scala.collection.JavaConversions._

  def register(callback: T => Unit): Unit = {
    callbacks.add(callback)
  }

  def publish(message: T) = {
    callbacks.foreach( c => c.apply(message) )
  }

}
