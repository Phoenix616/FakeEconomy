package de.themoep.fakeeconomy;

/*
 * FakeEconomy
 * Copyright (c) 2020 Max Lee aka Phoenix616 (max@themoep.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class FakeEconomy extends JavaPlugin {

    private static final BigDecimal DEFAULT_BALANCE = BigDecimal.valueOf(1000000);
    private Map<String, BigDecimal> balances = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("fakeeconomy").setExecutor(this);
        getServer().getServicesManager().register(Economy.class, new VaultEconomy(), this, ServicePriority.Highest);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if ("balance".equalsIgnoreCase(args[0])) {
                if (args.length > 1) {
                    sender.sendMessage(args[1] + "'s Balance: " + balances.getOrDefault(args[1].toLowerCase(), DEFAULT_BALANCE).toPlainString());
                    return true;
                }
            } else if ("setbalance".equalsIgnoreCase(args[0])) {
                if (args.length > 2) {
                    try {
                        balances.put(args[1].toLowerCase(), BigDecimal.valueOf(Double.parseDouble(args[2])));
                        sender.sendMessage(args[1] + "'s Balance set to " + balances.getOrDefault(args[1].toLowerCase(), DEFAULT_BALANCE).toPlainString());
                        return true;
                    } catch (NumberFormatException e) {
                        sender.sendMessage(e.getMessage());
                    }
                }
            }
        }
        return false;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matching(args[0], "balance", "setbalance");
        } else if (args.length == 2) {
            return getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3 && "setbalance".equalsIgnoreCase(args[0])) {
            if (args[2].length() == 1) {
                return Arrays.asList("-1", "0", args[2], args[2] + "0", args[2] + "00", args[2] + "000", args[2] + "0000", args[2] +  "00000", args[2] + "000000");
            } else {
                return Arrays.asList("-1", "0", "1", "10", "100", "1000", "10000", "100000", "1000000");
            }
        }
        return null;
    }

    private List<String> matching(String arg, String... possibilities) {
        List<String> completions = new ArrayList<>();
        for (String s : possibilities) {
            if (s.toLowerCase().startsWith(arg.toLowerCase())) {
                completions.add(s);
            }
        }
        return completions;
    }

    public class VaultEconomy extends AbstractEconomy {
        private static final String BANK_PREFIX = "bank:";

        @Override
        public boolean isEnabled() {
            return FakeEconomy.this.isEnabled();
        }

        @Override
        public String getName() {
            return getDescription().getName();
        }

        @Override
        public boolean hasBankSupport() {
            return true;
        }

        @Override
        public int fractionalDigits() {
            return 2;
        }

        @Override
        public String format(double v) {
            return BigDecimal.valueOf(v).setScale(fractionalDigits(), RoundingMode.HALF_UP).toPlainString();
        }

        @Override
        public String currencyNamePlural() {
            return "Moneyz";
        }

        @Override
        public String currencyNameSingular() {
            return "Money";
        }

        @Override
        public boolean hasAccount(String s) {
            return getBalance(s) > -1;
        }

        @Override
        public boolean hasAccount(String s, String s1) {
            return hasAccount(s);
        }

        @Override
        public double getBalance(String s) {
            return balances.getOrDefault(s.toLowerCase(), DEFAULT_BALANCE).doubleValue();
        }

        @Override
        public double getBalance(String s, String s1) {
            return getBalance(s);
        }

        @Override
        public boolean has(String s, double v) {
            return balances.getOrDefault(s.toLowerCase(), DEFAULT_BALANCE).compareTo(BigDecimal.valueOf(v)) >= 0;
        }

        @Override
        public boolean has(String s, String s1, double v) {
            return has(s, v);
        }

        @Override
        public EconomyResponse withdrawPlayer(String s, double v) {
            if (hasAccount(s)) {
                if (has(s, v)) {
                    getLogger().log(Level.INFO, "Withdraw " + v + " from " + s + "(Balance: " + getBalance(s) + ")");
                    balances.put(s.toLowerCase(), balances.getOrDefault(s.toLowerCase(), DEFAULT_BALANCE).subtract(BigDecimal.valueOf(v)));
                    return new EconomyResponse(v, getBalance(s), EconomyResponse.ResponseType.SUCCESS, null);
                }
                getLogger().log(Level.INFO, "Withdrawing " + v + " from " + s + " (Balance: " + getBalance(s) + ") failed. Not enough money.");
                return new EconomyResponse(0, getBalance(s), EconomyResponse.ResponseType.FAILURE, "Not enough money");
            }
            getLogger().log(Level.INFO, "Withdrawing " + v + " from " + s + " failed. No Account.");
            return new EconomyResponse(0, getBalance(s), EconomyResponse.ResponseType.FAILURE, "No account");
        }

        @Override
        public EconomyResponse withdrawPlayer(String s, String s1, double v) {
            return withdrawPlayer(s, v);
        }

        @Override
        public EconomyResponse depositPlayer(String s, double v) {
            if (hasAccount(s)) {
                getLogger().log(Level.INFO, "Deposit " + v + " to " + s + " (Balance: " + getBalance(s) + ")");
                balances.put(s.toLowerCase(), balances.getOrDefault(s.toLowerCase(), DEFAULT_BALANCE).add(BigDecimal.valueOf(v)));
                return new EconomyResponse(v, getBalance(s), EconomyResponse.ResponseType.SUCCESS, null);
            }
            getLogger().log(Level.INFO, "Deposit " + v + " to " + s + " failed. No Account.");
            return new EconomyResponse(0, getBalance(s), EconomyResponse.ResponseType.FAILURE, "No account");
        }

        @Override
        public EconomyResponse depositPlayer(String s, String s1, double v) {
            return depositPlayer(s, v);
        }

        @Override
        public EconomyResponse createBank(String s, String s1) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, null);
        }

        @Override
        public EconomyResponse deleteBank(String s) {
            balances.remove(BANK_PREFIX + s);
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, null);
        }

        @Override
        public EconomyResponse bankBalance(String s) {
            return new EconomyResponse(0, getBalance(BANK_PREFIX + s), EconomyResponse.ResponseType.SUCCESS, null);
        }

        @Override
        public EconomyResponse bankHas(String s, double v) {
            if (has(BANK_PREFIX + s, v)) {
                return new EconomyResponse(0, getBalance(BANK_PREFIX + s), EconomyResponse.ResponseType.SUCCESS, null);
            }
            return new EconomyResponse(0, getBalance(BANK_PREFIX + s), EconomyResponse.ResponseType.FAILURE, "That bank does not have that amount!");
        }

        @Override
        public EconomyResponse bankWithdraw(String s, double v) {
            return withdrawPlayer(BANK_PREFIX + s, v);
        }

        @Override
        public EconomyResponse bankDeposit(String s, double v) {
            return depositPlayer(BANK_PREFIX + s, v);
        }

        @Override
        public EconomyResponse isBankOwner(String s, String s1) {
            String owner = s.substring(BANK_PREFIX.length());
            if (owner.equalsIgnoreCase(s1)) {
                return new EconomyResponse(0, getBalance(BANK_PREFIX + s), EconomyResponse.ResponseType.SUCCESS, null);
            }
            return new EconomyResponse(0, getBalance(BANK_PREFIX + s), EconomyResponse.ResponseType.FAILURE, "Bank is owned by " + owner);
        }

        @Override
        public EconomyResponse isBankMember(String s, String s1) {
            return isBankOwner(s, s1);
        }

        @Override
        public List<String> getBanks() {
            return balances.keySet().stream().filter(s -> s.startsWith(BANK_PREFIX)).map(s -> s.substring(BANK_PREFIX.length())).collect(Collectors.toList());
        }

        @Override
        public boolean createPlayerAccount(String s) {
            return balances.putIfAbsent(s, BigDecimal.ZERO) == null;
        }

        @Override
        public boolean createPlayerAccount(String s, String s1) {
            return createPlayerAccount(s);
        }
    }
}
