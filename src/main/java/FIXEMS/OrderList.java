package FIXEMS;

import java.util.ArrayList;

public class OrderList {
    ArrayList<Order> orders = new ArrayList<>();
    private ArrayList<Order> ordersToFill = new ArrayList<>();

    public OrderList(){}

    /**
     * COMPLETE FOR AI: write function add
     * @param order Order object, with the order to add to the list of orders
     * @param toFill boolean variable - whether or not the order needs to be filled or not. If true, add the order to another arraylist named ordersToFill
     */

    public void add(Order order, boolean toFill){
        orders.add(order);
        if (toFill){
            ordersToFill.add(order);
        }
    }

    /**
     * COMPLETE FOR AI: write a function that prints the current order list called printOrderList()
     */
    public void printOrderList(){
        for (Order order: orders){
            order.printOrderDetails();
        }
    }
    public ArrayList<Order> getOrderList(){
        return orders;
    }
}