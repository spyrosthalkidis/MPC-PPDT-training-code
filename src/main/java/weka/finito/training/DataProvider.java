package weka.finito.training;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

// This class is a Data Provider.
// They can own multiple levels, as the data shouldn't leave their site from my understanding
public class DataProvider implements Runnable {

    int DataProviderIndex=0;

    int getDataProviderIndex(){
        return this.DataProviderIndex;
    }

    void setDataProviderIndex(int DPI){
        this.DataProviderIndex=DPI;
    }

    public DataProvider(int DPI) {
        this.DataProviderIndex=DPI;
    }

    @Override
    public void run() {
        // Within this run, you should complete the training
        int server_port=9000+getDataProviderIndex();
        try {
            ServerSocket serverSocket = new ServerSocket(server_port);
            Socket ss= serverSocket.accept();
            InputStreamReader in = new InputStreamReader(ss.getInputStream());
            BufferedReader bf = new BufferedReader(in);
        } catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
}
