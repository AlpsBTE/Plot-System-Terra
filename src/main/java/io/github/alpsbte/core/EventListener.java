package github.alpsbte.core;

import github.alpsbte.utils.Utils;
import me.arcaniax.hdb.api.DatabaseLoadEvent;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class EventListener implements Listener {

    @EventHandler
    public void onDatabaseLoad(DatabaseLoadEvent event) {
        Utils.headDatabaseAPI = new HeadDatabaseAPI();
    }

}
