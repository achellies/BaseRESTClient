package com.limemobile.app.sdk.http.internal;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.cookie.Cookie;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;

public class HttpUtils {
    protected static final String COOKIE_DATE_FORMAT = "EEE, dd MMM yyyy hh:mm:ss z";

    public static final int NETWORK_TYPE_MOBILE = ConnectivityManager.TYPE_MOBILE;
    public static final int NETWORK_TYPE_WIFI = ConnectivityManager.TYPE_WIFI;
    public static final String INTERFACE_ETH0 = "eth0";
    public static final String INTERFACE_WLAN0 = "wlan0";

    public static String createUserAgentString(Context applicationContext) {
        String appName = "";
        String appVersion = "";
        int height = 0;
        int width = 0;
        DisplayMetrics display = applicationContext.getResources()
                .getDisplayMetrics();
        Configuration config = applicationContext.getResources()
                .getConfiguration();

        // Always send screen dimension for portrait mode
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            height = display.widthPixels;
            width = display.heightPixels;
        } else {
            width = display.widthPixels;
            height = display.heightPixels;
        }

        try {
            PackageInfo packageInfo = applicationContext.getPackageManager()
                    .getPackageInfo(applicationContext.getPackageName(),
                            PackageManager.GET_CONFIGURATIONS);
            appName = packageInfo.packageName;
            appVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException ignore) {
            // this should never happen, we are looking up ourself
        }

        // Tries to conform to default android UA string without the Safari /
        // webkit noise, plus adds the screen dimensions
        return String
                .format("%1$s/%2$s (%3$s; U; Android %4$s; %5$s-%6$s; %12$s Build/%7$s; %8$s) %9$dX%10$d %11$s %12$s",
                        appName, appVersion, System.getProperty("os.name",
                                "Linux"), Build.VERSION.RELEASE, config.locale
                                .getLanguage().toLowerCase(), config.locale
                                .getCountry().toLowerCase(), Build.ID,
                        Build.BRAND, width, height, Build.MANUFACTURER,
                        Build.MODEL);
    }

    public static Bundle getCookiesBundle(List<Cookie> cookies) {
        Bundle bundle = new Bundle();
        if (cookies == null) {
            return bundle;
        }
        Calendar calendar = Calendar.getInstance();
        Map<String, ArrayList<String>> cookiesMap = new HashMap<String, ArrayList<String>>();
        for (Cookie cookie : cookies) {
            String domain = cookie.getDomain();
            if (TextUtils.isEmpty(domain)) {
                domain = "";
            }
            StringBuilder builder = new StringBuilder();
            builder.append(cookie.getName());
            builder.append("=");
            builder.append(cookie.getValue());
            builder.append("; domain=");
            builder.append(cookie.getDomain());
            if (cookie.getExpiryDate() != null) {
                builder.append("; expires=");
                calendar.setTime(cookie.getExpiryDate());
                builder.append(new SimpleDateFormat(COOKIE_DATE_FORMAT)
                        .format(calendar.getTimeInMillis()));
            }
            builder.append("; path=");
            builder.append(cookie.getPath());
            builder.append("; version=");
            builder.append(cookie.getVersion());
            ArrayList<String> list = null;
            if (cookiesMap.containsKey(domain)) {
                list = cookiesMap.get(domain);
                list.add(builder.toString());
            } else {
                list = new ArrayList<String>();
                list.add(builder.toString());
                cookiesMap.put(domain, list);
            }
        }
        Set<String> keys = cookiesMap.keySet();
        for (String key : keys) {
            bundle.putStringArrayList(key, cookiesMap.get(key));
        }
        return bundle;
    }

    public static boolean isNetworkAvaliable(Context ctx) {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            final NetworkInfo net = connectivityManager.getActiveNetworkInfo();
            if (net != null && net.isAvailable() && net.isConnected()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns MAC address of the given interface name.
     * 
     * @param interfaceName
     *            eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    public static String getMACAddress(Context ctx, String interfaceName,
            String defaultMac) {
        String macAddress = null;
        try {
            List<NetworkInterface> interfaces = Collections
                    .list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName))
                        continue;
                }
                byte[] mac = intf.getHardwareAddress();
                macAddress = convertMacAddress(mac);
                break;
            }
        } catch (Exception ex) {
        } // for now eat exceptions

        if (TextUtils.isEmpty(macAddress)
                && INTERFACE_WLAN0.equals(interfaceName)) {
            return getWifiMacAddress(ctx);
        }
        return macAddress;
        /*
         * try { // this is so Linux hack return
         * loadFileAsString("/sys/class/net/" +interfaceName +
         * "/address").toUpperCase().trim(); } catch (IOException ex) { return
         * null; }
         */
    }

    private static String convertMacAddress(byte[] mac) {
        if (mac == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        for (int idx = 0; idx < mac.length; idx++)
            buf.append(String.format("%02X:", mac[idx]));
        if (buf.length() > 0)
            buf.deleteCharAt(buf.length() - 1);

        // FIXME 为什么有些android获取不到mac地址，或者获取到的mac地址位数不对
        if (buf.length() != 12 && buf.length() != 17) {
            return null;
        }
        return buf.toString();
    }

    /**
     * http://www.gubatron.com/blog/2010/09/19/android-programming-how-to-obtain
     * -the-wifis-corresponding-networkinterface/
     * 
     * @param ctx
     * @return
     */
    private static String getWifiMacAddress(Context ctx) {
        WifiManager manager = null;
        try {
            manager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
            if (manager == null || manager.getConnectionInfo() == null) {
                return null;
            }
            Enumeration<NetworkInterface> interfaces = null;
            // the WiFi network interface will be one of these.
            interfaces = NetworkInterface.getNetworkInterfaces();

            // We'll use the WiFiManager's ConnectionInfo IP address and compare
            // it with the ips of the enumerated NetworkInterfaces to find the
            // WiFi
            // NetworkInterface.

            // Wifi manager gets a ConnectionInfo object that has the ipAdress
            // as an int It's endianness could be different as the one on
            // java.net.InetAddress
            // maybe this varies from device to device, the android API has no
            // documentation on this method.
            int wifiIP = manager.getConnectionInfo().getIpAddress();

            // so I keep the same IP number with the reverse endianness
            int reverseWifiIP = Integer.reverseBytes(wifiIP);

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // since each interface could have many InetAddresses...
                Enumeration<InetAddress> inetAddresses = iface
                        .getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress nextElement = inetAddresses.nextElement();
                    int byteArrayToInt = byteArrayToInt(
                            nextElement.getAddress(), 0);

                    // grab that IP in byte[] form and convert it to int, then
                    // compare it to the IP given by the WifiManager's
                    // ConnectionInfo. We compare
                    // in both endianness to make sure we get it.
                    if (byteArrayToInt == wifiIP
                            || byteArrayToInt == reverseWifiIP) {
                        byte[] mac = iface.getHardwareAddress();
                        return convertMacAddress(mac);
                    }
                }
            }
        } catch (Exception e) {
        }

        return null;
    }

    private static final int byteArrayToInt(byte[] arr, int offset) {
        if (arr == null || arr.length - offset < 4)
            return -1;

        int r0 = (arr[offset] & 0xFF) << 24;
        int r1 = (arr[offset + 1] & 0xFF) << 16;
        int r2 = (arr[offset + 2] & 0xFF) << 8;
        int r3 = arr[offset + 3] & 0xFF;
        return r0 + r1 + r2 + r3;
    }

    public static String getIPv4Address() {
        return getIPAddress(true);
    }

    public static String getIPv6Address() {
        return getIPAddress(false);
    }

    /**
     * Get IP address from first non-localhost interface
     * 
     * @param ipv4
     *            true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections
                    .list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf
                        .getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port
                                                                // suffix
                                return delim < 0 ? sAddr : sAddr.substring(0,
                                        delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public static int getNetworkType(Context con) {
        ConnectivityManager cm = (ConnectivityManager) con
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return NETWORK_TYPE_MOBILE;
        NetworkInfo netinfo = cm.getActiveNetworkInfo();
        if (netinfo != null && netinfo.isAvailable()) {
            if (netinfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return NETWORK_TYPE_WIFI;
            } else {
                return NETWORK_TYPE_MOBILE;
            }
        }
        return NETWORK_TYPE_MOBILE;
    }
}
