package players;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;

import core.actions.Action;
import core.game.GameState;
import org.json.JSONObject;
import utils.ElapsedCpuTimer;

public class NetworkAgent extends Agent {

    private String serverAddress;
    private int serverPort;

    public NetworkAgent(String serverAddress, int serverPort) {
        super(0);
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    @Override
    public Action act(GameState gs, ElapsedCpuTimer ect) {
        return null;
    }

    public void requestAction() {
        try (Socket socket = new Socket(serverAddress, serverPort)) {
            socket.setSoTimeout(1000);
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Create some test JSON data
                JSONObject jsonData = new JSONObject();
                jsonData.put("action", "move");
                jsonData.put("direction", "north");
                jsonData.put("steps", new Random().nextInt(10) + 1);

                // Send JSON data to the Python server
                out.println(jsonData);

                // Receive response from Python server
                String response = in.readLine();
                System.out.println("Response from Python server: " + response);

            } catch (SocketTimeoutException e) {
                System.err.println("Socket timed out waiting for a response.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Agent copy() { return null; }

    public static void main(String[] args) {
        NetworkAgent agent = new NetworkAgent("localhost", 9999); // Assuming the Python server is running locally on port 9999
        agent.requestAction();
    }
}
