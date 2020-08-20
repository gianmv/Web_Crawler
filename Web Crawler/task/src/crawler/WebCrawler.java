package crawler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.Queue;


public class WebCrawler extends JFrame implements BFS.Done {
    JTextField urlTextField;
    JToggleButton runButton;
    JTextField depthTextField;
    JCheckBox depthCheckBox;
    JLabel parsedLabel;
    JTextField exportUrlTextField;
    JButton exportButton;
    JTextField workersTextField;
    InputPanel inputPanel;

    Queue<String> queue = new ConcurrentLinkedQueue<>();
    Map<String, String> map = new ConcurrentHashMap<>();
    int numPagesParsed = 0;

    public WebCrawler() {
        super("Simple Window");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        setLocationRelativeTo(null);
        Dimension d = new Dimension(500,250);
        setSize(d);
        setMinimumSize(d);
        setLayout(new BorderLayout());
        this.inputPanel = new InputPanel();
        add(inputPanel, BorderLayout.CENTER);

        this.urlTextField = inputPanel.getUrlTextField();
        this.runButton = inputPanel.getRunButton();
        this.depthCheckBox = inputPanel.getDepthEnable();
        this.depthTextField = inputPanel.getDepthTextField();
        this.parsedLabel = inputPanel.getParsedLabel();
        this.workersTextField = inputPanel.getworkersTextField();

        SavePanel savePanel = new SavePanel();
        this.exportUrlTextField = savePanel.getExportUrlTextField();
        this.exportButton = savePanel.getExportButton();
        add(savePanel,BorderLayout.SOUTH);

        this.runButton.addActionListener(e -> {
            this.inputPanel.startTimer();
            this.runButton.setEnabled(false);
            this.parsedLabel.setText("");
            queue.clear();
            map.clear();
            queue.add(this.urlTextField.getText());

            String sWorkers = this.workersTextField.getText();
            int numWorkers = sWorkers.isEmpty() ? 2 : Integer.parseInt(sWorkers);

            String sDepth = this.depthTextField.getText();
            int numDepth = sDepth.isEmpty() ? 1 : Integer.parseInt(sDepth);
            numDepth = this.depthCheckBox.isSelected() ? numDepth : 1;
            new BFS(this.map, this.queue, numWorkers,numDepth,this).start();

        });

        this.exportButton.addActionListener(e -> {
            saveFile(this.exportUrlTextField.getText(),this.map);
        });

        pack();
    }

    protected void saveFile(String filePath, Map<String, String> map) {
        File file = new File(filePath);
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                writer.write(entry.getKey() + "\n");
                writer.write(entry.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void done() {
        this.inputPanel.stopTimer();
        this.runButton.setEnabled(true);
        this.numPagesParsed = 0;
        saveFile(this.exportUrlTextField.getText(),this.map);
    }

    @Override
    public void updateNumPages() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                parsedLabel.setText("" + map.size());
            }
        });

    }
}

class BFS extends Thread {
    Map<String, String> map;
    Queue<String> queue;
    int numWorkers;
    int depth;
    Done doneAction;

    interface Done{
        public void done();
        public void updateNumPages();
    }

    BFS(Map<String, String> map, Queue<String> queue, int numWorkers, int depth, Done action) {
        this.map = map;
        this.queue = queue;
        this.numWorkers = numWorkers;
        this.doneAction = action;
        this.depth = depth;
    }

    @Override
    public void run() {
        super.run();

        int counterDepth = 0;
        Queue<String> levelQueue = new ConcurrentLinkedQueue<>();
        List<worker> list = new ArrayList<>();
        while (!queue.isEmpty()) {
            ExecutorService taskExecutor = Executors.newFixedThreadPool(this.numWorkers);
            for (int i = 0; i < this.numWorkers;i++) {
                String url = queue.poll();
                if (url != null) {
                    levelQueue.offer(url);
                    list.add(new worker(map, levelQueue, doneAction));
                }
            }

            for (worker w: list) {
                taskExecutor.execute(w);
            }

            list.clear();

            taskExecutor.shutdown();
            try {
                taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (queue.isEmpty()) {
                queue.addAll(levelQueue);
                levelQueue.clear();
                counterDepth++;
            }

            if (counterDepth > this.depth - 1) {
                break;
            }
            System.out.printf("%d paginas restantes del nivel %d de %d\n", levelQueue.size(), counterDepth, depth);
        }

        this.doneAction.done();
    }
}

class worker extends Thread{
    String url;
    Spider spidy;
    Map<String, String> map;
    Queue<String> queue;
    BFS.Done action;

    worker(Map<String, String> map, Queue<String> queue, BFS.Done action) {
        this.url = queue.poll();
        this.spidy = new Spider(this.url != null ? this.url : "");
        this.map = map;
        this.queue = queue;
        this.action = action;
    }
    @Override
    public void run() {
        super.run();
        if (this.url != null) {
            Map<String, String> map = spidy.getAllLinks();
            for (String x : map.keySet()) {
                if (!this.map.containsKey(x)) {
                    this.map.put(x, map.get(x));
                    this.queue.offer(x);
                    synchronized (this) {
                        this.action.updateNumPages();
                    }
                }
            }
        }
    }
}


class InputPanel extends JPanel {
    protected final Insets PADDING = new Insets(5,5,0,5);
    protected JLabel urlLegend;
    protected JTextField urlTextField;
    protected JToggleButton runButton;

    protected JLabel workersLegend;
    protected JLabel depthLegend;
    protected JLabel timeLimitLegend;
    protected JLabel elapsedTimeLegend;
    protected JLabel parsedPageLegend;
    protected JLabel secondsLegend;
    protected JLabel timer;
    protected JLabel parsedLabel;

    protected JTextField workersTextField;
    protected JTextField depthTextField;
    protected JTextField timeLimitTextField;

    protected JCheckBox depthEnable;
    protected JCheckBox timeEnable;

    protected Timer stopwatch;
    private long secondLimit;
    private long currentSecond = 0;

    InputPanel() {
        setLayout(new GridBagLayout());

        this.runButton = new JToggleButton("Run");
        this.runButton.setName("RunButton");

        this.urlTextField = new JTextField();
        this.urlTextField.setName("UrlTextField");
        this.workersTextField = new JTextField();
        this.depthTextField = new JTextField();
        this.depthTextField.setName("DepthTextField");
        this.timeLimitTextField = new JTextField();

        this.depthEnable = new JCheckBox("Enabled");
        this.depthEnable.setSelected(true);
        this.depthEnable.setName("DepthCheckBox");
        this.timeEnable = new JCheckBox("Enabled");
        this.timeEnable.setSelected(false);

        this.urlLegend = new JLabel("Start URL:");
        this.workersLegend = new JLabel("Workers:");
        this.depthLegend = new JLabel("Maximum depth:");
        this.timeLimitLegend = new JLabel("Time limit:");
        this.elapsedTimeLegend = new JLabel("Elapsed time:");
        this.parsedPageLegend = new JLabel("Parsed pages:");
        this.secondsLegend = new JLabel("seconds");
        this.timer = new JLabel("0:00");
        this.parsedLabel = new JLabel("0");
        this.parsedLabel.setName("ParsedLabel");

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        c.insets = PADDING;
        c.anchor = GridBagConstraints.WEST;

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        add(this.urlLegend,c);

        c.weightx = 0.9;
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(this.urlTextField,c);

        c.weightx = 0;
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        add(runButton,c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        add(this.workersLegend,c);

        c.weightx = 0.9;
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(this.workersTextField,c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        add(this.depthLegend,c);

        c.weightx = 0.9;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(this.depthTextField,c);

        c.weightx = 0;
        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        add(this.depthEnable,c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        add(this.timeLimitLegend,c);

        c.weightx = 0.9;
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(this.timeLimitTextField,c);

        c.weightx = 0;
        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        add(this.secondsLegend,c);

        c.weightx = 0;
        c.gridx = 3;
        c.gridy = 3;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        add(this.timeEnable,c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        add(this.elapsedTimeLegend,c);

        c.weightx = 0;
        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        add(this.timer,c);

        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        add(this.parsedPageLegend,c);

        c.weightx = 0;
        c.gridx = 1;
        c.gridy = 5;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        add(this.parsedLabel,c);
    }

    public JTextField getUrlTextField() {
        return this.urlTextField;
    }

    public JToggleButton getRunButton() {
        return this.runButton;
    }

    public void startTimer() {
        String seconds = this.timeLimitTextField.getText();
        this.secondLimit = seconds.isEmpty() ? 0 : Integer.parseInt(seconds);
        ActionListener taskPerformer = e -> {
            if(this.timeEnable.isSelected()) {
                if (currentSecond < secondLimit) {
                    currentSecond++;
                    String time = formatSeconds(currentSecond);
                    timer.setText(time);
                } else {
                    currentSecond = 0;
                    secondLimit = -1;
                    ((Timer) e.getSource()).stop();
                }
            } else {
                currentSecond++;
                String time = formatSeconds(currentSecond);
                timer.setText(time);
            }
        };

        this.stopwatch = new Timer(1000,taskPerformer);
        this.stopwatch.start();
    }

    public void stopTimer() {
        this.stopwatch.stop();
        this.currentSecond = 0;
    }

    public static String formatSeconds(long timeInSeconds) {
        long hours = timeInSeconds / 3600;
        long secondsLeft = timeInSeconds - hours * 3600;
        long minutes = secondsLeft / 60;
        long seconds = secondsLeft - minutes * 60;

        String formattedTime = "";
        if (hours < 10)
            formattedTime += "0";
        formattedTime += hours + ":";

        if (minutes < 10)
            formattedTime += "0";
        formattedTime += minutes + ":";

        if (seconds < 10)
            formattedTime += "0";
        formattedTime += seconds ;

        return formattedTime;
    }

    public JTextField getDepthTextField() {
        return this.depthTextField;
    }

    public JCheckBox getDepthEnable() {
        return this.depthEnable;
    }

    public JLabel getParsedLabel() {
        return this.parsedLabel;
    }

    public JTextField getworkersTextField() {
        return this.workersTextField;
    }
}

class SavePanel extends JPanel {
    JLabel exportLabel;
    JTextField ExportUrlTextField;
    JButton ExportButton;


    SavePanel() {
        setLayout(new GridBagLayout());
        Insets padding = new Insets(5,5,5,5);

        this.exportLabel = new JLabel("Export: ");

        this.ExportUrlTextField = new JTextField();
        this.ExportUrlTextField.setName("ExportUrlTextField");

        this.ExportButton = new JButton("Save");
        this.ExportButton.setName("ExportButton");

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = padding;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;

        add(this.exportLabel,c);

        c.insets = padding;
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 4;
        add(this.ExportUrlTextField,c);

        c.insets = padding;
        c.weightx = 0;
        c.gridx = 5;
        c.gridy = 0;
        c.gridwidth = 1;
        add(this.ExportButton,c);


    }

    public JButton getExportButton() {
        return this.ExportButton;
    }

    public JTextField getExportUrlTextField() {
        return this.ExportUrlTextField;
    }
}