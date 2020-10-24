package ml.chriscommunity.survivalplus.main;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import ml.chriscommunity.economy.Economy;

public class Main extends JavaPlugin implements Listener {
	DataManager data = new DataManager(this);
	
	private static final Logger log = Logger.getLogger("Minecraft");
	private static Economy econ = null;
	

	@Override
	public void onEnable() {
		if(!setupEconomy()) {
			log.severe(String.format("[%s] - Disabled due to no EconomyPlus dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
		}
		data.saveConfig();

		getServer().getPluginManager().registerEvents(this, this);
	}
	
	private boolean setupEconomy() {
		if(getServer().getPluginManager().getPlugin("EconomyPlus") == null) {
			return false;
		}
		econ = new Economy();
		if (econ.initEcon() != 0) {
			return false;
		}
		return econ != null;
	}

	@Override
	public void onDisable() {

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player player = (Player) sender;
		if(player.hasPermission("nte.admin") || player.hasPermission("nte.owner")) {
			if(label.equalsIgnoreCase("coin")) {
				if(args.length == 0) {
					player.getInventory().setItem(player.getInventory().firstEmpty(), getCoin());
					return true;
				} else if(args.length == 1) {
					if(args[0].equalsIgnoreCase("give")) {
						player.sendMessage(ChatColor.RED + "Usage: /coin give <player> <num>");
						return true;
					} else if (args[0].equalsIgnoreCase("take")){
						player.sendMessage(ChatColor.RED + "Usage: /coin take <player> <num>");
						return true;
					} else {
						player.sendMessage(ChatColor.RED + "Usage: /coin take or /coin give");
						return true;
					}
				} else if(args.length == 2) {
					if(args[0].equalsIgnoreCase("give")) {
						player.sendMessage(ChatColor.RED + "Usage: /coin give <player> <num>");
						return true;
					} else if(args[0].equalsIgnoreCase("take")) {
						player.sendMessage(ChatColor.RED + "Usage: /coin take <player> <num>");
						return true;
					}
				} else if(args.length == 3) {
					if(args[0].equalsIgnoreCase("give")) {
						if(Double.parseDouble(args[2]) <= 0) {
							player.sendMessage(ChatColor.RED + "Amount cannot be less than 0.");
							return true;
						}
						if(econ.depositPlayer(Bukkit.getPlayer(args[1]), Double.parseDouble(args[2])) == 0) {
							Bukkit.getPlayer(args[1]).sendMessage(String.format("You were given %s and now have %f", Double.parseDouble(args[2]), econ.getBalance(Bukkit.getPlayer(args[1]))));
							//data.getConfig().set(Bukkit.getPlayer(args[1]).getUniqueId() + ".balance", r.balance);
							//data.saveConfig();
						}
						
						return true;
					} else if(args[0].equalsIgnoreCase("take")) {
						if(Double.parseDouble(args[2]) < 0) {
							player.sendMessage(ChatColor.RED + "Amount cannot be less than 0.");
							return true;
						}
						if(econ.withdrawPlayer(Bukkit.getPlayer(args[1]), Double.parseDouble(args[2])) == 0) {
							Bukkit.getPlayer(args[1]).sendMessage(String.format("%s was taken from you and now have %f", Double.parseDouble(args[2]), econ.getBalance(Bukkit.getPlayer(args[1]))));
							//data.getConfig().set(Bukkit.getPlayer(args[1]).getUniqueId() + ".balance", r.balance);
							//data.saveConfig();
						}
						
						return true;
					}
				}
			}
			if(label.equalsIgnoreCase("customize")) {
				player.openInventory(getCustom(player));
				return true;
			}
		}
		if(label.equalsIgnoreCase("rtp")) {
			if(getServer().getWorld("public_sv") == null) {
				getServer().createWorld(new WorldCreator("public_sv"));
			}
			Random rand = new Random();
			int x = 0;
			int y = 0;
			int z = 0;
			do {
				x = rand.nextInt(20000) - 10000;
				y = 256;
				z = rand.nextInt(20000) - 10000;
				while(getServer().getWorld("public_sv").getBlockAt(x, y, z).getType() == Material.AIR) {
					y--;
				}
			} while(getServer().getWorld("public_sv").getBlockAt(x, y-1, z).getType() == Material.LAVA || getServer().getWorld("public_sv").getBlockAt(x, y+1, z).getType() != Material.AIR);
			y+=6;
			Location loc = new Location(getServer().getWorld("public_sv"), x, y, z);
			getServer().getWorld("public_sv").loadChunk(x, z, true);
			player.teleport(loc);
		}
		if(label.equalsIgnoreCase("shop")) {
			Inventory shop = getShop(player, 0);
			player.openInventory(shop);
			return true;
		}
		if(label.equalsIgnoreCase("survivalplus") || label.equalsIgnoreCase("sp")) {
			if(args.length == 1) {
				if(args[0].equalsIgnoreCase("cs") || args[0].equalsIgnoreCase("configureshop")) {
					player.openInventory(getCustom(player));
				}
			}
		}
		return false;
	}
	
	Inventory getCustom(Player player) {
		Inventory inv = null;
		inv = Bukkit.createInventory(player, 54, "Shop Customizer");
		
		ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemMeta meta = border.getItemMeta();
		meta = border.getItemMeta();
		meta.setDisplayName(" ");
		border.setItemMeta(meta);
		for(int i = 0; i < 54; i++) {
			if(i < 9 || i % 9 == 0 || i % 9 == 8 || i > 43) {
				inv.setItem(i, border);
			}
		}
		ItemStack back = new ItemStack(Material.BARRIER);
		meta = back.getItemMeta();
		meta.setDisplayName("§r§c§lBack");
		back.setItemMeta(meta);
		inv.setItem(49, back);
		ItemStack help = new ItemStack(Material.GLOWSTONE);
		meta = help.getItemMeta();
		meta.setDisplayName("§r§eHelp");
		help.setItemMeta(meta);
		inv.setItem(43, help);
		ItemStack credit = new ItemStack(Material.PAPER);
		meta = credit.getItemMeta();
		meta.setDisplayName("§rMade by GhostlyGames41");
		credit.setItemMeta(meta);
		inv.setItem(37, credit);
		ItemStack categories = new ItemStack(Material.CHEST);
		meta = categories.getItemMeta();
		meta.setDisplayName("§r§lCategories");
		categories.setItemMeta(meta);
		inv.setItem(13, categories);
		ItemStack products = new ItemStack(Material.DIAMOND);
		meta = products.getItemMeta();
		meta.setDisplayName("§r§lProducts");
		products.setItemMeta(meta);
		inv.setItem(23, products);
		ItemStack settings = new ItemStack(Material.IRON_BLOCK);
		meta = settings.getItemMeta();
		meta.setDisplayName("§r§lShop Settings");
		settings.setItemMeta(meta);
		inv.setItem(21, settings);
		ItemStack prices = new ItemStack(Material.EMERALD_BLOCK);
		meta = prices.getItemMeta();
		meta.setDisplayName("§r§lPrices");
		prices.setItemMeta(meta);
		inv.setItem(31, prices);
		return inv;
	}
	
	Inventory getShop(Player player, int tab) {
		Inventory inv = null;
		if(tab == 0) {
			inv = Bukkit.createInventory(player, 27, "Shop");
			ItemStack building = new ItemStack(Material.GRASS);
			ItemMeta meta = building.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "Building");
			building.setItemMeta(meta);
			inv.setItem(12, building);
			ItemStack ores = new ItemStack(Material.IRON_INGOT);
			meta = ores.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "Ores/Metals");
			ores.setItemMeta(meta);
			inv.setItem(13, ores);
			ItemStack spawners = new ItemStack(Material.SPAWNER);
			meta = spawners.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "Spawners");
			spawners.setItemMeta(meta);
			inv.setItem(14, spawners);
		} else if(tab == 1) {
			inv = Bukkit.createInventory(player, 54, "Building");
			ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
			ItemMeta meta = border.getItemMeta();
			meta = border.getItemMeta();
			meta.setDisplayName(" ");
			border.setItemMeta(meta);
			for(int i = 0; i < 54; i++) {
				if(i < 9 || i % 9 == 0 || i % 9 == 8 || i > 43) {
					inv.setItem(i, border);
				}
			}
			ItemStack back = new ItemStack(Material.BARRIER);
			meta = back.getItemMeta();
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&c&lBack"));
			back.setItemMeta(meta);
			inv.setItem(49, back);
			// Line 1	slot			item id					price
			inv.setItem(10, getShopItem(Material.DIRT, 			1, 0.5f, 1));
			inv.setItem(11, getShopItem(Material.GRASS_BLOCK,	1, 0.75f, 1));
			inv.setItem(12, getShopItem(Material.STONE, 		2, 1, 1));
			inv.setItem(13, getShopItem(Material.COBBLESTONE, 	1, 0.75f, 1));
			inv.setItem(14, getShopItem(Material.STONE_BRICKS, 	8, 4, 1));
			inv.setItem(15, getShopItem(Material.GLOWSTONE, 	10, 7, 1));
			inv.setItem(16, getShopItem(Material.OBSIDIAN, 		40, 30, 1));
			// Line 2
			inv.setItem(19, getShopItem(Material.OAK_LOG, 		2, 1, 1));
			inv.setItem(20, getShopItem(Material.OAK_LEAVES, 	1, 0.75f, 1));
			inv.setItem(21, getShopItem(Material.QUARTZ_BLOCK, 	4, 3, 1));
			inv.setItem(22, getShopItem(Material.NETHERRACK, 	1, 0.25f, 1));
			inv.setItem(23, getShopItem(Material.GLASS, 		1.5f, 1.25f, 1));
			inv.setItem(24, getShopItem(Material.SAND, 			1, 0.5f, 1));
			inv.setItem(25, getShopItem(Material.GRAVEL, 		1, 0.5f, 1));
			// Line 3
			inv.setItem(28, getShopItem(Material.WHITE_WOOL, 	2, 1, 1));
			inv.setItem(29, getShopItem(Material.ICE, 			5, 4, 1));
		} else if(tab == 2) {
			inv = Bukkit.createInventory(player, 54, "Ores/Metals");
			ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
			ItemMeta meta = border.getItemMeta();
			meta = border.getItemMeta();
			meta.setDisplayName(" ");
			border.setItemMeta(meta);
			for(int i = 0; i < 54; i++) {
				if(i < 9 || i % 9 == 0 || i % 9 == 8 || i > 43) {
					inv.setItem(i, border);
				}
			}
			
			inv.setItem(10, getShopItem(Material.GOLD_INGOT, 5, 4, 1));
			inv.setItem(19, getShopItem(Material.GOLD_BLOCK, 45, 36, 1));
			inv.setItem(28, getShopItem(Material.GOLD_ORE, 3, 2, 1));
			inv.setItem(11, getShopItem(Material.DIAMOND, 10, 8, 1));
			inv.setItem(20, getShopItem(Material.DIAMOND_BLOCK, 90, 72, 1));
			inv.setItem(29, getShopItem(Material.DIAMOND_ORE, 9, 7, 1));
			inv.setItem(12, getShopItem(Material.EMERALD, 8, 6, 1));
			inv.setItem(21, getShopItem(Material.EMERALD_BLOCK, 72, 54, 1));
			inv.setItem(30, getShopItem(Material.EMERALD_ORE, 7, 5, 1));
			inv.setItem(13, getShopItem(Material.COAL, 3, 2, 1));
			inv.setItem(22, getShopItem(Material.COAL_BLOCK, 27, 18, 1));
			inv.setItem(31, getShopItem(Material.COAL_ORE, 2, 1.5f, 1));
			inv.setItem(14, getShopItem(Material.REDSTONE, 3, 2, 1));
			inv.setItem(23, getShopItem(Material.REDSTONE_BLOCK, 27, 18, 1));
			inv.setItem(32, getShopItem(Material.REDSTONE_ORE, 2, 1.5f, 1));
			ItemStack lapis = new ItemStack(Material.LAPIS_LAZULI);
			meta = lapis.getItemMeta();
			ArrayList<String> lore = new ArrayList<>();
			lore.add("Buy Price: " + 3 + " coins");
			lore.add("Sell Price: " + 2.75 + " coins");
			meta.setLore(lore);
			lapis.setItemMeta(meta);
			inv.setItem(15, lapis);
			inv.setItem(24, getShopItem(Material.LAPIS_BLOCK, 27, 24.75f, 1));
			inv.setItem(33, getShopItem(Material.LAPIS_ORE, 2, 1.75f, 1));
			inv.setItem(16, getShopItem(Material.IRON_INGOT, 4, 3.5f, 1));
			inv.setItem(25, getShopItem(Material.IRON_BLOCK, 36, 31.5f, 1));
			inv.setItem(34, getShopItem(Material.IRON_ORE, 3, 2.5f, 1));
			
			ItemStack back = new ItemStack(Material.BARRIER);
			meta = back.getItemMeta();
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&c&lBack"));
			back.setItemMeta(meta);
			inv.setItem(49, back);
		}
		return inv;
	}
	
	Inventory getBuyMenu(Player player, ItemStack item) {
		Inventory inv = Bukkit.createInventory(player, 54, "Purchase Item");
		inv.setItem(22, item);
		ItemStack addItem = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
		ItemStack addItems10 = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 10);
		ItemStack addItems64 = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 64);
		ItemMeta meta = addItem.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Add 1 Item");
		addItem.setItemMeta(meta);
		inv.setItem(24, addItem);
		meta = addItems10.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Add 10 Items");
		addItems10.setItemMeta(meta);
		inv.setItem(25, addItems10);
		meta = addItems64.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Set to 64 Items");
		addItems64.setItemMeta(meta);
		inv.setItem(26, addItems64);
		ItemStack removeItems32 = new ItemStack(Material.RED_STAINED_GLASS_PANE, 32);
		ItemStack removeItems10 = new ItemStack(Material.RED_STAINED_GLASS_PANE, 10);
		ItemStack setTo1 = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
		meta = removeItems32.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Remove 32 Items");
		removeItems32.setItemMeta(meta);
		inv.setItem(18, removeItems32);
		meta = removeItems10.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Remove 10 Items");
		removeItems10.setItemMeta(meta);
		inv.setItem(19, removeItems10);
		meta = setTo1.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Remove 1 Item");
		setTo1.setItemMeta(meta);
		inv.setItem(20, setTo1);
		ItemStack purchase = new ItemStack(Material.CHEST);
		String[] loreTokens = item.getItemMeta().getLore().get(0).split(" ");
		double price = Double.valueOf(loreTokens[2]);
		ArrayList<String> lore = new ArrayList<>();
		lore.add("Total Price: " + price);
		meta = purchase.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lPurchase"));
		meta.setLore(lore);
		purchase.setItemMeta(meta);
		inv.setItem(39, purchase);
		ItemStack back = new ItemStack(Material.BARRIER);
		meta = back.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&c&lCancel"));
		back.setItemMeta(meta);
		inv.setItem(41, back);
		ItemStack purchaseStacks = new ItemStack(Material.CHEST_MINECART);
		meta = purchaseStacks.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lPurchase Multiple Stacks"));
		purchaseStacks.setItemMeta(meta);
		inv.setItem(49, purchaseStacks);
		
		return inv;
	}
	
	Inventory getSellMenu(Player player, ItemStack item) {
		Inventory inv = Bukkit.createInventory(player, 54, "Sell Item");
		inv.setItem(22, item);
		ItemStack addItem = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 1);
		ItemStack addItems10 = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 10);
		ItemStack addItems64 = new ItemStack(Material.GREEN_STAINED_GLASS_PANE, 64);
		ItemMeta meta = addItem.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Add 1 Item");
		addItem.setItemMeta(meta);
		inv.setItem(24, addItem);
		meta = addItems10.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Add 10 Items");
		addItems10.setItemMeta(meta);
		inv.setItem(25, addItems10);
		meta = addItems64.getItemMeta();
		meta.setDisplayName(ChatColor.GREEN + "Set to 64 Items");
		addItems64.setItemMeta(meta);
		inv.setItem(26, addItems64);
		ItemStack removeItems32 = new ItemStack(Material.RED_STAINED_GLASS_PANE, 32);
		ItemStack removeItems10 = new ItemStack(Material.RED_STAINED_GLASS_PANE, 10);
		ItemStack setTo1 = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);
		meta = removeItems32.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Remove 32 Items");
		removeItems32.setItemMeta(meta);
		inv.setItem(18, removeItems32);
		meta = removeItems10.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Remove 10 Items");
		removeItems10.setItemMeta(meta);
		inv.setItem(19, removeItems10);
		meta = setTo1.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Remove 1 Item");
		setTo1.setItemMeta(meta);
		inv.setItem(20, setTo1);
		ItemStack purchase = new ItemStack(Material.CHEST);
		String[] loreTokens = item.getItemMeta().getLore().get(1).split(" ");
		double price = Double.valueOf(loreTokens[2]);
		ArrayList<String> lore = new ArrayList<>();
		lore.add("Total Price: " + price);
		meta = purchase.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lSell"));
		meta.setLore(lore);
		purchase.setItemMeta(meta);
		inv.setItem(39, purchase);
		ItemStack back = new ItemStack(Material.BARRIER);
		meta = back.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&c&lCancel"));
		back.setItemMeta(meta);
		inv.setItem(41, back);
		ItemStack purchaseStacks = new ItemStack(Material.CHEST_MINECART);
		meta = purchaseStacks.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lSell Multiple Stacks"));
		purchaseStacks.setItemMeta(meta);
		inv.setItem(49, purchaseStacks);
		
		return inv;
	}
	
	Inventory getMultipleStackBuy(Player player, ItemStack item) {
		String[] loreTokens = item.getItemMeta().getLore().get(0).split(" ");
		double price = Double.valueOf(loreTokens[2]);
		ArrayList<String> lore = new ArrayList<>();
		Inventory inv = Bukkit.createInventory(player, 27, "Purchase Multiple Stacks");
		inv.setItem(4, item);
		
		ItemStack purchase1 = new ItemStack(Material.CHEST_MINECART);
		ItemMeta meta = purchase1.getItemMeta();
		lore.add("Total Price: " + price * 64 + " coins");
		meta.setLore(lore);
		lore.clear();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lPurchase 1 Stack"));
		purchase1.setItemMeta(meta);
		inv.setItem(10, purchase1);
		ItemStack purchase2 = new ItemStack(Material.CHEST_MINECART, 2);
		meta = purchase2.getItemMeta();
		lore.add("Total Price: " + price * 64 * 2 + " coins");
		meta.setLore(lore);
		lore.clear();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lPurchase 2 Stacks"));
		purchase2.setItemMeta(meta);
		inv.setItem(11, purchase2);
		ItemStack purchase3 = new ItemStack(Material.CHEST_MINECART, 3);
		meta = purchase3.getItemMeta();
		lore.add("Total Price: " + price * 64 * 3 + " coins");
		meta.setLore(lore);
		lore.clear();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lPurchase 3 Stacks"));
		purchase3.setItemMeta(meta);
		inv.setItem(12, purchase3);
		ItemStack purchase4 = new ItemStack(Material.CHEST_MINECART, 4);
		meta = purchase4.getItemMeta();
		lore.add("Total Price: " + price * 64 * 4+ " coins");
		meta.setLore(lore);
		lore.clear();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lPurchase 4 Stacks"));
		purchase4.setItemMeta(meta);
		inv.setItem(13, purchase4);
		ItemStack purchase5 = new ItemStack(Material.CHEST_MINECART, 5);
		meta = purchase5.getItemMeta();
		lore.add("Total Price: " + price * 64 * 5 + " coins");
		meta.setLore(lore);
		lore.clear();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lPurchase 5 Stacks"));
		purchase5.setItemMeta(meta);
		inv.setItem(14, purchase5);
		ItemStack purchase6 = new ItemStack(Material.CHEST_MINECART, 6);
		meta = purchase6.getItemMeta();
		lore.add("Total Price: " + price * 64 * 6 + " coins");
		meta.setLore(lore);
		lore.clear();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lPurchase 6 Stacks"));
		purchase6.setItemMeta(meta);
		inv.setItem(15, purchase6);
		ItemStack purchase7 = new ItemStack(Material.CHEST_MINECART, 7);
		meta = purchase7.getItemMeta();
		lore.add("Total Price: " + price * 64 * 7 + " coins");
		meta.setLore(lore);
		lore.clear();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lPurchase 7 Stacks"));
		purchase7.setItemMeta(meta);
		inv.setItem(16, purchase7);
		ItemStack back = new ItemStack(Material.BARRIER);
		meta = back.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&c&lCancel"));
		back.setItemMeta(meta);
		inv.setItem(22, back);
		
		return inv;
	}
	
	ItemStack getShopItem(Material material, double price, double sellPrice, int amount) {
		ItemStack item = new ItemStack(material, amount);
		ItemMeta meta = item.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		lore.add("Buy Price: " + price + " coins");
		lore.add("Sell Price: " + sellPrice + " coins");
		meta.setLore(lore);
		item.setItemMeta(meta);
		
		return item;
	}

	ItemStack getCoin() {
		String url = "http://textures.minecraft.net/texture/8a03a8a877de7a4d6b167633a96ae3983998fd9d9a4c5e3fa817d138e81e4499";
		ArrayList<String> lore = new ArrayList<>();
		lore.add("Coin");

		GameProfile profile = new GameProfile(UUID.randomUUID(), "coin");
		PropertyMap propertyMap = profile.getProperties();
		if (propertyMap == null) {
			throw new IllegalStateException("Profile doesn't contain a property map");
		}
		byte[] encodedData = Base64.getEncoder().encode(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
		propertyMap.put("textures", new Property("textures", new String(encodedData)));
		ItemStack head = new ItemStack(Material.PLAYER_HEAD);
		ItemMeta headMeta = head.getItemMeta();
		Class<?> headMetaClass = headMeta.getClass();
		Reflections.getField(headMetaClass, "profile", GameProfile.class).set(headMeta, profile);
		headMeta.setDisplayName("Coin");
		head.setItemMeta(headMeta);
		return head;
	}
	@EventHandler
	void onInventoryClick(InventoryClickEvent event) {
		Inventory inv = event.getInventory();
		Player player = (Player) event.getWhoClicked();
		if(event.getView().getTitle() == "Shop") {
			event.setCancelled(true);
			if(event.getSlot() == 12) {
				player.closeInventory();
				player.openInventory(getShop(player, 1));
			} else if(event.getSlot() == 13) {
				player.closeInventory();
				player.openInventory(getShop(player, 2));
			}
		}
		if(event.getView().getTitle() == "Building") {
			event.setCancelled(true);
			if(event.getSlot() == 49) {
				player.closeInventory();
				player.openInventory(getShop(player, 0));
			}
			if(event.isLeftClick() && event.getSlot() >= 10 && event.getSlot() <= 29 && event.getSlot() % 9 != 0 && event.getSlot() % 9 != 8) {
				player.closeInventory();
				player.openInventory(getBuyMenu(player, event.getCurrentItem()));
			}
			if(event.isRightClick() && event.getSlot() >= 10 && event.getSlot() <= 29 && event.getSlot() % 9 != 0 && event.getSlot() % 9 != 8) {
				player.closeInventory();
				player.openInventory(getSellMenu(player, event.getCurrentItem()));
			}

		}
		if(event.getView().getTitle() == "Ores/Metals") {
			event.setCancelled(true);
			if(event.getSlot() == 49) {
				player.closeInventory();
				player.openInventory(getShop(player, 0));
			}
			if(event.isLeftClick() && event.getSlot() >= 10 && event.getSlot() <= 34 && event.getSlot() % 9 != 0 && event.getSlot() % 9 != 8) {
				player.closeInventory();
				player.openInventory(getBuyMenu(player, event.getCurrentItem()));
			}
			if(event.isRightClick() && event.getSlot() >= 10 && event.getSlot() <= 34 && event.getSlot() % 9 != 0 && event.getSlot() % 9 != 8) {
				player.closeInventory();
				player.openInventory(getSellMenu(player, event.getCurrentItem()));
			}
		}
		if(event.getView().getTitle() == "Purchase Item") {
			event.setCancelled(true);
			ItemStack item = inv.getItem(22);
			String[] buyTokens = item.getItemMeta().getLore().get(0).split(" ");
			double price = Double.valueOf(buyTokens[2]);
			String[] sellTokens = item.getItemMeta().getLore().get(1).split(" ");
			double sellPrice = Double.valueOf(sellTokens[2]);
			if(event.getSlot() == 24) {
				inv.setItem(22, getShopItem(item.getType(), price, sellPrice, item.getAmount() + 1));
			} else if(event.getSlot() == 25) {
				inv.setItem(22, getShopItem(item.getType(), price, sellPrice, item.getAmount() + 10));
			} else if(event.getSlot() == 26) {
				inv.setItem(22, getShopItem(item.getType(), price, sellPrice, 64));
			}
			if(item.getAmount() > 1) {
				if(event.getSlot() == 20) {
					inv.setItem(22, getShopItem(item.getType(), price, sellPrice, item.getAmount() - 1));
				} else if(event.getSlot() == 19) {
					if(item.getAmount() > 10) {
						inv.setItem(22, getShopItem(item.getType(), price, sellPrice, item.getAmount() - 10));
					} else {
						inv.setItem(22, getShopItem(item.getType(), price, sellPrice, 1));
					}
				} else if(event.getSlot() == 18) {
					if(item.getAmount() > 32) {
						inv.setItem(22, getShopItem(item.getType(), price, sellPrice, item.getAmount() - 32));
					} else {
						inv.setItem(22, getShopItem(item.getType(), price, sellPrice, 1));
					}
				}
			}
			if(event.getSlot() == 41) {
				player.closeInventory();
			} else if(event.getSlot() == 39) {
				if(player.getInventory().firstEmpty() != -1) {
					if(econ.withdrawPlayer(player, price * item.getAmount()) == 0) {
						player.getInventory().addItem(new ItemStack(item.getType(), item.getAmount(), item.getData().getData()));
						player.closeInventory();
						player.sendMessage(ChatColor.GREEN + "You purchased " + item.getAmount() + " " + item.getType().toString());
					} else {
						player.closeInventory();
						player.sendMessage(ChatColor.RED + "You do not have enough money.");
					}
				} else {
					player.sendMessage(ChatColor.RED + "You do not have enough space in your inventory.");
				}
			} else if (event.getSlot() == 49) {
				player.closeInventory();
				player.openInventory(getMultipleStackBuy(player, item));
			}
			ArrayList<String> lore = new ArrayList<>();
			lore.add("Total Price: " + price * inv.getItem(22).getAmount());
			ItemMeta meta = inv.getItem(39).getItemMeta();
			meta.setLore(lore);
			inv.getItem(39).setItemMeta(meta);
		}
		if(event.getView().getTitle() == "Sell Item") {
			event.setCancelled(true);
			ItemStack item = inv.getItem(22);
			String[] buyTokens = item.getItemMeta().getLore().get(0).split(" ");
			double price = Double.valueOf(buyTokens[2]);
			String[] sellTokens = item.getItemMeta().getLore().get(1).split(" ");
			double sellPrice = Double.valueOf(sellTokens[2]);
			if(event.getSlot() == 24) {
				inv.setItem(22, getShopItem(item.getType(), price, sellPrice, item.getAmount() + 1));
			} else if(event.getSlot() == 25) {
				inv.setItem(22, getShopItem(item.getType(), price, sellPrice, item.getAmount() + 10));
			} else if(event.getSlot() == 26) {
				inv.setItem(22, getShopItem(item.getType(), price, sellPrice, 64));
			}
			if(item.getAmount() > 1) {
				if(event.getSlot() == 20) {
					inv.setItem(22, getShopItem(item.getType(), price, sellPrice, item.getAmount() - 1));
				} else if(event.getSlot() == 19) {
					if(item.getAmount() > 10) {
						inv.setItem(22, getShopItem(item.getType(), price, sellPrice, item.getAmount() - 10));
					} else {
						inv.setItem(22, getShopItem(item.getType(), price, sellPrice, 1));
					}
				} else if(event.getSlot() == 18) {
					if(item.getAmount() > 32) {
						inv.setItem(22, getShopItem(item.getType(), price, sellPrice, item.getAmount() - 32));
					} else {
						inv.setItem(22, getShopItem(item.getType(), price, sellPrice, 1));
					}
				}
			}
			if(event.getSlot() == 41) {
				player.closeInventory();
			} else if(event.getSlot() == 39) {
				/*int itemAmount = 0;
				for(ItemStack itemStack : player.getInventory().getContents()) {
					if(itemStack.getType() == item.getType() && itemStack != null) {
						itemAmount += itemStack.getAmount();
					}
				}*/
				if(player.getInventory().containsAtLeast(new ItemStack(item.getType()), item.getAmount())) {
					if(econ.depositPlayer(player, sellPrice * item.getAmount()) == 0) {
						player.getInventory().removeItem(new ItemStack(item.getType(), item.getAmount(), item.getData().getData()));
						player.closeInventory();
						player.sendMessage(ChatColor.GREEN + "You sold " + item.getAmount() + " " + item.getType().toString());
					}
				} else {
					player.sendMessage(ChatColor.RED + "You do not have enough items to sell.");
				}
			} else if (event.getSlot() == 49) {
				player.closeInventory();
				player.openInventory(getMultipleStackBuy(player, item));
			}
			ArrayList<String> lore = new ArrayList<>();
			lore.add("Total Price: " + sellPrice * (double) inv.getItem(22).getAmount());
			ItemMeta meta = inv.getItem(39).getItemMeta();
			meta.setLore(lore);
			inv.getItem(39).setItemMeta(meta);
		}
		if(event.getView().getTitle() == "Purchase Multiple Stacks") {
			event.setCancelled(true);
			ItemStack item = inv.getItem(4);
			String[] loreTokens = item.getItemMeta().getLore().get(0).split(" ");
			double price = Double.parseDouble(loreTokens[2]);
			int emptySlots = 0;
			for (ItemStack itemStacks : player.getInventory().getContents()) {
				if(itemStacks == null) {
					emptySlots++;
				}
			}
			if(event.getSlot() == 10 && emptySlots >= 1) {
				if(econ.withdrawPlayer(player, price * 64) == 0) {
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.closeInventory();
					player.sendMessage(ChatColor.GREEN + "You purchased 64 " + item.getType().toString());
				} else {
					player.closeInventory();
					player.sendMessage(ChatColor.RED + "You do not have enough money.");
				}
			} else if(event.getSlot() == 11 && emptySlots >= 2) {
				if(econ.withdrawPlayer(player, price * 64 * 2) == 0) {
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.closeInventory();
					player.sendMessage(ChatColor.GREEN + "You purchased 2 stacks of " + item.getType().toString());
				} else {
					player.closeInventory();
					player.sendMessage(ChatColor.RED + "You do not have enough money.");
				}
			} else if(event.getSlot() == 12 && emptySlots >= 3) {
				if(econ.withdrawPlayer(player, price * 64 * 3) == 0) {
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.closeInventory();
					player.sendMessage(ChatColor.GREEN + "You purchased 3 stacks of " + item.getType().toString());
				} else {
					player.closeInventory();
					player.sendMessage(ChatColor.RED + "You do not have enough money.");
				}
			} else if(event.getSlot() == 13 && emptySlots >= 4) {
				if(econ.withdrawPlayer(player, price * 64 * 4) == 0) {
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.closeInventory();
					player.sendMessage(ChatColor.GREEN + "You purchased 4 stacks of " + item.getType().toString());
				} else {
					player.closeInventory();
					player.sendMessage(ChatColor.RED + "You do not have enough money.");
				}
			} else if(event.getSlot() == 14 && emptySlots >= 5) {
				if(econ.withdrawPlayer(player, price * 64 * 5) == 0) {
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.closeInventory();
					player.sendMessage(ChatColor.GREEN + "You purchased 5 stacks of " + item.getType().toString());
				} else {
					player.closeInventory();
					player.sendMessage(ChatColor.RED + "You do not have enough money.");
				}
			} else if(event.getSlot() == 15 && emptySlots >= 6) {
				if(econ.withdrawPlayer(player, price * 64 * 6) == 0) {
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.closeInventory();
					player.sendMessage(ChatColor.GREEN + "You purchased 6 stacks of " + item.getType().toString());
				} else {
					player.closeInventory();
					player.sendMessage(ChatColor.RED + "You do not have enough money.");
				}
			} else if(event.getSlot() == 16 && emptySlots >= 7) {
				if(econ.withdrawPlayer(player, price * 64 * 7) == 0) {
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.getInventory().addItem(new ItemStack(item.getType(), 64));
					player.closeInventory();
					player.sendMessage(ChatColor.GREEN + "You purchased 7 stacks of " + item.getType().toString());
				} else {
					player.closeInventory();
					player.sendMessage(ChatColor.RED + "You do not have enough money.");
				}
			} else if(event.getSlot() >= 10 && event.getSlot() <= 16) {
				player.closeInventory();
				player.sendMessage(ChatColor.RED + "You do not have enough space in your inventory.");
			}
			if(event.getSlot() == 22) {
				player.closeInventory();
			}
		}
		if(event.getView().getTitle() == "Shop Customizer") {
			event.setCancelled(true);
			if(event.getSlot() == 49) {
				player.closeInventory();
			}
		}
	}

	@EventHandler
	void onCoinClick(PlayerInteractEvent event) {
		Player player = (Player) event.getPlayer();
		Block clickedBlock = null; 

		if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			clickedBlock = event.getClickedBlock();
			if (clickedBlock.getState() instanceof Skull) {
				Skull skull = (Skull) clickedBlock.getState();
				if(skull.getOwner() == null)
					return;
				if(skull.getOwner().equalsIgnoreCase("coin") && !data.getConfig().getStringList(player.getUniqueId().toString() + ".coins").contains(clickedBlock.getLocation().toString())) {
					List<String> list = data.getConfig().getStringList(player.getUniqueId() + ".coins");
					list.add(clickedBlock.getLocation().toString());
					if(econ.depositPlayer(player, 1) == 0) {
						player.sendMessage(ChatColor.GOLD + "+1 coin");
					}
					
					data.getConfig().set(player.getUniqueId() + ".coins", list);
					data.saveConfig();
				}
			}
		} else {
			return;
		}
	}
	
	@EventHandler
	void onCommandPreprocess (PlayerCommandPreprocessEvent event) {
		String[] tokens = event.getMessage().split(" ");
		if(tokens[0].equalsIgnoreCase("/bal") || event.getMessage().equalsIgnoreCase("/balance")) {
			event.setCancelled(true);
			Player player = (Player) event.getPlayer();
			player.sendMessage(String.format("You have %s", econ.getBalance(player)));
		} else if (tokens[0].equalsIgnoreCase("/help") && !event.getPlayer().hasPermission("nte.admin")) {
			event.setCancelled(true);
			Player player = (Player) event.getPlayer();
			player.sendMessage(ChatColor.RED + "You cannot run this command.");
		} else if(tokens[0].equalsIgnoreCase("/?") && !event.getPlayer().hasPermission("nte.admin")) {
			event.setCancelled(true);
			Player player = (Player) event.getPlayer();
			player.sendMessage(ChatColor.RED + "You cannot run this command.");
		} else if(tokens[0].equalsIgnoreCase("/pay")) {
			event.setCancelled(true);
			Player player = (Player) event.getPlayer();
			if(tokens.length < 3) {
				player.sendMessage(ChatColor.RED + "Usage: /pay <player> <coins>");
				return;
			}
			if(tokens[1].equalsIgnoreCase(player.getName())) {
				player.sendMessage(ChatColor.RED + "You cannot pay yourself.");
				return;
			}
			
			if(Bukkit.getPlayer(tokens[1]) == null) {
				player.sendMessage(ChatColor.RED + "That player does not exist.");
				return;
			}
			try {
				Double.parseDouble(tokens[2]);
			} catch (NumberFormatException e) {
				player.sendMessage(ChatColor.RED + "Valid number not specified.");
			}
			if(Double.parseDouble(tokens[2]) > econ.getBalance(player)) {
				player.sendMessage(ChatColor.RED + "You do not have enough coins.");
				return;
			}
			if(econ.withdrawPlayer(player, Double.parseDouble(tokens[2])) == 0) {
				if(econ.depositPlayer(Bukkit.getPlayer(tokens[1]), Double.parseDouble(tokens[2])) == 0) {
					player.sendMessage(ChatColor.GREEN + "You have given "+tokens[1]+" "+tokens[2]+" dollars");
				}
			}
		}
	}
}
