package jkind.aeval;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jkind.JKindException;

public abstract class AevalProcess {
    protected static final String CHECKSAT = "(check-sat)";

    protected Process process;
    protected String scratchBase;
    protected BufferedReader fromAeval;

    protected AevalProcess(String scratchBase, String check) {
        this.scratchBase = scratchBase;
    }

    protected void callAeval(String check, boolean generateSkolem, boolean nondet, boolean compaction, boolean allinclusive) {
        ProcessBuilder processBuilder = new ProcessBuilder(getCommand(check, generateSkolem, nondet,
                compaction, allinclusive, this.scratchBase));
        processBuilder.redirectErrorStream(true);
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new JKindException("Unable to start AE-VAL by executing: "
                    + processBuilder.command().get(0), e);
        }
        fromAeval = new BufferedReader(new InputStreamReader(process.getInputStream()));

        addShutdownHook();
    }



    private List<String> getCommand(String check, boolean generateSkolem, boolean nondet,
                                    boolean compaction, boolean allinclusive, String scratchBase) {
        List<String> command = new ArrayList<>();
        command.add(getPath());
        if (generateSkolem) {
//            command.add("--debug");
            command.add("--skol");
        }
        if (nondet) {
            command.add("--nondet");
        }
        if (compaction) {
            command.add("--compaction");
        }

        if (allinclusive) {
            command.add("--all-inclusive");
        }

        command.addAll(getArgs(check, scratchBase));
        return command;
    }

    private String getPath() {
        String executable = getExecutable();
        String home = System.getenv(getHomeVariable());
        if (home != null) {
            return new File(getBinDir(home), executable).toString();
        }
        return executable;
    }

    private static File getBinDir(String homeString) {
        File home = new File(homeString);
        File bin = new File(home, "bin");
        if (bin.isDirectory()) {
            return bin;
        } else {
            return home;
        }
    }

    private List<String> getArgs(String check, String scratchBase) {
        List<String> args = new ArrayList<>();
        args.add(getSPart(scratchBase.split("\\.lus")[0] + "_" + check));
        args.add(getTPart(scratchBase.split("\\.lus")[0] + "_" + check));
        return args;
    }

    private String getSPart(String scratchBase) {
        return scratchBase + "_s_part.smt2";
    }

    private String getTPart(String scratchBase) {
        return scratchBase + "_t_part.smt2";
    }


    protected String getName() {
        return "AEVAL";
    }

    protected String getExecutable() {
        return getName().toLowerCase();
    }

    protected String getHomeVariable() {
        return getName().toUpperCase() + "_HOME";
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {
            @Override
            public void run() {
                AevalProcess.this.stop();
            }
        });
    }

    public synchronized void stop() {
        /**
         * This must be synchronized since two threads (an Engine or a shutdown
         * hook) may try to stop the solver at the same time
         */

        if (process != null) {
            process.destroy();
            process = null;
        }

    }
}
