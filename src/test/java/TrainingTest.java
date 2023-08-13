import org.junit.Before;
import org.junit.Test;
import weka.core.Instances;
import weka.finito.training.DataProvider;
import weka.finito.training.SiteMain;
import weka.finito.utils.DataHandling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class TrainingTest {
    private int [] level_site_ports;
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
        String [] level_site_ports_string = config.getProperty("level-site-ports").split(",");
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

        level_site_ports = new int[levels];
        for (int i = 0; i < levels; i++) {
            String port_string = level_site_ports_string[i].replaceAll("[^0-9]", "");
            level_site_ports[i] = Integer.parseInt(port_string);
        }
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
                    // Make sure to only test with hypothyroid?
                    if (data_set.startsWith("hypothyroid")) {
                        train(full_data_set_path);
                    }
                }
            }
        }
    }

    // Run it for one time
    public void train(String data_set) throws Exception {
        // Read the dataset
        int num_data_providers = levels;

        Instances [] vertical_data = DataHandling.createPartitions(data_set, num_data_providers);
        DataProvider [] providers = new DataProvider[num_data_providers];
        Thread server_thread;
        Thread [] data_provider_thread = new Thread[num_data_providers];

        // Initialize the Data Providers
        // Create all data providers with the split data initialized
        for (int i = 0; i < num_data_providers; i++) {
            // Have a thread start all the providers, they will be waiting on accept() from SiteMain
            providers[i] = new DataProvider(vertical_data[i], level_site_ports[i]);
            data_provider_thread[i] = new Thread(providers[i]);
            data_provider_thread[i].start();
        }

        // Initialize the server-site who will communicate with data providers
        // Create a new SiteMain that will collect attributes and
        // with Data Provider set what level-site data should be
        SiteMain server = new SiteMain(level_site_ips, level_site_ports);
        server_thread = new Thread(server);
        server_thread.start();

        // Close everything
        server_thread.join();

        for (int i = 0; i < num_data_providers; i++) {
            data_provider_thread[i].join();
        }
    }
}
