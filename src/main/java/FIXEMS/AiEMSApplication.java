package FIXEMS;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.ExecutionReport;

public class AiEMSApplication extends MessageCracker implements Application {
    public OrderList orders; //OWN CODE
    public SessionID sessionID; //OWN CODE
    private boolean logOn = false;

    @Override
    public void onCreate(SessionID sessionId) {
        sessionID = sessionId;
        System.out.printf("[Session] Created: %s%n", sessionId);
        orders = new OrderList();
    }

    @Override
    public void onLogon(SessionID sessionId) {
        System.out.printf("[Session] Logon: %s%n", sessionId);
        logOn = true;
    }

    @Override
    public void onLogout(SessionID sessionId) {
        System.out.printf("[Session] Logout: %s%n", sessionId);
        logOn = false;
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        // Not used in this minimal implementation
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        // Not used in this minimal implementation
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        // Not used in this minimal implementation
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) {
        try {
            crack(message, sessionId); // Route message to typed handlers
        } catch (Exception e) {
            System.err.printf("[ERROR] Failed to process message: %s%n", e.getMessage());
        }
    }

    // Handler for FIX 4.2 NewOrderSingle messages
    public void onMessage(quickfix.fix42.NewOrderSingle message, SessionID sessionID) {
        System.out.printf("[Order] Received NewOrderSingle: %s%n", message);

        // COMPLETE FOR AI: Add the order to the function
        try {
            Order order = new Order(message);
            order.setReceivedOrder(true); //handwritten
            order.setReceivedCancelRequest(false); //handwritten
            order.setReceivedReplaceRequest(false);
            boolean toFill = true;

            if (message.isSetField(TimeInForce.FIELD)) {
                char tif = message.getTimeInForce().getValue();
                toFill = (tif == TimeInForce.IMMEDIATE_OR_CANCEL);
            }

            orders.add(order, toFill);
            orders.printOrderList();

        } catch (FieldNotFound e) {
            System.err.println("Error processing order: " + e.getMessage());
        }
    }


    /**
     * PROMPT 3 COMPLETE FOR AI: Create a function that handles OrderCancelRequest messages. Function purpose:
     * - creates a new order for the OrderCancelRequest
     * - stores the order inside of the orders ArrayList
     * - print the order
     * @param message quickfix.fix42.OrderCancelRequest message
     * @param sessionID the session ID of the order
     */
    public void onMessage(quickfix.fix42.OrderCancelRequest message, SessionID sessionID) {
        System.out.printf("[Order] Received OrderCancelRequest: %s%n", message);

        try {
            Order order = new Order(message);
            // Cancel requests are tracked but not marked for immediate execution
            order.setReceivedOrder(false); //handwritten
            order.setReceivedCancelRequest(true); //handwritten
            order.setReceivedReplaceRequest(false);
            orders.add(order, false);
            orders.printOrderList();
        } catch (FieldNotFound e) {
            System.err.println("Error processing cancel request: " + e.getMessage());
        }
    }

    public void onMessage(quickfix.fix42.OrderCancelReplaceRequest message, SessionID sessionID) {
        System.out.printf("[Order] Received OrderCancelReplaceRequest: %s%n", message);
        try {
            Order order = new Order(message);
            // Cancel requests are tracked but not marked for immediate execution
            order.setReceivedOrder(false); //handwritten
            order.setReceivedCancelRequest(false); //handwritten
            order.setReceivedReplaceRequest(true);
            orders.add(order, false);
            orders.printOrderList();
        } catch (FieldNotFound e) {
            System.err.println("Error processing OrderCancelReplaceRequest: " + e.getMessage());
        }
    }

    /**
     * PROMPT 8 COMPLETE FOR AI: Write a function that acknowledges an order.
     *  - Creates an ExecutionOrder object and stores the attributes of the order into the object
     *  - calls another function - sendExecution(ExecutionOrder executionOrder) to send the executionReport to the OMS system
     * @param order
     */
    public void createAcknowledgeExecutionOrder(Order order){
        // Create ExecutionOrder linked to the original order
        ExecutionOrder ack = new ExecutionOrder(order);
        order.setReceivedOrder(false); //handwritten

        // Set the order's status to NEW to reflect acknowledgment
        order.setOrdStatus(OrdStatus.NEW);
        order.setReceivedReplaceRequest(false);

        // Configure FIX 4.2 execution attributes for acknowledgment
        ack.setExecType(ExecType.NEW);          // '0' = New (FIX 4.2)
        ack.setExecTransType(ExecTransType.NEW); // '0' = New transaction

        // Explicitly set execution metrics (redundant for safety, constructor initializes to 0)
        ack.setCumQty(0.0);    // No fills yet
        ack.setLeavesQty(order.getOrderQty()); // Full quantity remains
        ack.setAvgPx(0.0);      // No average price

        // Generate unique execution ID for tracking
        ack.setExecId(ack.generateExecID());

        // Transmit the execution report to the OMS
        sendExecution(ack);
    }


    /**
     * PROMPT 5 COMPLETE FOR AI: Create a function named createCancelExecutionOrder. This function:
     * - creates an ExecutionOrder object
     * - stores the attributes of the order into the object.
     * - Then calls another function - sendExecution(ExecutionOrder executionOrder) to send the executionReport to the OMS system
     * @param order Order object that it receives.
     */
    public void createCancelExecutionOrder(Order order) {
        if (order==null){
            throw new IllegalStateException("Can't be null");
        }

        // Create ExecutionOrder linked to the original order
        ExecutionOrder cancel = new ExecutionOrder(order);

        order.setReceivedOrder(false); //handwritten
        order.setReceivedCancelRequest(false); //handwritten
        order.setReceivedReplaceRequest(false);

        /**
         * COPIED AND PASTED FROM createCancelReplaceExecutionOrder
         */
        //get original client order id
        String origClOrdID = order.getOrigClOrdID();
        boolean origClOrdIDExist = true;
        if (origClOrdID == null || origClOrdID.isEmpty()) {
            origClOrdID = order.getClOrdID();
            origClOrdIDExist = false;
        }
        Order originalOrder = null;
        if (origClOrdIDExist) {
            for (int i = 0; i < orders.getOrderList().size(); i++) {
                if (orders.getOrderList().get(i).getClOrdID().equals(order.getOrigClOrdID())) {
                    originalOrder = orders.getOrderList().get(i);
                }
            }
        } else {
            originalOrder = order;
        }
        if (originalOrder == null){
            throw new IllegalStateException("Original order not found");
        }
        if (originalOrder.getOrdStatus() == OrdStatus.FILLED){
            throw new IllegalStateException("cannot be filled");
        }
        //HANDWRITTEN
        originalOrder.setOrdStatus(OrdStatus.CANCELED);
        originalOrder.setReceivedOrder(false);
        originalOrder.setLeavesQty(0.0);
        originalOrder.setAvgPx(0.0); //handwritten
        originalOrder.setLeavesQty(0);          // Active quantity → 0
        originalOrder.setLastShares(0);          // No new fills
        if (originalOrder.getCumQty() == 0) {   // Only reset avgPx if never filled
            originalOrder.setAvgPx(0.0);
        }

        // Configure FIX 4.2 execution attributes for cancellation
        cancel.setExecType(ExecType.CANCELED ); // '4' = ExecType.CANCELED (FIX 4.2)
        cancel.setExecTransType(ExecTransType.NEW);
        cancel.setOrdStatus(OrdStatus.CANCELED); // '4' = OrdStatus.CANCELED
        cancel.getOrder().setOrdStatus(OrdStatus.CANCELED); // '4' = OrdStatus.CANCELED
        cancel.setExecId(cancel.generateExecID()); // Unique execution ID
        cancel.setLeavesQty(0.0); // Canceled orders leave no remaining quantity
        cancel.setCumQty(order.getOrderQty()); //handwrite
        cancel.setAvgPx(0.0); //handwritten

        // Forward the execution report to the OMS
        sendExecution(cancel);

    }

    /**
     * PROMPT 9 COMPLETE FOR AI: write a function that creates an execution order to fill an order. This function
     * - creates an ExecutionOrder object
     * - stores the attributes of the order into the object
     * - does the calculations for a fill order, as well as for the variables of the fill order
     * - calls another function - sendExecution to send the ExecutionReport to the OMS system.
     * @param order Order object that it receives
     * @param amountToFill amount of orders to fill
     * @param priceToFill the price of the selected orders
     */
    public void createFillExecutionOrder(Order order, long amountToFill, double priceToFill){
        try{
            //consider edge cases:
            if (order == null) {
                throw new IllegalArgumentException("Order cannot be null.");
            }
            if (amountToFill < 0) {
                throw new IllegalArgumentException("Fill amount must be positive.");
            }
            if (priceToFill < 0.0) {
                throw new IllegalArgumentException("Fill price must be positive.");
            }
            if (order.getReceivedOrder()){
                throw new IllegalArgumentException("Order must be acknowledged first.");
            }
            char currentOrdStatus = order.getOrdStatus();
            if (currentOrdStatus == OrdStatus.FILLED) {
                throw new IllegalStateException("Order is already fully filled.");
            }
            if (currentOrdStatus == OrdStatus.CANCELED || currentOrdStatus == OrdStatus.REJECTED) {
                throw new IllegalStateException("Order is canceled/rejected and cannot be filled.");
            }

            //grab attribute values
            double currentLeavesQty = order.getLeavesQty(); //
            double currentCumQty = order.getCumQty(); //get the current orders that are filled
            double orderQty = order.getOrderQty(); //total quantity ordered

            // get the amount of orders left to fill still
            if (amountToFill > currentLeavesQty){
                throw new IllegalArgumentException(
                        String.format("Fill amount (%d) exceeds available quantity (%d).",
                                amountToFill, currentLeavesQty)
                );
            }

            //new amount of orders filled
            double newCumQty = currentCumQty + amountToFill;
            double newLeavesQty = currentLeavesQty - amountToFill;

            //adjust fill quantities
            if (newCumQty > orderQty) {
                throw new ArithmeticException("Cumulative quantity exceeds original order quantity.");
            }


            //adjust price quantities
            double currentAvgPx = order.getAvgPx();
            double newAvgPx = (currentAvgPx * (newCumQty - amountToFill) + priceToFill * amountToFill) / newCumQty; //calculate average price

            //change statuses using variables for cleaner adjustment
            char execType; //the type of exec report
            char ordStatus; //the status of the order
            if(newLeavesQty > 0){
                execType = ExecType.PARTIAL_FILL;
                ordStatus = OrdStatus.PARTIALLY_FILLED;
            } else{
                order.setReceivedOrder(false);
                execType = ExecType.FILL;
                ordStatus = OrdStatus.FILLED;
            }

            //set execution orders
            order.setOrdStatus(ordStatus);
            order.setLeavesQty(newLeavesQty);
            order.setCumQty(newCumQty);
            order.setAvgPx(newAvgPx);
            order.setLastShares(amountToFill);

            // Validate fill quantity
            ExecutionOrder fill = new ExecutionOrder(order);
            //sets executionOrder values
            fill.setExecTransType(ExecTransType.NEW);
            fill.setOrdStatus(ordStatus);
            fill.setExecType(execType);
            fill.setLeavesQty(newLeavesQty);    //update new amount of orders left to fill
            fill.setCumQty(newCumQty);          //update new amount of orders currently filled
            fill.setAvgPx(newAvgPx);            //update new average price of orders
            fill.setLastShares(amountToFill);  // Required for FIX execution reports
            fill.setLastPx(priceToFill);    // Required for FIX execution reports

            fill.setExecId(fill.generateExecID()); // Unique execution ID

            sendExecution(fill);
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to process fill: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * PROMPT 12: Write the createCancelReplaceExecutionOrder fynction that creates a replace order based on the information received
     * The function should:
     *    - Manage edge cases
     *    - Create an ExecutionOrder object to represent the attributes in a replace ExecutionReport
     *    - Store all the attributes in the order inside of the ExecutionOrder object.
     *    - Send over sendExecution() to send the executionOrder
     *
     * @param order the order that was received
     */
    public void createCancelReplaceExecutionOrder(Order order) {
        try {
            // Edge case: Null order
            if (order == null) {
                throw new IllegalArgumentException("Order cannot be null.");
            }

            //get original client order id
            String origClOrdID = order.getOrigClOrdID();
            if (origClOrdID == null || origClOrdID.isEmpty()) {
                throw new IllegalStateException("Replace request must reference an OrigClOrdID.");
            }

            Order originalOrder = null;
            for (int i = 0; i < orders.getOrderList().size(); i++){
                if (orders.getOrderList().get(i).getClOrdID().equals(order.getOrigClOrdID())){
                    originalOrder = orders.getOrderList().get(i);
                }
            }

            if (originalOrder == null) {
                throw new IllegalStateException("Original order not found for OrigClOrdID: " + origClOrdID);
            }

            originalOrder.setOrdStatus(OrdStatus.REPLACED);
            originalOrder.setReceivedOrder(false);

            // Edge case: Check if order is in a replaceable state
            char currentStatus = order.getOrdStatus();
            if (currentStatus == OrdStatus.FILLED) {
                throw new IllegalStateException("Filled orders cannot be replaced.");
            }
            if (currentStatus == OrdStatus.CANCELED || currentStatus == OrdStatus.REJECTED) {
                throw new IllegalStateException("Canceled/rejected orders cannot be replaced.");
            }

            //set order statuses
            order.setOrdStatus(OrdStatus.REPLACED);
            order.setReceivedOrder(true);
            order.setReceivedCancelRequest(false);
            order.setCumQty(originalOrder.getCumQty()); //Prompt 12b handwritten
            order.setLeavesQty(order.getOrderQty() - originalOrder.getCumQty()); //prompt 12b handwritten

            // Initialize ExecutionOrder with order data
            ExecutionOrder replaceExec = new ExecutionOrder(order);

            // Configure FIX 4.2 execution attributes for replacement
            replaceExec.setExecType(ExecType.REPLACE);      // '5' = Replaced (FIX 4.2)
            //HANDWRITTEN
            replaceExec.setOrdStatus(OrdStatus.REPLACED);
            replaceExec.setExecTransType(ExecTransType.NEW);  // '0' = New transaction
            replaceExec.setExecId(replaceExec.generateExecID()); // Unique execution ID

            // Update leaves quantity to reflect new order quantity after replacement
            replaceExec.setLeavesQty(order.getLeavesQty());

            // Carry forward existing fill metrics (unchanged by replacement)
            replaceExec.setCumQty(order.getCumQty());
            replaceExec.setAvgPx(order.getAvgPx());
            replaceExec.setLastShares(order.getLastShares());    // No fill occurs during replacement
            replaceExec.setLastPx(0.0);      // No fill price

            // Transmit the execution report
            sendExecution(replaceExec);
        } catch (Exception e) {
            System.err.println("[ERROR] Replace failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * PROMPT 6: Find the missing
     * @param executionOrder
     */
    private void sendExecution(ExecutionOrder executionOrder) {
        try {
            // Extract key fields from ExecutionOrder (assumes getters exist)
            //handwritten

            ExecutionReport execReport = new ExecutionReport(
                    new OrderID(executionOrder.getOrder().getOrdID()),
                    new ExecID(executionOrder.getExecId()),
                    new ExecTransType(executionOrder.getExecTransType()),
                    new ExecType(executionOrder.getExecType()),
                    new OrdStatus(executionOrder.getOrder().getOrdStatus()),
                    new Symbol(executionOrder.getOrder().getSymbol()),
                    new Side(executionOrder.getOrder().getSide()),
                    new LeavesQty(executionOrder.getLeavesQty()),
                    new CumQty(executionOrder.getCumQty()),
                    new AvgPx(executionOrder.getAvgPx())
            );

            // Additional fields for cancel acknowledgment
            execReport.set(new ClOrdID(executionOrder.getOrder().getClOrdID()));
            execReport.set(new OrigClOrdID(executionOrder.getOrder().getClOrdID()));
            execReport.set(new LastShares(executionOrder.getLastShares()));
            execReport.set(new LastPx(executionOrder.getLastPx()));

            execReport.set(new OrderQty(executionOrder.getOrder().getOrderQty())); //HANDWRITTEN
            execReport.set(new Price(executionOrder.getOrder().getPrice())); //HANDWRITTEN

            //added headers
            Message.Header header = execReport.getHeader(); //handwritten
            header.setField(new SenderCompID(executionOrder.getOrder().getTargetCompID())); //handwritten
            header.setField(new TargetCompID(executionOrder.getOrder().getSenderCompID())); //handwritten

            // Send via QuickFIX session
            Session.sendToTarget(execReport,sessionID);

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to send execution report: " + e.getMessage());
        }
    }

    public OrderList getOrders(){return orders;}
    public boolean getLogOn(){return logOn;}

    public String getConnectionName() {
        if (sessionID == null) {
            return "[No Active Session]";
        }
        return sessionID.getTargetCompID();
    }
}