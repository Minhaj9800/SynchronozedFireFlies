import javax.swing.*;
import java.awt.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This is the FireFly Class which we gonna create and see on the screen(as sky) and they will flashing.
 * The idea of threading,synchronization is used to solve this task.
 * @author Minhajur Rahman, Student ID-302258.
 *University of Prince Edward Island, Charlottetown, PEI.
 * Course(CS-2910(Computer Science III), Professor: Dr.Andrew Godbout
 * Submision Dtate: November 8th, 2019.
 */
public class Firefly {

    ///intitializing all the required variables.
    public static final int COLOUR_BYTES_LENGTH = 12;
    public static final int FIREFLY_BYTES_LENGTH = 25;

    public static final double TWO_PI = 2 * Math.PI;
    public static final Color FLASH_COLOUR = Color.GREEN;
    public static final Color HALO_COLOUR = Color.YELLOW;
    public static final Color OUTLINE_COLOUR = Color.DARK_GRAY;
    public static final int SIZE = 10;

    private Lock updateFlashingLock;
    private Lock updatePhaseLock;
    private Lock updateSeenLock;

    private static volatile Lock staticLock;

    protected volatile double phi, dphidt;
    protected final double omega = 0.785, K = 0.1;
    protected int x, y;
    protected final int M = 100;
    protected volatile int seen;
    protected volatile boolean flashing;

    protected static Color flashColour = FLASH_COLOUR;
    protected static Color haloColour = HALO_COLOUR;
    protected static Color outlineColour = OUTLINE_COLOUR;

    protected final JComponent parent;


    /**
     * Constructor
     * @param parent
     */
    public Firefly(JComponent parent) {
        this((int) (Math.random() * (FireflyPanel.WIDTH - SIZE)),
                (int) (Math.random() * (FireflyPanel.HEIGHT-FireflyPanel.HEIGHT_OFFSET - SIZE))
                               + FireflyPanel.HEIGHT_OFFSET, parent);
    }

    /**
     * Constructor for FireFly
     * @param x
     * @param y
     * @param parent
     */
    public Firefly(int x, int y, JComponent parent) {
        this(x, y, random2Pi(), 0.0, false, parent);
    }

    /**
     * Create a private constructor for our internal use.
     * @param x
     * @param y
     * @param phi
     * @param dphidt
     * @param flashing
     * @param parent
     */
    private Firefly(int x, int y,double phi, double dphidt, boolean flashing, JComponent parent) {
        this.parent = parent;
        this.x = x;
        this.y = y;
        this.phi = phi;
        this.dphidt = dphidt;
        this.flashing = flashing;

        updateFlashingLock = new ReentrantLock();
        updatePhaseLock = new ReentrantLock();
        updateSeenLock = new ReentrantLock();
        staticLock = new ReentrantLock();
    }

    /**
     * draw the shape of the fly
     * @param g
     */
    public void draw(Graphics g) {
        Color former = g.getColor();

        int width = (this.parent == null) ? FireflyPanel.WIDTH : this.parent.getWidth();
        int height = (this.parent == null) ? FireflyPanel.HEIGHT : this.parent.getHeight();

        if (flashing) {
            g.setColor(haloColour);
            //g.drawOval(this.x, this.y, SIZE, SIZE);
            g.drawOval((this.x*width)/FireflyPanel.WIDTH, (this.y*height)/FireflyPanel.HEIGHT, SIZE, SIZE);
            g.setColor(flashColour);
            //g.fillOval(this.x, this.y, SIZE-1, SIZE-1);
            g.fillOval((this.x*width)/FireflyPanel.WIDTH, (this.y*height)/FireflyPanel.HEIGHT, SIZE-1, SIZE-1);
        }
        else {
            g.setColor(outlineColour);
            //g.drawOval(this.x, this.y, SIZE, SIZE);
            g.drawOval((this.x*width)/FireflyPanel.WIDTH, (this.y*height)/FireflyPanel.HEIGHT, SIZE, SIZE);
        }
        g.setColor(former);
    }

    /**
     * updating with seenlock.
     */
    private void newUpdate() {
        updateSeenLock.lock();
        try {
            //Manipulate the shared resource.
            synchronized (this) {
                this.seen = 0;
            }
        }
        finally {
            updateSeenLock.unlock();
        }
    }

    /**
     * Update flashing of the flies based on specific requirements.
     */
    protected void updateFlashing() {
        updateFlashingLock.lock();
        try {
            //Manipulate the shared resource.
            synchronized (this) {
                if (flashing) flashing = false;
                else if (phi > TWO_PI) {
                    flashing = true;
                    updatePhaseLock.lock();
                    try {
                        //Manipulate the shared resource.
                        phi = mod2Pi(phi);
                    }
                    finally {
                        updatePhaseLock.unlock();
                    }
                }
                newUpdate();
            }
        }
        finally {
            updateFlashingLock.unlock();
        }
    }

    /**
     * Update the phase.
     */
    public void updatePhase() {
        updatePhaseLock.lock();
        try {
            //Manipulate the shared resource.
            synchronized (this) {
                phi = phi + dphidt;
            }
        }
        finally {
            updatePhaseLock.unlock();
        }
        // consider having their position change
    }

    /**
     * checking either seen each other(flies) or not.
     * @param other
     */
    public void checkSeen(Firefly other) {
        if (other.isFlashing() && d(other) < this.M) {
            updateSeenLock.lock();
            try {
                //Manipulate the shared resource.
                synchronized (this) {
                    this.seen++;
                }
            } finally {
                updateSeenLock.unlock();
            }
        }
    }

    /**
     * update the phase shift.
     */
    public synchronized void updatePhaseShift() {
        double change = this.K;

        updateSeenLock.lock();
        try {
            //Manipulate the shared resource.
            synchronized (this) {
                change *= this.seen;
            }
        } finally {
            updateSeenLock.unlock();
        }

        updatePhaseLock.lock();
        try {
            //Manipulate the shared resource.
            dphidt = omega + change * Math.sin(TWO_PI - phi);
        }
        finally {
            updatePhaseLock.unlock();
        }
    }

    /**
     * method d(.....) is for distance calculation.
     * @param other
     * @return
     */
    protected double d(Firefly other) {
        double dx = (this.x - other.x), dy = (this.y - other.y);
        return Math.sqrt(dx*dx + dy*dy);
    }

    /**
     * Locking.......
     */
    public static void lock(){
        if (!Thread.currentThread().toString().contains("AWT-EventQueue"))
            System.out.println("FireFly.lock() by"+Thread.currentThread());
        staticLock.lock();
        if (!Thread.currentThread().toString().contains("AWT-EventQueue"))
            System.out.println("FireFly.locked() by"+Thread.currentThread());
    }

    /**
     * unlocking.......
     * @throws IllegalMonitorStateException
     */
    public static void unlock() throws IllegalMonitorStateException {
        if (!Thread.currentThread().toString().contains("AWT-EventQueue"))
            System.out.println("FireFly.unlock() by"+Thread.currentThread());
        staticLock.unlock();
        if (!Thread.currentThread().toString().contains("AWT-EventQueue"))
            System.out.println("FireFly.unlocked() by"+Thread.currentThread());
    }

    /**
     * calculating random 2PI
     * @return
     */
    protected static double random2Pi() {
        return Math.random() * TWO_PI;
    }

    /**
     * mod2pi() method take double x as parameter and check and set mod and return the mod.
     * @param x
     * @return mod
     */
    protected static double mod2Pi(double x) {
        double mod = x;
        while (mod < 0) mod += TWO_PI;
        while (mod > TWO_PI) mod -= TWO_PI;
        return mod;
    }

    //some getter method if ever required for something.
    public double getPhi() {
        return phi;
    }

    public double getOmega() {
        return omega;
    }

    public double getdPhidt() {
        return dphidt;
    }

    public double getK() {
        return K;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getM() {
        return M;
    }

    /**
     * Checking flashes or not.
     * @return
     */
    public boolean isFlashing() {
        return flashing;
    }

    /**
     * Get the Flash colour.
     * @return flashColour
     */
    public static Color getFlashColour() {
        return flashColour;
    }

    /**
     * Setter for Flash Colour.
     * @param flashColour
     */
    public static void setFlashColour(Color flashColour) {
        Firefly.flashColour = flashColour;
    }

    /**
     * Getter for HaloColor.
     * @return
     */
    public static Color getHaloColour() {
        return haloColour;
    }

    /**
     * Setter for Halo Colour.
     * @param haloColour
     */
    public static void setHaloColour(Color haloColour) {
        Firefly.haloColour = haloColour;
    }

    /**
     * Getter for outline Colour
     * @return outlineColour
     */
    public static Color getOutlineColour() {
        return outlineColour;
    }

    /**
     * set outlineColour.
     * @param outlineColour
     */
    public static void setOutlineColour(Color outlineColour) {
        Firefly.outlineColour = outlineColour;
    }


    /**
     *Get the byte values.
     * @return bytes
     */
    public byte[] toBytes() {
        byte[] bytes = new byte[FIREFLY_BYTES_LENGTH];

        FireflyPanel.serlializeDouble(phi, bytes, 0);
        FireflyPanel.serlializeDouble(dphidt, bytes,8);
        FireflyPanel.serializeInt(x,bytes,16);
        FireflyPanel.serializeInt(y,bytes,20);
        bytes[24] = FireflyPanel.serlializeBoolean(flashing);

        return bytes;
    }

    /**
     * Get a new firefly with some specific charcteristic.
     * @param bytes
     * @param parent
     * @return new Firefly object.
     */
    public static Firefly fromBytes(byte[] bytes, JComponent parent) {
        double phi = FireflyPanel.deserlializeDouble(bytes, 0);
        double dphidt = FireflyPanel.deserlializeDouble(bytes,8);
        int x = FireflyPanel.deserializeInt(bytes,16);
        int y = FireflyPanel.deserializeInt(bytes,20);
        boolean flashing = FireflyPanel.deserlializeBoolean(bytes[24]);

        return new Firefly(x,y, phi,dphidt,flashing, parent);
    }

    /**
     * converting byte to array.
     * @return bytes
     */
    public static byte[] colorsToBytes(){
        byte[] bytes = new byte[COLOUR_BYTES_LENGTH];

        FireflyPanel.serializeInt(Firefly.flashColour.getRGB(), bytes, 0);
        FireflyPanel.serializeInt(Firefly.outlineColour.getRGB(), bytes, 4);
        FireflyPanel.serializeInt(Firefly.haloColour.getRGB(), bytes, 8);

        return bytes;
    }

    /**
     * Set the colour from Bytes
     * @param bytes byte array
     */
    public static void setColorsFromBytes(byte[] bytes){
        Firefly.flashColour = new Color(FireflyPanel.deserializeInt(bytes, 0));
        Firefly.outlineColour = new Color(FireflyPanel.deserializeInt(bytes, 4));
        Firefly.haloColour = new Color(FireflyPanel.deserializeInt(bytes, 8));
    }

}
