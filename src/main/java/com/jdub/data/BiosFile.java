package com.jdub.data;

import java.util.List;

/**
 * Created by jameswarren on 10/14/17.
 */
public class BiosFile {
    public  String filename;
    public BiosHeader biosHeader;
    public DataTables dataTables;
    public BiosVRamInfo biosVRamInfo;
    public List<BiosVRamTimingEntry> timingEntries;
    public boolean dirty = false;
}
