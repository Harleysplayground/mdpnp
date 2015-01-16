/*******************************************************************************
 * Copyright (c) 2014, MD PnP Program
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.mdpnp.apps.testapp;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

import org.mdpnp.devices.DeviceDriverProvider;
import org.mdpnp.devices.serial.SerialProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import org.mdpnp.devices.DeviceDriverProvider.DeviceType;

/**
 * @author Jeff Plourde
 *
 */
public class Configuration {
    enum Application {
        ICE_Supervisor, ICE_Device_Interface, ICE_ParticipantOnly;
    }


    private final Application application;
    private final DeviceDriverProvider deviceFactory;
    private final String address;
    private final int domainId;

    public Configuration(Application application, int domainId, DeviceDriverProvider deviceFactory, String address) {
        this.application = application;
        this.deviceFactory = deviceFactory;
        this.address = address;
        this.domainId = domainId;
    }

    public Application getApplication() {
        return application;
    }

    public int getDomainId() {
        return domainId;
    }

    public DeviceDriverProvider getDeviceFactory() {
        return deviceFactory;
    }

    public String getAddress() {
        return address;
    }

    private static final String APPLICATION = "application";
    private static final String DOMAIN_ID   = "domainId";
    private static final String DEVICE_TYPE = "deviceType";
    private static final String ADDRESS     = "address";

    private final static Logger log = LoggerFactory.getLogger(Configuration.class);

    private void write(PrintWriter os) throws IOException {
        Properties p = new Properties();
        p.setProperty(APPLICATION, application.name());
        p.setProperty(DOMAIN_ID,   Integer.toString(domainId));
        if (null != deviceFactory)
            p.setProperty(DEVICE_TYPE, deviceFactory.getDeviceType().getAlias());
        if (null != address)
            p.setProperty(ADDRESS, address);

        p.list(os);
    }

    public static Configuration read(InputStream is) throws IOException {

        Properties p = new Properties();
        p.load(is);

        Application app = null;
        int domainId = 0;
        DeviceDriverProvider deviceType = null;
        String address = null;

        if(p.containsKey(APPLICATION)) {
            String s = p.getProperty(APPLICATION);
            try {
                app = Application.valueOf(s);
            } catch (IllegalArgumentException iae) {
                log.warn("Ignoring unknown application type:" + s);
            }
        }
        if(p.containsKey(DOMAIN_ID)) {
            String s = p.getProperty(DOMAIN_ID);
            try {
                domainId = Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                log.warn("Ignoring unknown domainId:" + s);
            }
        }
        if(p.containsKey(DEVICE_TYPE)) {
            String s = p.getProperty(DEVICE_TYPE);
            try {
                deviceType = DeviceFactory.getDeviceDriverProvider(s);
            } catch (IllegalArgumentException iae) {
                log.warn("Ignoring unknown device type:" + s);
            }
        }
        if(p.containsKey(ADDRESS)) {
            address = p.getProperty(ADDRESS);
        }

        return new Configuration(app, domainId, deviceType, address);
    }

    public static void help(Class<?> launchClass, PrintStream out) {
        out.println(launchClass.getName() + " [Application] [domainId] [DeviceType[=DeviceAddress]]");
        out.println();
        out.println("For interactive graphical interface specify no command line options");
        out.println();
        out.println("Application may be one of:");
        for (Application a : Application.values()) {
            out.println("\t" + a.name());
        }
        out.println();
        out.println("domainId must be a DDS domain identifier");
        out.println();

        out.println("if Application is " + Application.ICE_Device_Interface.name() + " then DeviceType may be one of:");
        DeviceDriverProvider[] all =  DeviceFactory.getAvailableDevices();
        for (DeviceDriverProvider ddp : all) {
           DeviceType d=ddp.getDeviceType();
           out.println("\t" + (ice.ConnectionType.Serial.equals(d.getConnectionType()) ? "*" : "") + d.getAlias());
        }
        out.println("DeviceAddress is an optional string configuring the address of the device");
        out.println();
        out.println("DeviceTypes marked with * are serial devices for which the following DeviceAddress values are currently valid:");
        for (String s : SerialProviderFactory.getDefaultProvider().getPortNames()) {
            out.println("\t" + s);
        }
    }

    public static Configuration read(String[] args_) {
        Application app = null;
        int domainId = 0;
        DeviceDriverProvider deviceType = null;
        String address = null;

        List<String> args = new ArrayList<String>(Arrays.asList(args_));
        ListIterator<String> litr = args.listIterator();
        while (litr.hasNext()) {
            try {
                app = Application.valueOf(litr.next());
                litr.remove();
                break;
            } catch (IllegalArgumentException iae) {

            }
        }
        if (null == app) {
            return null;
        }
        
        litr = args.listIterator();
        while (litr.hasNext()) {
            try {
                String x = litr.next();
                try {
                    domainId = Integer.parseInt(x);
                    litr.remove();
                    break;
                } catch (NumberFormatException nfe) {

                }

            } catch (IllegalArgumentException iae) {

            }
        }

        if (Application.ICE_Device_Interface.equals(app)) {
            litr = args.listIterator();
            while (litr.hasNext()) {
                String x = litr.next();
                String[] y = x.split("\\=");
                try {
                    deviceType = DeviceFactory.getDeviceDriverProvider(y[0]);
                    litr.remove();
                    if (y.length > 1) {
                        address = y[1];
                    }
                    break;
                } catch (IllegalArgumentException iae) {
                    log.error("Unknown DeviceType:"+y[0]);
                }
            }
        }

        return new Configuration(app, domainId, deviceType, address);
    }


    public static Configuration getInstance(String[] args) throws Exception {

        Configuration runConf = null;

        File jumpStartSettings = new File(".JumpStartSettings");
        File jumpStartSettingsHome = new File(System.getProperty("user.home"), ".JumpStartSettings");

        boolean cmdline = false;

        if (args.length > 0) {
            runConf = Configuration.read(args);
            cmdline = true;
        } else if (jumpStartSettings.exists() && jumpStartSettings.canRead()) {
            FileInputStream fis = new FileInputStream(jumpStartSettings);
            runConf = Configuration.read(fis);
            fis.close();
        } else if (jumpStartSettingsHome.exists() && jumpStartSettingsHome.canRead()) {
            FileInputStream fis = new FileInputStream(jumpStartSettingsHome);
            runConf = Configuration.read(fis);
            fis.close();
        }

        Configuration writeConf = null;

        try {
            Class<?> cls = Class.forName("com.apple.eawt.Application");
            Method m1 = cls.getMethod("getApplication");
            Method m2 = cls.getMethod("setDockIconImage", Image.class);
            m2.invoke(m1.invoke(null), ImageIO.read(Main.class.getResource("icon.png")));
        } catch (Throwable t) {
            log.debug("Not able to set Mac OS X dock icon");
        }

        if (!cmdline) {
            ConfigurationDialog d = new ConfigurationDialog(runConf, null);

            d.setIconImage(ImageIO.read(Main.class.getResource("icon.png")));
            runConf = d.showDialog();
            // It's nice to be able to change settings even without running
            if (null == runConf) {
                writeConf = d.getLastConfiguration();
            }
        } else {
            // fall through to allow configuration via a file
        }

        if (null != runConf) {
            writeConf = runConf;
        }

        if (null != writeConf) {
            if (!jumpStartSettings.exists()) {
                jumpStartSettings.createNewFile();
            }

            if (jumpStartSettings.canWrite()) {
                PrintWriter fos = new PrintWriter(jumpStartSettings);
                writeConf.write(fos);
                fos.close();
            }

            if (!jumpStartSettingsHome.exists()) {
                jumpStartSettingsHome.createNewFile();
            }

            if (jumpStartSettingsHome.canWrite()) {
                PrintWriter fos = new PrintWriter(jumpStartSettingsHome);
                writeConf.write(fos);
                fos.close();
            }
        }

        return runConf;

    }
}
