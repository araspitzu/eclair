package fr.acinq.eclair.router

import fr.acinq.bitcoin.Crypto.PublicKey
import org.jgrapht.graph.DirectedWeightedPseudograph
import scala.collection.JavaConversions._
import scala.collection.mutable

object Graph {

  def shortestPath(g: DirectedWeightedPseudograph[PublicKey, DescEdge], sourceNode: PublicKey, targetNodeId: PublicKey): Seq[Hop] = {

    val distance = new mutable.HashMap[PublicKey, Double]
    val foundEdges = new mutable.HashMap[PublicKey, DescEdge]
    val vertexQueue = new PriorityQueue[PublicKey]()

    g.vertexSet().toSet[PublicKey].foreach {
      case pk if pk == sourceNode =>
        distance += pk -> 0 //source node has distance 0
        vertexQueue.enqueue(pk, 0)
      case pk                     =>
        distance += pk -> Double.MaxValue
        vertexQueue.enqueue(pk, Double.MaxValue)
    }

    while(!vertexQueue.isEmpty()) {

      val current = vertexQueue.dequeue()

      //for each neighbor
      g.edgesOf(current).toSet[DescEdge].foreach { edge =>

        val neighbor = edge.desc.b

        if(distance(current) + g.getEdgeWeight(edge) < distance(neighbor)){

          //update visiting tree
          foundEdges.update(current, edge)

          //update the minimum distance
          distance.update(neighbor, distance(current) + g.getEdgeWeight(edge))

          //update queue
          vertexQueue.enqueueOrUpdate(neighbor, distance(neighbor))

        }

      }

    }

    //build the result path from the visiting tree
    val resultPath = foundEdges.values.map(edge => Hop(edge.desc.a, edge.desc.b, edge.u)).toSeq
    val hopPath = new mutable.MutableList[Hop]

    //start backward from the target
    var current = targetNodeId
    while(resultPath.exists(_.nextNodeId == current)) {

      val Some(temp) = resultPath.find(_.nextNodeId == current)

      hopPath += temp
      current = temp.nodeId

    }

    hopPath.reverse
  }


  case class PriorityElem[T](data: T, weight: Double)

  /**
    * A stateful priority queue using efficient binary heap
    */
  class PriorityQueue[T](var heap: Array[PriorityElem[T]], var indexes: mutable.Map[T, Int], var lastIndex: Int) {

    //Constructor allocating an array of size + 1
    def this(size: Int) = this(new Array[PriorityElem[T]](size + 1), new mutable.HashMap[T, Int](), 0)

    def this() = this(new Array[PriorityElem[T]](100 + 1), new mutable.HashMap[T, Int](), 0)

    def isEmpty():Boolean = lastIndex == 0

    //Enqueue an element, the new element is added as last leaf of the binary heap and then moved up to the proper level (weight)
    def enqueue(data: T, weight: Double): Unit = {

      //if the heap is full, double the array size
      if(lastIndex == heap.length - 1){
        val newHeap = new Array[PriorityElem[T]](heap.length * 2)
        System.arraycopy(heap, 0, newHeap, 0, lastIndex)
        heap = newHeap
      }

      //insert the new element as leaf in the heap
      lastIndex += 1
      heap(lastIndex) = PriorityElem(data, weight)
      indexes += data -> lastIndex
      moveUp(lastIndex)

    }

    //Extracts the root of the tree (heap), replaces it with the last leaf and then moves it down
    def dequeue(): T = {
      if(isEmpty()) throw new IllegalArgumentException("Empty queue!")

      val ris = heap(1).data
      val lastLeaf = heap(lastIndex)
      heap(lastIndex) = null
      lastIndex -= 1

      if(lastIndex > 0){
        heap(1) = lastLeaf
        moveDown(1)
      }

      ris
    }

    //Deletes an element from the queue, after removal the tree is rebalanced
    def remove(value: T): Unit = {
      val Some(index) = indexes.get(value)

      //edge case: the element is in the last position
      if(index == lastIndex){
        heap(lastIndex) = null
        lastIndex -= 1
        return
      }

      //overwrite the node containing @data with the last leaf
      heap(index) = heap(lastIndex)
      lastIndex -= 1
      moveDown(index)
      moveUp(index)

    }

    //Updates the weight of an element or enqueues it
    def enqueueOrUpdate(value: T, weight: Double): Unit = {

      indexes.get(value) match {
        case None         => enqueue(value, weight)
        case Some(targetIndex)  =>
          heap(targetIndex) = PriorityElem(value, weight)
          moveDown(targetIndex)
          moveUp(targetIndex)
      }

    }

    //moves up the element at @index, until the correct level is reached
    private def moveUp(index: Int): Unit = {
      var i = index

      if(i < 1 || i > lastIndex) throw new IllegalArgumentException(s"Index $i not found")

      val tmp = heap(i)

      while(i > 1 && tmp.weight  < heap(i / 2).weight) {
        heap(i) = heap(i/2)
        indexes += heap(i).data -> i  //update the position
        i = i/2                       //one level up
      }

      indexes += tmp.data -> i
      heap(i) = tmp

    }

    //moves an element down to the correct level according to its weight
    private def moveDown(index: Int): Unit = {
      var i = index

      if(i > lastIndex) throw new IllegalArgumentException(s"Index $i not found")

      val tmp = heap(i)
      var j = i * 2
      var found = false

      while(!found && j  < lastIndex) {

        if( j + 1 <= lastIndex && heap(j+1).weight < heap(j).weight){
          //j is now the smaller of the children
          j += 1
        }

        if(tmp.weight <= heap(j).weight){
          found = true
        } else {
          heap(i) = heap(j)
          indexes += heap(i).data -> i
          i = j
          j = i * 2 //go one level deeper
        }

       }

      indexes += tmp.data -> i
      heap(i) = tmp

    }

  }


}