package com.inspur.eipatomapi.util;

public class IpUtil {
    IpUtil() {
    }

    public static String ipToLong(String strIp) {
        String[] ip = strIp.split("\\.");
        return Long.toString((Long.parseLong(ip[0]) << 24) + (Long.parseLong(ip[1]) << 16) + (Long.parseLong(ip[2]) << 8) + Long.parseLong(ip[3]));
    }

    private static String longToIP(long longIp) {
        StringBuffer sb = new StringBuffer("");
        sb.append(String.valueOf(longIp >>> 24));
        sb.append(".");
        sb.append(String.valueOf((longIp & 16777215L) >>> 16));
        sb.append(".");
        sb.append(String.valueOf((longIp & 65535L) >>> 8));
        sb.append(".");
        sb.append(String.valueOf(longIp & 255L));
        return sb.toString();
    }

    public static void test() {
        System.out.println(ipToLong("219.239.110.138"));
        Long l = new Long("3689901706");
        System.out.println(longToIP(l));
    }
}
