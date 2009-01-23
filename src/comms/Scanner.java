package comms;

import gui.Main;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

public class Scanner {
	private class DeviceDiscoverer implements DiscoveryListener, Runnable {
		private static final String DukeDave = "001B59EBD7A0";
		RemoteDevice phone = null;
		DiscoveryAgent discoveryAgent;
		UUID[] uuidSet = { new UUID("3738", true) };
		int[] attrSet = { 0x0100, 0x0003, 0x0004 };
		ServiceRecord serviceRecord;
		LocalDevice localDevice;

		public void run() {
			scan();
		}

		private void scan() {
			try {
				// Find devices
				localDevice = LocalDevice.getLocalDevice();
				discoveryAgent = localDevice.getDiscoveryAgent();
				System.out.println("Searching for Bluetooth devices in the vicinity...");
				discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
			} catch (Exception e) {
				System.err.println(e);
			}
		}

		public void deviceDiscovered(RemoteDevice remoteDevice, DeviceClass cod) {
			String addr = remoteDevice.getBluetoothAddress();
			System.out.println("Found " + addr);
			if (addr.equals(DukeDave)) {
				phone = remoteDevice;
				System.out.println("(DukeDave)");
			}
		}

		public void inquiryCompleted(int discType) {
			String inqStatus = null;
			if (discType == DiscoveryListener.INQUIRY_COMPLETED) {
				inqStatus = "Inquiry completed";
			} else if (discType == DiscoveryListener.INQUIRY_TERMINATED) {
				inqStatus = "Inquiry terminated";
			} else if (discType == DiscoveryListener.INQUIRY_ERROR) {
				inqStatus = "Inquiry error";
			}
			System.out.println(inqStatus + "");
			if (phone == null) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					System.out.println("Bad things :(");
				}
				scan();
			} else {
				servScan();
			}
		}

		public void servScan() {
			try {
				// Find services
				DiscoveryAgent discoveryAgent = localDevice.getDiscoveryAgent();
				System.out.println("Searching for Services");
				discoveryAgent.searchServices(attrSet, uuidSet, phone, this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
			for (int i = 0; i < servRecord.length; i++) {
				DataElement serviceNameElement = servRecord[i].getAttributeValue(0x0100);
				String serviceName = (String) serviceNameElement.getValue();
				System.out.println("A " + serviceName + " service has been found");
				if (serviceName.equals("WordSolverObex")) {
					try {
						serverURL = servRecord[i].getConnectionURL(1, false);
						System.out.println("The connection URL is: " + serverURL);
						serverName = servRecord[i].getHostDevice().getFriendlyName(true);
						main.sp.sendButton.setText("Send to " + serverName);
						main.sp.sendButton.setEnabled(true);
					} catch (Exception e) {
						System.out.println(e.toString());
					}
				}
			}
		}

		public void serviceSearchCompleted(int arg0, int arg1) {
			if (serverURL == null) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					System.out.println("Bad things :(");
				}
				servScan();
			} else {
				System.out.println("Service search completed\n");
			}
		}
	}
	public String serverName = null;
	public String serverURL = null;
	private Thread ddThread;
	private Main main;

	public Scanner(Main main) {
		this.main = main;
		ddThread = new Thread(new DeviceDiscoverer());
		ddThread.start();
	}
}
