package com.goovis.androidnfcreader.util;

import java.math.BigInteger;

/**
 * Created by ranjithsubramaniam on 4/6/17.
 */

public class Util {
    // To convert binary to hex decimal & get the string format of hex
    public static String bin2hex(byte[] data) {
        String sResult = "";

        if (data != null) {
            sResult = String.format("0x%0" + (data.length * 2) + "X",
                    new BigInteger(1, data));
        }

        return sResult;
    }
}
