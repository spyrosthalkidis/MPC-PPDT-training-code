package weka.finito.training;

import weka.attributeSelection.GainRatioAttributeEval;
import weka.classifiers.trees.j48.*;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.finito.utils.DataHandling;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    static String fullDatasetPath;
    protected ModelSelection m_toSelectModel;
    protected boolean m_isLeaf=false;
    protected boolean m_isEmpty=false;
    protected ClassifierTree[] m_sons;
    protected ClassifierSplitModel m_localModel;
    Object m_transactionVector;
    static int noPartitions = 7;
    String[] hosts;
    int[] ports;
    static int noParties=7;
    Instances m_insts[];
    Stack m_requirementStack;

    public boolean allAttributesInSameClass(Instances newData[]){
        boolean aAISC=true;
        int numAttributes = newData[0].numAttributes();
        String finalClass = newData[0].attribute(numAttributes-1).name();
        for (int i=1; i<newData.length; i++){
            numAttributes=newData[i].numAttributes();
            String currentClass=newData[i].attribute(numAttributes-1).name();
            if (!currentClass.equals(finalClass)){
                aAISC=false;
                break;
            }
        }

        return aAISC;
    }

    public boolean noneOfTheFeaturesProvideInfoGain(Instances newData[]) throws Exception{
        boolean nOTFPIG=true;
        double gainRatios[]=evaluateGainRatio(newData);
        for (int i=0; i< newData.length; i++){
            if (gainRatios[i]!=0){
                nOTFPIG=false;
                break;
            }
        }
        return nOTFPIG;
    }


    public boolean instanceOfPreviouslyUnseenClassEncountered(Instances[] m_insts, int i){
        boolean iOPUCE=true;
        String currentClass=m_insts[i].attribute(m_insts[i].size()-1).name();
        for (int j=0; j<i; j++){
            String myClass=m_insts[j].attribute(m_insts[j].size()-1).name();
            for (int k=0; k<j; k++) {
                String lastClass=m_insts[k].attribute(m_insts[k].size()-1).name();
                if (myClass.equals(lastClass)||myClass.equals(currentClass)) {
                    iOPUCE = false;
                    break;
                }
            }
        }
        return iOPUCE;
    }
    public void SiteMain(String[] hosts, int[] ports, int index, String fullDatasetPath, int noParties, ClassifierTree j48) throws Exception{
        double maxInfoGainRatio=0;
        this.hosts = hosts;
        this.ports = ports;


        Instances[] mydata=new Instances[1];
        m_insts= DataHandling.createPartitions(fullDatasetPath, noParties, mydata);
        for (int i=0; i<noParties; i++){
            Socket mysocket = new Socket(hosts[i], ports[i]);


            ObjectOutputStream to_server = new ObjectOutputStream(mysocket.getOutputStream());
            ObjectInputStream from_server = new ObjectInputStream(mysocket.getInputStream());


            // Send the partitioned data to the corresponding DataProvider
            to_server.writeObject(m_insts[i]);
            to_server.flush();

        }
        Instances[] m_insts_union;

        m_insts_union=mydata;
        m_localModel = m_toSelectModel.selectModel(m_insts_union[0]);
        ppdt=new ClassifierTree(m_toSelectModel);
        BinC45ModelSelection j48_model = new BinC45ModelSelection(2, m_insts[0], true, false);
        j48 = new C45PruneableClassifierTree(j48_model, true, (float) 0.25, true, true, true);
        if (allAttributesInSameClass(m_insts)){
            //create a leaf node for the decision tree saying to choose that same class
            String sameClass = m_insts[0].attribute(m_insts[0].size()-1).name();
            m_isLeaf = true;
            if (Utils.eq(m_insts[0].sumOfWeights(), 0)) {
                m_isEmpty = true;
            }
            String result=sameClass;

        } else {
            m_isLeaf=false;
        }
        m_sons = new ClassifierTree[m_localModel.numSubsets()];
        for (int i = 0; i < m_sons.length; i++) {
        if (noneOfTheFeaturesProvideInfoGain(m_insts)||instanceOfPreviouslyUnseenClassEncountered(m_insts, i)){
                ClassifierTree newTree = new ClassifierTree(m_toSelectModel);
                newTree.buildTree(m_insts[i], false);
                ppdt=newTree;
            }
        }

        Attribute[] attribute=new Attribute[1];
        maxInfoGainRatio=attribMaxInfoGainRatio(m_insts, attribute);
        for (int i=0; i<noParties; i++) {
            m_localModel.split(m_insts[i]);
            m_localModel.resetDistribution(m_insts[i]);
        }

        SiteMain(hosts, ports, index, fullDatasetPath, noParties, j48);
        m_sons = new ClassifierTree[m_localModel.numSubsets()];
        for (int i = 0; i < m_sons.length; i++) {
                ClassifierTree newTree = new ClassifierTree(m_toSelectModel);
                newTree.buildTree(m_insts[i], false);
                ppdt=newTree;
        }

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

    public double attribMaxInfoGainRatio(Instances[] data, Attribute m_classifyingAttribute[]) throws Exception{
        m_classifyingAttribute=new Attribute[1];
        int maxIndex;
        double[] gainRatios;
        gainRatios=new double[data.length];
        gainRatios=evaluateGainRatio(data);
        maxIndex= Utils.maxIndex(gainRatios);
        m_classifyingAttribute[0] = data[maxIndex].attribute(maxIndex);
        return gainRatios[maxIndex];
    }
    double[] evaluateGainRatio(Instances[] data) throws Exception{
        double[] gainRatios = new double[data.length];
        for (int i=0; i<data.length; i++){
            GainRatioAttributeEval gainRatioAttributeEval=new GainRatioAttributeEval();
            gainRatioAttributeEval.buildEvaluator(data[i]);
            gainRatios[i] = gainRatioAttributeEval.evaluateAttribute(0);
        }
        return gainRatios;
    }
}
