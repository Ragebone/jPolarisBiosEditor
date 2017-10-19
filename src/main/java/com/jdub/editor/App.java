package com.jdub.editor;


import com.jdub.data.*;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by jameswarren on 10/10/17.
 */
public class App {

    private static final byte CHECKSUM_OFFSET = (byte) 0x21;
    private static final byte HEADER_PTR = (byte) 0x48;

    private static final List<String> supportedDevices = new ArrayList<String>();
    public static final String COPY_COMMAND = "c";
    public static final String EDIT_COMMAND = "e";
    public static final String PRINT_TIMINGS_COMMAND = "t";
    public static final String PRINT_HEADER_COMMAND = "h";
    public static final String SAVE_FILE_COMMAND = "s";
    public static final String COMMAND_MENU = " (C)opy strap\n (E)dit strap\n (T)imings\n (H)eader\n (S)ave\n (Q)uit\n\nChoose command: ";
    public static final String SELECT_FILE = "Select File: ";
    public static final String MODDED_FILE_EXT = ".modded";
    private static BiosFile biosFile;

    private static final Scanner scanner = new Scanner(System.in);

    static {
        supportedDevices.add("67df");
        supportedDevices.add("1002");
    }

    private static ByteBuffer buffer;

    public static void main(String[] args) {
        selectBiosFile();

        String command = "";
        while (command.isEmpty() || !command.equalsIgnoreCase("q")) {
            command = selectCommand();
            handleCommand(command);

        }
    }

    private static void handleCommand(String command) {
        if (command.equalsIgnoreCase(COPY_COMMAND)) {
            handleCopyCommand();
            return;
        }

        if (command.equalsIgnoreCase(EDIT_COMMAND)) {
            handleEditCommand();
            return;
        }

        if (command.equalsIgnoreCase(PRINT_TIMINGS_COMMAND)) {
            printTimingEntries();
            return;
        }

        if (command.equalsIgnoreCase(PRINT_HEADER_COMMAND)) {
            printHeader(biosFile.biosHeader);
            return;
        }

        if (command.equalsIgnoreCase(SAVE_FILE_COMMAND)) {
            fixCheckSum();
            try {
                saveFile(biosFile.filename + MODDED_FILE_EXT);
                biosFile.dirty = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleEditCommand() {
        int sourceIndex = selectStrapIndex("Strap to Edit: ");

        printTimingEntry(biosFile, sourceIndex);

        scanner.nextLine();
        System.out.println();
        System.out.print("Paste timing strap: ");
        String strap = scanner.nextLine();

        if (modifyTimingEntry(sourceIndex, strap)) {
            buffer.position(biosFile.dataTables.VRAM_Info + biosFile.biosVRamInfo.usMemClkPatchTblOffset + 0x2E);
            updateTimingEntries();

            // reload straps
            buffer.position(biosFile.dataTables.VRAM_Info + biosFile.biosVRamInfo.usMemClkPatchTblOffset + 0x2E);
            biosFile.timingEntries = readVRAMTimingEntries();
            biosFile.dirty = true;
        }
    }

    private static int selectStrapIndex(String prompt) {
        System.out.println();
        System.out.print(prompt);
        return scanner.nextInt();
    }

    private static void handleCopyCommand() {
        int sourceIndex = selectStrapIndex("Source index: ");
        int destinationIndex = selectStrapIndex("Destination index: ");

        copyTimingEntry(sourceIndex, destinationIndex);

        buffer.position(biosFile.dataTables.VRAM_Info + biosFile.biosVRamInfo.usMemClkPatchTblOffset + 0x2E);
        updateTimingEntries();

        // reload straps
        buffer.position(biosFile.dataTables.VRAM_Info + biosFile.biosVRamInfo.usMemClkPatchTblOffset + 0x2E);
        biosFile.timingEntries = readVRAMTimingEntries();

        biosFile.dirty = true;
    }

    private static String selectCommand() {
        System.out.println();
        System.out.printf("    File  - %s\n", biosFile.filename);
        System.out.printf(" %s  Mod   - %s\n", (biosFile.dirty ? "*" : " "), biosFile.filename + MODDED_FILE_EXT);
        System.out.println();
        System.out.print(COMMAND_MENU);
        String command = scanner.next();
        System.out.println();
        return command;
    }

    private static void selectBiosFile() {
        File directory = new File(System.getProperty("user.dir"));
        File[] files = directory.listFiles((dir, name) -> name.endsWith("rom"));
        if (files == null || files.length == 0) {
            System.out.println("No files matching *.rom found in directory");
            System.exit(1);
            return;
        }
        outputFilesInDirectory(files);

        System.out.println();
        System.out.print(SELECT_FILE);
        int fileIdx = scanner.nextInt();
        if (fileIdx < 0 || fileIdx >= files.length) {
            System.out.println("Please select the index value next to the filename");
            System.exit(1);
            return;
        }
        String workingFile = files[fileIdx].getAbsolutePath();
        biosFile = loadFile(workingFile);
        deleteModdedFile();
    }

    private static void deleteModdedFile() {
        try {
            Files.deleteIfExists(Paths.get(biosFile.filename + MODDED_FILE_EXT));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void outputFilesInDirectory(File[] files) {
        int i = 0;
        for (File file : files) {
            System.out.printf(" %d %s\n", i++, file.getName());
        }
    }

    private static void printHeader(BiosHeader biosHeader) {
        System.out.println("Vendor ID: " + Integer.toHexString(biosHeader.usVendorID));
        System.out.println("Device ID: " + Integer.toHexString(biosHeader.usDeviceID));
        System.out.println("Sub ID: " + Integer.toHexString(biosHeader.usSubsystemID));
        System.out.println("Sub Vendor ID: " + Integer.toHexString(biosHeader.usSubsystemVendorID));
    }

    private static BiosFile loadFile(String filename) {
        if (!new File(filename).exists()) {
            System.out.println("File does not exist");
            System.exit(1);
        }
        BiosFile biosFile = new BiosFile();
        try {
            buffer = ByteBuffer.wrap(Files.readAllBytes(new File(filename).toPath()));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            if (buffer.capacity() < 524288) {
                System.out.println("This BIOS is less than the standard 512KB size.\nFlashing this BIOS may corrupt your graphics card.");
            }
            buffer.position(HEADER_PTR);
            buffer.position(readHeaderOffset());

            biosFile.filename = filename;
            biosFile.biosHeader = readHeader();

            if (!deviceSupported(biosFile.biosHeader)) {
                System.out.printf("The %s is not supported", Integer.toHexString(biosFile.biosHeader.usDeviceID));
                return null;
            }

            buffer.position(biosFile.biosHeader.usMasterDataTableOffset);
            biosFile.dataTables = readDataTables();

            buffer.position(biosFile.dataTables.VRAM_Info);
            biosFile.biosVRamInfo = readVRAMInfo();

            buffer.position(biosFile.dataTables.VRAM_Info + biosFile.biosVRamInfo.usMemClkPatchTblOffset + 0x2E);
            biosFile.timingEntries = readVRAMTimingEntries();

            if (isCheckSumGood(generateChecksum())) {
                System.out.println("Checksum is GOOD");
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return biosFile;
    }

    private static boolean deviceSupported(BiosHeader biosHeader) {
        return supportedDevices.contains(Integer.toHexString(biosHeader.usDeviceID));
    }

    private static void saveFile(String filename) throws IOException {
        buffer.position(0);
        File outputFile = new File(filename);
        FileChannel writeChannel = new FileOutputStream(outputFile).getChannel();
        writeChannel.write(buffer);
        writeChannel.close();
        System.out.println("Saved file - " + filename);
    }

    private static void printTimingEntry(BiosFile biosFile, int sourceIndex) {
        BiosVRamTimingEntry timingEntry = biosFile.timingEntries.get(sourceIndex);
        System.out.println("Strap " + sourceIndex++ + " " + timingEntry.toString());
    }

    private static void printTimingEntries() {
        int i = 0;
        for (BiosVRamTimingEntry entry : biosFile.timingEntries) {
            System.out.println("Strap " + i++ + " " + entry.toString());
        }
    }

    private static byte generateChecksum() {
        buffer.position(0x02);
        int size = read8Bit() * 512;
        byte offset = 0;

        buffer.position(0);
        for (int i = 0; i < size; i++) {
            offset += buffer.get();
        }
        return offset;
    }

    private static boolean isCheckSumGood(byte offset) {
        byte checksum = buffer.get(CHECKSUM_OFFSET);
        if (checksum == (checksum - offset)) {
            return true;
        }
        return false;
    }

    private static void fixCheckSum() {
        byte offset = generateChecksum();
        if (isCheckSumGood(offset)) return;

        buffer.put(CHECKSUM_OFFSET, (byte) (buffer.get(CHECKSUM_OFFSET) - offset));
        offset = generateChecksum();
        if (isCheckSumGood(offset)) {
            System.out.println("Checksum UPDATED");
        }
    }

    private static boolean modifyTimingEntry(int destinationIndex, String strap) {
        BiosVRamTimingEntry dest = biosFile.timingEntries.get(destinationIndex);
        try {
            byte[] strapBytes = Hex.decodeHex(strap.toCharArray());
            if (strapBytes.length != 48) {
                System.out.println("Not a valid Strap");
                return false;
            }
            dest.ucLatency = strapBytes;
        } catch (DecoderException e) {
            System.out.println("Not a valid Strap");
            return false;
        }
        return true;
    }

    private static void copyTimingEntry(int sourceIndex, int destinationIndex) {
        BiosVRamTimingEntry source = biosFile.timingEntries.get(sourceIndex);
        BiosVRamTimingEntry dest = biosFile.timingEntries.get(destinationIndex);

        dest.ucLatency = source.ucLatency;
    }

    private static void updateTimingEntries() {
        for (BiosVRamTimingEntry entry : biosFile.timingEntries) {
            buffer.putInt(entry.ulClkRange);
            buffer.put(entry.ucLatency);
        }

    }

    private static List<BiosVRamTimingEntry> readVRAMTimingEntries() {
        List<BiosVRamTimingEntry> timingEntries = new ArrayList<BiosVRamTimingEntry>();
        for (int i = 0; i < 24; i++) {
            BiosVRamTimingEntry timingEntry = new BiosVRamTimingEntry();
            timingEntry.ulClkRange = readUnsigned32Bit();

            if (timingEntry.ulClkRange == 0)
                break;

            timingEntry.ucLatency = new byte[0x30];
            buffer.get(timingEntry.ucLatency);

            timingEntries.add(timingEntry);
        }

        return timingEntries;
    }

    private static BiosVRamInfo readVRAMInfo() throws IOException {
        BiosVRamInfo biosVRamInfo = new BiosVRamInfo();

        biosVRamInfo.usStructureSize = read16Bit();
        biosVRamInfo.ucTableFormatRevision = read8Bit();
        biosVRamInfo.ucTableContentRevision = read8Bit();

        biosVRamInfo.usMemAdjustTblOffset = readUnsigned16Bit();
        biosVRamInfo.usMemClkPatchTblOffset = readUnsigned16Bit();
        biosVRamInfo.usMcAdjustPerTileTblOffset = readUnsigned16Bit();
        biosVRamInfo.usMcPhyInitTableOffset = readUnsigned16Bit();
        biosVRamInfo.usDramDataRemapTblOffset = readUnsigned16Bit();
        biosVRamInfo.usReserved1 = readUnsigned16Bit();
        biosVRamInfo.ucNumOfVRAMModule = read8Bit();
        biosVRamInfo.ucMemoryClkPatchTblVer = read8Bit();
        biosVRamInfo.ucVramModuleVer = read8Bit();
        biosVRamInfo.ucMcPhyTileNum = read8Bit();

        return biosVRamInfo;
    }

    private static DataTables readDataTables() throws IOException {
        DataTables dataTables = new DataTables();

        dataTables.usStructureSize = read16Bit();
        dataTables.ucTableFormatRevision = read8Bit();
        dataTables.ucTableContentRevision = read8Bit();

        dataTables.UtilityPipeLine = readUnsigned16Bit();
        dataTables.MultimediaCapabilityInfo = readUnsigned16Bit();
        dataTables.MultimediaConfigInfo = readUnsigned16Bit();
        dataTables.StandardVESA_Timing = readUnsigned16Bit();
        dataTables.FirmwareInfo = readUnsigned16Bit();
        dataTables.PaletteData = readUnsigned16Bit();
        dataTables.LCD_Info = readUnsigned16Bit();
        dataTables.DIGTransmitterInfo = readUnsigned16Bit();
        dataTables.SMU_Info = readUnsigned16Bit();
        dataTables.SupportedDevicesInfo = readUnsigned16Bit();
        dataTables.GPIO_I2C_Info = readUnsigned16Bit();
        dataTables.VRAM_UsageByFirmware = readUnsigned16Bit();
        dataTables.GPIO_Pin_LUT = readUnsigned16Bit();
        dataTables.VESA_ToInternalModeLUT = readUnsigned16Bit();
        dataTables.GFX_Info = readUnsigned16Bit();
        dataTables.PowerPlayInfo = readUnsigned16Bit();
        dataTables.GPUVirtualizationInfo = readUnsigned16Bit();
        dataTables.SaveRestoreInfo = readUnsigned16Bit();
        dataTables.PPLL_SS_Info = readUnsigned16Bit();
        dataTables.OemInfo = readUnsigned16Bit();
        dataTables.XTMDS_Info = readUnsigned16Bit();
        dataTables.MclkSS_Info = readUnsigned16Bit();
        dataTables.Object_Header = readUnsigned16Bit();
        dataTables.IndirectIOAccess = readUnsigned16Bit();
        dataTables.MC_InitParameter = readUnsigned16Bit();
        dataTables.ASIC_VDDC_Info = readUnsigned16Bit();
        dataTables.ASIC_InternalSS_Info = readUnsigned16Bit();
        dataTables.TV_VideoMode = readUnsigned16Bit();
        dataTables.VRAM_Info = readUnsigned16Bit();
        dataTables.MemoryTrainingInfo = readUnsigned16Bit();
        dataTables.IntegratedSystemInfo = readUnsigned16Bit();
        dataTables.ASIC_ProfilingInfo = readUnsigned16Bit();
        dataTables.VoltageObjectInfo = readUnsigned16Bit();
        dataTables.PowerSourceInfo = readUnsigned16Bit();
        dataTables.ServiceInfo = readUnsigned16Bit();

        return dataTables;
    }

    private static BiosHeader readHeader() throws IOException {
        BiosHeader biosHeader = new BiosHeader();

        biosHeader.usStructureSize = read16Bit();
        biosHeader.ucTableFormatRevision = read8Bit();
        biosHeader.ucTableContentRevision = read8Bit();

        biosHeader.uaFirmWareSignature = readUnsigned32Bit();
        biosHeader.usBiosRuntimeSegmentAddress = readUnsigned16Bit();
        biosHeader.usProtectedModeInfoOffset = readUnsigned16Bit();
        biosHeader.usConfigFilenameOffset = readUnsigned16Bit();
        biosHeader.usCRC_BlockOffset = readUnsigned16Bit();
        biosHeader.usBIOS_BootupMessageOffset = readUnsigned16Bit();
        biosHeader.usInt10Offset = readUnsigned16Bit();
        biosHeader.usPciBusDevInitCode = readUnsigned16Bit();
        biosHeader.usIoBaseAddress = readUnsigned16Bit();
        biosHeader.usSubsystemVendorID = readUnsigned16Bit();
        biosHeader.usSubsystemID = readUnsigned16Bit();
        biosHeader.usPCI_InfoOffset = readUnsigned16Bit();
        biosHeader.usMasterCommandTableOffset = readUnsigned16Bit();
        biosHeader.usMasterDataTableOffset = readUnsigned16Bit();
        biosHeader.ucExtendedFunctionCode = read8Bit();
        biosHeader.ucReserved = read8Bit();
        biosHeader.ulPSPDirTableOffset = readUnsigned32Bit();
        biosHeader.usVendorID = readUnsigned16Bit();
        biosHeader.usDeviceID = readUnsigned16Bit();

        return biosHeader;
    }

    private static int readHeaderOffset() throws IOException {
        return readUnsigned16Bit();
    }

    private static int read32Bit() {
        return buffer.getInt();
    }

    private static int readUnsigned32Bit() {
        // Little Endian
        byte b4 = buffer.get();
        byte b3 = buffer.get();
        byte b2 = buffer.get();
        byte b1 = buffer.get();

        // Read as UNSIGNED
        int i4 = b4 & 0xFF;
        int i3 = b3 & 0xFF;
        int i2 = b2 & 0xFF;
        int i1 = b1 & 0x7F;

        int result = (i1 << 24) | (i2 << 16) | (i3 << 8) | i4;
        if ((b1 & 0x80000000) > 0) {
            result = result + 0x80000000;
        }
        return result;
    }

    private static int read16Bit() {
        return buffer.getShort();
    }

    private static int readUnsigned16Bit() {
        // Little Endian
        byte b2 = buffer.get();
        byte b1 = buffer.get();

        // Read as UNSIGNED
        int i2 = b2 & 0xFF;
        int i1 = b1 & 0x7F;
        int result = (i1 << 8) | i2;
        if ((b1 & 0x8000) > 0) {
            result = result + 0x8000;
        }

        return result;
    }

    private static int read8Bit() {
        // Little Endian
        byte b1 = buffer.get();
        // Read as UNSIGNED
        int i1 = b1 & 0x7F;
        int result = i1;
        if ((b1 & 0x80) > 0) {
            result = result + 0x80;
        }
        return result;
    }

}
