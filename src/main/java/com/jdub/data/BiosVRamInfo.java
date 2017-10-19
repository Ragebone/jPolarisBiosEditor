package com.jdub.data;

/**
 * Created by jameswarren on 10/10/17.
 */
public class BiosVRamInfo {
    public int usStructureSize;
    public int ucTableFormatRevision;
    public int ucTableContentRevision;

    public int usMemAdjustTblOffset;
    public int usMemClkPatchTblOffset;
    public int usMcAdjustPerTileTblOffset;
    public int usMcPhyInitTableOffset;
    public int usDramDataRemapTblOffset;
    public int usReserved1;
    public int ucNumOfVRAMModule;
    public int ucMemoryClkPatchTblVer;
    public int ucVramModuleVer;
    public int ucMcPhyTileNum;
}
