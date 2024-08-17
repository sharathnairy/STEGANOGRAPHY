package steganocryptography;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;

public class SeamEncryption extends JFrame implements ActionListener {
    JButton open = new JButton("Open"), embed = new JButton("Embed"),
            save = new JButton("Save into new file"), reset = new JButton("Reset");

    JTextArea key = new JTextArea(1, 30);
    JTextArea message = new JTextArea(5, 30);
    BufferedImage sourceImage = null, embeddedImage = null;
    JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    JScrollPane originalPane = new JScrollPane(),
            embeddedPane = new JScrollPane();

    private final String PLACEHOLDER = "Enter key (7 characters only)";

    public SeamEncryption() {
        super("Embed steganographic message in image");
        assembleInterface();
        open.setBackground(Color.black);
        open.setForeground(Color.WHITE);
        open.setFont(new Font("Monaco", Font.BOLD, 20));

        embed.setBackground(Color.black);
        embed.setForeground(Color.WHITE);
        embed.setFont(new Font("Monaco", Font.BOLD, 20));

        save.setBackground(Color.black);
        save.setForeground(Color.WHITE);
        save.setFont(new Font("Monaco", Font.BOLD, 20));

        reset.setBackground(Color.black);
        reset.setForeground(Color.WHITE);
        reset.setFont(new Font("Monaco", Font.BOLD, 20));

        this.setSize(1000, 670);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setVisible(true);
        sp.setDividerLocation(0.5);
        this.validate();
    }

    private void assembleInterface() {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(open);
        buttonPanel.add(embed);
        buttonPanel.add(save);
        buttonPanel.add(reset);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        open.addActionListener(this);
        embed.addActionListener(this);
        save.addActionListener(this);
        reset.addActionListener(this);

        open.setMnemonic('O');
        embed.setMnemonic('E');
        save.setMnemonic('S');
        reset.setMnemonic('R');

        JPanel keyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        keyPanel.setBorder(BorderFactory.createTitledBorder("Key"));

        // Set up key text area with placeholder text
        key.setText(PLACEHOLDER);
        key.setForeground(Color.GRAY);
        key.setFont(new Font("Arial", Font.BOLD, 20));

        key.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (key.getText().equals(PLACEHOLDER)) {
                    key.setText("");
                    key.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (key.getText().isEmpty()) {
                    key.setText(PLACEHOLDER);
                    key.setForeground(Color.GRAY);
                }
            }
        });

        keyPanel.add(key);
        this.getContentPane().add(keyPanel, BorderLayout.NORTH);

        JPanel messagePanel = new JPanel(new GridLayout(1, 1));
        messagePanel.add(new JScrollPane(message));
        message.setFont(new Font("Arial", Font.BOLD, 20));
        messagePanel.setBorder(BorderFactory.createTitledBorder("Message to be embedded"));

        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.Y_AXIS));
        containerPanel.add(keyPanel);
        containerPanel.add(messagePanel);
        this.getContentPane().add(containerPanel, BorderLayout.NORTH);

        sp.setLeftComponent(originalPane);
        sp.setRightComponent(embeddedPane);
        originalPane.setBorder(BorderFactory.createTitledBorder("Original Image"));
        embeddedPane.setBorder(BorderFactory.createTitledBorder("Steganographed Image"));
        this.getContentPane().add(sp, BorderLayout.CENTER);

        this.validate();
    }

    public void actionPerformed(ActionEvent ae) {
        Object o = ae.getSource();
        if (o == open)
            openImage();
        else if (o == embed)
            embedMessage();
        else if (o == save)
            saveImage();
        else if (o == reset)
            resetInterface();
    }

    private java.io.File showFileDialog(final boolean open) {
        JFileChooser fc = new JFileChooser("Open an image");
        javax.swing.filechooser.FileFilter ff = new javax.swing.filechooser.FileFilter() {
            public boolean accept(java.io.File f) {
                String name = f.getName().toLowerCase();
                if (open)
                    return f.isDirectory() || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                            name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".tiff") ||
                            name.endsWith(".bmp") || name.endsWith(".dib");
                return f.isDirectory() || name.endsWith(".png") || name.endsWith(".bmp");
            }

            public String getDescription() {
                if (open)
                    return "Image (*.jpg, *.jpeg, *.png, *.gif, *.tiff, *.bmp, *.dib)";
                return "Image (*.png, *.bmp)";
            }
        };
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(ff);

        java.io.File f = null;
        if (open && fc.showOpenDialog(this) == fc.APPROVE_OPTION)
            f = fc.getSelectedFile();
        else if (!open && fc.showSaveDialog(this) == fc.APPROVE_OPTION)
            f = fc.getSelectedFile();
        return f;
    }

    private void openImage() {
        java.io.File f = showFileDialog(true);
        try {
            sourceImage = ImageIO.read(f);
            JLabel l = new JLabel(new ImageIcon(sourceImage));
            originalPane.getViewport().add(l);
            this.validate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void embedMessage() {
        String mess = message.getText();
        String keyvalue = key.getText();
        embeddedImage = sourceImage.getSubimage(0, 0, sourceImage.getWidth(), sourceImage.getHeight());
        if (mess.length() * 8+keyvalue.length()* 8 + 32 > embeddedImage.getHeight()) {
            JOptionPane.showMessageDialog(this, "Message is too long for the chosen image",
                    "Message too long!", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int[] messageData = stringToBinary(mess);
        int[] messageLengthData = intToBinary(mess.length(), 32);
        int[] keyData = stringToBinary(keyvalue);
        int[] combinedData = combineData(combineData(messageLengthData, keyData), messageData);
        int[][] embeddedImageArray = embedData(imageToArray(embeddedImage), combinedData);
        embeddedImage = arrayToImage(embeddedImageArray);

        JLabel l = new JLabel(new ImageIcon(embeddedImage));
        embeddedPane.getViewport().add(l);
        this.validate();
    }

    public static int[][] embedData(int[][] image, int[] data) {
        int width = image.length;
        int height = image[0].length;
        int messageLength = data.length;
        int messageIndex = 0;

        while (messageIndex < messageLength) {
            int[][] energyMap = computeEnergyMap(image);
            int[][] seam = findMinimumEnergySeam(energyMap);
            embedMessage(image, data, seam, messageIndex);
            messageIndex += width; // Increment by width as we embed one column at a time
        }

        return image;
    }

    private static void embedMessage(int[][] image, int[] message, int[][] seam, int messageIndex) {
        int width = image.length;
        int height = image[0].length;
        int totalMessageLength = message.length;

        for (int i = 0; i < height; i++) {
            int x = seam[i][0];
            if (messageIndex < totalMessageLength) {
                int bitToEmbed = message[messageIndex];
                int pixel = image[x][i];
                int b = pixel & 0xFF;
                b = (b & ~1) | bitToEmbed;
                image[x][i] = (pixel & 0xFFFFFF00) | b;
            }
            messageIndex++;
        }
    }

    private static int[][] computeEnergyMap(int[][] image) {
        int width = image.length;
        int height = image[0].length;
        int[][] energyMap = new int[width][height];

        for (int i = 1; i < width - 1; i++) {
            for (int j = 1; j < height - 1; j++) {
                int dx = Math.abs(image[i + 1][j] - image[i - 1][j]);
                int dy = Math.abs(image[i][j + 1] - image[i][j - 1]);
                energyMap[i][j] = dx + dy;
            }
        }

        return energyMap;
    }

    private static int[][] findMinimumEnergySeam(int[][] energyMap) {
        int width = energyMap.length;
        int height = energyMap[0].length;
        int[][] seam = new int[height][2];

        for (int j = 0; j < height; j++) {
            int minEnergy = Integer.MAX_VALUE;
            int minIndex = 0;

            for (int i = 0; i < width; i++) {
                if (energyMap[i][j] < minEnergy) {
                    minEnergy = energyMap[i][j];
                    minIndex = i;
                }
            }

            seam[j][0] = minIndex;
        }

        return seam;
    }

    private void saveImage() {
        if (embeddedImage == null) {
            JOptionPane.showMessageDialog(this, "No message has been embedded!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        java.io.File f = showFileDialog(false);
        String name = f.getName();
        String ext = name.substring(name.lastIndexOf(".") + 1).toLowerCase();

        if (!ext.equals("png") && !ext.equals("bmp")) {
            ext = "png";
            f = new java.io.File(f.getAbsolutePath() + ".png");
        }

        try {
            if (f.exists()) f.delete();
            ImageIO.write(embeddedImage, ext.toUpperCase(), f);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void resetInterface() {
        key.setText("");
        message.setText("");
        originalPane.getViewport().removeAll();
        embeddedPane.getViewport().removeAll();
        sourceImage = null;
        embeddedImage = null;
        sp.setDividerLocation(0.5);
        this.validate();
    }

    private static int[] stringToBinary(String input) {
        byte[] bytes = input.getBytes();
        int[] binary = new int[bytes.length * 8];

        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                binary[i * 8 + j] = (bytes[i] >> (7 - j)) & 1;
            }
        }

        return binary;
    }

    private static int[] intToBinary(int number, int bits) {
        int[] binary = new int[bits];
        for (int i = 0; i < bits; i++) {
            binary[bits - 1 - i] = (number >> i) & 1;
        }
        return binary;
    }

    private static int[] combineData(int[] data1, int[] data2) {
        int[] combined = new int[data1.length + data2.length];
        System.arraycopy(data1, 0, combined, 0, data1.length);
        System.arraycopy(data2, 0, combined, data1.length, data2.length);
        return combined;
    }

    private static int[][] imageToArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] array = new int[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                array[i][j] = image.getRGB(i, j);
            }
        }

        return array;
    }

    private static BufferedImage arrayToImage(int[][] array) {
        int width = array.length;
        int height = array[0].length;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                image.setRGB(i, j, array[i][j]);
            }
        }

        return image;
    }

    public static void main(String[] args) {
        new SeamEncryption();
    }
}
