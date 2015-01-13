/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.config.shell;

import org.apache.karaf.cellar.config.ConfigurationSupport;
import org.apache.karaf.cellar.config.Constants;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.osgi.service.cm.Configuration;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Command(scope = "cluster", name = "config-list", description = "List the configurations in a cluster group")
public class ListCommand extends ConfigCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "pid", description = "The configuration PID to look for", required = false, multiValued = false)
    String searchPid;

    @Option(name = "-m", aliases = {"--minimal"}, description = "Don't display the properties of each configuration", required = false, multiValued = false)
    boolean minimal;

    @Option(name = "--cluster", description = "Shows only configurations on the cluster", required = false, multiValued = false)
    boolean onlyCluster;

    @Option(name = "--local", description = "Shows only configurations on the local node", required = false, multiValued = false)
    boolean onlyLocal;

    @Option(name = "--blocked", description = "Shows only blocked configurations", required = false, multiValued = false)
    boolean onlyBlocked;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        ConfigurationSupport support = new ConfigurationSupport();
        support.setClusterManager(clusterManager);
        support.setGroupManager(groupManager);
        support.setConfigurationAdmin(configurationAdmin);

        Map<String, ConfigurationState> configurations = gatherConfigurations();

        if (configurations != null && !configurations.isEmpty()) {
            for (String pid : configurations.keySet()) {
                if (searchPid == null || (searchPid != null && searchPid.equals(pid))) {
                    ConfigurationState state = configurations.get(pid);

                    String located = "";
                    boolean cluster = state.isCluster();
                    boolean local = state.isLocal();
                    if (cluster && local)
                        located = "cluster/local";
                    if (cluster && ! local) {
                        located = "cluster";
                        if (onlyLocal)
                            continue;
                    }
                    if (local && !cluster) {
                        located = "local";
                        if (onlyCluster)
                            continue;
                    }

                    String blocked = "";
                    boolean inbound = support.isAllowed(group, Constants.CATEGORY, pid, EventType.INBOUND);
                    boolean outbound = support.isAllowed(group, Constants.CATEGORY, pid, EventType.OUTBOUND);
                    if (inbound && outbound && onlyBlocked)
                        continue;
                    if (!inbound && !outbound)
                        blocked = "in/out";
                    if (!inbound && outbound)
                        blocked = "in";
                    if (!outbound && inbound)
                        blocked = "out";

                    System.out.println("----------------------------------------------------------------");
                    System.out.println("Pid:            " + pid);
                    System.out.println("Located:        " + located);
                    System.out.println("Blocked:        " + blocked);
                    if (!minimal) {
                        Properties properties = state.getProperties();
                        if (properties != null) {
                            System.out.println("Properties:");
                            for (Enumeration e = properties.keys(); e.hasMoreElements(); ) {
                                Object key = e.nextElement();
                                System.out.println("   " + key + " = " + properties.get(key));
                            }
                        }
                    }
                }
            }
        } else System.err.println("No configuration PID found in cluster group " + groupName);

        return null;
    }

    private Map<String, ConfigurationState> gatherConfigurations() throws Exception {
        Map<String, ConfigurationState> configurations = new HashMap<String, ConfigurationState>();

        // retrieve cluster configurations
        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
        for (String key : clusterConfigurations.keySet()) {
            Properties properties = clusterConfigurations.get(key);
            ConfigurationState state = new ConfigurationState();
            state.setProperties(properties);
            state.setCluster(true);
            state.setLocal(false);
            configurations.put(key, state);
        }

        // retrieve local configurations
        for (Configuration configuration : configurationAdmin.listConfigurations(null)) {
            String key = configuration.getPid();
            if (configurations.containsKey(key)) {
                ConfigurationState state = configurations.get(key);
                state.setLocal(true);
            } else {
                ConfigurationState state = new ConfigurationState();
                state.setLocal(true);
                state.setCluster(false);
                ConfigurationSupport support = new ConfigurationSupport();
                state.setProperties(support.dictionaryToProperties(configuration.getProperties()));
                configurations.put(key, state);
            }
        }

        return configurations;
    }

    class ConfigurationState {

        private Properties properties;
        private boolean cluster;
        private boolean local;

        public Properties getProperties() {
            return properties;
        }

        public void setProperties(Properties properties) {
            this.properties = properties;
        }

        public boolean isCluster() {
            return cluster;
        }

        public void setCluster(boolean cluster) {
            this.cluster = cluster;
        }

        public boolean isLocal() {
            return local;
        }

        public void setLocal(boolean local) {
            this.local = local;
        }
    }

}
