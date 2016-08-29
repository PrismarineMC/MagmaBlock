package org.itxtech.synapseapi;

import cn.nukkit.Nukkit;
import cn.nukkit.Server;
import cn.nukkit.network.SourceInterface;
import cn.nukkit.network.RakNetInterface;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.utils.Logger;
import com.google.gson.Gson;
import org.itxtech.synapseapi.event.player.SynapsePlayerCreationEvent;
import org.itxtech.synapseapi.network.SynLibInterface;
import org.itxtech.synapseapi.network.SynapseInterface;
import org.itxtech.synapseapi.network.protocol.spp.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Created by boybook on 16/6/24.
 */
public class SynapseAPI {

    private static SynapseAPI instance;

    public static SynapseAPI getInstance() {
        return instance;
    }

    private String serverIp;
    private int port;
    private boolean isMainServer;
    private String password;
    private SynapseInterface synapseInterface;
    private boolean verified = false;
    private long lastUpdate;
    private long lastRecvInfo;
    private Map<UUID, SynapsePlayer> players = new HashMap<>();
    private SynLibInterface synLibInterface;
    private Map<String, Map<String, String>> clientData;
    private String serverDescription;
    private long connectionTime = Long.MAX_VALUE;

    public static boolean enable = true;

    private Server server;

    public Logger getLogger(){
        return this.getServer().getLogger();
    }

    public Server getServer(){
        return server;
    }

    public boolean isEnabled(){
        return enable;
    }

    public void setEnabled(boolean bool){
        enable = bool;
    }

    public SynapseAPI(Server server) {
        instance = this;
        this.server = server;
        enable = (boolean) server.getAdvancedProperty("synapse.enable", true);
        if (!enable) {
            return;
        }
        this.serverIp = (String) server.getAdvancedProperty("synapse.server-ip", "127.0.0.1");
        this.port = (int) server.getAdvancedProperty("synapse.server-port", 10305);
        this.isMainServer = (boolean) server.getAdvancedProperty("synapse.is-main-server", true);
        this.password = (String) server.getAdvancedProperty("synapse.password", "0123456789abcdef");
        if (this.password.length() != 16) {
            this.getLogger().warning("You must use a 16 bit length key!");
            this.getLogger().warning("The Synapse will not be enabled!");
            enable = false;
            return;
        }
        this.serverDescription = (String) server.getAdvancedProperty("synapse.description", "A Synapse client");
        for(SourceInterface interfaz : this.getServer().getNetwork().getInterfaces()){
            if(interfaz instanceof RakNetInterface){
                boolean disableRak = (boolean) server.getAdvancedProperty("synapse.disable-rak", false);
                if(disableRak){
                    interfaz.shutdown();
                    this.getLogger().notice("RakNet has been disabled by synapse.disable-rak option.");
                    break;
                }
            }
        }
        this.synapseInterface = new SynapseInterface(this, this.serverIp, this.port);
        this.synLibInterface = new SynLibInterface(this.synapseInterface);
        this.lastUpdate = System.currentTimeMillis();
        this.lastRecvInfo = System.currentTimeMillis();
        try{
            this.connect();
        } catch(Exception e){
            this.connect(); //Ya, this works!
        }
    }

    public Map<String, Map<String, String>> getClientData() {
        return clientData;
    }
    
    public String getClientHashByDescription(String des){
        for(Map.Entry<String, Map<String, String>> client : clientData.entrySet()){
            if(client.getValue().get("description").equals(des)){
                return client.getKey();
            }
        }
        return null;
    }

    public SynapseInterface getSynapseInterface() {
        return synapseInterface;
    }

    public void shutdown(){
        if(this.verified){
            DisconnectPacket pk = new DisconnectPacket();
            pk.type = DisconnectPacket.TYPE_GENERIC;
            pk.message = "Server closed";
            this.sendDataPacket(pk);
            this.getLogger().debug("Synapse client has disconnected from Synapse server");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                //ignore
            }
        }
        if (this.synapseInterface != null) this.synapseInterface.shutdown();
    }

    public String getServerDescription() {
        return serverDescription;
    }

    public void setServerDescription(String serverDescription) {
        this.serverDescription = serverDescription;
    }

    public void sendDataPacket(SynapseDataPacket pk){
        this.synapseInterface.putPacket(pk);
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public void setMainServer(boolean mainServer) {
        isMainServer = mainServer;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getServerIp() {
        return serverIp;
    }

    public int getPort() {
        return port;
    }

    public void broadcastPacket(SynapsePlayer[] players, DataPacket packet){
        this.broadcastPacket(players, packet, false);
    }

    public void broadcastPacket(SynapsePlayer[] players, DataPacket packet, boolean direct){
        packet.encode();
        BroadcastPacket broadcastPacket = new BroadcastPacket();
        broadcastPacket.direct = direct;
        broadcastPacket.payload = packet.getBuffer();
        broadcastPacket.entries = new ArrayList<>();
        for (SynapsePlayer player : players){
            broadcastPacket.entries.add(player.getUniqueId());
        }
        this.sendDataPacket(broadcastPacket);
    }

    public boolean isMainServer() {
        return isMainServer;
    }

    public String getHash() {
        return this.serverIp + ":" + this.port;
    }

    public void connect(){
        this.getLogger().debug("Connecting " + this.getHash());
        this.verified = false;
        ConnectPacket pk = new ConnectPacket();
        pk.password = this.password;
        pk.isMainServer = this.isMainServer();
        pk.description = this.serverDescription;
        pk.maxPlayers = this.getServer().getMaxPlayers();
        pk.protocol = SynapseInfo.CURRENT_PROTOCOL;
        this.sendDataPacket(pk);
        Thread ticker = new Thread(new Ticker());
        ticker.setName("SynapseAPI Ticker");
        ticker.start();
        this.connectionTime = System.currentTimeMillis();
    }

    public class Ticker implements Runnable {
        public void run() {
            long startTime = System.currentTimeMillis();
            while (isEnabled()) {
                try {
                    tick();
                } catch (Exception e) {
                    getLogger().alert("Catch the exception in Synapse ticking: " + e.getMessage());
                    getServer().getLogger().logException(e);
                }

                long duration = System.currentTimeMillis() - startTime;
                if (duration < 10) {
                    try{
                        Thread.sleep(10 - duration);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
                startTime = System.currentTimeMillis();
            }
        }
    }

    public void tick(){
        this.synapseInterface.process();
        long time = System.currentTimeMillis();
        if((time - this.lastUpdate) >= 1000){//Heartbeat!
            this.lastUpdate = time;
            HeartbeatPacket pk = new HeartbeatPacket();
            pk.tps = this.getServer().getTicksPerSecondAverage();
            pk.load = this.getServer().getTickUsageAverage();
            pk.upTime = (System.currentTimeMillis() - Nukkit.START_TIME) / 1000;
            this.sendDataPacket(pk);
        }

        time = System.currentTimeMillis();
        if(((time - this.lastUpdate) >= 15000) && this.synapseInterface.isConnected()){  //15 seconds timeout
            this.synapseInterface.reconnect();
        }
        if(time - this.connectionTime >= 10000 && !this.verified){
            this.synapseInterface.reconnect();
        }
    }

    public DataPacket getPacket(byte[] buffer){
        byte pid = buffer[0];
        byte start = 1;
        if(pid == (byte) 0xfe){
            pid = buffer[1];
            start++;
        }
        DataPacket data = this.getServer().getNetwork().getPacket(pid);
        if(data == null){
            return null;
        }
        data.setBuffer(buffer, start);
        return data;
    }
    
    public void removePlayer(SynapsePlayer player){
        UUID uuid = player.getUniqueId();
        if(this.players.containsKey(uuid)){
            this.players.remove(uuid);
        }
    }

    public void removePlayer(UUID uuid){
        if(this.players.containsKey(uuid)){
            this.players.remove(uuid);
        }
    }
    
    public void handleDataPacket(DataPacket pk){
        //this.getLogger().debug("Received packet " + pk.pid() + " from " + this.serverIp + ":" + this.port);
        switch(pk.pid()){
            case SynapseInfo.DISCONNECT_PACKET:
                DisconnectPacket disconnectPacket = (DisconnectPacket) pk;
                this.verified = false;
                switch(disconnectPacket.type){
                    case DisconnectPacket.TYPE_GENERIC:
                        this.getLogger().notice("Synapse Client has disconnected due to " + disconnectPacket.message);
                        this.synapseInterface.reconnect();
                        break;
                    case DisconnectPacket.TYPE_WRONG_PROTOCOL:
                        this.getLogger().error(disconnectPacket.message);
                        break;
                }
                break;
            case SynapseInfo.INFORMATION_PACKET:
                InformationPacket informationPacket = (InformationPacket)pk;
                switch(informationPacket.type){
                    case InformationPacket.TYPE_LOGIN:
                        if (informationPacket.message.equals(InformationPacket.INFO_LOGIN_SUCCESS)){
                            this.getLogger().info("Login success to " + this.serverIp + ":" + this.port);
                            this.verified = true;
                        } else if(informationPacket.message.equals(InformationPacket.INFO_LOGIN_FAILED)){
                        this.getLogger().info("Login failed to " + this.serverIp + ":" + this.port);
                    }
                    break;
                    case InformationPacket.TYPE_CLIENT_DATA:
                        Map<String, Map<String, Map<String, String>>> data = new HashMap<String, Map<String, Map<String, String>>>();
                        data = new Gson().fromJson(informationPacket.message, data.getClass());
                        this.clientData = data.get("clientList");
                        this.lastRecvInfo = System.currentTimeMillis();
                        //this.getLogger().debug("Received ClientData from " + this.serverIp + ":" + this.port);
                        break;
                }
                break;
            case SynapseInfo.PLAYER_LOGIN_PACKET:
                PlayerLoginPacket playerLoginPacket = (PlayerLoginPacket)pk;
                SynapsePlayerCreationEvent ev = new SynapsePlayerCreationEvent(this.synLibInterface, SynapsePlayer.class, SynapsePlayer.class, new Random().nextLong(), playerLoginPacket.address, playerLoginPacket.port);
                this.getServer().getPluginManager().callEvent(ev);
                Class<? extends SynapsePlayer> clazz = ev.getPlayerClass();
                try {
                    Constructor constructor = clazz.getConstructor(SourceInterface.class, Long.class, String.class, int.class);
                    SynapsePlayer player = (SynapsePlayer) constructor.newInstance(this.synLibInterface, ev.getClientId(), ev.getAddress(), ev.getPort());
                    player.isSynapseLogin = true;
                    player.setUniqueId(playerLoginPacket.uuid);
                    this.players.put(playerLoginPacket.uuid, player);
                    this.getServer().addPlayer(playerLoginPacket.uuid.toString(), player);
                    player.handleLoginPacket(playerLoginPacket);
                } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    Server.getInstance().getLogger().logException(e);
                }
                break;
            case SynapseInfo.REDIRECT_PACKET:
                RedirectPacket redirectPacket = (RedirectPacket)pk;
                UUID uuid = redirectPacket.uuid;
                if(this.players.containsKey(uuid)){
                    pk = this.getPacket(redirectPacket.mcpeBuffer);
                    if(pk != null) {
                        pk.decode();
                        this.players.get(uuid).handleDataPacket(pk);
                    }
                }
                break;
            case SynapseInfo.PLAYER_LOGOUT_PACKET:
                PlayerLogoutPacket playerLogoutPacket = (PlayerLogoutPacket) pk;
                UUID uuid1;
                if(this.players.containsKey(uuid1 = playerLogoutPacket.uuid)){
                    this.players.get(uuid1).close("", playerLogoutPacket.reason, false);
                    this.removePlayer(uuid1);
                }
                break;
        }
    }
}
