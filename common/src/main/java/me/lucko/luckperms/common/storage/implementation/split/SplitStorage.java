/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.storage.implementation.split;

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.PlayerSaveResult;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.bulkupdate.comparisons.Constraint;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SplitStorage implements StorageImplementation {
    private final LuckPermsPlugin plugin;
    private final Map<StorageType, StorageImplementation> backing;
    private final Map<SplitStorageType, StorageType> types;
    
    public SplitStorage(LuckPermsPlugin plugin, Map<StorageType, StorageImplementation> backing, Map<SplitStorageType, StorageType> types) {
        this.plugin = plugin;
        this.backing = ImmutableMap.copyOf(backing);
        this.types = ImmutableMap.copyOf(types);
    }

    @Override
    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public String getImplementationName() {
        return "Split Storage";
    }

    @Override
    public void init() {
        boolean failed = false;
        for (StorageImplementation ds : this.backing.values()) {
            try {
                ds.init();
            } catch (Exception ex) {
                failed = true;
                ex.printStackTrace();
            }
        }
        if (failed) {
            throw new RuntimeException("One of the backings failed to init");
        }
    }

    @Override
    public void shutdown() {
        for (StorageImplementation ds : this.backing.values()) {
            try {
                ds.shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Map<String, String> getMeta() {
        Map<String, String> ret = new LinkedHashMap<>();
        ret.put("Types", this.types.toString());
        for (StorageImplementation backing : this.backing.values()) {
            ret.putAll(backing.getMeta());
        }
        return ret;
    }

    @Override
    public void logAction(LogEntry entry) throws Exception {
        this.backing.get(this.types.get(SplitStorageType.LOG)).logAction(entry);
    }

    @Override
    public Log getLog() throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.LOG)).getLog();
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) throws Exception {
        StorageType userType = this.types.get(SplitStorageType.USER);
        StorageType groupType = this.types.get(SplitStorageType.GROUP);

        this.backing.get(userType).applyBulkUpdate(bulkUpdate);

        // if differs
        if (userType != groupType) {
            this.backing.get(groupType).applyBulkUpdate(bulkUpdate);
        }
    }

    @Override
    public User loadUser(UUID uuid, String username) throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.USER)).loadUser(uuid, username);
    }

    @Override
    public void saveUser(User user) throws Exception {
        this.backing.get(this.types.get(SplitStorageType.USER)).saveUser(user);
    }

    @Override
    public Set<UUID> getUniqueUsers() throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.USER)).getUniqueUsers();
    }

    @Override
    public List<HeldPermission<UUID>> getUsersWithPermission(Constraint constraint) throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.USER)).getUsersWithPermission(constraint);
    }

    @Override
    public Group createAndLoadGroup(String name) throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.GROUP)).createAndLoadGroup(name);
    }

    @Override
    public Optional<Group> loadGroup(String name) throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.GROUP)).loadGroup(name);
    }

    @Override
    public void loadAllGroups() throws Exception {
        this.backing.get(this.types.get(SplitStorageType.GROUP)).loadAllGroups();
    }

    @Override
    public void saveGroup(Group group) throws Exception {
        this.backing.get(this.types.get(SplitStorageType.GROUP)).saveGroup(group);
    }

    @Override
    public void deleteGroup(Group group) throws Exception {
        this.backing.get(this.types.get(SplitStorageType.GROUP)).deleteGroup(group);
    }

    @Override
    public List<HeldPermission<String>> getGroupsWithPermission(Constraint constraint) throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.GROUP)).getGroupsWithPermission(constraint);
    }

    @Override
    public Track createAndLoadTrack(String name) throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.TRACK)).createAndLoadTrack(name);
    }

    @Override
    public Optional<Track> loadTrack(String name) throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.TRACK)).loadTrack(name);
    }

    @Override
    public void loadAllTracks() throws Exception {
        this.backing.get(this.types.get(SplitStorageType.TRACK)).loadAllTracks();
    }

    @Override
    public void saveTrack(Track track) throws Exception {
        this.backing.get(this.types.get(SplitStorageType.TRACK)).saveTrack(track);
    }

    @Override
    public void deleteTrack(Track track) throws Exception {
        this.backing.get(this.types.get(SplitStorageType.TRACK)).deleteTrack(track);
    }

    @Override
    public PlayerSaveResult savePlayerData(UUID uuid, String username) throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.UUID)).savePlayerData(uuid, username);
    }

    @Override
    public UUID getPlayerUuid(String username) throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.UUID)).getPlayerUuid(username);
    }

    @Override
    public String getPlayerName(UUID uuid) throws Exception {
        return this.backing.get(this.types.get(SplitStorageType.UUID)).getPlayerName(uuid);
    }
}
