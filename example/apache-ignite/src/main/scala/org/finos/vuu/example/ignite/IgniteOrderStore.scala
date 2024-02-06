package org.finos.vuu.example.ignite

import com.typesafe.scalalogging.StrictLogging
import org.apache.ignite.cache.CachePeekMode
import org.apache.ignite.cache.query.{FieldsQueryCursor, IndexQuery, IndexQueryCriteriaBuilder, IndexQueryCriterion, SqlFieldsQuery}
import org.apache.ignite.cluster.ClusterState
import org.apache.ignite.{IgniteCache, Ignition}
import org.finos.vuu.core.module.simul.model.{ChildOrder, OrderStore, ParentOrder}
import org.finos.vuu.example.ignite.schema.IgniteEntitySchema
import org.finos.vuu.example.ignite.order.ChildOrderSchema

import java.util
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._
import scala.jdk.javaapi.CollectionConverters.asJava

object IgniteOrderStore {

  /**
   * Creates an instance of IgniteOrderStore
   *
   * @param clientMode defines whether the node is a client or a server that is, if cluster node keeps cache data in current jvm or not
   * @return an instance of IgniteOrderStore
   */
  def apply(clientMode: Boolean = true, persistenceEnabled: Boolean = false): IgniteOrderStore = {
    IgniteLocalConfig.setPersistenceEnabled(persistenceEnabled)
    val childOrderSchema = ChildOrderSchema.create()
    val config = IgniteLocalConfig.create(clientMode = clientMode)
    val ignite = Ignition.getOrStart(config)

    ignite.cluster().state(ClusterState.ACTIVE)

    val parentOrderCache = ignite.getOrCreateCache[Int, ParentOrder](IgniteLocalConfig.parentOrderCacheName)
    val childOrderCache = ignite.getOrCreateCache[Int, ChildOrder](IgniteLocalConfig.childOrderCacheName)

    new IgniteOrderStore(parentOrderCache, childOrderCache, childOrderSchema)
  }
}

class IgniteOrderStore(private val parentOrderCache: IgniteCache[Int, ParentOrder],
                       private val childOrderCache: IgniteCache[Int, ChildOrder],
                       val childOrderSchema: IgniteEntitySchema) extends OrderStore with StrictLogging {

  def storeParentOrder(parentOrder: ParentOrder): Unit = {
    parentOrderCache.put(parentOrder.id, parentOrder)
  }

  def storeChildOrder(parentOrder: ParentOrder, childOrder: ChildOrder): Unit = {
    storeParentOrder(parentOrder)
    childOrderCache.put(childOrder.id, childOrder)
  }

  def storeParentOrderWithChildren(parentOrder: ParentOrder, childOrders: Iterable[ChildOrder]): Unit = {
    storeParentOrder(parentOrder)

    val localCache = mutable.Map.empty[Int, ChildOrder]
    childOrders.foreach(order => localCache(order.id) = order)

    childOrderCache.putAll(asJava(localCache))
  }

  def findParentOrderById(id: Int): ParentOrder = {
    parentOrderCache.get(id)
  }

  def getDistinct(columnName: String, rowCount: Int): Iterable[String] = {
    val query = new SqlFieldsQuery(s"select distinct $columnName from ChildOrder limit ?")
    query.setArgs(rowCount)

    val results = childOrderCache.query(query)

    val (counter, buffer) = mapToString(results)

    logger.info(s"Loaded Ignite ChildOrder column $columnName for $counter rows")

    buffer
  }

  def getDistinct(columnName: String, startsWith: String, rowCount: Int): Iterable[String]  = {
    val query = new SqlFieldsQuery(s"select distinct $columnName from ChildOrder where $columnName LIKE \'$startsWith%\' limit ?")
    query.setArgs(rowCount)
    logger.info(query.getSql)
    val results = childOrderCache.query(query)

    val (counter, buffer) = mapToString(results)

    logger.info(s"Loaded Ignite ChildOrder column $columnName for $counter rows")

    buffer

  }

  def findChildOrder(sqlFilterQueries: String, sqlSortQueries: String, rowCount: Int, startIndex: Long): Iterable[ChildOrder] = {
    val whereClause = if(sqlFilterQueries == null || sqlFilterQueries.isEmpty) "" else s" where $sqlFilterQueries"
    val orderByClause = if(sqlSortQueries == null || sqlSortQueries.isEmpty) " order by id" else s" order by $sqlSortQueries"
    val query = new SqlFieldsQuery(s"select * from ChildOrder$whereClause$orderByClause limit ? offset ?")
    query.setArgs(rowCount, startIndex)

    val results = childOrderCache.query(query)

    var counter = 0
    val buffer = mutable.ListBuffer[ChildOrder]()
    results.forEach(item => {
      buffer.addOne(toChildOrder(item))
      counter += 1
    })

    logger.info(s"Loaded Ignite ChildOrder for $counter rows, from index : $startIndex where $whereClause order by $sqlSortQueries")

    buffer
  }

  def findChildOrderFilteredBy(filterQueryCriteria: List[IndexQueryCriterion]): Iterable[ChildOrder] = {
  //  val filter: IgniteBiPredicate[Int, ChildOrder]  = (key, p) => p.openQty > 0

    val query: IndexQuery[Int, ChildOrder] =
      new IndexQuery[Int, ChildOrder](classOf[ChildOrder])
        .setCriteria(filterQueryCriteria.asJava)
       // .setFilter(filter)

    childOrderCache
      .query(query)
      .getAll.asScala
      .map(x => x.getValue)
  }
  def findChildOrderByParentId(parentId: Int): Iterable[ChildOrder] = {
    val query: IndexQuery[Int, ChildOrder] = new IndexQuery[Int, ChildOrder](classOf[ChildOrder])

    val criterion = IndexQueryCriteriaBuilder.eq("parentId", parentId)
    query.setCriteria(criterion)
    childOrderCache.query(query)
      .getAll.asScala
      .map(x => x.getValue)
  }

  // todo - make it metadata aware and extract to another class.
  private def toChildOrder(cols: java.util.List[_]): ChildOrder = {
    ChildOrder(
      parentId = cols.get(0).asInstanceOf[Int],
      id = cols.get(1).asInstanceOf[Int],
      ric = cols.get(2).asInstanceOf[String],
      price = cols.get(3).asInstanceOf[Double],
      quantity = cols.get(4).asInstanceOf[Int],
      side = cols.get(5).asInstanceOf[String],
      account = cols.get(6).asInstanceOf[String],
      strategy = cols.get(7).asInstanceOf[String],
      exchange = cols.get(8).asInstanceOf[String],
      ccy = cols.get(9).asInstanceOf[String],
      volLimit = cols.get(10).asInstanceOf[Double],
      filledQty = cols.get(11).asInstanceOf[Int],
      openQty = cols.get(12).asInstanceOf[Int],
      averagePrice = cols.get(13).asInstanceOf[Double],
      status = cols.get(14).asInstanceOf[String]
    )
  }

  private def mapToString(results: FieldsQueryCursor[util.List[_]]): (Int, ListBuffer[String]) = {
    var counter = 0
    val buffer = mutable.ListBuffer[String]()
    results.forEach(row => {
      buffer.addOne(row.get(0).asInstanceOf[String])
      counter += 1
    })
    (counter, buffer)
  }

  def parentOrderCount(): Long = {
    parentOrderCache.sizeLong(CachePeekMode.ALL)
  }

  def childOrderCount(): Long = {
    childOrderCache.sizeLong(CachePeekMode.ALL)
  }

  def findWindow(startIndex: Long, rowCount: Int): Iterable[ChildOrder] = {
    val query = new SqlFieldsQuery(s"select * from ChildOrder order by id limit ? offset ?")
    query.setArgs(rowCount, startIndex)
 //   val query = new SqlFieldsQuery(s"select * from ChildOrder")
    val results = childOrderCache.query(query)

    var counter = 0
    val buffer = mutable.ListBuffer[ChildOrder]()
    results.forEach(item => {
      buffer.addOne(toChildOrder(item))
      counter += 1
    })

    logger.debug(s"Loaded $counter rows, from index : $startIndex, rowCou")

    buffer
  }
}
