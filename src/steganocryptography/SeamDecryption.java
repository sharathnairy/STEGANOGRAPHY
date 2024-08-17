package steganocryptography;

import java.awt.image.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.*;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public class SeamDecryption extends JFrame implements ActionListener {
    JButton open = new JButton("Open"), decode = new JButton("Decode"), reset = new JButton("Reset");
    JTextArea key = new JTextArea(1, 30);
    JTextArea message = new JTextArea(10, 3);
    BufferedImage image = null;
    JScrollPane imagePane = new JScrollPane();
    String secretKey = null;
    private final String PLACEHOLDER = "Enter key (7 characters only)";

    public SeamDecryption() {
        super("Decode steganographic message in image");
        assembleInterface();
        this.setSize(1000, 670);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setVisible(true);

        open.setBackground(Color.black);
        open.setForeground(Color.WHITE);
        open.setFont(new Font("Monaco", Font.BOLD, 20));

        decode.setBackground(Color.black);
        decode.setForeground(Color.WHITE);
        decode.setFont(new Font("Monaco", Font.BOLD, 20));

        reset.setBackground(Color.black);
        reset.setForeground(Color.WHITE);
        reset.setFont(new Font("Monaco", Font.BOLD, 20));
    }

    private void assembleInterface() {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(open);
        buttonPanel.add(decode);
        buttonPanel.add(reset);
        this.getContentPane().add(buttonPanel, BorderLayout.NORTH);
        open.addActionListener(this);
        decode.addActionListener(this);
        reset.addActionListener(this);

        JPanel keyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        keyPanel.setBorder(BorderFactory.createTitledBorder("Key"));
        keyPanel.add(key);
        key.setFont(new Font("Arial", Font.BOLD, 20));
        key.setText(PLACEHOLDER);
        key.setForeground(Color.GRAY);
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
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(keyPanel);

        JPanel imageAndMessagePanel = new JPanel(new GridLayout(1, 2, 20, 20));
        imagePane.setPreferredSize(new Dimension(600, 500));
        imagePane.setBorder(BorderFactory.createTitledBorder("Steganographed Image"));
        imageAndMessagePanel.add(imagePane);

        JPanel messagePanel = new JPanel(new GridLayout(1, 1));
        messagePanel.add(new JScrollPane(message));
        message.setFont(new Font("Arial", Font.BOLD, 20));
        messagePanel.setBorder(BorderFactory.createTitledBorder("Decoded message"));
        message.setEditable(false);
        imageAndMessagePanel.add(messagePanel);

        mainPanel.add(imageAndMessagePanel);
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent ae) {
        Object o = ae.getSource();
        if (o == open)
            openImage();
        else if (o == decode)
            decodeMessage();
        else if (o == reset)
            resetInterface();
    }

    private java.io.File showFileDialog(boolean open) {
        JFileChooser fc = new JFileChooser("Open an image");
        javax.swing.filechooser.FileFilter ff = new javax.swing.filechooser.FileFilter() {
            public boolean accept(java.io.File f) {
                String name = f.getName().toLowerCase();
                return f.isDirectory() || name.endsWith(".png") || name.endsWith(".bmp");
            }

            public String getDescription() {
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
            image = ImageIO.read(f);
            JLabel l = new JLabel(new ImageIcon(image));
            imagePane.getViewport().add(l);
            this.validate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void decodeMessage() {
        secretKey = key.getText();
        if (image == null && (secretKey.isEmpty() || secretKey.equals(PLACEHOLDER))) {
            JOptionPane.showMessageDialog(null, "First open a picture and enter the key");
            return;
        }
        if (image == null) {
            JOptionPane.showMessageDialog(null, "First open a picture");
            return;
        }
        if (secretKey.isEmpty() || secretKey.equals(PLACEHOLDER)) {
            JOptionPane.showMessageDialog(null, "Enter the Secret Key");
            return;
        }
        int[] lengthData = extractData(imageToArray(image), 0, 32);
        int messageLength = binaryToInt(lengthData);
        int[] extractedKeyData = extractData(imageToArray(image), 32, 56);
        String extractedKey = binaryToString(extractedKeyData, 56);

        if (!verifyKey(secretKey, extractedKey)) {
            JOptionPane.showMessageDialog(null, "Wrong key");
            return;
        } else {
            int[] messageData = extractData(imageToArray(image), 32 + 56, messageLength * 8);
            String extractedMessage = binaryToString(messageData, messageLength * 8);
            message.setText(extractedMessage);
        }
    }

    public static int[] extractData(int[][] image, int offset, int length) {
        int[] extractedData = new int[length];
        int width = image.length;
        int height = image[0].length;
        int messageIndex = 0;
        int totalBits = offset + length;

        while (messageIndex < totalBits) {
            int[][] energyMap = computeEnergyMap(image);
            int[][] seam = findMinimumEnergySeam(energyMap);
            for (int y = 0; y < height; y++) {
                int x = seam[y][0];
                if (messageIndex >= offset && messageIndex < totalBits) {
                    int pixel = image[x][y];
                    int bitExtracted = pixel & 1; // Extract the LSB
                    extractedData[messageIndex - offset] = bitExtracted;
                }
                messageIndex++;
            }
            removeSeam(image, seam);
        }

        return extractedData;
    }

    private static void removeSeam(int[][] image, int[][] seam) {
        int width = image.length;
        int height = image[0].length;

        for (int y = 0; y < height; y++) {
            int x = seam[y][0];
            if (x < width - 1) {
                System.arraycopy(image, x + 1, image, x, width - x - 1);
            }
            image[width - 1][y] = 0; // Clear the last column
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
        int[][] cumulativeMap = new int[width][height];

        for (int i = 0; i < width; i++) {
            cumulativeMap[i][0] = energyMap[i][0];
        }

        for (int j = 1; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int up = cumulativeMap[i][j - 1];
                int leftUp = (i > 0) ? cumulativeMap[i - 1][j - 1] : Integer.MAX_VALUE;
                int rightUp = (i < width - 1) ? cumulativeMap[i + 1][j - 1] : Integer.MAX_VALUE;
                cumulativeMap[i][j] = energyMap[i][j] + Math.min(up, Math.min(leftUp, rightUp));
            }
        }

        int minIndex = 0;
        for (int i = 1; i < width; i++) {
            if (cumulativeMap[i][height - 1] < cumulativeMap[minIndex][height - 1]) {
                minIndex = i;
            }
        }

        seam[height - 1][0] = minIndex;
        seam[height - 1][1] = height - 1;
        for (int j = height - 2; j >= 0; j--) {
            int prevSeamPos = seam[j + 1][0];
            int leftUp = (prevSeamPos > 0) ? cumulativeMap[prevSeamPos - 1][j] : Integer.MAX_VALUE;
            int up = cumulativeMap[prevSeamPos][j];
            int rightUp = (prevSeamPos < width - 1) ? cumulativeMap[prevSeamPos + 1][j] : Integer.MAX_VALUE;
            int minEnergy = Math.min(up, Math.min(leftUp, rightUp));

            if (minEnergy == leftUp) {
                seam[j][0] = prevSeamPos - 1;
            } else if (minEnergy == rightUp) {
                seam[j][0] = prevSeamPos + 1;
            } else {
                seam[j][0] = prevSeamPos;
            }
            seam[j][1] = j;
        }

        return seam;
    }

    private static int[][] imageToArray(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] array = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                array[x][y] = image.getRGB(x, y);
            }
        }
        return array;
    }

    private static String binaryToString(int[] binary, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i += 8) {
            int ascii = 0;
            for (int j = 0; j < 8; j++) {
                ascii = (ascii << 1) | binary[i + j];
            }
            sb.append((char) ascii);
        }
        return sb.toString();
    }

    private static int binaryToInt(int[] binary) {
        int value = 0;
        for (int i = 0; i < binary.length; i++) {
            value = (value << 1) | binary[i];
        }
        return value;
    }

    private static boolean verifyKey(String inputKey, String extractedKey) {
        return inputKey.equals(extractedKey);
    }

    private void resetInterface() {
        key.setText(PLACEHOLDER);
        key.setForeground(Color.GRAY);
        message.setText("");
        imagePane.getViewport().removeAll();
        image = null;
        this.validate();
    }
}
