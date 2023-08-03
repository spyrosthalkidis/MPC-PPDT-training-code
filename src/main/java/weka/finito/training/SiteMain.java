package weka.finito.training;

import weka.attributeSelection.GainRatioAttributeEval;
import weka.classifiers.trees.j48.ClassifierTree;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.finito.utils.DataHandling;

import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.Stack;

// This will work with DataProviders to collect the needed data on how to make PPDT
// Will help Data Providers create level-sites
public class SiteMain implements Runnable {

    // Main Objective
    // 1- get info from all providers?
    // 2- Somehow tell data providers what level-site(s) they are creating?

    // ppdt could be full PPDT?
    ClassifierTree ppdt;

    Object m_transactionVector;
    int noPartitions = 7;
    String[] hosts;
    int[] ports;
    int noParties=7;
    Instances m_insts[];
    Stack m_requirementStack;

    public SiteMain(String[] hosts, int[] ports, int index, String fullDatasetPath) throws Exception{
        this.hosts = hosts;
        this.ports = ports;
        m_insts= DataHandling.createPartitions(fullDatasetPath, noParties);
        m_transactionVector = Array.newInstance(boolean.class, m_insts[index].numInstances());
    }

    @Override
    public void run() {
        hosts = new String[noPartitions];
        ports = new int[noPartitions];
        int initialPort = 9000;
        for (int i = 0; i < hosts.length; i++) {
            hosts[i] = "127.0.0.1";
            ports[i] = initialPort + i;
        }

        Socket[] sockets = new Socket[hosts.length];
        try {
            for (int i = 0; i < hosts.length; i++) {
                sockets[i] = new Socket(hosts[i], ports[i]);
                PrintWriter pr = new PrintWriter(sockets[i].getOutputStream());
                pr.flush();
            }
        } catch (java.io.IOException e) {
            System.out.println("IO Error or bad URL");
        }
    }

    public double attribMaxInfoGainRatio(Instances[] data) throws Exception{
        Attribute m_classifyingAttribute;
        double[] gainRatios;
        gainRatios=new double[data.length];
        gainRatios=evaluateGainRatio(data);
        int maxIndex = Utils.maxIndex(gainRatios);
        m_classifyingAttribute = data[maxIndex].attribute(maxIndex);
        return gainRatios[maxIndex];
    }
    double[] evaluateGainRatio(Instances[] data) throws Exception{
        double[] gainRatios = new double[data.length];
        for (int i=0; i<data.length; i++){
            GainRatioAttributeEval gainRatioAttributeEval=new GainRatioAttributeEval();
            gainRatios[i] = gainRatioAttributeEval.evaluateAttribute(i);
        }
        return gainRatios;
    }
}