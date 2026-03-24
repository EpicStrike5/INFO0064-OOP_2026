// Required package declaration — see Element.java for explanation.
package be.uliege.montefiore.oop;

// =============================================================================
// Placement — a tiny record that describes what sits in one cell of the grid.
//
// Once the solver decides that piece #i should go at grid position (row, col)
// rotated by r quarter-turns, it stores Placement(i, r) at grid[row][col].
//
// This class has NO logic — it is purely a data container (sometimes called
// a "value object" or "DTO" in OOP). It answers two questions:
//   - Which original piece? → getIndex()
//   - How many times was it rotated clockwise? → getRotation()
//
// The output format required by the spec is exactly these two numbers:
//   "<1-based index> <rotation>"    e.g.  "3 2"
// Puzzle.main() reads them from each Placement when printing the solution.
// =============================================================================
public class Placement {

    // -------------------------------------------------------------------------
    // index    — 0-based position of the piece in puzzle.getList().
    //            +1 when printing, because the spec numbers pieces from 1.
    // rotation — number of clockwise 90° turns applied (0, 1, 2 or 3).
    // -------------------------------------------------------------------------
    private int index;
    private int rotation;

    // -------------------------------------------------------------------------
    // Constructor — called by fill_outside and fill_inside in Functions.java
    // each time a valid piece placement is found.
    // -------------------------------------------------------------------------
    public Placement(int index, int rotation) {
        this.index    = index;
        this.rotation = rotation;
    }

    // -------------------------------------------------------------------------
    // Getters and setters — standard accessors.
    // BUG FIXED: setRotation was originally spelled `seRotation` (missing 't'),
    // which would cause a compile error for any caller using the correct name.
    // -------------------------------------------------------------------------
    public int  getIndex()              { return this.index; }
    public void setIndex(int index)     { this.index = index; }

    public int  getRotation()           { return this.rotation; }
    public void setRotation(int rotation) { this.rotation = rotation; }
}
