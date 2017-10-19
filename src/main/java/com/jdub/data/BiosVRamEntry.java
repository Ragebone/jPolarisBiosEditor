package com.jdub.data;

/**
 * Created by jameswarren on 10/10/17.
 */
public class BiosVRamEntry {
    public int ulChannelMapCfg;
    public int usModuleSize;
    public int usMcRamCfg;
    public int usEnableChannels;
    public int ucExtMemoryID;
    public int ucMemoryType;
    public int ucChannelNum;
    public int ucChannelWidth;
    public int ucDensity;
    public int ucBankCol;
    public int ucMisc;
    public int ucVREFI;
    public int usReserved;
    public int usMemorySize;
    public int ucMcTunningSetId;
    public int ucRowNum;
    public int usEMRS2Value;
    public int usEMRS3Value;
    public int ucMemoryVenderID;
    public int ucRefreshRateFactor;
    public int ucFIFODepth;
    public int ucCDR_Bandwidth;
    public int ulChannelMapCfg1;
    public int ulBankMapCfg;
    public int ulReserved;
//            [MarshalAs(UnmanagedType.ByValArray, SizeConst = 20)]
    public byte[] strMemPNString;
}
