package org.finos.vuu.example.ignite.schema

import org.finos.vuu.example.ignite.schema.EntitySchema.ColumnName
import org.finos.vuu.example.ignite.schema.IgniteDataType.IgniteDataType
import scala.collection.mutable

//N.B columnDef need to be a key value collection that preserve order of columns.
//todo need to make columnDef Immutable?

case class IgniteEntitySchema(columnDef: mutable.LinkedHashMap[ColumnName, IgniteDataType], queryIndex: List[ColumnName])
{
  def indexOf(columnName: ColumnName): Int = columnDef.keys.toList.indexOf(columnName)
}
object EntitySchema {
  type ColumnName = String
}
object IgniteDataType extends Enumeration {
  type IgniteDataType = String
  val Int : IgniteDataType =  classOf[Int].getName
  val String : IgniteDataType =  classOf[String].getName
  val Double : IgniteDataType =  classOf[Int].getName
}
object IgniteEntitySchemaBuilder {
  def apply(): IgniteEntitySchemaBuilder =
    IgniteEntitySchemaBuilder(mutable.LinkedHashMap.empty, List.empty)
}
case class IgniteEntitySchemaBuilder(fieldDef: mutable.LinkedHashMap[ColumnName, IgniteDataType], index: List[ColumnName]){

  def withColumn(columnName: ColumnName, dataType: IgniteDataType): IgniteEntitySchemaBuilder =
    IgniteEntitySchemaBuilder(fieldDef += (columnName -> dataType), index)

  def withIndex(columnName: ColumnName): IgniteEntitySchemaBuilder =
    IgniteEntitySchemaBuilder(fieldDef, index :+ columnName)
  def build(): IgniteEntitySchema =
    IgniteEntitySchema(fieldDef, index)

}



