package FIXEMS;


public class ExecutionOrder {
    /**
     * PROMPT 4: Create private variables that store the attributes of an ExecutionReport
     */
    private Order order; // Reference to the original order
    private double avgPx;      // Average price of fills (FIX: AvgPx)
    private double cumQty;     // Cumulative quantity filled (FIX: CumQty)
    private double lastPx;     // Price of the last fill (FIX: LastPx)
    private double lastQty;    // Quantity of the last fill (FIX: LastQty)
    private String execId;     // Unique execution ID (FIX: ExecID)
    private char execType;     // Execution type (FIX: ExecType e.g., '0'=New, 'F'=Trade)
    private char ordStatus;    // Order status (FIX: OrdStatus e.g., '0'=New, '1'=Partially filled)
    private double leavesQty;  // Remaining quantity to execute (FIX: LeavesQty)
    private char execTransType; // FIX ExecTransType (e.g., '0'=New, '1'=Cancel) - Added for PROMPT 6
    private long lastShares;


    /**
     * Constructor initializes execution attributes based on the linked Order.
     * @param order The Order object linked to this execution report.
     */
    public ExecutionOrder(Order order) {
        this.order = order;
        // Initialize execution-specific fields to default values
        this.avgPx = 0.0;
        this.cumQty = 0.0;
        this.lastPx = 0.0;
        this.lastQty = 0.0;
        this.lastShares = order.getLastShares();
        this.execId = ""; // To be populated externally
        this.execType = '0'; // '0' = New (per FIX 4.2)
        this.ordStatus = '0'; // '0' = New
        this.leavesQty = order.getOrderQty(); // Initial leaves = total order quantity
    }

    /**
     * PROMPT 4: Getters and setters for ExecutionReport attributes
     */

    public double getAvgPx() {
        return avgPx;
    }

    public void setAvgPx(double avgPx) {
        this.avgPx = avgPx;
    }

    public double getCumQty() {
        return cumQty;
    }

    public void setCumQty(double cumQty) {
        this.cumQty = cumQty;
    }

    public double getLastPx() {
        return lastPx;
    }

    public void setLastPx(double lastPx) {
        this.lastPx = lastPx;
    }

    public double getLastQty() {
        return lastQty;
    }

    public void setLastQty(double lastQty) {
        this.lastQty = lastQty;
    }

    public String getExecId() {
        return execId;
    }

    public void setExecId(String execId) {
        this.execId = execId;
    }

    public char getExecType() {
        return execType;
    }

    public void setExecType(char execType) {
        this.execType = execType;
    }

    public char getOrdStatus() {
        return ordStatus;
    }

    public void setOrdStatus(char ordStatus) {
        this.ordStatus = ordStatus;
    }

    public double getLeavesQty() {
        return leavesQty;
    }

    public void setLeavesQty(double leavesQty) {
        this.leavesQty = leavesQty;
    }

    public Order getOrder() {return order;}

    public char getExecTransType() {
        return execTransType;
    }

    public void setExecTransType(char execTransType) {
        this.execTransType = execTransType;
    }
    public long getLastShares() { return lastShares; }
    public void setLastShares(long lastShares) { this.lastShares = lastShares; }
    /**
     * This function generates an ExecutionID
     * @return
     */
    public String generateExecID(){
        return "EXEC_" + System.currentTimeMillis(); //handwritten
    }

}