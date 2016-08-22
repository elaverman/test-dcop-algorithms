/*
 *  @author Mihaela Verman
 *  
 *  Copyright 2015 University of Zurich
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

package com.signalcollect.dcop.custom

import java.io.BufferedReader
import scala.util.Random
import com.signalcollect._
import com.signalcollect.dcop.modules._
import com.signalcollect.dcop.graph._

//TODO: Decouple the functions for building vertices/edges from the algorithm. Add pluggable function for action initialization.
class ScheduleGraphReader(
  eventAlgo: IntAlgorithm,
  slotAlgo: IntAlgorithm,
  eventsNumber: Int,
  timeSlots: Int,
  rooms: Int,
  commonPeopleFile: String,
  lectureSizeFile: String,
  roomsFile: String,
  minOccupationRate: Double = 0.0) extends GraphInstantiator {
  var util = 0L

  def build(graphBuilder: GraphBuilder[Any, Any] = GraphBuilder): Graph[Any, Any] = {

    val g = graphBuilder.build

    val allSlots = (for { ts <- (1 to timeSlots); r <- (1 to rooms) }
      yield getSlotId(r, ts)).toList
    val allEvents = (for (e <- (1 to eventsNumber))
      yield (e * 2)).toList

    val commonPeople = computeCommonPeopleForEvents(allEvents)
    val lectureSize = computeLectureSize()
    val roomSize = computeRoomSize()
    val (eventsToRooms, roomsToEvents) = computeEdges(allEvents, allSlots, roomSize, lectureSize)

    val allSlotsList = allSlots.toList
    val allEventsList = allEvents.toList

    // TODO: Repair domain to reflect actual edges.
    for (eventId <- allEvents) {
      // TODO: Select at random the initial action.
      if (eventsToRooms.contains(eventId)) {
        val domain = (for { ts <- (1 to timeSlots); r <- eventsToRooms(eventId) }
          yield getSlotId(r, ts)).toSet
        g.addVertex(eventAlgo.createVertex(eventId, getRandomFromDomain(domain), domain, Some(commonPeople(eventId / 2))))
      } else {
        g.addVertex(eventAlgo.createVertex(eventId, 0, Set(0), Some(commonPeople(eventId / 2))))
      }
    }
    for (slotId <- allSlots) {
      // Slots can also be non-allocated, so the domain includes 0.
      g.addVertex(slotAlgo.createVertex(slotId, 0, roomsToEvents(getRoomId(slotId)) + 0))
    }

    //Add edges
    for (eventId <- allEvents) {
      //event to event
      for ((ev2, (s, p)) <- commonPeople(eventId / 2)) {
        g.addEdge(eventId, eventAlgo.createEdge(targetId = ev2.toInt))
        util += s + p
      }
      val thisLectureSize = lectureSize.get(eventId) match {
        case Some(x) => x
        case None => throw new Exception(s"Lecture Id $eventId was not found.")
      }

      //event to slot and slot to event
      //Only if there are outgoing edges. That might not be the case, e.g. classes with 0 students.
      if (eventsToRooms.contains(eventId)) {
        for (roomId <- eventsToRooms(eventId)) {
          val thisRoomSize = roomSize.get(roomId) match {
            case Some(x) => x
            case None => throw new Exception(s"Room Id ${roomId} was not found.")
          }
          if ((thisLectureSize > thisRoomSize * minOccupationRate)
            && (thisLectureSize <= thisRoomSize)) {
            for (ts <- (1 to timeSlots)) {
              val slotId = getSlotId(roomId, ts)
              g.addEdge(eventId, eventAlgo.createEdge(targetId = slotId))
              g.addEdge(slotId, slotAlgo.createEdge(targetId = eventId))
            }
          }
        }
      }
    }
    println("Reading is done")
    g
  }

  def size = eventsNumber //number of vertices

  def maxUtility = rooms * timeSlots * eventsNumber + util

  override def toString = "Events" + eventsNumber + "_timeSlots" + timeSlots + "_Rooms" + rooms

  /*
   * Returns the number of common professors and students for all pairs of events (lectures).
   */
  def computeCommonPeopleForEvents(allEvents: List[Int]): Array[scala.collection.mutable.Map[Int, (Int, Int)]] = {
    val bufferedSource = io.Source.fromFile(commonPeopleFile)

    var commonPeople = new Array[scala.collection.mutable.Map[Int, (Int, Int)]](eventsNumber + 1)

    for (eventId <- allEvents) {
      commonPeople(eventId / 2) = scala.collection.mutable.Map.empty[Int, (Int, Int)]
    }
    for (line <- bufferedSource.getLines) {
      val Array(ev1, ev2, stud, prof) = line.split(",").map(_.trim).map(_.toInt)
      commonPeople(ev1.toInt) += ((ev2 * 2, (stud, prof)))
    }
    bufferedSource.close

    commonPeople
  }

  /*
   * Returns the edges between events and slots.
   */
  def computeEdges(
    allEvents: List[Int],
    allSlots: List[Int],
    roomSize: scala.collection.mutable.Map[Int, Int],
    lectureSize: scala.collection.mutable.Map[Int, Int]): (scala.collection.mutable.Map[Int, Set[Int]], scala.collection.mutable.Map[Int, Set[Int]]) = {

    var eventToRoomEdges = scala.collection.mutable.Map.empty[Int, Set[Int]]
    var roomToEventEdges = scala.collection.mutable.Map.empty[Int, Set[Int]]

    for (eventId <- allEvents) {
      for (roomId <- 1 to rooms) {
        val thisRoomSize = roomSize.get(roomId) match {
          case Some(x) => x
          case None => throw new Exception(s"Room Id ${roomId} was not found.")
        }
        val thisLectureSize = lectureSize.get(eventId) match {
          case Some(x) => x
          case None => throw new Exception(s"Lecture Id $eventId was not found.")
        }
        if ((thisLectureSize > thisRoomSize * minOccupationRate)
          && (thisLectureSize <= thisRoomSize)) {
          val oldEventToSlot = eventToRoomEdges.getOrElse(eventId, Set.empty[Int])
          eventToRoomEdges += ((eventId, oldEventToSlot + roomId))
          val oldSlotToEvent = roomToEventEdges.getOrElse(roomId, Set.empty[Int])
          roomToEventEdges += ((roomId, oldSlotToEvent + eventId))
        }
      }
    }

    (eventToRoomEdges, roomToEventEdges)
  }

  /*
   * Returns the number of students for each event (lecture).
   */
  def computeLectureSize(): scala.collection.mutable.Map[Int, Int] = {
    val bufferedSource = io.Source.fromFile(lectureSizeFile)

    var lectureSize = scala.collection.mutable.Map.empty[Int, Int]

    for (line <- bufferedSource.getLines) {
      val Array(ev1, stud, prof) = line.split(",").map(_.trim).map(_.toInt)
      lectureSize += ((ev1 * 2, stud))
    }
    bufferedSource.close

    lectureSize
  }

  /*
   * Returns the capacity for each room.
   */
  def computeRoomSize(): scala.collection.mutable.Map[Int, Int] = {
    val bufferedSource = io.Source.fromFile(roomsFile)

    var roomSize = scala.collection.mutable.Map.empty[Int, Int]

    println("Room size")

    for (line <- bufferedSource.getLines) {
      val Array(room, size, roomType) = line.split(",").map(_.trim)
      roomSize += ((room.toInt, size.toInt))
    }
    bufferedSource.close

    roomSize
  }

  /*
   * The following three functions provide the transformations from slotId to roomId and timeSlotId.
   */
  def getSlotId(roomId: Int, timeSlotId: Int) = (roomId * 100 + timeSlotId) * 2 + 1

  def getRoomId(id: Int) = (id / 2) / 100

  def timeSlotId(id: Int) = (id / 2) % 100

  def getRandomFromDomain(domain: Set[Int]): Int = domain.toVector(Random.nextInt(domain.size))

}