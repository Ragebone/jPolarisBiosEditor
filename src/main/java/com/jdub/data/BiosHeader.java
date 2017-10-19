package com.jdub.data;

/**
 * Created by jameswarren on 10/10/17.
 */
public class BiosHeader {
    public int usStructureSize;
    public int ucTableFormatRevision;
    public int ucTableContentRevision;

    public int uaFirmWareSignature;
    public int usBiosRuntimeSegmentAddress;
    public int usProtectedModeInfoOffset;
    public int usConfigFilenameOffset;
    public int usCRC_BlockOffset;
    public int usBIOS_BootupMessageOffset;
    public int usInt10Offset;
    public int usPciBusDevInitCode;
    public int usIoBaseAddress;
    public int usSubsystemVendorID;
    public int usSubsystemID;
    public int usPCI_InfoOffset;
    public int usMasterCommandTableOffset;
    public int usMasterDataTableOffset;
    public int ucExtendedFunctionCode;
    public int ucReserved;
    public int ulPSPDirTableOffset;
    public int usVendorID;
    public int usDeviceID;
}
