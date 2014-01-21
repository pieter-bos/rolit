package rolit.model.networking.server;

import rolit.model.networking.client.ClientProtocol;
import rolit.model.networking.client.CreateGamePacket;
import rolit.model.networking.client.JoinGamePacket;
import rolit.model.networking.client.StartGamePacket;
import rolit.model.networking.common.Command;
import rolit.model.networking.common.Packet;
import rolit.model.networking.common.ProtocolException;
import rolit.model.networking.client.ChallengePacket;
import rolit.model.networking.client.ChallengeResponsePacket;
import rolit.model.networking.client.HandshakePacket;
import rolit.model.networking.client.MovePacket;
import rolit.util.Strings;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Server server;
    private final Socket client;
    private BufferedReader input;
    private PrintStream output;
    private final Thread thread;

    private ClientHandlerState state;

    private int clientSupports;
    private String clientName;

    public ClientHandler(Server server, Socket client) throws IOException {
        this.server = server;
        this.client = client;
        thread = new Thread(this);
        state = new InitialClientHandlerState(this);
    }

    public void start() {
        thread.start();
    }

    private void handlePacket(Packet packet) throws ProtocolException {
        if(packet instanceof ChallengePacket) {
            state = state.challenge((ChallengePacket) packet);
        } else if(packet instanceof ChallengeResponsePacket) {
            state = state.challengeResponse((ChallengeResponsePacket) packet);
        } else if(packet instanceof CreateGamePacket) {
            state = state.createGame((CreateGamePacket) packet);
        } else if(packet instanceof HandshakePacket) {
            state = state.handshake((HandshakePacket) packet);
        } else if(packet instanceof HighscorePacket) {
            highscore((HighscorePacket) packet);
        } else if(packet instanceof JoinGamePacket) {
            state = state.joinGame((JoinGamePacket) packet);
        } else if(packet instanceof MessagePacket) {
            message((MessagePacket) packet);
        } else if(packet instanceof MovePacket) {
            state = state.move((MovePacket) packet);
        } else if(packet instanceof StartGamePacket) {
            state = state.startGame((StartGamePacket) packet);
        } else {
            throw new ProtocolException("Client caused the server to be in an impossible condition", ServerProtocol.ERROR_GENERIC);
        }
    }

    private void message(MessagePacket packet) {

    }

    private void highscore(HighscorePacket packet) {

    }

    @Override
    public void run() {
        try {
            try {
                this.input = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
                this.output = new PrintStream(client.getOutputStream(), true, "UTF-8");

                while(true) {
                    handlePacket(Packet.readClientPacketFrom(input));
                }
            } catch (ProtocolException e) {
                server.fireClientError("ProtocolException: " + e.getMessage());
                new ErrorPacket(e.getCode()).writeTo(output);
                client.close();
            }
        } catch (IOException e) {
            server.fireClientError("IOException: " + e.getMessage());
        }
    }

    public int getClientSupports() {
        return clientSupports;
    }

    public void setClientSupports(int clientSupports) {
        this.clientSupports = clientSupports;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}
