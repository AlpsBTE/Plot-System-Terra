package com.alpsbte.plotsystemterra.core.plotsystem;

import com.alpsbte.plotsystemterra.core.DatabaseConnection;
import com.alpsbte.plotsystemterra.utils.ItemBuilder;
import com.alpsbte.plotsystemterra.utils.LoreBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.mask.BinaryMask;
import org.ipvp.canvas.mask.Mask;
import org.ipvp.canvas.type.ChestMenu;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CreatePlotMenu {
    private final Menu createPlotMenu = ChestMenu.builder(6).title("Create Plot").redraw(true).build();
    private final Menu difficultyMenu = ChestMenu.builder(3).title("Select Plot Difficulty").redraw(true).build();

    private final List<CityProject> cityProjects = getCityProjects();
    private int selectedCityID = -1;

    private final Player player;

    public CreatePlotMenu(Player player) {
        this.player = player;
        getCityProjectUI().open(player);
    }

    public Menu getCityProjectUI() {
        Mask mask = BinaryMask.builder(createPlotMenu)
                .item(new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte)7).setName(" ").build())
                .pattern("111101111") // First row
                .pattern("000000000") // Second row
                .pattern("000000000") // Third row
                .pattern("000000000") // Fourth row
                .pattern("000000000") // Fifth row
                .pattern("111010111") // Sixth row
                .build();
        mask.apply(createPlotMenu);

        createPlotMenu.getSlot(4).setItem(getStats(player.getLocation()));

        for(int i = 0; i < cityProjects.size(); i++) {
            int cityID = i;
            createPlotMenu.getSlot(9 + i).setClickHandler((clickPlayer, clickInformation) -> {
                if(selectedCityID != cityID) {

                    selectedCityID = cityID;
                    createPlotMenu.getSlot(4).setItem(getStats(player.getLocation()));

                    clickPlayer.playSound(clickPlayer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                }
            });
        }

        createPlotMenu.getSlot(48).setItem(
                new ItemBuilder(Material.WOOL, 1, (byte) 13)
                        .setName("§a§lContinue")
                        .build());
        createPlotMenu.getSlot(48).setClickHandler((clickPlayer, clickInformation) -> {
            clickPlayer.closeInventory();
            getDifficultyMenu().open(clickPlayer);
        });

        createPlotMenu.getSlot(50).setItem(
                new ItemBuilder(Material.WOOL, 1, (byte) 14)
                        .setName("§c§lCancel")
                        .build());
        createPlotMenu.getSlot(50).setClickHandler((clickPlayer, clickInformation) -> {
            clickPlayer.closeInventory();
        });

        return createPlotMenu;
    }

    public Menu getDifficultyMenu() {
        CityProject cityProject = cityProjects.get(selectedCityID);

        // Set glass border
        Mask mask = BinaryMask.builder(createPlotMenu)
                .item(new ItemBuilder(Material.STAINED_GLASS_PANE, 1, (byte)7).setName(" ").build())
                .pattern("111111111") // First row
                .pattern("000000000") // Second row
                .pattern("111111111") // Third row
                .build();
        mask.apply(difficultyMenu);

        difficultyMenu.getSlot(10).setItem(
                new ItemBuilder(Material.WOOL, 1, (byte) 5)
                        .setName("§a§lEasy")
                        .build()
        );
        difficultyMenu.getSlot(10).setClickHandler((clickPlayer, clickInformation) -> {
            clickPlayer.closeInventory();
            PlotCreator.Create(clickPlayer, cityProject, 1);
        });

        difficultyMenu.getSlot(13).setItem(
                new ItemBuilder(Material.WOOL, 1, (byte) 1)
                        .setName("§6§lMedium")
                        .build()
        );
        difficultyMenu.getSlot(13).setClickHandler((clickPlayer, clickInformation) -> {
            clickPlayer.closeInventory();
            PlotCreator.Create(clickPlayer, cityProject, 2);
        });

        difficultyMenu.getSlot(16).setItem(
                new ItemBuilder(Material.WOOL, 1, (byte) 14)
                        .setName("§c§lHard")
                        .build()
        );
        difficultyMenu.getSlot(16).setClickHandler((clickPlayer, clickInformation) -> {
            clickPlayer.closeInventory();
            PlotCreator.Create(clickPlayer, cityProject, 3);
        });

        return difficultyMenu;
    }

    private List<CityProject> getCityProjects() {
        List<CityProject> listProjects = new ArrayList<>();

        int counter = 0;
        try {
            ResultSet rs = DatabaseConnection.createStatement("SELECT id, country_id, name FROM plotsystem_city_projects").executeQuery();

            while (rs.next()) {
                CityProject city = new CityProject(rs.getInt(1), rs.getInt(2), rs.getString(3));
                createPlotMenu.getSlot(9 + counter).setItem(city.getItem());
                listProjects.add(city);
                counter++;
            }
            rs.close();
        } catch (SQLException ex) {
            createPlotMenu.getSlot(9 + counter).setItem(new ItemBuilder(Material.BARRIER)
                .setName("§c§lError")
                .setLore(new LoreBuilder()
                        .addLine("Could not load city project.")
                        .build())
            .build());
        }

        return listProjects;
    }

    private ItemStack getStats(Location coords) {
        return new ItemBuilder((selectedCityID == -1) ? new ItemStack(Material.NAME_TAG) : cityProjects.get(selectedCityID).getItem())
                .setName("§6§lSTATS")
                .setLore(new LoreBuilder()
                        .addLines("§bX: §7" + coords.getX(),
                                  "§bY: §7" + coords.getY(),
                                  "§bZ: §7" + coords.getZ(),
                                  "§bCity: §7" + ((selectedCityID != -1) ? cityProjects.get(selectedCityID).getName() : "none"))
                        .build())
                .build();
    }
}
