package com.zergatul.cheatutils.scripting.api.modules;

import com.zergatul.cheatutils.concurrent.TickEndExecutor;
import com.zergatul.cheatutils.scripting.api.HelpText;

import java.util.concurrent.CompletableFuture;

public class DelayApi {
    @HelpText("""
            Stops script execution for specified amount of ticks
            """)
    public CompletableFuture<Void> ticks(int ticks) {
        if (ticks <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        TickEndExecutor.instance.waitTicks(ticks, () -> future.complete(null));
        return future;
    }
}
