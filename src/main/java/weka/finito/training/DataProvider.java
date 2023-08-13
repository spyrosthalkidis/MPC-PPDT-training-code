package weka.finito.training;

import weka.core.Instances;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

// This class is a Data Provider.
// They can own multiple levels, as the data shouldn't leave their site from my understanding
public class DataProvider implements Runnable {

    private int port;
    Instances local_instances;

    public DataProvider(Instances local_instances, int port) {
        this.port = port;
        this.local_instances = local_instances;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            try(Socket client_site = serverSocket.accept()) {
                ObjectOutputStream to_site_master = new ObjectOutputStream(client_site.getOutputStream());
                ObjectInputStream from_site_master = new ObjectInputStream(client_site.getInputStream());

                // Connection made...
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}