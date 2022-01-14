package io.siggi.magichopper;

import io.siggi.magichopper.rule.Rule;
import io.siggi.magichopper.rule.RuleAllow;
import io.siggi.magichopper.rule.RuleBlock;
import io.siggi.magichopper.rule.RuleCompact;
import io.siggi.magichopper.rule.RuleMatchFurnace;
import io.siggi.magichopper.rule.RuleSkip;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class MagicHopper extends JavaPlugin {
	@Override
	public void onEnable() {
		instance = this;
		getServer().getPluginManager().registerEvents(eventHandler = new HopperEventHandler(this), this);
	}

	private static MagicHopper instance = null;
	private HopperEventHandler eventHandler = null;

	public static void tickLater(Block block) {
		instance.eventHandler.tickLater(block);
	}

	public List<Rule> getRules(Block hopper) {
		List<Sign> signs = Util.getSignsOnBlock(hopper);
		List<Rule> rules = new ArrayList<>();
		Consumer<Rule> addRule = (rule) -> {
			for (Iterator<Rule> it = rules.iterator(); it.hasNext(); ) {
				Rule existingRule = it.next();
				Rule merged = rule.mergeWith(existingRule);
				if (merged != null) {
					it.remove();
					rules.add(merged);
					return;
				}
			}
			rules.add(rule);
		};
		for (Sign sign : signs) {
			String topLine = sign.getLine(0);
			String stripped = ChatColor.stripColor(topLine);
			if (topLine.equals(stripped) || !stripped.equals("[MH]"))
				continue;
			int lineIdx = -1;
			for (String line : sign.getLines()) {
				lineIdx += 1;
				int spacePos = line.indexOf(" ");
				String ruleType;
				String ruleData;
				if (spacePos == -1) {
					ruleType = line;
					ruleData = null;
				} else {
					ruleType = line.substring(0, spacePos);
					ruleData = line.substring(spacePos + 1);
				}
				Rule rule = null;
				switch (ruleType.toLowerCase()) {
					case "allow": {
						rule = new RuleAllow(ruleData);
					}
					break;
					case "block": {
						rule = new RuleBlock(ruleData);
					}
					break;
					case "matchfurnace": {
						rule = new RuleMatchFurnace();
					}
					break;
					case "compact": {
						rule = new RuleCompact();
					}
					break;
					case "skip": {
						try {
							String[] split = ruleData.split("/");
							if (split.length == 1) {
								rule = new RuleSkip(0, Integer.parseInt(split[0]), sign, lineIdx);
							} else {
								rule = new RuleSkip(Integer.parseInt(split[0]), Integer.parseInt(split[1]), sign, lineIdx);
							}
						} catch (Exception e) {
						}
					}
					break;
				}
				if (rule != null)
					addRule.accept(rule);
			}
		}
		return rules;
	}
}