package weka.finito.utils;

import org.jetbrains.annotations.NotNull;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class DataHandling {

    public static Instances create_Partitions(String fullDataSetPath, int noParties) throws Exception {
        Instances data = read_data(fullDataSetPath);
        data.setClassIndex(data.numAttributes()-1);
        int classIndex = data.classIndex();
        int instanceIndex = data.numAttributes()-1; // Index of the instance you want to get the class value for
        Instance instance = data.get(instanceIndex);
        int noLines, numAttributes, noElementsForPartition, noElementsForLastPartition;

        noLines = data.numInstances();
        numAttributes = data.numAttributes();
        noElementsForPartition = numAttributes / noParties;
        noElementsForLastPartition = numAttributes;
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        for (int i=0; i<numAttributes; i++){
            attributes.add(data.attribute(i));
        }
        int [] indices = new int[numAttributes];
        //data = new Instances("partitionedInstances", attributes, 0);
        double newData[][]=new double[numAttributes][noLines];
        double newInst[]=new double[2];
        System.out.println("Last attribute:" + data.attribute(numAttributes-1).name());
        if (noElementsForPartition == 0) {
            System.out.println("--");

            for (int i = 0; i < numAttributes; i++)
            {
                newInst[0] = (double) data.attribute(0).addStringValue(data.attribute(i).name());
                // Print the current attribute.
                System.out.print(data.attribute(i).name() + ": ");
                newData[i]=data.get(i).toDoubleArray();
                newInst[1] = (double) data.attribute(1).addStringValue(newData[i].toString());
                data.add(new DenseInstance(1.0, newInst));
            }
        }else {

            for (int i=0; i<noParties; i++) {
                System.out.println("--");

                //indices[0]=0;
                indices[numAttributes - 1] = numAttributes - 1;
                int noElements;
                if (i != noParties - 1) {
                    noElements = noElementsForPartition;
                    noElementsForLastPartition -= noElements;
                }
                else {
                    noElements = noElementsForLastPartition - 2;
                }
                if (noElements != 0) {
                    //System.out.print(indices[0] + " ");
                    for (int j = 1; j < noElements + 1; j++) {
                        indices[j] = i * noElementsForPartition + j;
                        System.out.print(indices[j] + " ");
                    }
                    System.out.println(indices[numAttributes - 1] + " ");
                }
                if (i!=noParties-1) {
                    for (int j = i*noElementsForPartition; j < (i+1)*noElementsForPartition; j++) {
                        int index = indices[j];
                        newInst[0] = (double) data.attribute(0).addStringValue(data.attribute(i).name());
                        // Print the current attribute.
                        System.out.print(data.attribute(j).name() + ": ");
                        newData[j] = data.get(index).toDoubleArray();
                        newInst[1] = (double) data.attribute(1).addStringValue(newData[j].toString());
                        data.add(new DenseInstance(1.0, newInst));

                    }
                    classIndex = data.classIndex();
                    String classValue = instance.stringValue(classIndex);
                    System.out.println(classValue);
                    //Attribute classAttribute = data.attribute(classIndex);

                    //System.out.println(classAttribute.toString());
                } else if (i==noParties-1){
                    for (int j=i*noElementsForPartition; j<i*noElementsForPartition+noElementsForLastPartition; j++){
                        int index = indices[j];
                        newInst[0] = (double) data.attribute(0).addStringValue(data.attribute(i).name());
                        // Print the current attribute.
                        System.out.print(data.attribute(j).name() + ": ");
                        newData[j] = data.get(index).toDoubleArray();
                        newInst[1] = (double) data.attribute(1).addStringValue(newData[j].toString());
                        data.add(new DenseInstance(1.0, newInst));
                    }
                    classIndex = data.classIndex();
                    String classValue = instance.stringValue(classIndex);
                    System.out.println(classValue);
                    //Attribute classAttribute = data.attribute(classIndex);

                    //System.out.println(classAttribute.toString());
                }
                System.out.println();
            }
        }

        return data;
    }
    // Read either ARFF or CSV file
    public static Instances read_data(@NotNull String file) throws IOException {
        Instances data = null;
        if (file.endsWith(".csv")) {
            // If this is a .csv file
            CSVLoader loader = new CSVLoader();
            loader.setSource(new File(file));
            data = loader.getDataSet();
        }
        else if (file.endsWith(".arff")) {
            // If this is a .arff file
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                data = new Instances(reader);
            }
        }
        return data;
    }
}
