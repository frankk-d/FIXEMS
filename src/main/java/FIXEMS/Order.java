package FIXEMS;

import quickfix.FieldNotFound;
import quickfix.field.*;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.OrderCancelReplaceRequest;
import quickfix.fix42.OrderCancelRequest;

public class Order {
    //COMPLETE FOR AI: write the specific variables here that are used in QuickFIX 4.2 for orders
    private String senderCompID;
    private String targetCompID;
    private String clOrdID;
    private String origClOrdID;
    private char handlInst;
    private String symbol;
    private char side;
    private char ordType;
    private double price;
    private double orderQty; //total orders
    private char timeInForce;
    private double cumQty;    // orders filled
    private double leavesQty; //orders that havn't been filled yet
    private long lastShares; //number of shares bought on the last fill
    private char ordStatus; // FIX OrdStatus (e.g., '0'=New) - Added for PROMPT 6
    private String ordID; // Added to store FIX OrderID (tag 37)
    private long entryTime;
    private boolean receivedOrder;
    private boolean receivedCancelRequest;
    private boolean receivedReplaceRequest;
    private double avgPx;
    private double stopPx;


    /**
     * COMPLETE FOR AI: A constructor for creating a new order with a NewOrderSingle
     * @param message FIX 4.2 message NewOrderSingle
     */
    public Order(NewOrderSingle message) throws FieldNotFound {
        this.clOrdID = message.getClOrdID().getValue();
        this.ordID = generateOrdID();
        // Extract values from header
        this.senderCompID = message.getHeader().getString(SenderCompID.FIELD);
        this.targetCompID = message.getHeader().getString(TargetCompID.FIELD);
        this.handlInst = message.getHandlInst().getValue();
        this.symbol = message.getSymbol().getValue();
        this.side = message.getSide().getValue();
        this.ordType = message.getOrdType().getValue();
        this.orderQty = message.getOrderQty().getValue();
        this.leavesQty = (long) message.getDouble(OrderQty.FIELD); //handwritten
        this.cumQty = 0; //handwritten
        this.lastShares = 0; //handwritten

        if (message.isSetField(Price.FIELD)) {
            this.price = message.getPrice().getValue();
        }
        if (message.isSetField(StopPx.FIELD)){
            this.stopPx = message.getStopPx().getValue();
        }

        if (message.isSetField(TimeInForce.FIELD)) {
            this.timeInForce = message.getTimeInForce().getValue();
        }
        // get entry time
        entryTime = System.currentTimeMillis();
    }

    /**
     * COMPLETE FOR AI: A constructor for creating a new order with a OrderCancelRequest
     * @param message FIX 4.2 message, OrderCancelRequest
     */
    public Order(OrderCancelRequest message) throws FieldNotFound {
        this.origClOrdID = message.getOrigClOrdID().getValue();
        this.ordID = generateOrdID();
        this.senderCompID = message.getHeader().getString(SenderCompID.FIELD);
        this.targetCompID = message.getHeader().getString(TargetCompID.FIELD);
        this.clOrdID = message.getClOrdID().getValue();
        this.symbol = message.getSymbol().getValue();
        this.side = message.getSide().getValue();
        this.leavesQty = 0; //handwritten
        this.cumQty = 0; //handwritten
        this.lastShares = 0; //handwritten
        // get entry time
        entryTime = System.currentTimeMillis();
    }

    /**
     * PROMPT 10: Write a constructor for creating a new order with a OrderCancelReplaceRequest. Store all the attributes inside of this object.
     * @param message FIX 4.2 message, OrderCancelReplaceRequest
     */
    public Order(OrderCancelReplaceRequest message) throws FieldNotFound {
        // Extract original and new client order IDs
        this.origClOrdID = message.getOrigClOrdID().getValue();
        this.clOrdID = message.getClOrdID().getValue();

        // Generate a unique order ID and set header fields
        this.ordID = generateOrdID();
        this.senderCompID = message.getHeader().getString(SenderCompID.FIELD);
        this.targetCompID = message.getHeader().getString(TargetCompID.FIELD);

        // Extract handling instruction and instrument details
        this.handlInst = message.getHandlInst().getValue();
        this.symbol = message.getSymbol().getValue();
        this.side = message.getSide().getValue();
        this.ordType = message.getOrdType().getValue();

        // Set order quantity and initialize fill-related quantities
        this.orderQty = message.getOrderQty().getValue();
        this.leavesQty = (long) message.getDouble(OrderQty.FIELD);
        this.cumQty = this.orderQty - this.leavesQty; // PROMPT 12c Handwritten
        this.lastShares = 0;

        // Handle optional price and time in force fields
        if (message.isSetField(Price.FIELD)) {
            this.price = message.getPrice().getValue();
        }
        if (message.isSetField(TimeInForce.FIELD)) {
            this.timeInForce = message.getTimeInForce().getValue();
        }
        if (message.isSetField(StopPx.FIELD)){
            this.stopPx = message.getStopPx().getValue();
        }
        // Record the entry time of the order
        this.entryTime = System.currentTimeMillis();
    }

    /**
     * COMPLETE FOR AI: Write a series of getter and setter methods for each of the variables present in a NewOrderSingle and OrderCancelRequest
     */
    public String getClOrdID() { return clOrdID; }
    public String getOrigClOrdID() { return origClOrdID; }
    public char getHandlInst() { return handlInst; }
    public String getSymbol() { return symbol; }
    public char getSide() { return side; }
    public char getOrdType() { return ordType; }
    public double getPrice() { return price; }
    public double getOrderQty() { return orderQty; } //change
    public char getTimeInForce() { return timeInForce; }
    public double getLeavesQty() {return leavesQty; } //handwritten
    public double getCumQty() {return cumQty;} //handwritten
    public long getLastShares() { return lastShares;} //handwritten
    public char getOrdStatus() {return ordStatus;}
    public String getOrdID() {return ordID;}
    public String getSenderCompID() { //HANDWRITTEN
        return senderCompID;
    }
    public long getEntryTime() {
        return entryTime;
    } //HANDWRITTEN
    public String getTargetCompID() { return targetCompID; } //HANDWRITTEN
    public boolean getReceivedOrder(){
        return receivedOrder;
    } //handwritten
    public boolean getReceivedCancelRequest() {return receivedCancelRequest; }; //handwritten
    public double getStopPx(){
        return stopPx;
    } //handwritten
    public boolean getReceivedReplaceRequest() {return receivedReplaceRequest; }; //handwritten
    public double getAvgPx() { return avgPx;} //HANDWRITTEN

    public void setClOrdID(String clOrdID) { this.clOrdID = clOrdID; }
    public void setOrigClOrdID(String origClOrdID) { this.origClOrdID = origClOrdID; }
    public void setHandlInst(char handlInst) { this.handlInst = handlInst; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setSide(char side) { this.side = side; }
    public void setOrdType(char ordType) { this.ordType = ordType; }
    public void setPrice(double price) { this.price = price; }
    public void setOrderQty(double orderQty) { this.orderQty = orderQty; }
    public void setTimeInForce(char timeInForce) { this.timeInForce = timeInForce; }
    public void setLeavesQty(double leavesQty) {this.leavesQty = leavesQty; } //handwritten
    public void setCumQty(double cumQty) {this.cumQty = cumQty; } //handwritten
    public void setLastShares(long lastShares) { this.lastShares = lastShares;} //handwritten
    public void setOrdStatus(char ordStatus) {this.ordStatus = ordStatus;}
    public void setOrdID(String ordID) {this.ordID = ordID;}
    public void setReceivedCancelRequest(boolean receivedCancelRequest){ this.receivedCancelRequest = receivedCancelRequest;} //handwritten
    public void setReceivedOrder(boolean receivedOrder){
        this.receivedOrder = receivedOrder;
    } //handwritten
    public void setAvgPx(double avgPx){
        this.avgPx = avgPx;
    } //handwritten
    public void setStopPx(double stopPx){
        this.stopPx = stopPx;
    } //handwritten

    public void setReceivedReplaceRequest(boolean req){ this.receivedReplaceRequest = req;} //handwritten

    /**
     * COMPLETE FOR AI: Write a function that prints the order details, returns void
     */
    public void printOrderDetails() {
        if (receivedOrder) {
            System.out.print("RECEIVED_");
        } else if (receivedCancelRequest) {
            System.out.print("CANCEL_REQ_");
        } else if (ordStatus == OrdStatus.NEW) {
            System.out.print("NEW_");
        } else if (ordStatus == OrdStatus.PARTIALLY_FILLED) {
            System.out.print("PARTIALLY_FILLED_");
        } else if (ordStatus == OrdStatus.FILLED) {
            System.out.print("FILLED_");
        } else if (ordStatus == OrdStatus.DONE_FOR_DAY) {
            System.out.print("DONE_FOR_DAY_");
        } else if (ordStatus == OrdStatus.CANCELED) {
            System.out.print("CANCELED_");
        } else if (ordStatus == OrdStatus.REPLACED) {
            System.out.print("REPLACED_");
        } else if (ordStatus == OrdStatus.PENDING_CANCEL) {
            System.out.print("PENDING_CANCEL_");
        } else if (ordStatus == OrdStatus.STOPPED) {
            System.out.print("STOPPED_");
        } else if (ordStatus == OrdStatus.REJECTED) {
            System.out.print("REJECTED_");
        } else if (ordStatus == OrdStatus.SUSPENDED) {
            System.out.print("SUSPENDED_");
        } else if (ordStatus == OrdStatus.PENDING_NEW) {
            System.out.print("PENDING_NEW_");
        } else if (ordStatus == OrdStatus.CALCULATED) {
            System.out.print("CALCULATED_");
        } else if (ordStatus == OrdStatus.EXPIRED) {
            System.out.print("EXPIRED_");
        } else if (ordStatus == OrdStatus.ACCEPTED_FOR_BIDDING) {
            System.out.print("ACCEPTED_FOR_BIDDING_");
        } else if (ordStatus == OrdStatus.PENDING_REPLACE) {
            System.out.print("PENDING_REPLACE_");
        } else {
            System.out.print("UNKNOWN_STATUS_");
        }

        System.out.print(String.format("Order Details: ClOrdID=%s, Symbol=%s, SenderCompID=%s, TargetCompID=%s, Side=%s, OrdType=%s, Price=%s, OrderQty=%s, TimeInForce=%s, EntryTime=%s",
                getClOrdID(), getSymbol(), getSenderCompID(), getTargetCompID(), getSide(), getOrdType(), getPrice(), getOrderQty(), getTimeInForce(), getEntryTime()));
        System.out.println();
    }

    public String generateOrdID(){
        return "ORDER_" + System.currentTimeMillis();

    }

}