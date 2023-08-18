package weka.finito.training;

import weka.attributeSelection.GainRatioAttributeEval;
import weka.classifiers.trees.j48.*;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.finito.utils.DataHandling;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Stack;

// This will work with DataProviders to collect the needed data on how to make PPDT
// Will help Data Providers create level-sites
public class SiteMain implements Runnable {

    // Main Objective
    // 1- get info from all providers?
    // 2- Somehow tell data providers what level-site(s) they are creating?

    // ppdt could be full PPDT?
    ClassifierTree ppdt;

    protected ModelSelection m_toSelectModel;
    protected boolean m_isLeaf=false;
    protected boolean m_isEmpty=false;
    protected ClassifierTree[] m_sons;
    protected ClassifierSplitModel m_localModel;
    protected double[] m_distribution;
    protected Instances m_trainInstances;

    Instances m_insts;
    Stack m_requirementStack;

    private String [] data_provider_ips;
    private int [] ports;
    private int port = -1;

    public static int noParties=7;
    public Attribute m_classifyingAttribute;

    public SiteMain(String [] data_provider_ips, int [] ports) {
        this.data_provider_ips = data_provider_ips;
        this.ports = ports;
    }

    public boolean allAttributesInSameClass(Instances m_instances) {
        boolean aAISC=true;
        int numAttributes = m_instances.get(0).numAttributes();
        String finalClass = m_instances.get(0).attribute(numAttributes-1).name();
        for (int i=1; i<m_instances.numInstances(); i++) {
            numAttributes=m_instances.get(i).numAttributes();
            String currentClass=m_instances.get(i).attribute(numAttributes-1).name();
            if (!currentClass.equals(finalClass)) {
                aAISC=false;
                break;
            }
        }
        return aAISC;
    }

    public boolean noneOfTheFeaturesProvideInfoGain(Instances newData) throws Exception{
        boolean nOTFPIG=true;
        double[] gainRatios =evaluateGainRatio(newData);
        for (int i=0; i< newData.numInstances(); i++) {
            if (gainRatios[i]!=0) {
                nOTFPIG=false;
                break;
            }
        }
        return nOTFPIG;
    }

    public boolean instanceOfPreviouslyUnseenClassEncountered(Instances m_insts, int i){
        boolean iOPUCE=true;
        String currentClass=m_insts.get(i).attribute(m_insts.get(i).numValues()-1).name();
        for (int j = 0; j < i; j++){
            String myClass = m_insts.get(i).attribute(m_insts.get(i).numValues()-1).name();
            for (int k = 0; k < j; k++) {
                String lastClass = m_insts.get(k).attribute(m_insts.get(i).numValues()-1).name();
                if (myClass.equals(lastClass)||myClass.equals(currentClass)) {
                    iOPUCE = false;
                    break;
                }
            }
        }
        return iOPUCE;
    }
    public static void main(String[] args) {
        String[] hosts;
        int[] ports;

        hosts=new String[noParties];
        ports=new int[noParties];
        for (int i=0; i<noParties; i++){
            hosts[i]="127.0.0.1";
            ports[i]=9000+i;
        }
        Instances data=null;
        String fullDatasetPath="/home/spyros/MPC-PPDT-training/data/D1.arff";
        try {
            data = DataHandling.read_data(fullDatasetPath);
        } catch(IOException ioe){
            ioe.printStackTrace();
        }
        try {
            SiteMain siteMain = new SiteMain(hosts, ports, 0, fullDatasetPath, noParties, data, false);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public SiteMain(String[] hosts, int[] ports, int index, String fullDatasetPath, int noParties,
                         Instances data, boolean isSubtree) throws Exception {
        double maxInfoGainRatio = 0;
        this.data_provider_ips = hosts;
        this.ports = ports;

        Instances mydata;
        double[] classlabels;

        m_insts = DataHandling.create_Partitions(fullDatasetPath, noParties);
        mydata = DataHandling.read_data(fullDatasetPath);


        Instances m_insts_union=null;
        if (!isSubtree) {
            m_insts_union = mydata;
            m_insts_union.setClassIndex(data.numAttributes()-1);
            m_toSelectModel=new BinC45ModelSelection(2, m_insts_union, true, false);
            m_localModel = m_toSelectModel.selectModel(m_insts_union);
        } else {
            m_insts_union=mydata;
        }
        this.m_trainInstances=m_insts_union;
        m_insts_union.setClassIndex(m_insts_union.numAttributes() - 1);
        classlabels=m_insts_union.attributeToDoubleArray(m_insts_union.numAttributes()-1);
        ArrayList<Instance> mInstances = new ArrayList<>();
        for(int i=0; i<=m_insts_union.numInstances()-1; i++){
            mInstances.add(m_insts_union.instance(i));
        }
        ppdt = new ClassifierTree(m_toSelectModel);
        BinC45ModelSelection j48_model = new BinC45ModelSelection(2, m_insts_union, true, false);
        C45PruneableClassifierTree j48 = new C45PruneableClassifierTree(j48_model, true, (float) 0.25, true, true, true);
        if (allAttributesInSameClass(m_insts_union)) {
            //create a leaf node for the decision tree saying to choose that same class
            String sameClass = m_insts_union.attribute(0).name();
            m_isLeaf = true;
            if (Utils.eq(m_insts_union.sumOfWeights(), 0)) {
                int noClasses=m_insts_union.numClasses();
                m_distribution=new double[noClasses];
                m_isEmpty = true;
                return;
            }
            String result = sameClass;

        } else {
            m_isLeaf = false;
        }
        m_sons = new ClassifierTree[m_localModel.numSubsets()];
        for (int i = 0; i < m_sons.length; i++) {
            if (noneOfTheFeaturesProvideInfoGain(m_insts_union)
                    || instanceOfPreviouslyUnseenClassEncountered(m_insts_union, i)) {
                ClassifierTree newTree = new ClassifierTree(m_toSelectModel);
                newTree.buildTree(m_insts_union, false);
                ppdt = newTree;
            }
        }


        maxInfoGainRatio = attribMaxInfoGainRatio(m_insts_union);

        Instances[] splitData = splitData(m_insts_union);
        m_localModel.resetDistribution(m_insts_union);

        SiteMain siteMain=new SiteMain(hosts, ports, index, fullDatasetPath, noParties, splitData[0], true);

        m_sons = new ClassifierTree[m_localModel.numSubsets()];
        for (int i = 0; i < m_sons.length; i++) {
            ClassifierTree newTree = new ClassifierTree(m_toSelectModel);
            newTree.buildTree(m_insts, false);
            ppdt = newTree;
        }
    }

    public Instances[] splitData(Instances data) {

        Instances[] splitData = new Instances[m_classifyingAttribute.numValues()];
        for (int j = 0; j < m_classifyingAttribute.numValues(); j++) {
            splitData[j] = new Instances(data, data.numInstances());
        }
        Enumeration instEnum = data.enumerateInstances();
        while (instEnum.hasMoreElements()) {
            Instance inst = (Instance) instEnum.nextElement();
            splitData[(int) inst.value(m_classifyingAttribute)].add(inst);
        }
        return splitData;
    }
    public double attribMaxInfoGainRatio(Instances data) throws Exception{

        int maxIndex;
        double [] gainRatios;
        gainRatios = evaluateGainRatio(data);
        maxIndex = Utils.maxIndex(gainRatios);
        m_classifyingAttribute = data.get(maxIndex).attribute(maxIndex);
        return gainRatios[maxIndex];
    }
    double[] evaluateGainRatio(Instances data) throws Exception{
        double[] gainRatios = new double[data.numAttributes()];
        for (int i = 0; i < data.numAttributes(); i++) {
            GainRatioAttributeEval gainRatioAttributeEval = new GainRatioAttributeEval();
            gainRatioAttributeEval.buildEvaluator(data);
            gainRatios[i] = gainRatioAttributeEval.evaluateAttribute(i);
        }
        return gainRatios;
    }

    @Override
    public void run() {
        int connection_port;

        try {
            for (int i = 0; i < data_provider_ips.length; i++) {
                if (port == -1) {
                    assert ports != null;
                    connection_port = ports[i];
                } else {
                    connection_port = port;
                }

                try (Socket data_provider_i = new Socket(data_provider_ips[i], connection_port)) {
                    ObjectOutputStream to_site_master = new ObjectOutputStream(data_provider_i.getOutputStream());
                    ObjectInputStream from_site_master = new ObjectInputStream(data_provider_i.getInputStream());
                    System.out.println("SiteMain connected to Provider  " + i);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
