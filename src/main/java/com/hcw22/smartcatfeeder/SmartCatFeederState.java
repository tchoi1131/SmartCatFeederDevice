/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hcw22.smartcatfeeder;

/**
 *
 * @author Tom Wong
 */
public class SmartCatFeederState {
    public State state = new State();

    public static class State {
        public Document reported = new Document();
        public Document desired = new Document();
    }

    public static class Document {
        public String date = "1900-01-01";
        public double catWeight = 0.0;
        public double foodWeight = 0.0;
    }
}
