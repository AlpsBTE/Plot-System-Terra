package com.alpsbte.plotsystemterra.core.database;

import com.alpsbte.alpslib.io.database.SqlHelper;
import com.alpsbte.plotsystemterra.core.data.DataException;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class SqlExceptionUtil {
    private SqlExceptionUtil() { /* Prevent instantiation */ }

    public static <T> T handle(@NotNull SqlHelper.SQLCheckedSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (SQLException e) {
            throw new DataException(e.getMessage(), e);
        }
    }

    public static void handle(@NotNull SqlHelper.SQLRunnable supplier) {
        try {
            supplier.get();
        } catch (SQLException e) {
            throw new DataException(e.getMessage(), e);
        }
    }
}
