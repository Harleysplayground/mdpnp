package org.mdpnp.apps.testapp;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.TimeZone;

import javax.swing.JFrame;
import javax.swing.UIManager;

import org.mdpnp.apps.testapp.xray.XRayVentPanel;
import org.mdpnp.comms.Gateway;
import org.mdpnp.comms.nomenclature.PulseOximeter;
import org.mdpnp.comms.nomenclature.Ventilator;
import org.mdpnp.rti.dds.DDS;
import org.mdpnp.transport.Device;
import org.mdpnp.transport.NetworkController;

public class DemoApp {
	
	private static String goback = null;
	private static Runnable goBackAction = null;
	private static DemoPanel panel;
	private static String gobackPatient, gobackBed;
	private static Font gobackPatientFont;
	private static Color gobackPatientColor;
	private static int verticalAlignment, verticalTextAlignment;
	private static CardLayout ol;
	
	private static void setGoBack(String goback, Runnable goBackAction) {
		DemoApp.goback = goback;
		DemoApp.goBackAction = goBackAction;
		DemoApp.gobackPatient = panel.getPatientLabel().getText();
		DemoApp.gobackBed = panel.getBedLabel().getText();
		DemoApp.gobackPatientFont = panel.getPatientLabel().getFont();
		DemoApp.gobackPatientColor = panel.getPatientLabel().getForeground();
		DemoApp.verticalAlignment = panel.getPatientLabel().getVerticalAlignment();
		DemoApp.verticalTextAlignment = panel.getPatientLabel().getVerticalTextPosition();
		panel.getBack().setVisible(null != goback);
	}
	
	private static void goback() {
		if(null != goBackAction) {
			goBackAction.run();
			goBackAction = null;
		}
		panel.getPatientLabel().setFont(gobackPatientFont);
		panel.getPatientLabel().setForeground(gobackPatientColor);
		panel.getPatientLabel().setText(DemoApp.gobackPatient);
		panel.getBedLabel().setText(DemoApp.gobackBed);
		ol.show(panel.getContent(), DemoApp.goback);
		panel.getPatientLabel().setVerticalAlignment(DemoApp.verticalAlignment);
		panel.getPatientLabel().setVerticalTextPosition(DemoApp.verticalTextAlignment);
		panel.getBack().setVisible(false);
	}
	
	public static final void main(String[] args) throws Exception {
		if(!DDS.init()) {
			throw new RuntimeException("Unable to setup DDS");
		}
		UIManager.setLookAndFeel(new MDPnPLookAndFeel());

		
//				Pointer logger = RTICLibrary.INSTANCE.NDDS_Config_Logger_get_instance();
//		RTICLibrary.INSTANCE.NDDS_Config_Logger_set_verbosity(logger, RTICLibrary.NDDS_CONFIG_LOG_VERBOSITY_STATUS_ALL);
//		RTICLibrary.INSTANCE.NDDS_Config_Logger_set_print_format(logger, RTICLibrary.NDDS_CONFIG_LOG_PRINT_FORMAT_MAXIMAL);

		
		System.err.println("TimeZone:"+TimeZone.getDefault());
		TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
		final Gateway gateway = new Gateway();
		final NetworkController nc = new NetworkController(0, gateway);
		final VitalsModel vitalsModel = new VitalsModel();
//		vitalsModel.addInterest(PulseOximeter.PULSE);
		vitalsModel.addInterest(Ventilator.END_TIDAL_CO2_MMHG);
//		vitalsModel.addInterest(Ventilator.EXP_CO2_MMHG);
		vitalsModel.addInterest(Ventilator.RESPIRATORY_RATE);
		vitalsModel.addInterest(PulseOximeter.SPO2);
		vitalsModel.addInterest(PulseOximeter.PULSE);
//		vitalsModel.addInterest(Capnograph.AIRWAY_RESPIRATORY_RATE);
		gateway.addListener(vitalsModel);
		
		final DemoFrame frame = new DemoFrame("Integrated Clinical Environment");
		panel = new DemoPanel();
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if(goBackAction != null) {
					goBackAction.run();
					goBackAction = null;
				}
				nc.tearDown();
				super.windowClosing(e);
			}
		});
		
		frame.getContentPane().add(panel);
		ol = new CardLayout();
		panel.getContent().setLayout(ol);
		
//		LoginPanel loginPanel = new LoginPanel();
//		
//		panel.getContent().add(loginPanel, "login");
		
		
		final MainMenuPanel mainMenuPanel = new MainMenuPanel();
		panel.getContent().add(mainMenuPanel, "main");
		ol.show(panel.getContent(), "main");
		
		RoomSyncPanel roomSyncPanel = new RoomSyncPanel(gateway);
		panel.getContent().add(roomSyncPanel, "roomsync");
		
		final DevicePanel devicePanel = new DevicePanel();
		panel.getContent().add(devicePanel, "devicepanel");
		
		final PCAPanel pcaPanel = new PCAPanel(vitalsModel, gateway);
		panel.getContent().add(pcaPanel, "pca");
		
//		final Gateway xray_gateway = new Gateway();
//		final ApolloImpl xray_device = new ApolloImpl(xray_gateway);
		final XRayVentPanel xrayVentPanel = new XRayVentPanel(gateway, panel, nc.getAcceptedDevices());
//		final GetConnected getConnected = new GetConnected(frame, xray_gateway) {
//			@Override
//			protected void abortConnect() {
//				goback();
//			}
//		};
//		
//		
		panel.getContent().add(xrayVentPanel, "xray");
		
//		final EngineerConsole engineerConsole = new EngineerConsole(gateway);
//		panel.getContent().add(engineerConsole, "biomed");
		
		mainMenuPanel.getBiomedConsole().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				nc.solicit();
			}
		});
		
//		mainMenuPanel.getBiomedConsole().addActionListener(new ActionListener() {
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//
//					setGoBack("main");
//					ol.show(panel.getContent(), "biomed");
//			}
//			
//		});
		
		panel.getBack().setVisible(false);
		
		panel.getBack().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				goback();
			}
			
		});
		
		
		mainMenuPanel.getDeviceList().setModel(nc.getAcceptedDevices());
		
		mainMenuPanel.getAppList().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int idx = mainMenuPanel.getAppList().locationToIndex(e.getPoint());
				if(idx >= 0) {
					Object o = mainMenuPanel.getAppList().getModel().getElementAt(idx);
					if("Data Fusion".equals(o)) {
						setGoBack("main", null);
						ol.show(panel.getContent(), "roomsync");
					} else if("Infusion Safety".equals(o)) {
						setGoBack("main", null);
						pcaPanel.reset();
						ol.show(panel.getContent(), "pca");
					} else if("X-Ray Ventilator Sync".equals(o)) {
						setGoBack("main", new Runnable() {
							public void run() {
							    xrayVentPanel.stop();
//								getConnected.disconnect();
							}
						});
						ol.show(panel.getContent(), "xray");
						xrayVentPanel.start();
//						getConnected.connect();
					}
				}
				super.mouseClicked(e);
			}
		});
		mainMenuPanel.getDeviceList().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int idx  = mainMenuPanel.getDeviceList().locationToIndex(e.getPoint());
				if(idx>=0) {
					Device o = (Device) mainMenuPanel.getDeviceList().getModel().getElementAt(idx);
					devicePanel.setModel(gateway, o.getSource());
					setGoBack("main", null);
					ol.show(panel.getContent(), "devicepanel");
				}
				super.mouseClicked(e);
			}
		});
		
//		mainMenuPanel.getDeviceList().setModel(nc.getAcceptedDevices());
		
//		loginPanel.getClinicianId().addActionListener(new ActionListener() {
//
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				setGoBack("login", null);
//				ol.show(panel.getContent(), "main");
//			}
//			
//		});
		
		
		
		DemoPanel.setChildrenOpaque(panel, false);
		
		
		panel.setOpaque(true);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(640,480);
		frame.setVisible(true);
		nc.solicit();
	}
}