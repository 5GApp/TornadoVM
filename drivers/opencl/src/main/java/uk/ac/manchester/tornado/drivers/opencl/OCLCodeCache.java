/*
 * This file is part of Tornado: A heterogeneous programming framework:
 * https://github.com/beehive-lab/tornadovm
 *
 * Copyright (c) 2020, APT Group, Department of Computer Science,
 * School of Engineering, The University of Manchester. All rights reserved.
 * Copyright (c) 2013-2020, APT Group, Department of Computer Science,
 * The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Authors: James Clarkson
 *
 */
package uk.ac.manchester.tornado.drivers.opencl;

import static uk.ac.manchester.tornado.api.exceptions.TornadoInternalError.guarantee;
import static uk.ac.manchester.tornado.drivers.opencl.enums.OCLBuildStatus.CL_BUILD_SUCCESS;
import static uk.ac.manchester.tornado.runtime.common.Tornado.debug;
import static uk.ac.manchester.tornado.runtime.common.Tornado.error;
import static uk.ac.manchester.tornado.runtime.common.Tornado.getProperty;
import static uk.ac.manchester.tornado.runtime.common.Tornado.info;
import static uk.ac.manchester.tornado.runtime.common.Tornado.warn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

import uk.ac.manchester.tornado.api.exceptions.TornadoBailoutRuntimeException;
import uk.ac.manchester.tornado.api.exceptions.TornadoRuntimeException;
import uk.ac.manchester.tornado.drivers.opencl.enums.OCLBuildStatus;
import uk.ac.manchester.tornado.drivers.opencl.enums.OCLDeviceType;
import uk.ac.manchester.tornado.drivers.opencl.exceptions.OCLException;
import uk.ac.manchester.tornado.drivers.opencl.graal.OCLInstalledCode;
import uk.ac.manchester.tornado.runtime.common.RuntimeUtilities;
import uk.ac.manchester.tornado.runtime.common.Tornado;
import uk.ac.manchester.tornado.runtime.common.TornadoOptions;
import uk.ac.manchester.tornado.runtime.tasks.meta.TaskMetaData;

public class OCLCodeCache {

    public static final String LOOKUP_BUFFER_KERNEL_NAME = "lookupBufferAddress";

    private static final String FALSE = "False";
    private static final String TRUE = "True";
    private final String OPENCL_SOURCE_SUFFIX = ".cl";
    private final boolean OPENCL_CACHE_ENABLE = Boolean.parseBoolean(getProperty("tornado.opencl.codecache.enable", FALSE));
    private final boolean OPENCL_DUMP_BINS = Boolean.parseBoolean(getProperty("tornado.opencl.codecache.dump", FALSE));
    private final boolean OPENCL_DUMP_SOURCE = Boolean.parseBoolean(getProperty("tornado.opencl.source.dump", FALSE));
    private final boolean OPENCL_PRINT_SOURCE = Boolean.parseBoolean(getProperty("tornado.opencl.source.print", FALSE));
    private final boolean PRINT_LOAD_TIME = false;
    private final String OPENCL_CACHE_DIR = getProperty("tornado.opencl.codecache.dir", "/var/opencl-codecache");
    private final String OPENCL_SOURCE_DIR = getProperty("tornado.opencl.source.dir", "/var/opencl-compiler");
    private final String OPENCL_LOG_DIR = getProperty("tornado.opencl.log.dir", "/var/opencl-logs");
    private final String FPGA_CONFIGURATION_FILE = getProperty("tornado.fpga.conf.file", null);
    private final String INTEL_ALTERA_OPENCL_COMPILER = "aoc";
    private final String XILINX_OPENCL_COMPILER = "xocc";
    private final String FPGA_CLEANUP_SCRIPT = System.getenv("TORNADO_SDK") + "/bin/cleanFpga.sh";
    private String fpgaName;
    private String compilationFlags;
    private String directoryBitstream;
    public static String fpgaBinLocation;
    private String fpgaSourceDir;

    // ID -> KernelName (TaskName)
    private ConcurrentHashMap<String, ArrayList<Pair>> pendingTasks;

    private ArrayList<String> linkObjectFiles;

    /**
     * OpenCL Binary Options: -Dtornado.precompiled.binary=<path/to/binary,task>
     *
     * e.g.,
     *
     * <p>
     * <code>
     * -Dtornado.precompiled.binary=</tmp/saxpy,s0.t0.device=0:1>
     * </code>
     * </p>
     */
    private final StringBuffer OPENCL_BINARIES = TornadoOptions.FPGA_BINARIES;

    private final boolean PRINT_WARNINGS = false;

    private final ConcurrentHashMap<String, OCLInstalledCode> cache;
    private final OCLDeviceContext deviceContext;

    private boolean kernelAvailable;

    private HashMap<String, String> precompiledBinariesPerDevice;

    private static class Pair {
        private String taskName;
        private String entryPoint;

        public Pair(String id, String entryPoint) {
            this.taskName = id;
            this.entryPoint = entryPoint;
        }
    }

    public OCLCodeCache(OCLDeviceContext deviceContext) {
        this.deviceContext = deviceContext;
        cache = new ConcurrentHashMap<>();
        pendingTasks = new ConcurrentHashMap<>();
        linkObjectFiles = new ArrayList<>();

        if (deviceContext.isPlatformFPGA()) {
            precompiledBinariesPerDevice = new HashMap<>();
            parseFPGAConfigurationFile();
            if (OPENCL_BINARIES != null) {
                processPrecompiledBinaries();
            }
        }
    }

    private void parseFPGAConfigurationFile() {
        FileReader fileReader;
        BufferedReader bufferedReader;
        try {
            fileReader = new FileReader((FPGA_CONFIGURATION_FILE != null) ? FPGA_CONFIGURATION_FILE
                    : (new File("").getAbsolutePath() + ((deviceContext.getDevice().getDeviceVendor().toLowerCase().equals("xilinx")) ? "/etc/xilinx-fpga.conf" : "/etc/intel-fpga.conf")));
            bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                switch (line.split("=")[0]) {
                    case "DEVICE_NAME":
                        fpgaName = line.split("=")[1];
                        break;
                    case "DIRECTORY_BITSTREAM":
                        directoryBitstream = line.split("=")[1];
                        fpgaBinLocation = "./" + directoryBitstream + LOOKUP_BUFFER_KERNEL_NAME;
                        fpgaSourceDir = directoryBitstream;
                        break;
                    case "FLAGS":
                        compilationFlags = line.split("=")[1];
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Wrong configuration file or invalid settings. Please ensure that you have configured the configuration file with valid options!");
            System.exit(1);
        }
    }

    private void processPrecompiledBinaries() {
        String[] binaries = OPENCL_BINARIES.toString().split(",");

        if (binaries.length == 1) {
            // We try to parse a configuration file
            binaries = processPrecompiledBinariesFromFile(binaries[0]);
        } else if ((binaries.length % 2) != 0) {
            throw new RuntimeException("tornado.precompiled.binary=<path>,taskName.device");
        }

        for (int i = 0; i < binaries.length; i += 2) {
            String binaryFile = binaries[i];
            String taskAndDeviceInfo = binaries[i + 1];
            String task = taskAndDeviceInfo.split("\\.")[0] + "." + taskAndDeviceInfo.split("\\.")[1];
            addNewEntryInBitstreamHashMap(task, binaryFile);

            // For each entry, we should add also an entry for
            // lookup-buffer-address
            String device = taskAndDeviceInfo.split("\\.")[2];
            addNewEntryInBitstreamHashMap("oclbackend.lookupBufferAddress", binaryFile);
        }
    }

    private String[] processPrecompiledBinariesFromFile(String fileName) {
        StringBuilder listBinaries = new StringBuilder();
        BufferedReader fileContent = null;
        try {
            fileContent = new BufferedReader(new FileReader(fileName));
            String line = fileContent.readLine();
            while (line != null) {
                if (!line.isEmpty() && !line.startsWith("#")) {
                    listBinaries.append(line + ",");
                }
                line = fileContent.readLine();
            }
            listBinaries.deleteCharAt(listBinaries.length() - 1);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File: " + fileName + " not found");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fileContent.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return listBinaries.toString().split(",");
    }

    public boolean isLoadBinaryOptionEnabled() {
        return (OPENCL_BINARIES != null);
    }

    public String getOpenCLBinary(String taskName) {
        if (precompiledBinariesPerDevice != null) {
            return precompiledBinariesPerDevice.get(taskName);
        } else {
            return null;
        }
    }

    private Path resolveDirectory(String dir) {
        final String tornadoRoot = (deviceContext.isPlatformFPGA()) ? System.getenv("PWD") : System.getenv("TORNADO_SDK");
        final String deviceDir = String.format("device-%d-%d", deviceContext.getPlatformContext().getPlatformIndex(), deviceContext.getDevice().getIndex());
        final Path outDir = (deviceContext.isPlatformFPGA()) ? Paths.get(tornadoRoot + "/" + dir) : Paths.get(tornadoRoot + "/" + dir + "/" + deviceDir);
        if (!Files.exists(outDir)) {
            try {
                Files.createDirectories(outDir);
            } catch (IOException e) {
                error("unable to create dir: %s", outDir.toString());
                error(e.getMessage());
            }
        }

        guarantee(Files.isDirectory(outDir), "target directory is not a directory: %s", outDir.toAbsolutePath().toString());
        return outDir;
    }

    private Path resolveBitstreamDirectory() {
        return resolveDirectory(directoryBitstream);
    }

    private Path resolveCacheDirectory() {
        return resolveDirectory(OPENCL_CACHE_DIR);
    }

    private Path resolveSourceDirectory() {
        return resolveDirectory(OPENCL_SOURCE_DIR);
    }

    private Path resolveLogDirectory() {
        return resolveDirectory(OPENCL_LOG_DIR);
    }

    boolean isKernelAvailable() {
        return kernelAvailable;
    }

    private void appendSourceToFile(String id, String entryPoint, byte[] source) {
        final Path outDir = deviceContext.isPlatformFPGA() ? resolveBitstreamDirectory() : resolveSourceDirectory();
        File file = new File(outDir + "/" + LOOKUP_BUFFER_KERNEL_NAME + OPENCL_SOURCE_SUFFIX);
        boolean createFile = false;
        if (!entryPoint.equals(LOOKUP_BUFFER_KERNEL_NAME)) {
            createFile = true;
        }
        RuntimeUtilities.writeStreamToFile(file, source, createFile);
    }

    private String[] composeIntelHLSCommand(String inputFile, String outputFile) {
        StringJoiner bufferCommand = new StringJoiner(" ");

        bufferCommand.add(INTEL_ALTERA_OPENCL_COMPILER);
        bufferCommand.add(inputFile);

        bufferCommand.add(compilationFlags);
        bufferCommand.add(Tornado.FPGA_EMULATION ? ("-march=emulator") : ("-board=" + fpgaName));
        bufferCommand.add("-o " + outputFile);
        return bufferCommand.toString().split(" ");
    }

    private String[] composeXilinxHLSCompileCommand(String inputFile, String kernelName) {
        StringJoiner bufferCommand = new StringJoiner(" ");

        bufferCommand.add(XILINX_OPENCL_COMPILER);

        bufferCommand.add(Tornado.FPGA_EMULATION ? ("-t " + "sw_emu") : ("-t " + "hw"));
        bufferCommand.add("--platform " + fpgaName + " -c " + "-k " + kernelName);
        bufferCommand.add("-g " + "-I./" + directoryBitstream);
        bufferCommand.add("--xp " + "misc:solution_name=lookupBufferAddress");
        bufferCommand.add("--report_dir " + directoryBitstream + "reports");
        bufferCommand.add("--log_dir " + directoryBitstream + "logs");
        bufferCommand.add("-o " + directoryBitstream + kernelName + ".xo " + inputFile);

        return bufferCommand.toString().split(" ");
    }

    private void addObjectKernelsToLinker(StringJoiner bufferCommand) {
        for (String kernelNameObject : linkObjectFiles) {
            bufferCommand.add(directoryBitstream + kernelNameObject + ".xo");
        }
    }

    private String[] composeXilinxHLSLinkCommand(String kernelName) {
        StringJoiner bufferCommand = new StringJoiner(" ", "xocc ", "");
        bufferCommand.add(Tornado.FPGA_EMULATION ? ("-t " + "sw_emu") : ("-t " + "hw"));
        bufferCommand.add("--platform " + fpgaName + " -l " + "-g");
        bufferCommand.add("--xp " + "misc:solution_name=link");
        bufferCommand.add("--report_dir " + directoryBitstream + "reports");
        bufferCommand.add("--log_dir " + directoryBitstream + "logs");
        bufferCommand.add(compilationFlags);
        bufferCommand.add("--remote_ip_cache " + directoryBitstream + "ip_cache");
        bufferCommand.add("-o " + directoryBitstream + LOOKUP_BUFFER_KERNEL_NAME + ".xclbin");
        addObjectKernelsToLinker(bufferCommand);
        return bufferCommand.toString().split(" ");
    }

    private void invokeShellCommand(String[] command) {
        try {
            if (command != null) {
                RuntimeUtilities.systemCall(command, true);
            }
        } catch (IOException e) {
            throw new TornadoRuntimeException(e);
        }
    }

    private boolean shouldGenerateXilinxBitstream(File fpgaBitStreamFile, OCLDeviceContext deviceContext) {
        if (!RuntimeUtilities.ifFileExists(fpgaBitStreamFile)) {
            return (deviceContext.getPlatformContext().getPlatform().getVendor().equals("Xilinx"));
        } else {
            return false;
        }
    }

    private boolean isPlatform(String platformName) {
        return deviceContext.getPlatformContext().getPlatform().getVendor().toLowerCase().startsWith(platformName);
    }

    private String[] splitTaskScheduleAndTaskName(String id) {
        if (id.contains(".")) {
            String[] names = id.split("\\.");
            return names;
        }
        return new String[] { id };
    }

    private void addNewEntryInBitstreamHashMap(String id, String bitstreamDirectory) {
        if (precompiledBinariesPerDevice != null) {
            String lookupBufferDeviceKernelName = id + String.format(".device=%d:%d", deviceContext.getDevice().getIndex(), deviceContext.getPlatformContext().getPlatformIndex());
            precompiledBinariesPerDevice.put(lookupBufferDeviceKernelName, bitstreamDirectory);
        }
    }

    private String getDeviceVendor() {
        return deviceContext.getPlatformContext().getPlatform().getVendor().toLowerCase().split("\\(")[0];
    }

    OCLInstalledCode installFPGASource(String id, String entryPoint, byte[] source, boolean shouldCompile) { // TODO Override this method for each FPGA backend
        String[] compilationCommand;
        final String inputFile = fpgaSourceDir + LOOKUP_BUFFER_KERNEL_NAME + OPENCL_SOURCE_SUFFIX;
        final String outputFile = fpgaSourceDir + LOOKUP_BUFFER_KERNEL_NAME;
        File fpgaBitStreamFile = new File(fpgaBinLocation);

        appendSourceToFile(id, entryPoint, source);

        if (OPENCL_PRINT_SOURCE) {
            String sourceCode = new String(source);
            System.out.println(sourceCode);
        }

        String[] commandRename;
        String[] linkCommand = null;
        String[] taskNames;

        if (!entryPoint.equals(LOOKUP_BUFFER_KERNEL_NAME)) {
            taskNames = splitTaskScheduleAndTaskName(id);
            if (pendingTasks.containsKey(taskNames[0])) {
                pendingTasks.get(taskNames[0]).add(new Pair(taskNames[1], entryPoint));
            } else {
                ArrayList<Pair> tasks = new ArrayList<>();
                tasks.add(new Pair(taskNames[1], entryPoint));
                pendingTasks.put(taskNames[0], tasks);
            }
        }

        if (!entryPoint.equals(LOOKUP_BUFFER_KERNEL_NAME) & shouldCompile) {
            if (isPlatform("xilinx")) {
                compilationCommand = composeXilinxHLSCompileCommand(inputFile, entryPoint);
                linkObjectFiles.add(entryPoint);
                linkCommand = composeXilinxHLSLinkCommand(entryPoint);
            } else if (isPlatform("intel")) {
                compilationCommand = composeIntelHLSCommand(inputFile, outputFile);
            } else {
                // Should not reach here
                throw new TornadoRuntimeException("[ERROR] FPGA vendor not supported yet.");
            }

            String vendor = getDeviceVendor();

            commandRename = new String[] { FPGA_CLEANUP_SCRIPT, vendor, fpgaSourceDir };
            Path path = Paths.get(fpgaBinLocation);
            addNewEntryInBitstreamHashMap(id, fpgaBinLocation);
            if (RuntimeUtilities.ifFileExists(fpgaBitStreamFile)) {
                return installEntryPointForBinaryForFPGAs(id, path, LOOKUP_BUFFER_KERNEL_NAME);
            } else {
                invokeShellCommand(compilationCommand);
                invokeShellCommand(commandRename);
                invokeShellCommand(linkCommand);
            }
            return installEntryPointForBinaryForFPGAs(id, path, LOOKUP_BUFFER_KERNEL_NAME);
        } else {
            // For Xilinx we can compile separated modules and then link them together in
            // the final phase.
            if (shouldGenerateXilinxBitstream(fpgaBitStreamFile, deviceContext)) {
                linkObjectFiles.add(entryPoint);
                compilationCommand = composeXilinxHLSCompileCommand(inputFile, entryPoint);
                invokeShellCommand(compilationCommand);
            }
        }
        return null;
    }

    public OCLInstalledCode installSource(TaskMetaData meta, String id, String entryPoint, byte[] source) {

        info("Installing code for %s into code cache", entryPoint);
        final OCLProgram program = deviceContext.createProgramWithSource(source, new long[] { source.length });

        if (OPENCL_DUMP_SOURCE) {
            final Path outDir = resolveSourceDirectory();
            File file = new File(outDir + "/" + id + "-" + entryPoint + OPENCL_SOURCE_SUFFIX);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(source);
            } catch (IOException e) {
                error("unable to dump source: ", e.getMessage());
            }
        }

        if (deviceContext.getDevice().getDeviceType() == OCLDeviceType.CL_DEVICE_TYPE_ACCELERATOR) {
            appendSourceToFile(id, entryPoint, source);
        }

        if (OPENCL_PRINT_SOURCE) {
            String sourceCode = new String(source);
            System.out.println(sourceCode);
        }

        final long t0 = System.nanoTime();
        program.build(meta.getCompilerFlags());
        final long t1 = System.nanoTime();

        final OCLBuildStatus status = program.getStatus(deviceContext.getDeviceId());
        debug("\tOpenCL compilation status = %s", status.toString());

        final String log = program.getBuildLog(deviceContext.getDeviceId()).trim();

        if (PRINT_WARNINGS || (status == OCLBuildStatus.CL_BUILD_ERROR)) {
            if (!log.isEmpty()) {
                debug(log);
            }
            final Path outDir = resolveLogDirectory();
            final String identifier = id + "-" + entryPoint;
            error("Unable to compile task %s: check logs at %s/%s.log", identifier, outDir.toAbsolutePath(), identifier);

            File file = new File(outDir + "/" + identifier + ".log");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(log.getBytes());
            } catch (IOException e) {
                error("unable to write error log: ", e.getMessage());
            }
            file = new File(outDir + "/" + identifier + OPENCL_SOURCE_SUFFIX);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(source);
            } catch (IOException e) {
                error("unable to write error log: ", e.getMessage());
            }
        }

        if (status == OCLBuildStatus.CL_BUILD_ERROR) {
            throw new TornadoBailoutRuntimeException("Error during code compilation with the OpenCL driver");
        }

        final OCLKernel kernel = (status == CL_BUILD_SUCCESS) ? program.getKernel(entryPoint) : null;

        if (kernel != null) {
            kernelAvailable = true;
        }

        final OCLInstalledCode code = new OCLInstalledCode(entryPoint, source, deviceContext, program, kernel);

        if (status == CL_BUILD_SUCCESS) {
            debug("\tOpenCL Kernel id = 0x%x", kernel.getId());
            if (meta.shouldPrintCompileTimes()) {
                debug("compile: kernel %s opencl %.9f\n", entryPoint, (t1 - t0) * 1e-9f);
            }
            cache.put(id + "-" + entryPoint, code);

            // BUG Apple does not seem to like implementing the OpenCL spec
            // properly, this causes a sigfault.
            if ((OPENCL_CACHE_ENABLE || OPENCL_DUMP_BINS) && !deviceContext.getPlatformContext().getPlatform().getVendor().equalsIgnoreCase("Apple")) {
                final Path outDir = resolveCacheDirectory();
                program.dumpBinaries(outDir.toAbsolutePath().toString() + "/" + entryPoint);
            }
        } else {
            warn("\tunable to compile %s", entryPoint);
            code.invalidate();
        }

        return code;
    }

    private OCLInstalledCode installBinary(String id, String entryPoint, byte[] binary) throws OCLException {
        info("Installing binary for %s into code cache", entryPoint);

        if (entryPoint.contains("-")) {
            entryPoint = entryPoint.split("-")[1];
        }

        OCLProgram program = null;
        OCLBuildStatus status = CL_BUILD_SUCCESS;
        if (shouldReuseProgramObject(entryPoint)) {
            program = cache.get(LOOKUP_BUFFER_KERNEL_NAME).getProgram();
        } else {
            long beforeLoad = (Tornado.TIME_IN_NANOSECONDS) ? System.nanoTime() : System.currentTimeMillis();
            program = deviceContext.createProgramWithBinary(binary, new long[] { binary.length });
            long afterLoad = (Tornado.TIME_IN_NANOSECONDS) ? System.nanoTime() : System.currentTimeMillis();

            if (PRINT_LOAD_TIME) {
                System.out.println("Binary load time: " + (afterLoad - beforeLoad) + (Tornado.TIME_IN_NANOSECONDS ? " ns" : " ms") + " \n");
            }

            if (program == null) {
                throw new OCLException("unable to load binary for " + entryPoint);
            }

            program.build("");

            status = program.getStatus(deviceContext.getDeviceId());
            debug("\tOpenCL compilation status = %s", status.toString());

            final String log = program.getBuildLog(deviceContext.getDeviceId()).trim();
            if (!log.isEmpty()) {
                debug(log);
            }
        }

        final OCLKernel kernel = (status == CL_BUILD_SUCCESS) ? program.getKernel(entryPoint) : null;
        final OCLInstalledCode code = new OCLInstalledCode(entryPoint, binary, deviceContext, program, kernel);

        if (status == CL_BUILD_SUCCESS) {
            debug("\tOpenCL Kernel id = 0x%x", kernel.getId());
            cache.put(entryPoint, code);
            if (entryPoint.equals(LOOKUP_BUFFER_KERNEL_NAME)) {
                cache.put("internal-" + entryPoint, code);
            }

            String taskScheduleName = splitTaskScheduleAndTaskName(id)[0];
            if (pendingTasks.containsKey(taskScheduleName)) {
                ArrayList<Pair> pendingKernels = pendingTasks.get(taskScheduleName);
                for (Pair pair : pendingKernels) {
                    String childKernelName = pair.entryPoint;
                    if (!childKernelName.equals(entryPoint)) {
                        final OCLKernel kernel2 = program.getKernel(childKernelName);
                        final OCLInstalledCode code2 = new OCLInstalledCode(entryPoint, binary, deviceContext, program, kernel2);
                        cache.put(taskScheduleName + "." + pair.taskName + "-" + childKernelName, code2);
                    }
                }
                pendingKernels.clear();
            }

            if ((OPENCL_CACHE_ENABLE || OPENCL_DUMP_BINS)) {
                final Path outDir = resolveCacheDirectory();
                RuntimeUtilities.writeToFile(outDir.toAbsolutePath().toString() + "/" + entryPoint, binary);
            }
        } else {
            warn("\tunable to install binary for %s", entryPoint);
            code.invalidate();
        }

        return code;
    }

    private boolean shouldReuseProgramObject(String entryPoint) {
        return !entryPoint.equals(LOOKUP_BUFFER_KERNEL_NAME) && deviceContext.getDevice().getDeviceName().toLowerCase().startsWith("xilinx");
    }

    public void reset() {
        for (OCLInstalledCode code : cache.values()) {
            code.invalidate();
        }
        cache.clear();
    }

    public OCLInstalledCode installEntryPointForBinaryForFPGAs(String id, Path lookupPath, String entrypoint) {
        final File file = lookupPath.toFile();
        OCLInstalledCode lookupCode = null;
        if (file.length() == 0) {
            error("Empty input binary: %s (%s)", file);
        }
        try {
            final byte[] binary = Files.readAllBytes(lookupPath);
            lookupCode = installBinary(id, entrypoint, binary);
        } catch (OCLException | IOException e) {
            error("unable to load binary: %s (%s)", file, e.getMessage());
        }
        return lookupCode;
    }

    public boolean isCached(String id, String entryPoint) {
        return cache.containsKey(id + "-" + entryPoint);
    }

    public OCLInstalledCode getInstalledCode(String id, String entryPoint) {
        return cache.get(id + "-" + entryPoint);
    }
}
