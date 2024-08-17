package steganocryptography;

import java.awt.image.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.*;

public class Decryption extends JFrame implements ActionListener {
    JButton open = new JButton("Open"), decode = new JButton("Decode"), reset = new JButton("Reset");
    JTextArea key = new JTextArea(1, 30);
    JTextArea message = new JTextArea(10, 3);
    BufferedImage image = null;
    JScrollPane imagePane = new JScrollPane();
    String secretkey=null;
    int count=0;
    public Decryption() {
        super("Decode stegonographic message in image");
        assembleInterface();
        this.setSize(1000, 670);  // Increased the frame size to accommodate larger image panel
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
        open.setMnemonic('O');
        decode.setMnemonic('D');
        reset.setMnemonic('R');

        JPanel keyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        keyPanel.setBorder(BorderFactory.createTitledBorder("Key"));
        keyPanel.add(key);
        key.setFont(new Font("Arial", Font.BOLD, 20));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(keyPanel);

        JPanel imageAndMessagePanel = new JPanel(new GridLayout(1, 2, 20, 20));
        imagePane.setPreferredSize(new Dimension(600, 500));  // Adjusted the size of the image panel
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
         secretkey=key.getText();
         if(image == null && (secretkey.isEmpty()|| secretkey==null) ){
         JOptionPane.showMessageDialog(null, "first open a picture and enter the key");
         return;
     }
       if(image == null){
         JOptionPane.showMessageDialog(null, "first open a picture");
         return;
        }
         if (secretkey.isEmpty()|| secretkey==null) {
            JOptionPane.showMessageDialog(null, "Enter the Secret Key");
            return;
        }
        int len = extractInteger(image, 0, 0);
        int keylen=extractInteger(image,32,0);
        byte[] keyBytes = secretkey.getBytes();
        
        byte k[]=new byte[keylen];
        for (int i = 0; i < keylen; i++)
        {
            k[i] = extractByte(image, i * 8 + 64, 0);  
        }
        if(keylen!=secretkey.length())
        {
           JOptionPane.showMessageDialog(null, "Wrong key");
         return; 
        }
        else
        {
             for (int i = 0; i < k.length; i++)
             {
               if((keyBytes[i] ^ k[i])!=0)
               {
                   count++;
                   break;
               } 
             }
             if(count>0)
             {
                JOptionPane.showMessageDialog(null, "invalid key");
                return; 
             }
        }
        byte b[] = new byte[len];
        for (int i = 0; i < len; i++)
            b[i] = extractByte(image, i * 8 + (64+secretkey.length()*8), 0);
        message.setText(new String(b));
     }

    private int extractInteger(BufferedImage img, int start, int storageBit) {
        int maxX = img.getWidth(), maxY = img.getHeight(), startX = start / maxY, startY = start - startX * maxY, count = 0;
        int length = 0;
        for (int i = startX; i < maxX && count < 32; i++) {
            for (int j = startY; j < maxY && count < 32; j++) {
                int rgb = img.getRGB(i, j), bit = getBitValue(rgb, storageBit);
                length = setBitValue(length, count, bit);
                count++;
            }
        }
        return length;
    }

    private byte extractByte(BufferedImage img, int start, int storageBit) {
        int maxX = img.getWidth(), maxY = img.getHeight(), startX = start / maxY, startY = start - startX * maxY, count = 0;
        byte b = 0;
        for (int i = startX; i < maxX && count < 8; i++) {
            for (int j = startY; j < maxY && count < 8; j++) {
                int rgb = img.getRGB(i, j), bit = getBitValue(rgb, storageBit);
                b = (byte) setBitValue(b, count, bit);
                count++;
            }
        }
        return b;
    }

    private void resetInterface() {
        key.setText("");
        message.setText("");
        imagePane.getViewport().removeAll();
        image = null;
        this.validate();
    }

    private int getBitValue(int n, int location) {
        int v = n & (int) Math.round(Math.pow(2, location));
        return v == 0 ? 0 : 1;
    }

    private int setBitValue(int n, int location, int bit) {
        int toggle = (int) Math.pow(2, location), bv = getBitValue(n, location);
        if (bv == bit)
            return n;
        if (bv == 0 && bit == 1)
            n |= toggle;
        else if (bv == 1 && bit == 0)
            n ^= toggle;
        return n;
    }

    public static void main(String arg[]) {
        Decryption newClass = new Decryption();
    }
}
