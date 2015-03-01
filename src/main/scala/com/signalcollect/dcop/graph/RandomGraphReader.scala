package com.signalcollect.dcop.graph

import scala.util.Random
import com.signalcollect.dcop.modules._
import com.signalcollect._
import com.signalcollect.dcop.graph._
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.dcop.modules.SignalCollectAlgorithmBridge
import java.io.BufferedReader
import java.io.FileReader

//TODO: Decouple the functions for building vertices/edges from the algorithm. Add pluggable function for action initialization.
class RandomGraphReader(myAlgo: IntAlgorithm, numberOfVertices: Int, edgeDensity: Int, numberOfColors: Int, graphNumber: Int) extends GraphInstantiator {

  def build(graphBuilder: GraphBuilder[Any, Any] = GraphBuilder): Graph[Any, Any] = {

    val inputFileName:String = s"inputGraphs/V${numberOfVertices}_ED${edgeDensity}_Col${numberOfColors}_${graphNumber}.txt"
    val sourceFile = new java.io.File(inputFileName)
    val br = new BufferedReader(new FileReader(sourceFile));

    val Array(contentNumberOfVertices, contentNumberOfEdges, contentEdgeDensity, contentNumberOfColors) = br.readLine().split(" ").map(_.toInt)
    val domain = (0 until numberOfColors).toSet

    def randomFromDomain = domain.toSeq(Random.nextInt(domain.size))

    val g = graphBuilder.build

    //Add vertices
    for (i <- (0 until numberOfVertices)) {
      g.addVertex(myAlgo.createVertex(i, randomFromDomain, domain))
    }

    //Add edges
    for (i <- 0 until contentNumberOfEdges) {
      val Array(src, trg) = br.readLine().split(" ").map(_.toInt)
      g.addEdge(src, myAlgo.createEdge(targetId = trg))
    }

    br.close()
    g
  }

  def size = numberOfVertices //number of vertices

  def maxUtility = numberOfVertices * edgeDensity //number of edges

  override def toString = "RandomGraph" + size.toString + "density" + edgeDensity + "colors" + numberOfColors

}