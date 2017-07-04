package com.hdr.wristband.xrz;

import android.util.Log;
import com.hdr.wristband.utils.StringUtils;

import java.util.Random;

/**
 * Created by hdr on 16/7/11.
 */
public class ProtocolHelper {
    private final static byte COMMAND_ENCRYPT_OFFSET = (byte) 0xD5;

    private final static byte COMMAND_FIRST_BYTE = (byte) 0x8A;
    private final static byte COMMAND_SECOND_BYTE = (byte) 0xFF;
    private final static byte COMMAND_LAST_BYTE = 0x06;

    private final static Random random = new Random();

    /**
     * 合并 命令头,用于没有数据段的指令
     *
     * @param cmdHead 命令头
     * @return 返回最终下发的命令
     */
    public static byte[] merge(byte[] cmdHead) {
        return merge(cmdHead, new byte[0]);
    }

    public static byte[] merge(int[] cmdHead) {
        return merge(intArray2ByteArray(cmdHead), new byte[0]);
    }

    public static byte[] intArray2ByteArray(int[] intArr) {
        byte[] byteArr = new byte[intArr.length];
        for (int i = 0; i < intArr.length; i++) {
            byteArr[i] = (byte) (intArr[i] & 0xFF);
        }
        return byteArr;
    }

    public static byte[] merge(int[] cmdHead, int[] data){
        return merge(intArray2ByteArray(cmdHead), intArray2ByteArray(data));
    }

    public static void decryptData(byte[] cmd) {
        byte secretKey = (byte) (cmd[8] ^ (cmd[4] & cmd[5]));
        for (int i = 9; i < cmd.length - 2; i++) {
            cmd[i] = (byte) (cmd[i] ^ secretKey);
        }
    }

    public static byte[] decryptData(byte cmdHead1,byte cmdHead2,byte pbKey,byte...data){
        byte secretKey = (byte) (pbKey ^ (cmdHead1& cmdHead2));
        byte[] decryptData = new byte[data.length];
        for(int i=0;i<data.length;i++) {
            decryptData[i] = (byte) (data[i]^secretKey);
        }
        return decryptData;
    }

    /**
     * 合并 命令头,数据
     *
     * @param cmdHead 命令头
     * @param data    数据
     * @return 返回最终下发的命令
     */
    public static byte[] merge(byte[] cmdHead, byte[] data) {
        byte[] randomBuf = new byte[2];
        random.nextBytes(randomBuf);
        final byte secretKey = (byte) ((randomBuf[0] ^ randomBuf[1]) & COMMAND_ENCRYPT_OFFSET);
        final byte publicKey = (byte) ((cmdHead[0] & cmdHead[1]) ^ secretKey);
        int cmdLength = 11 + data.length;
        byte[] cmd = new byte[cmdLength];
        cmd[0] = COMMAND_FIRST_BYTE;
        cmd[1] = COMMAND_SECOND_BYTE;
        cmd[2] = (byte) (cmdLength / 256);
        cmd[3] = (byte) (cmdLength % 256);
        cmd[4] = cmdHead[0];
        cmd[5] = cmdHead[1];
        cmd[6] = randomBuf[0];
        cmd[7] = randomBuf[1];
        cmd[8] = publicKey;

        if (data.length > 0) {
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (secretKey ^ data[i]);
            }
            System.arraycopy(data, 0, cmd, 9, data.length);
        }
        cmd[cmdLength - 1] = COMMAND_LAST_BYTE;


        return cmd;
    }

}
