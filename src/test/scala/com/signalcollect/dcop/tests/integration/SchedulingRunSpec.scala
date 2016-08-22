/*
 *  @author Mihaela Verman
 *  
 *  Copyright 2016 University of Zurich
 *      
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *         http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 */
package com.signalcollect.dcop.tests.integration

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import org.scalatest.prop.Checkers
import scala.math.Ordering.Boolean

import com.signalcollect._
import com.signalcollect.configuration.ExecutionMode
import com.signalcollect.dcop.algorithms._
import com.signalcollect.dcop.evaluation._
import com.signalcollect.dcop.graph._

import com.signalcollect.ExecutionConfiguration
import com.signalcollect.GraphBuilder
import com.signalcollect.dcop.custom._
import com.signalcollect.dcop.modules.ArgmaxADecisionRule
import com.signalcollect.dcop.modules.MemoryLessTargetFunction
import com.signalcollect.dcop.modules.NashEquilibriumConvergence
import com.signalcollect.dcop.modules.ParallelRandomAdjustmentSchedule
import com.signalcollect.dcop.modules.SignalCollectAlgorithmBridge

/**
 *  Two events with no participants in common and two slots.
 *  Events must have even IDs and slots odd IDs
 */
class SchedulingRunSpec extends FlatSpec with ShouldMatchers with Checkers with TestTools {

  println("Start")
  println("Start graph building")
  val g = new ScheduleGraphReader(
    eventAlgo = SchedulingEventAlgorithm,
    slotAlgo = SchedulingSlotAlgorithm,
    eventsNumber = 4,
    timeSlots = 2,
    rooms = 2,
    commonPeopleFile = "inputGraphs/inputScheduling/test/test_llsp.csv",
    lectureSizeFile = "inputGraphs/inputScheduling/test/test_lsp.csv",
    roomsFile = "inputGraphs/inputScheduling/test/test_rc.csv").
    build(GraphBuilder.withConsole(true))

  g.foreachVertex(x => println(x))
  println("Executing")
  g.execute(ExecutionConfiguration.withExecutionMode(ExecutionMode.PureAsynchronous))//.Interactive))

  g.foreachVertex(x => println(x))

  println("Shutting down")
  g.shutdown

  println("bye")
}
