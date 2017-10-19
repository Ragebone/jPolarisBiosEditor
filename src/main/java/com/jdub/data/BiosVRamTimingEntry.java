package com.jdub.data;

import org.apache.commons.codec.binary.Hex;

/**
 * Created by jameswarren on 10/13/17.
 */
public class BiosVRamTimingEntry {
    public int ulClkRange;
//    [MarshalAs(UnmanagedType.ByValArray, SizeConst = 0x30)]
    public byte[] ucLatency;

    public String toString() {
        int tbl = ulClkRange >> 24;
        return String.format("{ %d : %d } %s", tbl ,(ulClkRange & 0x00FFFFFF) / 100, Hex.encodeHexString(ucLatency));
    }
}
