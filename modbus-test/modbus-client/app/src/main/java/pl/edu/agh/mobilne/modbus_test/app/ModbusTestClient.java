package pl.edu.agh.mobilne.modbus_test.app;

import android.util.Log;

import com.ghgande.j2mod.modbus.ModbusCoupler;
import com.ghgande.j2mod.modbus.net.ModbusTCPListener;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleInputRegister;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class ModbusTestClient implements TestClient {

    protected static String addr = "0.0.0.0"; //the slave's address
    ModbusTCPListener listener = new ModbusTCPListener(1);

    protected static final int port = 3000;

    @Override
    public void setMessageHandler(MessageHandler handler) {}

    @Override
    public boolean create() {
        try {
            SimpleProcessImage spi = new SimpleProcessImage();
            spi.addRegister(1, new TestRegister());
            ModbusCoupler.getReference().setProcessImage(spi);
            ModbusCoupler.getReference().setMaster(false);

            listener.setPort(port);
            listener.setAddress(InetAddress.getByName(addr));
            listener.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Log.i("modbus", "connected");
        return true;
    }

    @Override
    public void dispose() {
        Statistics.reset();
    }
}

class TestRegister implements Register {

    byte head = (byte) 255;
    byte val = (byte) 255;

    @Override
    public void setValue(int i) {

    }

    @Override
    public void setValue(short i) {

    }

    @Override
    public void setValue(byte[] bytes) {

    }

    @Override
    public int getValue() {
        Statistics.addMessage(val+"");
        Log.i("modbus", "val is now " + val);
        val++;
        return val;
    }

    @Override
    public int toUnsignedShort() {
        return getValue();
    }

    @Override
    public short toShort() {
        return (short) getValue();
    }

    @Override
    public byte[] toBytes() {
        byte[] t = new byte[2];
        getValue();
        t[0] = head;
        t[1] = val;
        return t;
    }
}