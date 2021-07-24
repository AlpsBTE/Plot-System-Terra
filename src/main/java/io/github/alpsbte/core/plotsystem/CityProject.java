package github.alpsbte.core.plotsystem;

import github.alpsbte.utils.ItemBuilder;
import github.alpsbte.utils.LoreBuilder;
import github.alpsbte.utils.Utils;
import org.bukkit.inventory.ItemStack;

public class CityProject {

    private final int ID;
    private final String name;
    private final String headID;

    public CityProject(int ID, String name, String headID) {
        this.ID = ID;
        this.name = name;
        this.headID = headID;
    }

    public int getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public ItemStack getItem() {
        return new ItemBuilder(Utils.getItemHead(headID))
                .setName("§b§l" + name)
                .setLore(new LoreBuilder()
                        .addLine("§bID: §7" + getID())
                        .build())
                .build();
    }
}
