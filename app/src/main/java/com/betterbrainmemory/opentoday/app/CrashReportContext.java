package com.betterbrainmemory.opentoday.app;

import com.betterbrainmemory.opentoday.util.time.TimeUtil;

import java.util.Stack;

public class CrashReportContext {
    public static final Front FRONT = new Front();
    public static final Back BACK = new Back();

    static {
        FRONT.push("<static>");
        BACK.push("<static>");
    }
    private static String mainActivityStatus = "NON-CREATED";
    private static String mainRootFragment = "NON-CREATED";
    private static String backendInitializerState = "no-information";

    public static void mainActivityCreate() {
        mainActivityStatus = "Created";
    }
    
    public static void mainActivityDestroy() {
        mainActivityStatus = "Destroyed";
    }

    public static void mainActivityPause() {
        mainActivityStatus = "Paused";
    }

    public static void setMainRootFragment(String mainRootFragment) {
        CrashReportContext.mainRootFragment = mainRootFragment;
    }


    public static void setBackendInitializerState(String s) {
        CrashReportContext.backendInitializerState = s;
    }


    public static String getAsString() {
        StringBuilder builder = new StringBuilder(String.format("""
                == CrashReportContext ==
                mainActivityStatus=%s
                mainRootFragment=%s
                backendInitializerState=%s
                """, mainActivityStatus,
                mainRootFragment,
                backendInitializerState));

        builder.append("\nFRONT: ");
        for (String s : FRONT.stack) {
            builder.append("(").append(s).append(")").append(", ");
        }
        builder.delete(builder.lastIndexOf(","), builder.lastIndexOf(" "));

        builder.append("\nBACK: ");
        for (String s : BACK.stack) {
            builder.append("(").append(s).append(")").append(", ");
        }
        builder.delete(builder.lastIndexOf(","), builder.lastIndexOf(" "));

        return builder.toString();
    }

    public static class Front {
        private final Stack<String> stack = new Stack<>();

        public void push(String s) {
            stack.push(TimeUtil.getDebugDate() + " " + Thread.currentThread().getName() + " " + s);
        }

        public void pop() {
            stack.pop();
        }
    }

    public static class Back {
        private final Stack<String> stack = new Stack<>();

        public void push(String s) {
            stack.push(TimeUtil.getDebugDate() + " " + Thread.currentThread().getName() + " " + s);
        }

        public void pop() {
            stack.pop();
        }

        public void swap(String s) {
            pop();
            push(s);
        }
    }
}