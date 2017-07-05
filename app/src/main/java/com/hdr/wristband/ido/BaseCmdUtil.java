package com.hdr.wristband.ido;

/**
 * Created by yolanda on 2017/6/13.
 */

public class BaseCmdUtil {
    public static final int BYTE_LEN_TOTAL = 20;

    public BaseCmdUtil() {
    }

    public static boolean isHealthCmd(byte[] cmd) {
        return getCmdId(cmd) == 8 | getCmdId(cmd) == 9;
    }

    public static void copy(byte[] from, byte[] to) {
        if(from != null && to != null) {
            int lenFrom = from.length;
            int lenTo = to.length;
            lenFrom = lenFrom < lenTo?lenFrom:lenTo;

            for(int i = 0; i < lenFrom; ++i) {
                to[i] = from[i];
            }

        }
    }

    public static byte[] createCmd(byte cmdId, byte key, byte[] value) {
        byte[] cmd = createNullCmd();
        cmd[0] = cmdId;
        cmd[1] = key;
        int index = 2;
        byte[] var8 = value;
        int var7 = value.length;

        for(int var6 = 0; var6 < var7; ++var6) {
            byte b = var8[var6];
            cmd[index++] = b;
            if(index >= 20) {
                break;
            }
        }

        return cmd;
    }

    public static byte[] createCmd(byte cmdId, byte key, byte value) {
        byte[] cmd = createNullCmd();
        cmd[0] = cmdId;
        cmd[1] = key;
        cmd[2] = value;
        return cmd;
    }

    private static byte[] createNullCmd() {
        byte[] cmd = new byte[20];

        for(int i = 0; i < 20; ++i) {
            cmd[i] = 0;
        }

        return cmd;
    }

    public static byte getCmdId(byte[] cmd) {
        return cmd[0];
    }

    public static byte getCmdKey(byte[] cmd) {
        byte key = -1;
        if(cmd != null && cmd.length == 20) {
            key = cmd[1];
        }

        return key;
    }

    public static boolean isHealthHead(byte[] cmd){
        if(isHealthCmd(cmd)){
            if((cmd[1] >=3 && cmd[1] <=8) && (cmd[2] == 1 || cmd[2] == 2)){
                return true;
            }
        }
        return false;
    }
}
