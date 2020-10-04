package ml.chriscommunity.survivalplus.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
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
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;

public class Main extends JavaPlugin implements Listener {
	DataManager data = new DataManager(this);
	
	private static final Logger log = Logger.getLogger("Minecraft");
	private static Economy econ = null;
	private static Permission perms = null;
	private static Chat chat = null;
	

	@Override
	public void onEnable() {
		if(!setupEconomy()) {
			log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
		}
		setupPermissions();
		setupChat();
		data.saveConfig();

		getServer().getPluginManager().registerEvents(this, this);
	}
	
	private boolean setupEconomy() {
		if(getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if(rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
				
		return econ != null;
	}
	
	private boolean setupChat() {
		RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
		chat = rsp.getProvider();
		return chat != null;
	}
	
	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
		return chat != null;
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
						EconomyResponse r = econ.depositPlayer(Bukkit.getPlayer(args[1]), Double.parseDouble(args[2]));
						if(r.transactionSuccess()) {
							Bukkit.getPlayer(args[1]).sendMessage(String.format("You were given %s and now have %f", econ.format(r.amount), r.balance));
							data.getConfig().set(Bukkit.getPlayer(args[1]).getUniqueId() + ".balance", r.balance);
							data.saveConfig();
						}
						
						return true;
					} else if(args[0].equalsIgnoreCase("take")) {
						if(Double.parseDouble(args[2]) < 0) {
							player.sendMessage(ChatColor.RED + "Amount cannot be less than 0.");
							return true;
						}
						EconomyResponse r = econ.withdrawPlayer(Bukkit.getPlayer(args[1]), Double.parseDouble(args[2]));
						if(r.transactionSuccess()) {
							Bukkit.getPlayer(args[1]).sendMessage(String.format("%s was taken from you and now have %f", econ.format(Math.abs(r.amount)), r.balance));
							data.getConfig().set(Bukkit.getPlayer(args[1]).getUniqueId() + ".balance", r.balance);
							data.saveConfig();
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
		
		ItemStack border = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)15);
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
			ItemStack spawners = new ItemStack(Material.MOB_SPAWNER);
			meta = spawners.getItemMeta();
			meta.setDisplayName(ChatColor.RESET + "Spawners");
			spawners.setItemMeta(meta);
			inv.setItem(14, spawners);
		} else if(tab == 1) {
			inv = Bukkit.createInventory(player, 54, "Building");
			ItemStack back = new ItemStack(Material.BARRIER);
			ItemMeta meta = back.getItemMeta();
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&c&lBack"));
			back.setItemMeta(meta);
			inv.setItem(49, back);
			
			inv.setItem(10, getShopItem(Material.DIRT, 10, 1));
			
		}
		return inv;
	}
	
	Inventory getBuyMenu(Player player, ItemStack item) {
		Inventory inv = Bukkit.createInventory(player, 54, "Purchase Item");
		inv.setItem(22, item);
		ItemStack addItem = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)5);
		ItemStack addItems10 = new ItemStack(Material.STAINED_GLASS_PANE, 10, (short)5);
		ItemStack addItems64 = new ItemStack(Material.STAINED_GLASS_PANE, 64, (short)5);
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
		ItemStack removeItems32 = new ItemStack(Material.STAINED_GLASS_PANE, 32, (short)14);
		ItemStack removeItems10 = new ItemStack(Material.STAINED_GLASS_PANE, 10, (short)14);
		ItemStack setTo1 = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)14);
		meta = removeItems32.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Remove 32 Items");
		removeItems32.setItemMeta(meta);
		inv.setItem(18, removeItems32);
		meta = removeItems10.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Remove 10 Items");
		removeItems10.setItemMeta(meta);
		inv.setItem(19, removeItems10);
		meta = setTo1.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Set to 1 Item");
		setTo1.setItemMeta(meta);
		inv.setItem(20, setTo1);
		ItemStack purchase = new ItemStack(Material.CHEST);
		meta = purchase.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&a&lPurchase"));
		purchase.setItemMeta(meta);
		inv.setItem(39, purchase);
		ItemStack back = new ItemStack(Material.BARRIER);
		meta = back.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&c&lCancel"));
		back.setItemMeta(meta);
		inv.setItem(41, back);
		
		return inv;
	}
	
	ItemStack getShopItem(Material material, int price, int amount) {
		ItemStack item = new ItemStack(material, amount);
		ItemMeta meta = item.getItemMeta();
		ArrayList<String> lore = new ArrayList<>();
		lore.add("Buy Price: " + price + " coins");
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
		byte[] encodedData = Base64.encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
		propertyMap.put("textures", new Property("textures", new String(encodedData)));
		ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
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
		if(inv.getName() == "Shop") {
			event.setCancelled(true);
			if(event.getSlot() == 12) {
				player.closeInventory();
				player.openInventory(getShop(player, 1));
			}
		}
		if(inv.getName() == "Building") {
			event.setCancelled(true);
			if(event.getSlot() == 49) {
				player.closeInventory();
				player.openInventory(getShop(player, 0));
			}
			if(event.getSlot() >= 10 && event.getSlot() <= 10) {
				player.closeInventory();
				player.openInventory(getBuyMenu(player, event.getCurrentItem()));
			}
		}
		if(inv.getName() == "Purchase Item") {
			event.setCancelled(true);
			ItemStack item = inv.getItem(22);
			String[] loreTokens = item.getItemMeta().getLore().get(0).split(" ");
			int price = Integer.parseInt(loreTokens[2]);
			if(event.getSlot() == 24) {
				inv.setItem(22, getShopItem(item.getType(), price, item.getAmount() + 1));
			} else if(event.getSlot() == 25) {
				inv.setItem(22, getShopItem(item.getType(), price, item.getAmount() + 10));
			} else if(event.getSlot() == 26) {
				inv.setItem(22, getShopItem(item.getType(), price, 64));
			}
			if(item.getAmount() > 1) {
				if(event.getSlot() == 20) {
					inv.setItem(22, getShopItem(item.getType(), price, 1));
				} else if(event.getSlot() == 19) {
					if(item.getAmount() > 10) {
						inv.setItem(22, getShopItem(item.getType(), price, item.getAmount() - 10));
					} else {
						inv.setItem(22, getShopItem(item.getType(), price, 1));
					}
				} else if(event.getSlot() == 18) {
					if(item.getAmount() > 32) {
						inv.setItem(22, getShopItem(item.getType(), price, item.getAmount() - 32));
					} else {
						inv.setItem(22, getShopItem(item.getType(), price, 1));
					}
				}
			}
			if(event.getSlot() == 41) {
				player.closeInventory();
			} else if(event.getSlot() == 39) {
				if(econ.getBalance(player) >= price * item.getAmount()) {
					player.getInventory().addItem(new ItemStack(item.getType(), item.getAmount()));
					player.closeInventory();
				}
			}
		}
		if(inv.getName() == "Shop Customizer") {
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
					EconomyResponse r = econ.depositPlayer(player, 1);
					if(r.transactionSuccess()) {
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
			player.sendMessage(String.format("You have %s", econ.format(econ.getBalance(player))));
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
			EconomyResponse r1 = econ.withdrawPlayer(player, Double.parseDouble(tokens[2]));
			if(r1.transactionSuccess()) {
				EconomyResponse r2 = econ.depositPlayer(Bukkit.getPlayer(tokens[1]), Double.parseDouble(tokens[2]));
				if(r2.transactionSuccess()) {
					player.sendMessage(ChatColor.GREEN + "You have given "+tokens[1]+" "+tokens[2]+" dollars");
				}
			}
		}
	}
}
