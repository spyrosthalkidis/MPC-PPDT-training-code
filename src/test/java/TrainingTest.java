import org.junit.Before;
import org.junit.Test;
import weka.core.Instances;
import weka.finito.utils.DataHandling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class TrainingTest {
    private String [] level_site_ports_string;
    private String [] level_site_ips;
    private int levels;
    private int key_size;
    private int precision;
    private String data_directory;
    private int server_port;
    private String server_ip;

    @Before
    public void read_properties() throws IOException {
        // Arguments:
        System.out.println("Running Full Local Test...");
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        Properties config = new Properties();
        try (FileReader in = new FileReader("config.properties")) {
            config.load(in);
        }
        level_site_ports_string = config.getProperty("level-site-ports").split(",");
        levels = level_site_ports_string.length;
        level_site_ips = new String[levels];
        for (int i = 0; i < levels; i++) {
            level_site_ips[i] = "127.0.0.1";
        }
        key_size = Integer.parseInt(config.getProperty("key_size"));
        precision = Integer.parseInt(config.getProperty("precision"));
        data_directory = config.getProperty("data_directory");
        server_ip = config.getProperty("server-ip");
        server_port = Integer.parseInt(config.getProperty("server-port"));
    }

    // Run training for all data-sets in the folder, based on answers.csv
    @Test
    public void train_all() throws Exception {
        String answer_path = new File(data_directory, "answers.csv").toString();
        // Parse CSV file with various tests
        try (BufferedReader br = new BufferedReader(new FileReader(answer_path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String [] values = line.split(",");
                String data_set = values[0];
                String full_data_set_path = new File(data_directory, data_set).toString();

                if (!data_set.endsWith(".model")) {
                    train(full_data_set_path);
                }
            }
        }
    }

    // Run it for one time
    public void train(String data_set) throws Exception {
        // Read the dataset
        int num_data_providers = 7;

        Instances[] mydata=new Instances[1];
        Instances [][] vertical_data = DataHandling.createPartitions(data_set, num_data_providers);

        // Initialize them
        for (int i = 0; i < num_data_providers; i++) {

        }

        // Create all data providers with the split data initialized

        // Create a new SiteMain that will collect attributes and
        // with Data Provider set what level-site data should be


    }
}
