package com.salk.migration.extend.undo;

public class UndoContext {
    private static final ThreadLocal<UndoParam> UNDO_CONTEXT = new ThreadLocal<>();

    public static void setUndoContext(UndoParam requestParam) {
        UNDO_CONTEXT.set(requestParam);
    }
    public static UndoParam getUndoContext() {
        return UNDO_CONTEXT.get()==null?new UndoParam():UNDO_CONTEXT.get();
    }

    public static void remove() {

        UNDO_CONTEXT.remove();
    }

    public static class UndoParam {
        private boolean isUndo;
        private String undoPath;

        public boolean isUndo() {
            return isUndo;
        }

        public UndoParam setUndo(boolean undo) {
            isUndo = undo;
            return this;
        }

        public String getUndoPath() {
            return undoPath;
        }

        public UndoParam setUndoPath(String undoPath) {
            this.undoPath = undoPath;
            return this;
        }
    }

}
