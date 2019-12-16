import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is the FireFly panelclass which contains a main method.to see the output. This class is user interactive, user can chhose
 *to save file with firefly extension(I proovide my firefly own extension) user can also load that saved state by clicking
 *button or use the menu bar, they can ever chhose sky color, flies color etc.
 * @author Minhajur Rahman, Student ID-302258.
 *University of Prince Edward Island, Charlottetown, PEI.
 * Course(CS-2910(Computer Science III), Professor: Dr.Andrew Godbout
 * Submision Dtate: November 8th, 2019.
 */
public class FireflyPanel extends JPanel {
    static final int WIDTH = 1000;
    static final int HEIGHT = 500;
    static final int HEIGHT_OFFSET = 50;
    static final Color SKY_COLOUR = Color.BLACK;

    static final int BUTTON_WIDTH = 100;
    static final int BUTTON_HEIGHT = 30;
    static final Color BUTTON_COLOUR = Color.GRAY;
    static final Color BUTTON_TEXT_COLOUR = Color.BLACK;
    static final Color BUTTON_BORDER = Color.WHITE;
    static final int BORDER_SIZE = 2;
    static final int BUTTON_SPACING = 5;

    private Lock fireflyLock;
    private final List<Firefly> fireflies;
    private final Timer timer;
    private Color skyColour = SKY_COLOUR;

    private ArrayList<AbstractAction> actions;
    //private SaveAction saveAction = new SaveAction();

    public FireflyPanel() {
        this(100);
    }

    /**
     * Constructor
     * @param n
     */
    public FireflyPanel(int n) {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(skyColour);

        fireflyLock = new ReentrantLock();

        fireflies = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            fireflies.add(new Firefly(this));
        }

        timer = new Timer(150, new FireflyTimer());

        actions = new ArrayList<>();

        actions.add(new SaveAction());
        actions.add(new LoadAction());
        actions.add(new PauseAction());
        //actions.add(new SkyAction());
        actions.add(new ColourAction("Sky", KeyEvent.VK_S) {
            @Override
            public void setColour(Color c) {
                skyColour = c;
                setBackground(c);
            }
            @Override
            public Color getColour() {
                return skyColour;
            }
        });
        actions.add(new ColourAction("Outline", KeyEvent.VK_O) {
            @Override
            public void setColour(Color c) {
                Firefly.setOutlineColour(c);
            }
            @Override
            public Color getColour() {
                return Firefly.getOutlineColour();
            }
        });
        actions.add(new ColourAction("Flash", KeyEvent.VK_F) {
            @Override
            public void setColour(Color c) {
                Firefly.setFlashColour(c);
            }
            @Override
            public Color getColour() {
                return Firefly.getFlashColour();
            }
        });
        actions.add(new ColourAction("Halo", KeyEvent.VK_H) {
            @Override
            public void setColour(Color c) {
                Firefly.setHaloColour(c);
            }
            @Override
            public Color getColour() {
                return Firefly.getHaloColour();
            }
        });

        this.setFocusable(true);
        timer.start();
        //timer.schedule(new FireflyTimer(), 0, 100);
    }

    /**
     * Update the fireflies based on condition.
     */
    public void updateFireflies() {
        // Two locks is overkill, but for certain types of multithreading it may be necessary
        fireflyLock.lock(); // This is enough to keep save and update from happening at the same time
        Firefly.lock(); // This would keep this method safe if other classes have access, so long as they also use the lock.
        try {
           
            synchronized (fireflies) {
                Firefly firefly1, firefly2;
                for (int i = 0; i < this.fireflies.size(); i++) {
                    firefly1 = fireflies.get(i);
                    for (int j = i + 1; j < this.fireflies.size(); j++) {
                        firefly2 = fireflies.get(j);

                        firefly1.checkSeen(firefly2);
                        firefly2.checkSeen(firefly1);
                    }
                }

                for (Firefly f : fireflies) {
                    f.updatePhaseShift();
                }

                for (Firefly f : fireflies) {
                    f.updatePhase();
                }

                for (Firefly f : fireflies) {
                    f.updateFlashing();
                }
            }
        }
        finally {
            Firefly.unlock();
            fireflyLock.unlock();
        }

        //graphics update
        repaint();
    }

    /**
     * Overriding paintComponet
     * @param g
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

       
        fireflies.forEach(f -> f.draw(g));
    }

    /**
     * serializeInt values
     * @param c
     * @return
     */
    public static byte[] serializeInt(int c){
        byte[] arr = new byte[4];

        for(int j = 0; j < arr.length; j++){
            arr[j] = (byte) (c >>> (8*j));
        }

        return arr;
    }

    /**
     * serializeInt values.
     * @param c
     * @param arr
     * @param index
     * @return
     */
    public static byte[] serializeInt(int c, byte[] arr, int index){
        for(int j = 0; j < arr.length && j < 4; j++){
            arr[index+j] = (byte) (c >>> (8*j));
        }

        return arr;
    }

    /**
     * deserializeInt
     * @param arr
     * @return var
     */
    public static int deserializeInt(byte [] arr){
        int var = 0;
        for(int j = 0; j < arr.length && j < 4; j++){
            var |= Byte.toUnsignedInt(arr[j]) << (8*j);
        }
        return  var;
    }

    /**
     * deserialize int
     * @param arr
     * @param index
     * @return var
     */
    public static int deserializeInt(byte [] arr, int index){
        int var = 0;
        for(int j = 0; index+j < arr.length && j < 4; j++){
            var |= Byte.toUnsignedInt(arr[index+j]) << (8*j);
        }
        return  var;
    }

    /**
     * serializecolur.
     * @param c
     * @return
     */
    public static byte[] serializeColour(Color c){
        byte[] arr = new byte[4];

        for(int j = 0; j < arr.length; j++){
            arr[j] = (byte) (c.getRGB() >>> (8*j));
        }

        return arr;
    }

    /**
     * deserialize colour value.
     * @param d
     * @return
     */
    public static byte[] serlializeDouble(double d){
        long bits = Double.doubleToRawLongBits(d);

        byte[] arr = new byte[8];

        for(int j = 0; j < arr.length; j++){
            arr[j] = (byte) (bits >>> (8*j));
        }

        //System.out.println(d+" -> "+ Long.toBinaryString(bits) +" -> "+ Arrays.toString(arr) +" -> "+ Double.longBitsToDouble(bits));

        return arr;
    }

    /**
     * seriliaze double
     * @param d
     * @param arr
     * @param index
     */
    public static void serlializeDouble(double d, byte[] arr, int index){
        long bits = Double.doubleToRawLongBits(d);

        for(int j = 0; index+j < arr.length && j < 8; j++){
            arr[index+j] = (byte) (bits >>> (8*j));
        }

        //System.out.println(d+" -> "+ Long.toBinaryString(bits) +" -> "+ Arrays.toString(arr) +" -> "+ Double.longBitsToDouble(bits));
    }

    /**
     * deserialize double
     * @param arr
     * @return
     */
    public static double deserlializeDouble(byte[] arr){
        long bits = 0;

        for(int j = 0; j < arr.length && j < 8; j++){
        //for(int j = 8; j >= 0; j--){
            //bits = (bits << 8) | arr[j];
            //System.out.println(Long.toBinaryString(bits)+ "<<= 8 | "+((long)arr[j]));
            bits |= Byte.toUnsignedLong(arr[j]) << (8*j);
            //System.out.println(Long.toBinaryString(bits)+ "="+((long)arr[j])+ "<<"+ (8*j));
        }

        //System.out.println(Double.longBitsToDouble(bits)+" <- "+ Long.toBinaryString(bits) +" <- "+ Arrays.toString(arr));

        return Double.longBitsToDouble(bits);
    }
    /**
     * deserialize double
     * @param arr
     * @return
     */
    public static double deserlializeDouble(byte[] arr, int index){
        long bits = 0;

        for(int j = 0; index+j < arr.length && j < 8; j++){
        //for(int j = 8; j >= 0; j--){
            //bits = (bits << 8) | Byte.toUnsignedLong(arr[index+j]);
            //System.out.println(Long.toBinaryString(bits)+ "<<= 8 | "+((long)arr[index+j]));
            bits |= Byte.toUnsignedLong(arr[index+j]) << (8*j);
            //System.out.println(Long.toBinaryString(bits)+ "="+((long)arr[index+j])+ "<<"+ (8*j));
        }

        //System.out.println(Double.longBitsToDouble(bits)+" <- "+ Long.toBinaryString(bits) +" <- "+ Arrays.toString(arr));

        return Double.longBitsToDouble(bits);
    }
    /**
     * deserialize boolean
     * @param b
     * @return boolean value
     */
    public static byte serlializeBoolean(boolean b){
        if (b) {
            return (byte) 1;
        }
        return (byte) 0;
    }

    public static boolean deserlializeBoolean(byte b){
        if (b == 0) {
            return false;
        }
        return true;
    }

    /**
     * Save a state with firefly extension(Provide own extenson)
     */
    public void save() {
        boolean errored = false;

        JFileChooser chooser = new JFileChooser(".");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Firefly state save files", "firefly");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showSaveDialog(this.getParent());

        if(returnVal == JFileChooser.APPROVE_OPTION) {
           File saveFile = chooser.getSelectedFile();
           if (!saveFile.getName().endsWith(".firefly")) {
               saveFile = new File(saveFile.getPath()+".firefly");
           }

            try(ByteArrayOutputStream saveBytes = new ByteArrayOutputStream();
                FileOutputStream saveOutput = new FileOutputStream(saveFile)) {

                saveBytes.write(serializeInt(fireflies.size()));
                saveBytes.write(serializeInt(skyColour.getRGB()));

                // Two locks is overkill, but for certain types of multithreading it may be necessary
                fireflyLock.lock(); // This is enough to keep save and update from happening at the same time
                Firefly.lock(); // This would keep this method safe if other classes have access, so long as they also use the lock.
                try {
                    saveBytes.write(Firefly.colorsToBytes());
                    for (Firefly fly : fireflies) {
                        saveBytes.write(fly.toBytes());
                    }
                    //fireflies.forEach(f -> System.out.println(f));
                }
                finally {
                    try {
                        Firefly.unlock();
                    } catch (IllegalMonitorStateException ie) {
                        errored = true;
                    }
                    fireflyLock.unlock();
                }

                if (!errored) saveBytes.writeTo(saveOutput);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Load the saved state
     */
    public void load() {
        //fireflies.forEach(f -> System.out.println(f));
        //System.out.println("Loading...");
        boolean errored = false;

        JFileChooser chooser = new JFileChooser(".");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Firefly state save files", "firefly");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this.getParent());

        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File loadFile = chooser.getSelectedFile();

            try(FileInputStream loadInput = new FileInputStream(loadFile)) {
                //ByteArrayInputStream loadBytes = new ByteArrayInputStream();

                int n = deserializeInt(loadInput.readNBytes(4));
                Color loadedSky = new Color(deserializeInt(loadInput.readNBytes(4)));


                byte[] fireflyColours = loadInput.readNBytes(Firefly.COLOUR_BYTES_LENGTH);

                ArrayList <Firefly> newFireflies = new ArrayList<>(n);

                for(int i = 0; i < n; i++){
                    newFireflies.add(Firefly.fromBytes(loadInput.readNBytes(Firefly.FIREFLY_BYTES_LENGTH), this));
                }

                //System.out.println("newFireflies.size: "+newFireflies.size());


                // Two locks is overkill, but for certain types of multithreading it may be necessary
                fireflyLock.lock(); // This is enough to keep save and update from happening at the same time
                Firefly.lock(); // This would keep this method safe if other classes have access, so long as they also use the lock.
                try {
                    this.skyColour = loadedSky;
                    this.fireflies.clear();
                    this.fireflies.addAll(newFireflies);

                    Firefly.setColorsFromBytes(fireflyColours);

                    //System.out.println("fireflies.size: "+fireflies.size());
                    //fireflies.forEach(f -> System.out.println(f));
                }
                finally {
                    try {
                        Firefly.unlock();
                    } catch (IllegalMonitorStateException ie) {
                        errored = true;
                    }
                    fireflyLock.unlock();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //System.out.println(timer);
       // System.out.println(timer.isRunning());
    }

    //inner class
    private class FireflyTimer implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateFireflies();
        }
    }

    private class SaveAction extends AbstractAction {
        public SaveAction() {
            super("save");
            putValue(SHORT_DESCRIPTION, "Save the current state.");
            putValue(LONG_DESCRIPTION, "This will save the current state of the firefly panel.");
            putValue(MNEMONIC_KEY, KeyEvent.VK_S);
            putValue(NAME, "Save");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            save();
        }
    }

    private class LoadAction extends AbstractAction {
        public LoadAction() {
            super("load");
            putValue(SHORT_DESCRIPTION, "Load a past state.");
            putValue(LONG_DESCRIPTION, "This will load a past state to the firefly panel.");
            putValue(MNEMONIC_KEY, KeyEvent.VK_L);
            putValue(NAME, "Load");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            load();
        }
    }

    private class PauseAction extends AbstractAction {
        public PauseAction() {
            super("pause");
            putValue(SHORT_DESCRIPTION, "Pauses the fireflies.");
            putValue(LONG_DESCRIPTION, "This will pause the fireflies.");
            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
            putValue(NAME, "Pause");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (timer.isRunning()) {
                timer.stop();
                this.putValue(NAME, "Play");
            }
            else {
                timer.start();
                this.putValue(NAME, "Pause");
            }
        }
    }

    private abstract class ColourAction extends AbstractAction {
        String name;
        public ColourAction(String name, int key) {
            super(name);

            this.name = name;

            putValue(SHORT_DESCRIPTION, "Pick the "+name.toLowerCase()+" colour.");
            putValue(LONG_DESCRIPTION, "This will allow you to pick the "+name.toLowerCase()+" colour.");
            putValue(MNEMONIC_KEY, key);
            putValue(NAME, name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Color newColour = JColorChooser.showDialog(FireflyPanel.this, this.getValue(SHORT_DESCRIPTION).toString(), getColour());

            if (newColour != null) {
                //skyColour = newColour;
                //setBackground(skyColour);
                setColour(newColour);
            }
        }

        abstract void setColour(Color c);

        abstract Color getColour();
    }

    private JMenuBar createMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();

        // create our main menu
        JMenu fileMenu = new JMenu("File");
        JMenu editMenu = new JMenu("Edit");
        JMenu colourMenu = new JMenu("Colours");

        // create our menu items, using the same actions the toolbar buttons use
        int num = 0;
        for (AbstractAction a : this.actions) {
            JMenuItem item = new JMenuItem(a);

            if (num < 2) {
                fileMenu.add(item);
            }
            else if (num > 2) {
                colourMenu.add(item);
            }
            else {
                editMenu.add(item);
            }

            num++;
        }

        // add the menus to the menubar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(colourMenu);

        return menuBar;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        int flies = 100;

        FireflyPanel fireflyPanel = new FireflyPanel(flies);

        //fireflyPanel.add(new JButton("Test"));

        JPanel buttonPanel = fireflyPanel;
        //JPanel buttonPanel = new JPanel();
        //buttonPanel.setPreferredSize(new Dimension((BUTTON_WIDTH+BUTTON_SPACING)*fireflyPanel.actions.size(), BUTTON_HEIGHT));
        //buttonPanel.setBackground(Color.RED);

        //buttonPanel.add(new JButton("Test"));

        ArrayList<JButton> buttons = new ArrayList<>();

        for (AbstractAction a : fireflyPanel.actions) {
            JButton b = new JButton(a);
            //JButton b = new JButton("Test"+a.getValue(Action.NAME).toString());
            buttons.add(b);
        }

        buttonPanel.add(Box.createRigidArea(new Dimension(BUTTON_SPACING, 0)));
        for (JButton b : buttons) {
            b.setVerticalTextPosition(AbstractButton.CENTER);
            b.setHorizontalTextPosition(AbstractButton.CENTER);

            b.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));

            b.setBackground(BUTTON_COLOUR);
            b.setForeground(BUTTON_TEXT_COLOUR);

            //b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BUTTON_BORDER), BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE)));

            buttonPanel.add(b);
            b.setEnabled(true);
            //b.setVisible(true);

            buttonPanel.add(Box.createRigidArea(new Dimension(BUTTON_SPACING, 0)));

            //System.out.println(b);
        }
        buttonPanel.add(Box.createHorizontalGlue());




        //frame.add(buttonPanel);
        frame.add(fireflyPanel);


        frame.setJMenuBar(fireflyPanel.createMenuBar());

        //frame.setLayout(new BoxLayout(frame, BoxLayout.Y_AXIS));

        frame.pack();
        frame.setVisible(true);
    }
}
