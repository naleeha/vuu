package org.finos.vuu.example.ignite.schema

import org.apache.ignite.cache.QueryEntity

object ChildOrderSchema {

  def create(): IgniteEntitySchema =
    IgniteEntitySchemaBuilder()
      .withColumn("parentId", IgniteDataType.Int)
      .withColumn("id",  IgniteDataType.Int)
      .withColumn("ric", IgniteDataType.String)
      .withColumn("price", IgniteDataType.Double)
      .withColumn("quantity", IgniteDataType.Int)
      .withColumn("side", IgniteDataType.String)
      .withColumn("account", IgniteDataType.String)
      .withColumn("strategy", IgniteDataType.String)
      .withColumn("exchange", IgniteDataType.String)
      .withColumn("ccy", IgniteDataType.String)
      .withColumn("volLimit", IgniteDataType.Double)
      .withColumn("filledQty", IgniteDataType.Int)
      .withColumn("openQty", IgniteDataType.Int)
      .withColumn("averagePrice", IgniteDataType.Double)
      .withColumn("status", IgniteDataType.String)
      .withIndex("id")
      .withIndex("parentId")
      .build()

  def toQueryEntity():QueryEntity =
    QueryEntity

}
