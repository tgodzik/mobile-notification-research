package pl.edu.agh.mobilne.modbus_test.app

import java.net.InetAddress
import java.util.Date

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest
import com.ghgande.j2mod.modbus.net.TCPMasterConnection

object ModbusServer extends App {
  val addr = InetAddress.getByName("192.168.1.108")
  val port = 3000
  val con = new TCPMasterConnection(addr)
  con.setPort(port)
  con.connect()
  val count = 1000
  val noOfMsg = 1

  val avg = (0 until count).map { x =>
    (0 until noOfMsg).map { i =>
      val req = new ReadMultipleRegistersRequest(1, 1)
      val trans = new ModbusTCPTransaction(con)
      trans.setRequest(req)
      trans.execute()
      val bytes = trans.getResponse.getMessage.drop(1)
      val timestampEnd = bytes(0) * 256 + bytes(1)
      val timestampNow = new Date().getTime
      val timestampSent = timestampNow - (timestampNow % 65536) + timestampEnd
      println((timestampNow - timestampSent) % 65536)
      (timestampNow - timestampSent) % 65536
    }.sum.toDouble
  }.sum / count

  println("avg " + avg)

  con.close()
}
