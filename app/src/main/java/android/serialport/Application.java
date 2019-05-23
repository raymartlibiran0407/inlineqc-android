
package android.serialport;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.HashMap;


public class Application extends android.app.Application {

	private SerialPort mSerialPort = null;

	public SerialPort getSerialPort() throws SecurityException, IOException, InvalidParameterException {
		try {
			if (mSerialPort == null) {

				/**
				 * FILE,BAUDRATE
				 */
				mSerialPort = new SerialPort(new File("/dev/ttyS3"), 9600);
			}
			return mSerialPort;
		}catch (Exception e)
		{
			e.printStackTrace();
			return  null;
		}
	}

	public SerialPort getSerialPort(String serialPort ) throws SecurityException, IOException, InvalidParameterException {
		try {
			if (mSerialPort == null) {

				/**
				 * FILE,BAUDRATE
				 */
				System.out.println("Scanning on serial port: " + serialPort);
				mSerialPort = new SerialPort(new File(serialPort), 9600);
			}
			return mSerialPort;
		}catch (Exception e)
		{
			e.printStackTrace();
			return  null;
		}
	}

	public void closeSerialPort() {
		if (mSerialPort != null) {
			mSerialPort.close();
			mSerialPort = null;
		}
	}
}
