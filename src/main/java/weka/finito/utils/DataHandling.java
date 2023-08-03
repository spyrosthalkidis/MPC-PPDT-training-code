package weka.finito.utils;

import org.jetbrains.annotations.NotNull;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DataHandling {

    // Kick off Data Handling by reading the data set and splitting it.
    // Question: Are we assuming all Data Providers know all classes??? Perhaps we can assume yes for now...
    public static Instances @NotNull [] createPartitions(String fullDataSetPath, int noParties)
            throws Exception {
        Instances data = read_data(fullDataSetPath);

        //data.deleteWithMissingClass();
        int numAttributes, noElementsForPartition, noElementsForLastPartition;

        numAttributes = data.numAttributes();
        noElementsForPartition = numAttributes / noParties;
        noElementsForLastPartition = numAttributes;

        //System.out.println("First attribute:"+data.attribute(0).name());
        // TODO: Set as a parameter probably?
        System.out.println("Last attribute:" + data.attribute(numAttributes-1).name());
        Remove removeFilter = new Remove();
        Instances [] newData = new Instances[noParties];
        int [] indices = new int[numAttributes];
        if (noElementsForPartition == 0){
            System.out.println("--");
            for (int j = 0; j < numAttributes; j++){
                indices[j] = j;
                System.out.print(indices[j] + " ");
            }
            System.out.println();
            removeFilter.setAttributeIndicesArray(indices);
            removeFilter.setInvertSelection(true);
            removeFilter.setInputFormat(data);
            newData[0] = Filter.useFilter(data, removeFilter);
        }
        else {

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
                removeFilter.setAttributeIndicesArray(indices);
                removeFilter.setInvertSelection(true);
                removeFilter.setInputFormat(data);
                newData[i] = Filter.useFilter(data, removeFilter);
            }
        }
        return newData;
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
