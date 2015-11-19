package pl.edu.agh.mobilne.modbus_test.app

import java.net.InetAddress

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest
import com.ghgande.j2mod.modbus.net.TCPMasterConnection

object ModbusServer extends App {
  val addr = InetAddress.getByName("192.168.1.108")
  val port = 3000
  val con = new TCPMasterConnection(addr)
  con.setPort(port)
  con.connect()

  (0 until 100).foreach{ i =>
    val req = new ReadMultipleRegistersRequest(1, 1)
    val trans = new ModbusTCPTransaction(con)
    trans.setRequest(req)
    trans.execute()
    println(trans.getResponse.getHexMessage.substring(27))
  }
  con.close()
}
