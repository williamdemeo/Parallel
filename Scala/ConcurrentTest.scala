package test2

/**
  * Testing this with itCount = 9000 conistently took about 10 second i
  * in serial and from 20 to 33 seconds in parallel.  
  * Not impressive
  * 
  */ 

import akka.actor._

class ConcurrentTest
  
object ConcurrentTest {


  val a = 1 //47
  val b = 1 //53
  val c = 10001
  def g(x : Int, y : Int) : Int = (a*x + b*y) % c

  val itCount = 9000
  var timeEven = 0
  var timeOdd = 0

  val syncedMap = new scala.collection.mutable.LinkedHashMap[Int,Int]() with scala.collection.mutable.SynchronizedMap[Int,Int]

}

class EvenActor extends Actor {
  def receive = {
    case "go" => {
        var i = 0; 
        val map = ConcurrentTest.syncedMap
        val t0 = System.currentTimeMillis().toInt
        while (i < ConcurrentTest.itCount) {
          var j = 0
          while (j < ConcurrentTest.itCount) {
            val v = ConcurrentTest.g(i,j)
            if (!map.contains(v)) map.put(v,0)
            j += 2
          }
          i += 1
        }  
        val time = System.currentTimeMillis().toInt - t0
        ConcurrentTest.timeEven = time
      }
    case _ => println("what")  
  }
}

class OddActor extends Actor {
  def receive = {
    case "go" => {
        var i = 0;
        val map = ConcurrentTest.syncedMap
        val t0 = System.currentTimeMillis().toInt
        while (i < ConcurrentTest.itCount) {
          var j = 1
          while (j < ConcurrentTest.itCount) {
            val v = ConcurrentTest.g(i,j)
            if (!map.contains(v)) map.put(v,1)
            j += 2
          }
          i += 1
        }
        val time = System.currentTimeMillis().toInt - t0
        ConcurrentTest.timeOdd = time
      }
    case _ => println("what")
  }    
}

object Main2 extends App {
  def printStuff() {
    println("")
    //println(ConcurrentTest.syncedMap.keySet.size)
    println("timeEven: " + ConcurrentTest.timeEven)
    println("timeOdd: " + ConcurrentTest.timeOdd)
    var count = 0
    for ((key, v) <- ConcurrentTest.syncedMap) if (v == 0) count += 1
    println("even count: " + count)
    println("total: " + ConcurrentTest.syncedMap.keySet.size)
  }

  def testParallel() {
    val system = ActorSystem("cctest")
    val evenActor = system.actorOf(Props[EvenActor], name = "evenActor")
    val oddActor = system.actorOf(Props[OddActor], name = "oddActor")
    oddActor ! "go"
    evenActor ! "go"
    var count = 0
    while (ConcurrentTest.timeEven == 0 || ConcurrentTest.timeOdd == 0) {
      Thread.sleep(1000)
      count += 1
      print(count + " ")
    }
    printStuff
    system.shutdown
  }

  def testSerial() {
    val t0 = System.currentTimeMillis().toInt
    var i = 0
    val map = ConcurrentTest.syncedMap
    while (i < ConcurrentTest.itCount) {
      var j = 1
      while (j < ConcurrentTest.itCount) {
        val v = ConcurrentTest.g(i,j)
        if (!map.contains(v)) map.put(v,1)
        j += 1
      }
      i += 1
    }
    val time = System.currentTimeMillis().toInt - t0
    println("timeSerial: " + time)
    println("size: " + ConcurrentTest.syncedMap.size)
  }
  
  testSerial
  ConcurrentTest.syncedMap.clear
  testParallel
}

