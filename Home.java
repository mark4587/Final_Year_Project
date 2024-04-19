package FYP;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

public class Home extends JFrame 
{
    private CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);
    private JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    private Timer timer;
    private List<String> rssUrls;
    private JTable table;

    public Home() 
    {
        initUI();
        startTimer();
    }

    private void initUI() 
    {
        setTitle("Incident Monitoring Tool");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createNavigationPanel();
        createHomePanel();
        createAboutYouPanel();
        createAboutIMTPanel();

        getContentPane().add(navPanel, BorderLayout.PAGE_START);
        getContentPane().add(cardPanel, BorderLayout.CENTER);
    }

    // Creates the 'header' of buttons 
    private void createNavigationPanel() 
    {
        JButton homeButton = createStyledButton("Home");
        JButton aboutIMTButton = createStyledButton("About IMT");

        homeButton.addActionListener(e -> cardLayout.show(cardPanel, "Home"));
        aboutIMTButton.addActionListener(e -> cardLayout.show(cardPanel, "About IMT"));

        navPanel.add(homeButton);
        navPanel.add(aboutIMTButton);
        navPanel.setBackground(Color.decode("#007bff"));
    }

    
    // Creates the Home Panel
    private void createHomePanel() 
    {
        rssUrls = Arrays.asList(
        		"https://www.kaspersky.com/blog/feed/",
        		"https://www.cshub.com/rss/categories/attacks",
        		"https://threatpost.com/feed/"
        );

        JPanel homePanel = new JPanel(new BorderLayout());
        String[] columnNames = {"Date", "Title", "Description", "Link"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        homePanel.add(scrollPane, BorderLayout.CENTER);

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }}, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fetchAndPopulateRSSFeeds();

        // Add mouse listener to the table
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int row = table.rowAtPoint(evt.getPoint());
                int col = table.columnAtPoint(evt.getPoint());
                if (row >= 0 && col >= 0) {
                    String date = (String) table.getValueAt(row, 0);
                    String title = (String) table.getValueAt(row, 1);
                    String description = (String) table.getValueAt(row, 2);
                    String link = (String) table.getValueAt(row, 3);
                    String openaiResponse = IMT.sendOpenAIRequest("Using the link " + link + ", list the following in a concise RSS feed format: <Kind_of_attack>, <One_Mitigation_Technique>, <MITRE_TTID>, <Severity_Level>");

                    // Format OpenAI response from RSS Feed Style
                    String[] parts = openaiResponse.split(",");
                    String kindOfAttack = parts[0].trim();
                    String mitigationTechnique = parts[1].trim();
                    String mitreTTID = parts[2].trim();
                    String severityLevel = parts[3].trim();

                    String reportInfo = "Date: " + date + "\n"
                            + "Title: " + title + "\n"
                            + "Description: " + description + "\n"
                            + "Link: " + link + "\n"
                            + "Severity: " + severityLevel + "\n"
                            + "Kind of Attack: " + kindOfAttack + "\n"
                            + "Mitigation Technique: " + mitigationTechnique + "\n"
                            + "MITRE TTID: " + mitreTTID ;
                           

                    JOptionPane.showMessageDialog(null, reportInfo, "Report Information", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        cardPanel.add(homePanel, "Home");
    }

    // Buttons
    private JButton createStyledButton(String text) 
    {
        JButton button = new JButton(text);
        button.setBackground(Color.decode("#007bff"));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        return button;
    }

    
    // Using a timer, it resets the table to update it so that it shows the latest reports
    private void startTimer() 
    {
        timer = new Timer(1800000, new ActionListener() 
        {
            public void actionPerformed(ActionEvent e) 
            {
                fetchAndPopulateRSSFeeds();
            }
        });
        timer.start();
    }

    
    private void fetchAndPopulateRSSFeeds() 
    {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        for (String rssUrl : rssUrls) {
            fetchAndPopulateRSSFeed(rssUrl, model);
        }
    }

    // Pulls the Rss Feeds and insert them into their columns based on the 
    private void fetchAndPopulateRSSFeed(String rssUrl, DefaultTableModel model) 
    {
        try 
        {	
            URL url = new URL(rssUrl);
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.76");

            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dBuilder.parse(httpcon.getInputStream());

            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) 
            {
                Node node = items.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) 
                {
                    Element element = (Element) node;

                    String date = getElementTextContent(element, "pubDate");
                    String title = getElementTextContent(element, "title");
                    String description = getElementTextContent(element, "description");
                    String link = getElementTextContent(element, "link");

                    model.addRow(new Object[]{date, title, removeHtmlTags(description), link});
                }
            }
        } 
        
        catch (Exception e) 
        {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error loading RSS Feed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getElementTextContent(Element element, String tagName) 
    {
        NodeList nodes = element.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : "N/A";
    }

    private String removeHtmlTags(String input) 
    {
        return input.replaceAll("<[^>]*>", "");
    }

    private void createAboutYouPanel() 
    {
        JPanel aboutYouPanel = new JPanel();
        cardPanel.add(aboutYouPanel, "About You");
    }

    private void createAboutIMTPanel() 
    {
        JPanel aboutIMTPanel = new JPanel();
        aboutIMTPanel.setLayout(new BorderLayout());
        JLabel aboutIMTLabel = new JLabel("<html><center>Welcome to the Incident Monitoring tool<br><br>Using the following sites as a source of information as RSS Feeds<br>https://www.kaspersky.com/blog/feed/<br>https://www.cshub.com/rss/categories/attacks<br>https://threatpost.com/feed/<br><br>Using OpenAi the tool is able to present accurate information to you about the reports you see in the table, which varies from<br>Severity of the Threat<br>Kind of attack/threat<br>Mitigation Technique<br>MITRE TTID</center></html>", SwingConstants.CENTER);
        aboutIMTLabel.setForeground(Color.BLACK);
        aboutIMTLabel.setFont(new Font("Arial", Font.PLAIN, 14)); // Set the font size to 16
        aboutIMTPanel.add(aboutIMTLabel, BorderLayout.CENTER);
        aboutIMTPanel.setBackground(Color.WHITE);
        cardPanel.add(aboutIMTPanel, "About IMT");
    }


    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> 
        {
            Home frame = new Home();
            frame.setVisible(true);
        });
    }
}
