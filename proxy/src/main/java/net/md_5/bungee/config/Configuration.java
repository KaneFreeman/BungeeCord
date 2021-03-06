package net.md_5.bungee.config;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ConfigurationAdapter;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.tablist.GlobalPing;
import net.md_5.bungee.tablist.Global;
import net.md_5.bungee.tablist.ServerUnique;

/**
 * Core configuration for the proxy.
 */
@Getter
public class Configuration
{

    /**
     * The default tab list options available for picking.
     */
    private enum DefaultTabList
    {

        GLOBAL, GLOBAL_PING, SERVER;
    }
    /**
     * Time before users are disconnected due to no network activity.
     */
    private int timeout = 30000;
    /**
     * UUID used for metrics.
     */
    private String uuid = UUID.randomUUID().toString();
    /**
     * Set of all listeners.
     */
    private Collection<ListenerInfo> listeners;
    /**
     * Set of all servers.
     */
    private Map<String, ServerInfo> servers;
    /**
     * Should we check minecraft.net auth.
     */
    private boolean onlineMode = true;
    private int playerLimit = -1;

    public void load()
    {
        ConfigurationAdapter adapter = ProxyServer.getInstance().getConfigurationAdapter();
        adapter.load();

        timeout = adapter.getInt( "timeout", timeout );
        uuid = adapter.getString( "stats", uuid );
        onlineMode = adapter.getBoolean( "online_mode", onlineMode );
        playerLimit = adapter.getInt( "player_limit", playerLimit );

        DefaultTabList tab = DefaultTabList.valueOf( adapter.getString( "tab_list", "GLOBAL_PING" ) );
        if ( tab == null )
        {
            tab = DefaultTabList.GLOBAL_PING;
        }
        switch ( tab )
        {
            case GLOBAL:
                ProxyServer.getInstance().setTabListHandler( new Global() );
                break;
            case GLOBAL_PING:
                ProxyServer.getInstance().setTabListHandler( new GlobalPing() );
                break;
            case SERVER:
                ProxyServer.getInstance().setTabListHandler( new ServerUnique() );
                break;
        }

        listeners = adapter.getListeners();
        Preconditions.checkArgument( listeners != null && !listeners.isEmpty(), "No listeners defined." );

        Map<String, ServerInfo> newServers = adapter.getServers();
        Preconditions.checkArgument( newServers != null && !newServers.isEmpty(), "No servers defined" );

        if ( servers == null )
        {
            servers = newServers;
        } else
        {
            for ( ServerInfo oldServer : servers.values() )
            {
                // Don't allow servers to be removed
                Preconditions.checkArgument( newServers.containsValue( oldServer ), "Server %s removed on reload!", oldServer.getName() );
            }

            // Add new servers
            for ( Map.Entry<String, ServerInfo> newServer : newServers.entrySet() )
            {
                if ( !servers.containsValue( newServer.getValue() ) )
                {
                    servers.put( newServer.getKey(), newServer.getValue() );
                }
            }
        }

        for ( ListenerInfo listener : listeners )
        {
            Preconditions.checkArgument( servers.containsKey( listener.getDefaultServer() ), "Default server %s is not defined", listener.getDefaultServer() );
        }
    }
}
