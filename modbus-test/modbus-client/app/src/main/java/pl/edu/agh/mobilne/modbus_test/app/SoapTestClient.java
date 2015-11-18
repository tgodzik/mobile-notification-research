package pl.edu.agh.mobilne.modbus_test.app;

import android.os.Handler;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SoapTestClient implements TestClient {

    private Handler messageHandler = new Handler();
    private MessageHandler messageListener;
    private String lastMessage = "no msg";
    protected static TCPMasterConnection con = null; //the conection
    protected static ModbusTCPTransaction trans = null; //the transaction
    protected static WriteMultipleRegistersRequest req = null; //the request
    protected static WriteMultipleRegistersResponse res = null; //the response
    /* Variables for storing the parameters */
    protected static InetAddress addr = null; //the slave's address
    protected static final int port = 3000;
    protected Register register = null;
    protected Register[] registers = null;

    final Runnable returnMessage = new Runnable() {
        public void run() {
            messageListener.onReceiveMessage(lastMessage);
        }
    };

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageListener = handler;
    }

    @Override
    public boolean create() {
        try {
            addr = InetAddress.getByName("10.0.2.2");
            con = new TCPMasterConnection(addr);
            con.setPort(port);
            con.connect();

            int NUMBER_OF_PARALLEL_REQUESTS = 100;
            for (int i = 1; i <= NUMBER_OF_PARALLEL_REQUESTS; i++) {
                register = new SimpleRegister(3);
                registers = new Register[1];
                registers[0] = register;
                req = new WriteMultipleRegistersRequest(0, registers);
                trans = new ModbusTCPTransaction(con);
                res = (WriteMultipleRegistersResponse) trans.getResponse();
                lastMessage = new String(res.getMessage());
                System.out.println(lastMessage);
                messageHandler.post(returnMessage);
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } finally {
            con.close();
        }
        return true;
    }

    @Override
    public void dispose() {
        Statistics.reset();
    }
}
