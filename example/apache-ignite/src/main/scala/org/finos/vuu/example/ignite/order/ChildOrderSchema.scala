package org.finos.vuu.example.ignite.order

import org.finos.vuu.example.ignite.schema.{IgniteEntitySchema, IgniteEntitySchemaBuilder}

object ChildOrderSchema {

   def create(): IgniteEntitySchema =
    IgniteEntitySchemaBuilder()
      .withColumn("parentId", classOf[Int].getName)
      .withColumn("id", classOf[Int].getName)
      .withColumn("ric", classOf[String].getName)
      .withColumn("price", classOf[Double].getName)
      .withColumn("quantity", classOf[Int].getName)
      .withColumn("side", classOf[String].getName)
      .withColumn("account", classOf[String].getName)
      .withColumn("strategy", classOf[String].getName)
      .withColumn("exchange", classOf[String].getName)
      .withColumn("ccy", classOf[String].getName)
      .withColumn("volLimit", classOf[Double].getName)
      .withColumn("filledQty", classOf[Int].getName)
      .withColumn("openQty", classOf[Int].getName)
      .withColumn("averagePrice", classOf[Double].getName)
      .withColumn("status", classOf[String].getName)
      .withIndex("id")
      .withIndex("parentId")
      .build()

}
