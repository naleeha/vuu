package org.finos.vuu.core.module.basket.provider

class SpikeBasketProvider {
  //this will be injected in through constructor normally
  val table = new TableApiImp

  def updateTable(): Unit = {
    //this will be sourced from data source
    val basketTableData = BasketTableData("myBakset1", 100)

    //this avoid issue where your key passed to row update is different value from the key on the data as key will be inferred from the data itself
    table.updateRow(basketTableData)
  }

}

trait TableApi{
  def updateRow[T <: TableRowData](data:T): Unit
}

class TableApiImp extends TableApi {
  override def updateRow[T <: TableRowData](data: T): Unit = ???
}

trait TableRowData {
  val key: Any
}

case class BasketTableData(
                            id:String,
                            quantity:Int
                          ) extends TableRowData{
  override val key: Any = id
}