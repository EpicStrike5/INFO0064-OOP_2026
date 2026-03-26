package be.uliege.montefiore.oop;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

// optional graphical window — activated with --display flag
public class PuzzleDisplay extends JPanel {

    private static final int   CELL_SIZE  = 100;
    private static final int   PADDING    = 50;
    private static final int   SMALL_SIZE = 65;  // size of unplaced pieces rendered below
    private static final int   GAP        = 40;  // spacing between unplaced pieces (needs to cover bump overhang ~18px each side)

    // 10-colour palette, pieces coloured by (index % 10)
    private static final Color[] PALETTE = {
        new Color(255, 213,  79),
        new Color( 77, 182, 172),
        new Color(255, 138,  91),
        new Color(149, 117, 205),
        new Color(240, 128, 128),
        new Color( 79, 195, 247),
        new Color(174, 213, 129),
        new Color(240, 157, 181),
        new Color(255, 241, 118),
        new Color(128, 203, 196),
    };

    private final Puzzle puzzle;

    public PuzzleDisplay(Puzzle puzzle) {
        this.puzzle = puzzle;
        setBackground(new Color(240, 240, 240));
    }

    @Override
    public Dimension getPreferredSize() {
        int winW = puzzle.getWidth()  * CELL_SIZE + 2 * PADDING;
        int winH = puzzle.getHeight() * CELL_SIZE + 2 * PADDING + unplacedAreaHeight();
        return new Dimension(winW, winH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        // pass 1a: gray placeholders for empty cells (drawn first so piece fills go on top)
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                if (puzzle.getPlacement(row, col) == null) {
                    g2.setColor(new Color(210, 210, 210));
                    g2.fillRect((int)pixelX(col), (int)pixelY(row), CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // pass 1b: piece fills on top — bumps that protrude into empty cells stay visible
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                Placement p = puzzle.getPlacement(row, col);
                if (p == null) continue;

                Element e   = getEffective(p);
                Path2D path = buildPath(e, pixelX(col), pixelY(row), CELL_SIZE);
                Color base  = PALETTE[p.getIndex() % PALETTE.length];

                g2.setColor(base.darker());
                g2.translate(3, 3);
                g2.fill(path);
                g2.translate(-3, -3);

                g2.setColor(base);
                g2.fill(path);
            }
        }

        // pass 2: borders
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                Placement p = puzzle.getPlacement(row, col);

                if (p == null) {
                    // dashed border for empty cells
                    g2.setColor(new Color(170, 170, 170));
                    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            1f, new float[]{6, 4}, 0));
                    g2.drawRect((int)pixelX(col), (int)pixelY(row), CELL_SIZE, CELL_SIZE);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    continue;
                }

                g2.setColor(new Color(40, 40, 40));
                Path2D path = buildPath(getEffective(p), pixelX(col), pixelY(row), CELL_SIZE);
                g2.draw(path);
            }
        }

        // pass 3: piece index labels (white halo + black text)
        int fontSize = Math.max(11, CELL_SIZE / 7);
        g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();

        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                Placement p = puzzle.getPlacement(row, col);
                if (p == null) continue;

                String label = String.valueOf(p.getIndex() + 1);
                int tx = (int)(pixelX(col) + CELL_SIZE / 2f - fm.stringWidth(label) / 2f);
                int ty = (int)(pixelY(row) + CELL_SIZE / 2f + fm.getAscent()  / 2f - 3);

                g2.setColor(Color.WHITE);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        if (dx != 0 || dy != 0)
                            g2.drawString(label, tx + dx, ty + dy);

                g2.setColor(Color.BLACK);
                g2.drawString(label, tx, ty);
            }
        }

        // pass 4: draw unplaced pieces below the grid (only if puzzle was unsolvable)
        drawUnplacedPieces(g2);
    }

    // renders unplaced pieces in a tiled section below the main grid
    private void drawUnplacedPieces(Graphics2D g2) {
        List<Integer> unplaced = getUnplacedIndices();
        if (unplaced.isEmpty()) return;

        int mainW  = puzzle.getWidth() * CELL_SIZE + 2 * PADDING;
        int startY = puzzle.getHeight() * CELL_SIZE + 2 * PADDING;

        // separator line
        g2.setColor(new Color(160, 160, 160));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                1f, new float[]{6, 4}, 0));
        g2.drawLine(PADDING, startY, mainW - PADDING, startY);

        // section label
        startY += PADDING / 2 + 4;
        g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
        g2.setColor(new Color(100, 100, 100));
        g2.drawString("Unplaced pieces (" + unplaced.size() + "):", PADDING, startY + 12);
        startY += 38; // extra room so the label doesn't overlap the pieces

        int availW = mainW - 2 * PADDING;
        int cols   = Math.max(1, availW / (SMALL_SIZE + GAP));

        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        for (int k = 0; k < unplaced.size(); k++) {
            int idx     = unplaced.get(k);
            float px    = PADDING + (k % cols) * (SMALL_SIZE + GAP);
            float py    = startY  + (k / cols) * (SMALL_SIZE + GAP);

            // draw in original orientation (rotation 0) since the piece was never placed
            Element e   = puzzle.getList().get(idx);
            Path2D path = buildPath(e, px, py, SMALL_SIZE);
            Color base  = PALETTE[idx % PALETTE.length];

            g2.setColor(base.darker());
            g2.translate(2, 2);
            g2.fill(path);
            g2.translate(-2, -2);

            g2.setColor(base);
            g2.fill(path);

            g2.setColor(new Color(40, 40, 40));
            g2.draw(path);

            // label
            String label   = String.valueOf(idx + 1);
            int labelSize  = Math.max(9, SMALL_SIZE / 7);
            g2.setFont(new Font("SansSerif", Font.BOLD, labelSize));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (int)(px + SMALL_SIZE / 2f - fm.stringWidth(label) / 2f);
            int ty = (int)(py + SMALL_SIZE / 2f + fm.getAscent()        / 2f - 2);

            g2.setColor(Color.WHITE);
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    if (dx != 0 || dy != 0)
                        g2.drawString(label, tx + dx, ty + dy);

            g2.setColor(Color.BLACK);
            g2.drawString(label, tx, ty);
        }
    }

    // builds the closed jigsaw outline for one piece, traced clockwise from top-left
    private Path2D buildPath(Element e, float x, float y, int size) {
        float s = size;
        Path2D.Float path = new Path2D.Float();
        path.moveTo(x, y);

        // normalX/Y = outward unit normal for each side
        drawSide(path, e.getTop(),    x,     y,     x + s, y,      0, -1, size);
        drawSide(path, e.getRight(),  x + s, y,     x + s, y + s,  1,  0, size);
        drawSide(path, e.getBottom(), x + s, y + s, x,     y + s,  0,  1, size);
        drawSide(path, e.getLeft(),   x,     y + s, x,     y,     -1,  0, size);

        path.closePath();
        return path;
    }

    // appends one side to the path: straight line for F, bezier bump/pit for B/P
    private void drawSide(Path2D.Float path, char type,
                          float fromX, float fromY,
                          float toX,   float toY,
                          float normalX, float normalY,
                          int size) {

        if (type == 'F') {
            path.lineTo(toX, toY);
            return;
        }

        float dir   = (type == 'B') ? 1f : -1f;
        float t     = size * 0.28f; // bump/pit height scales with cell size
        float neckW = t * 0.40f;
        float headW = t * 0.52f;

        float nx = normalX * dir * t;
        float ny = normalY * dir * t;

        float midX = (fromX + toX) / 2f;
        float midY = (fromY + toY) / 2f;

        float dx  = toX - fromX;
        float dy  = toY - fromY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float tux = dx / len;
        float tuy = dy / len;

        float nsX = midX - tux * neckW; // neck entry
        float nsY = midY - tuy * neckW;
        float neX = midX + tux * neckW; // neck exit
        float neY = midY + tuy * neckW;

        path.lineTo(nsX, nsY);

        // curve: neck entry → head centre
        path.curveTo(
            nsX + normalX * dir * t * 0.72f, nsY + normalY * dir * t * 0.72f,
            midX + nx - tux * headW,          midY + ny - tuy * headW,
            midX + nx,                        midY + ny
        );

        // curve: head centre → neck exit (mirror)
        path.curveTo(
            midX + nx + tux * headW,          midY + ny + tuy * headW,
            neX  + normalX * dir * t * 0.72f, neY  + normalY * dir * t * 0.72f,
            neX,                              neY
        );

        path.lineTo(toX, toY);
    }

    // returns the piece at this placement with rotation applied
    private Element getEffective(Placement p) {
        return puzzle.getList()
                     .get(p.getIndex())
                     .rotate_element(p.getRotation());
    }

    // list of piece indices that were not placed in the grid
    private List<Integer> getUnplacedIndices() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < puzzle.getList().size(); i++)
            if (!puzzle.getUsed(i)) result.add(i);
        return result;
    }

    // pixel height of the unplaced pieces section (0 if all pieces are placed)
    private int unplacedAreaHeight() {
        List<Integer> unplaced = getUnplacedIndices();
        if (unplaced.isEmpty()) return 0;
        int mainW  = puzzle.getWidth() * CELL_SIZE + 2 * PADDING;
        int availW = mainW - 2 * PADDING;
        int cols   = Math.max(1, availW / (SMALL_SIZE + GAP));
        int rows   = (unplaced.size() + cols - 1) / cols;
        return PADDING / 2 + 4 + 38 + rows * (SMALL_SIZE + GAP) + PADDING / 2;
    }

    private float pixelX(int col) { return PADDING + col * CELL_SIZE; }
    private float pixelY(int row) { return PADDING + row * CELL_SIZE; }

    // opens the window on the Swing EDT
    public static void show(final Puzzle puzzle) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // check if the puzzle is partial (some pieces unplaced)
                boolean partial = false;
                for (int i = 0; i < puzzle.getList().size(); i++)
                    if (!puzzle.getUsed(i)) { partial = true; break; }

                String title = "Puzzle  " + puzzle.getWidth() + " \u00d7 " + puzzle.getHeight()
                    + "  (" + puzzle.getList().size() + " pieces)"
                    + (partial ? "  \u2014  unsolvable" : "");

                JFrame frame = new JFrame(title);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.getContentPane().add(new PuzzleDisplay(puzzle));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setResizable(false);
                frame.setVisible(true);
            }
        });
    }
}
