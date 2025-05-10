package FIXEMS;

import quickfix.*;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AiEMS {

    public static void main(String[] args) {
        try {
            // Initialize FIX components
            AiEMSApplication application = new AiEMSApplication();
            SessionSettings settings = new SessionSettings("ems-settings.cfg");
            MessageStoreFactory storeFactory = new FileStoreFactory(settings);
            LogFactory logFactory = new FileLogFactory(settings);
            MessageFactory messageFactory = new DefaultMessageFactory();

            // Start FIX acceptor
            Acceptor acceptor = new SocketAcceptor(
                    application, storeFactory, settings, logFactory, messageFactory
            );

            System.out.println("Starting EMS FIX Acceptor...");
            acceptor.start();

            // Launch the GUI
            SwingUtilities.invokeLater(() -> {
                EMSGUI gui = new EMSGUI(application);
                gui.setVisible(true);
            });


            /**
             * PROMPT 7: Write a buffered reader input loop. This is what it will do:
             *  - outputs the list of orders numbered
             *  - prompt the user to input with a format
             *  - if the user inputs "cancel [order number]", run createCancelExecutionOrder function
             *  - after every single time the system receives an order, print out the total number of orders numbered.
             */
            // PROMPT 7 Implementation
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Welcome to the manual EMS system.");
            System.out.println(("cancel [order index] cancels order, acknowledge [order index] acknowledges the order"));
            System.out.println("fill [order index] [ordQty] [price] fills the order");
            System.out.println("replace [order index] replaces order");

            while(true) {
                OrderList orders = application.getOrders();
                orders.printOrderList();

                String[] fullInput;
                String selection;
                int orderNumber;

                String input = br.readLine();

                fullInput = input.split(" ");
                selection = fullInput[0];
                orderNumber = Integer.parseInt(fullInput[1]);

                if (selection.equals("cancel")) {
                    application.createCancelExecutionOrder(orders.getOrderList().get(orderNumber));
                } else if (selection.equals("acknowledge")) {
                    application.createAcknowledgeExecutionOrder(orders.getOrderList().get(orderNumber));
                } else if (selection.equals("fill")) {
                    long amountToFill = Long.parseLong(fullInput[2]);
                    double priceToFill = Double.parseDouble(fullInput[3]);
                    application.createFillExecutionOrder(orders.getOrderList().get(orderNumber), amountToFill, priceToFill);
                } else if (selection.equals("replace")){
                    application.createCancelReplaceExecutionOrder(orders.getOrderList().get(orderNumber));
                }else if (input.equals("break")){
                    break;
                }
            }

            acceptor.stop();
            System.out.println("EMS FIX Acceptor stopped.");
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}