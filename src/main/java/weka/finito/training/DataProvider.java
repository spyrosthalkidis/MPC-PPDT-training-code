package weka.finito.training;

import weka.core.Instances;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

// This class is a Data Provider.
// They can own multiple levels, as the data shouldn't leave their site from my understanding
public class DataProvider implements Runnable {

    int DataProviderIndex=0;
    ObjectInputStream fromSiteMain;
    ObjectOutputStream toSiteMain;
    Instances[] localInstances;
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
            Socket client_socket=new Socket("127.0.0.1", server_port);
            toSiteMain = new ObjectOutputStream(client_socket.getOutputStream());
            fromSiteMain = new ObjectInputStream(client_socket.getInputStream());
            Object x=fromSiteMain.readObject();
            if (x instanceof Instances[]){
                localInstances=(Instances[])x;
            }
        } catch (IOException ioe){
            ioe.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
