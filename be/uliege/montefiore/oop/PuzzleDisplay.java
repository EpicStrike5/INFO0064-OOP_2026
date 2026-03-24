// Required package declaration — see Element.java for explanation.
package be.uliege.montefiore.oop;

// javax.swing.*  — JPanel, JFrame, SwingUtilities (the window toolkit)
import javax.swing.*;
// java.awt.*     — Graphics, Graphics2D, Color, Font, FontMetrics, BasicStroke, Dimension
import java.awt.*;
// java.awt.geom.* — Path2D (the class we use to draw arbitrary shapes)
import java.awt.geom.*;

// =============================================================================
// PuzzleDisplay — optional graphical window for a solved puzzle.
//
// Extends JPanel so it can be dropped into a JFrame and act as a drawing canvas.
// Swing calls paintComponent() automatically whenever the window needs repainting.
//
// HOW TO ACTIVATE:
//   Pass --display as the second command-line argument:
//     java be.uliege.montefiore.oop.Puzzle Puzzles/4x4.pzl.txt --display
//
// WHAT IT DRAWS:
//   Each piece is drawn as a true jigsaw shape.
//   Sides are rendered as:
//     F (flat) → straight line
//     B (bump) → rounded tab protruding outward (away from the piece centre)
//     P (pit)  → rounded notch going inward  (into the piece)
//   Bumps and pits use cubic Bézier curves (smooth S-shaped path) so they look
//   like real jigsaw connectors with a narrow neck and a rounded head.
//
// RENDERING ORDER (3 passes):
//   Pass 1 — fill all piece shapes
//   Pass 2 — draw all piece borders on top of the fills
//   Pass 3 — draw piece index numbers on top of everything
//   This order ensures borders and labels are never buried under neighbouring fills.
// =============================================================================
public class PuzzleDisplay extends JPanel {

    // =========================================================================
    // CONSTANTS
    // =========================================================================

    // Size of one puzzle cell in pixels.
    private static final int CELL_SIZE = 100;

    // Empty space around the grid (in pixels) so pieces at the border
    // have room to show their bumps without being clipped by the window edge.
    private static final int PADDING = 50;

    // Height of a bump/pit measured from the piece edge to the head tip.
    // 28% of CELL_SIZE gives a realistic-looking connector.
    private static final float TAB = CELL_SIZE * 0.28f;

    // Colour palette — pieces are coloured by (originalIndex % 10).
    // Using RGB values directly (no named constants) for Java 8 compatibility.
    private static final Color[] PALETTE = {
        new Color(255, 213,  79),   // 0 amber
        new Color( 77, 182, 172),   // 1 teal
        new Color(255, 138,  91),   // 2 orange
        new Color(149, 117, 205),   // 3 purple
        new Color(240, 128, 128),   // 4 coral
        new Color( 79, 195, 247),   // 5 sky blue
        new Color(174, 213, 129),   // 6 light green
        new Color(240, 157, 181),   // 7 pink
        new Color(255, 241, 118),   // 8 lemon
        new Color(128, 203, 196),   // 9 mint
    };

    // =========================================================================
    // FIELDS
    // =========================================================================

    // The solved puzzle we are displaying. Read-only after construction.
    private final Puzzle puzzle;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    // -------------------------------------------------------------------------
    // Sets the panel's preferred size so JFrame.pack() sizes the window
    // exactly to fit the grid plus padding on all sides.
    // setBackground() paints the area outside the grid a light grey.
    // -------------------------------------------------------------------------
    public PuzzleDisplay(Puzzle puzzle) {
        this.puzzle = puzzle;
        // Total pixel size = pieces × cell size + padding on both sides
        int winW = puzzle.getWidth()  * CELL_SIZE + 2 * PADDING;
        int winH = puzzle.getHeight() * CELL_SIZE + 2 * PADDING;
        setPreferredSize(new Dimension(winW, winH));
        setBackground(new Color(240, 240, 240)); // light grey background
    }

    // =========================================================================
    // RENDERING — called automatically by Swing whenever the window repaints
    // =========================================================================

    // -------------------------------------------------------------------------
    // paintComponent — the main drawing method, called by Swing automatically.
    //
    // We MUST call super.paintComponent(g) first: it clears the panel and
    // draws the background colour before we draw anything on top.
    //
    // We cast Graphics → Graphics2D to get access to curves, transforms,
    // anti-aliasing, and the stroke API (not available on plain Graphics).
    // -------------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);                 // clear + draw background
        Graphics2D g2 = (Graphics2D) g;          // upgrade to the modern 2D API

        // Quality settings for the renderer.
        // KEY_ANTIALIASING OFF keeps edges crisp (the user changed this deliberately).
        // KEY_RENDERING QUALITY prefers accuracy over speed.
        // KEY_STROKE_CONTROL PURE draws strokes at exact float positions (no pixel snapping).
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = puzzle.getWidth();
        int h = puzzle.getHeight();

        // ---------------------------------------------------------
        // PASS 1 — fill every piece with its colour + a drop shadow.
        //
        // We fill all pieces before drawing any borders so that a bump
        // protruding into a neighbouring cell's area stays visible:
        // it is drawn first, then the neighbour's FILL covers the
        // surrounding area (leaving the bump exposed through the pit notch).
        // ---------------------------------------------------------
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                Placement p = puzzle.getPlacement(row, col);
                if (p == null) continue; // cell not yet filled (shouldn't happen)

                // Reconstruct the piece with its placed rotation applied.
                Element e = getEffective(p);
                // Build the jigsaw outline shape for this piece.
                Path2D path = buildPath(e, pixelX(col), pixelY(row));

                // Draw a darker shifted copy to simulate a drop shadow.
                Color base = PALETTE[p.getIndex() % PALETTE.length];
                g2.setColor(base.darker());
                g2.translate(3, 3);   // shift the coordinate system 3px down-right
                g2.fill(path);        // shadow fill
                g2.translate(-3, -3); // restore the coordinate system

                // Draw the actual coloured fill on top of the shadow.
                g2.setColor(base);
                g2.fill(path);
            }
        }

        // ---------------------------------------------------------
        // PASS 2 — draw the piece outlines (borders) on top of all fills.
        //
        // BasicStroke(width, CAP_ROUND, JOIN_ROUND):
        //   width=1.8f  → line thickness in pixels
        //   CAP_ROUND   → rounded line endings (looks smoother)
        //   JOIN_ROUND  → rounded corners where lines meet
        // ---------------------------------------------------------
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(40, 40, 40)); // near-black border colour
        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                Placement p = puzzle.getPlacement(row, col);
                if (p == null) continue;
                Element e  = getEffective(p);
                Path2D path = buildPath(e, pixelX(col), pixelY(row));
                g2.draw(path); // draw only the outline, do not fill
            }
        }

        // ---------------------------------------------------------
        // PASS 3 — draw piece index numbers in the centre of each cell.
        //
        // FontMetrics lets us measure the rendered text size so we can
        // centre it precisely:
        //   fm.stringWidth(label)  → pixel width of this specific string
        //   fm.getAscent()         → height of characters above the baseline
        //
        // The "white halo" technique: draw the same text 8 times in white
        // (shifted ±1 pixel in every direction), then once in black on top.
        // This creates a 1-pixel white outline that makes the number readable
        // on any background colour.
        // ---------------------------------------------------------
        int fontSize = Math.max(11, CELL_SIZE / 7);
        g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        FontMetrics fm = g2.getFontMetrics();

        for (int row = 0; row < h; row++) {
            for (int col = 0; col < w; col++) {
                Placement p = puzzle.getPlacement(row, col);
                if (p == null) continue;

                // +1 because Placement stores 0-based index; spec uses 1-based.
                String label = String.valueOf(p.getIndex() + 1);

                // Centre the text inside the cell both horizontally and vertically.
                int tx = (int)(pixelX(col) + CELL_SIZE / 2f - fm.stringWidth(label) / 2f);
                int ty = (int)(pixelY(row) + CELL_SIZE / 2f + fm.getAscent()  / 2f - 3);

                // Draw white halo: 8 copies shifted by 1px in every direction.
                g2.setColor(Color.WHITE);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        if (dx != 0 || dy != 0)
                            g2.drawString(label, tx + dx, ty + dy);

                // Draw the actual black label on top of the halo.
                g2.setColor(Color.BLACK);
                g2.drawString(label, tx, ty);
            }
        }
    }

    // =========================================================================
    // PATH CONSTRUCTION — builds the jigsaw outline for one piece
    // =========================================================================

    // -------------------------------------------------------------------------
    // buildPath — assembles the closed Path2D outline for one piece.
    //
    // A Path2D is a sequence of drawing commands (moveTo, lineTo, curveTo, …).
    // We trace the piece boundary CLOCKWISE starting at the top-left corner:
    //   top side   (left → right)
    //   right side (top  → bottom)
    //   bottom side (right → left)
    //   left side  (bottom → top)
    //
    // Each side is either a straight line (F) or a bump/pit shape (B/P),
    // handled by drawSide().
    //
    // Parameters:
    //   e   — the piece (already rotated to its placed orientation)
    //   x,y — pixel coordinates of the TOP-LEFT corner of this cell
    // -------------------------------------------------------------------------
    private Path2D buildPath(Element e, float x, float y) {
        float s = CELL_SIZE;
        Path2D.Float path = new Path2D.Float();
        path.moveTo(x, y); // start at top-left corner

        // normalX/normalY = the outward unit normal for each side:
        //   top:    normal points UP    → (0, -1)
        //   right:  normal points RIGHT → (1,  0)
        //   bottom: normal points DOWN  → (0,  1)
        //   left:   normal points LEFT  → (-1, 0)
        drawSide(path, e.getTop(),    x,     y,     x + s, y,      0, -1); // top
        drawSide(path, e.getRight(),  x + s, y,     x + s, y + s,  1,  0); // right
        drawSide(path, e.getBottom(), x + s, y + s, x,     y + s,  0,  1); // bottom
        drawSide(path, e.getLeft(),   x,     y + s, x,     y,     -1,  0); // left

        path.closePath(); // draw the last segment back to the start point
        return path;
    }

    // -------------------------------------------------------------------------
    // drawSide — appends one side of a puzzle piece to the path.
    //
    // type = 'F' → straight line from (fromX,fromY) to (toX,toY).
    // type = 'B' → tab protruding in the +normal direction (outward).
    // type = 'P' → notch going in the -normal direction (inward).
    //
    // The bump/pit shape (for B and P) is made of:
    //   1. A straight segment from the previous corner to the NECK ENTRY point.
    //   2. A cubic Bézier curve from neck entry to the HEAD CENTRE (round top).
    //   3. A cubic Bézier curve (mirror of #2) from head centre to the NECK EXIT.
    //   4. A straight segment from neck exit to the next corner.
    //
    //   Visually (for a bump):
    //     ---- neck --\   /-- neck ----
    //                  | |
    //                  ---   (head)
    //
    // Parameters:
    //   path            — the path we are building (appended to, not replaced)
    //   type            — 'F', 'B', or 'P'
    //   fromX/Y, toX/Y  — start and end of this side (corners of the cell)
    //   normalX/Y       — outward unit normal (tells us which direction is "outside")
    // -------------------------------------------------------------------------
    private void drawSide(Path2D.Float path, char type,
                          float fromX, float fromY,
                          float toX,   float toY,
                          float normalX, float normalY) {

        // Flat side — just a straight line, nothing to compute.
        if (type == 'F') {
            path.lineTo(toX, toY);
            return;
        }

        // B = bump (outward, dir=+1); P = pit (inward, dir=-1)
        float dir   = (type == 'B') ? 1f : -1f;
        float t     = TAB;
        float neckW = t * 0.40f; // half-width of the narrow neck
        float headW = t * 0.52f; // half-width of the wider head

        // nx,ny = displacement from the base line to the head tip.
        // For a bump this is outward (+normal direction).
        // For a pit  this is inward  (-normal direction).
        float nx = normalX * dir * t;
        float ny = normalY * dir * t;

        // Mid-point of this side (where the tab is centred).
        float midX = (fromX + toX) / 2f;
        float midY = (fromY + toY) / 2f;

        // Unit tangent vector — points from "from" to "to" along the side.
        // Used to offset the neck entry/exit points along the side.
        float dx  = toX - fromX;
        float dy  = toY - fromY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float tux = dx / len; // tangent unit x
        float tuy = dy / len; // tangent unit y

        // Neck entry (nsX,nsY) and neck exit (neX,neY) — on the base line,
        // symmetrically placed either side of the midpoint.
        float nsX = midX - tux * neckW; // neck start (entry)
        float nsY = midY - tuy * neckW;
        float neX = midX + tux * neckW; // neck end (exit)
        float neY = midY + tuy * neckW;

        // Segment 1: straight line from the previous corner to the neck entry.
        path.lineTo(nsX, nsY);

        // Segment 2: Bézier curve from neck entry → head centre.
        //   cp1: pulled outward near the neck (creates the curved neck shoulder)
        //   cp2: at head height, left of centre (creates the round head left half)
        //   end: head centre
        path.curveTo(
            nsX + normalX * dir * t * 0.72f, nsY + normalY * dir * t * 0.72f, // cp1
            midX + nx - tux * headW,          midY + ny - tuy * headW,          // cp2
            midX + nx,                        midY + ny                          // end
        );

        // Segment 3: Bézier curve from head centre → neck exit (mirror of #2).
        //   cp1: at head height, right of centre
        //   cp2: pulled outward near the neck exit
        //   end: neck exit
        path.curveTo(
            midX + nx + tux * headW,          midY + ny + tuy * headW,          // cp1
            neX  + normalX * dir * t * 0.72f, neY  + normalY * dir * t * 0.72f, // cp2
            neX,                              neY                                 // end
        );

        // Segment 4: straight line from the neck exit to the next corner.
        path.lineTo(toX, toY);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    // -------------------------------------------------------------------------
    // getEffective — reconstructs the piece stored at this Placement,
    // with the stored rotation already applied.
    // Used every time we need the actual oriented piece for drawing.
    // -------------------------------------------------------------------------
    private Element getEffective(Placement p) {
        return puzzle.getList()
                     .get(p.getIndex())           // original piece (unrotated)
                     .rotate_element(p.getRotation()); // returns a rotated copy
    }

    // -------------------------------------------------------------------------
    // pixelX / pixelY — convert grid column/row indices to pixel coordinates.
    // PADDING offsets everything away from the window edge so bumps are not clipped.
    // -------------------------------------------------------------------------
    private float pixelX(int col) { return PADDING + col * CELL_SIZE; }
    private float pixelY(int row) { return PADDING + row * CELL_SIZE; }

    // =========================================================================
    // STATIC FACTORY — called from Puzzle.main()
    // =========================================================================

    // -------------------------------------------------------------------------
    // show — creates the JFrame window and displays the solved puzzle.
    //
    // SwingUtilities.invokeLater() schedules the window creation on the
    // "Event Dispatch Thread" (EDT) — the dedicated Swing UI thread.
    // All Swing operations MUST run on the EDT; calling them from the main
    // thread directly can cause subtle drawing bugs. invokeLater() is the
    // standard safe way to hand off work to the EDT.
    //
    // JFrame setup:
    //   DISPOSE_ON_CLOSE  → closing the window frees its resources but does
    //                        NOT exit the whole JVM (unlike EXIT_ON_CLOSE).
    //   pack()            → sizes the frame to exactly fit the panel's
    //                        preferred size (set in the constructor).
    //   setLocationRelativeTo(null) → centres the window on the screen.
    // -------------------------------------------------------------------------
    public static void show(final Puzzle puzzle) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Build the window title from puzzle dimensions.
                JFrame frame = new JFrame(
                    "Puzzle  " + puzzle.getWidth() + " \u00d7 " + puzzle.getHeight()
                    + "  (" + puzzle.getList().size() + " pieces)");

                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.getContentPane().add(new PuzzleDisplay(puzzle)); // add the canvas
                frame.pack();                        // size window to canvas
                frame.setLocationRelativeTo(null);   // centre on screen
                frame.setResizable(false);           // fixed size
                frame.setVisible(true);              // show it
            }
        });
    }
}
