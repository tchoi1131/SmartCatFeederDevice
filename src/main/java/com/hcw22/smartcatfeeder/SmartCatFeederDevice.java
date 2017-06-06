/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hcw22.smartcatfeeder;

import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcw22.smartcatfeeder.SystemConfig.KeyStorePasswordPair;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.*;
import org.jdatepicker.impl.*;
import org.json.JSONObject;

/**
 *
 * @author Tom Wong
 */
public class SmartCatFeederDevice extends javax.swing.JFrame {

    public class DateLabelFormatter extends AbstractFormatter {

        private SimpleDateFormat dateFormatter = new SimpleDateFormat(SystemConfig.DATE_PATTERN);

        @Override
        public Object stringToValue(String text) throws ParseException {
            return dateFormatter.parseObject(text);
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value != null) {
                Calendar cal = (Calendar) value;
                return dateFormatter.format(cal.getTime());
            }

            return "";
        }
    }
    
    /**
     * Creates new form SmartCatFeederDevice
     */
    public SmartCatFeederDevice() {
        initComponents();
        
        thingNameLbl = new JLabel();
        thingNameTxtFld = new JTextField();
        connectBtn = new JButton("Connect");
        foodAmountLbl = new JLabel();
        foodAmountSpnr = new JSpinner();
        feedBtn = new JButton("Feed");
        dateLbl = new JLabel();
        catWeightLbl = new JLabel();
        catWeightSpnr = new JSpinner();
        foodWeightLbl = new JLabel();
        foodWeightSpnr = new JSpinner();
        sendDataBtn = new JButton("Send");
        
        thingNameLbl.setText("Device/Thing Name: ");
        thingNameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        connectionPanel.add(thingNameLbl);
        
        thingNameTxtFld.setText(SystemConfig.DEFAULT_THING_NAME);
        connectionPanel.add(thingNameTxtFld);
        
        connectBtn.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                if(connectBtn.getText() == "Connect"){
                    try {
                        connectAWSIoTDevice(thingNameTxtFld.getText());
                    } catch (IOException ex) {
                        Logger.getLogger(SmartCatFeederDevice.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    connectBtn.setText("Disconnect");
                }
                else{
                    disconnectAWSIoTDevice();
                    connectBtn.setText("Connect");
                }
            }
        });
        
        connectionPanel.add(connectBtn);
        
        foodAmountLbl.setText("Amount of Food: ");
        foodAmountLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        feedPanel.add(foodAmountLbl);
        
        foodAmountSpnr.setModel(new SpinnerNumberModel(10.0, 0.0, 100.0, 0.1));
        feedPanel.add(foodAmountSpnr);
        
        feedBtn.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                try {
                    feed((double)foodAmountSpnr.getValue());
                } catch (IOException ex) {
                    Logger.getLogger(SmartCatFeederDevice.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        feedPanel.add(feedBtn);
        
        dateLbl.setText("Date: ");
        dateLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        dataPanel.add(dateLbl);
        
        UtilDateModel model = new UtilDateModel();
        model.setValue(Calendar.getInstance().getTime());
        Properties p = new Properties();
        p.put("text.today", "Today");
        p.put("text.month", "Month");
        p.put("text.year", "Year");
        JDatePanelImpl datePanel = new JDatePanelImpl(model,p);
        datePicker = new JDatePickerImpl(datePanel,new DateLabelFormatter());
        
        dataPanel.add(datePicker);

        catWeightLbl.setText("Cat Weight: ");
        catWeightLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        dataPanel.add(catWeightLbl);
        
        catWeightSpnr.setModel(new SpinnerNumberModel(3.0, 0.0, 100.0, 0.1));
        dataPanel.add(catWeightSpnr);

        foodWeightLbl.setText("Food Weight: ");
        foodWeightLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        
        dataPanel.add(foodWeightLbl);
        
        foodWeightSpnr.setModel(new SpinnerNumberModel(3.0, 0.0, 100.0, 0.1));
        dataPanel.add(foodWeightSpnr);
        
        dataPanel.add(new JLabel());
        
        sendDataBtn.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                try {
                    setFeederShadowState();
                } catch (JsonProcessingException ex) {
                    Logger.getLogger(SmartCatFeederDevice.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParseException ex) {
                    Logger.getLogger(SmartCatFeederDevice.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        dataPanel.add(sendDataBtn);

        pack();
        
        initIoTMQTTClient();
    }
    
    private void feed(double amount) throws IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        int dateValue   = datePicker.getModel().getYear() * 10000
                        + (datePicker.getModel().getMonth() + 1) * 100
                        + datePicker.getModel().getDay();
        
        feederState.state.desired.date = dateValue;
        feederState.state.desired.mode = 0;
        feederState.state.desired.addFoodWeight = 0;
        feederState.state.desired.catWeight = (double)catWeightSpnr.getValue();
        feederState.state.desired.foodWeight = (double)foodWeightSpnr.getValue();
        
        feederState.state.reported.date = dateValue;
        feederState.state.reported.mode = 0;
        feederState.state.reported.addFoodWeight = 0;
        feederState.state.reported.catWeight = (double)catWeightSpnr.getValue();
        feederState.state.reported.foodWeight = (double)foodWeightSpnr.getValue();
        String jsonState = objectMapper.writeValueAsString(feederState);

        try {
            // Send updated document to the shadow
            //device = new AWSIotDevice(thingNameTxtFld.getText());
            //awsIotClient.attach(device);
            awsIotClient.publish(SystemConfig.CAT_FEEDER_UPDATE_TOPIC, AWSIotQos.QOS0, jsonState);
            //device.update(jsonState);
        } catch (AWSIotException e) {
            System.out.println(System.currentTimeMillis() + ": update failed for " + jsonState);
        }
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(SmartCatFeederDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        double updatedFoodWeight = (double)foodWeightSpnr.getValue() + amount;
        
        feederState.state.desired.date = dateValue;
        feederState.state.desired.mode = 1;
        feederState.state.desired.addFoodWeight = amount;
        feederState.state.desired.catWeight = (double)catWeightSpnr.getValue();
        feederState.state.desired.foodWeight = updatedFoodWeight;
        
        feederState.state.reported.date = dateValue;
        feederState.state.reported.mode = 1;
        feederState.state.reported.addFoodWeight = amount;
        feederState.state.reported.catWeight = (double)catWeightSpnr.getValue();
        feederState.state.reported.foodWeight = updatedFoodWeight;
        jsonState = objectMapper.writeValueAsString(feederState);

        try {
            // Send updated document to the shadow
            awsIotClient.publish(SystemConfig.CAT_FEEDER_UPDATE_TOPIC, jsonState);
            //device.update(jsonState);
        } catch (AWSIotException e) {
            System.out.println(System.currentTimeMillis() + ": update failed for " + jsonState);
        }
        
        foodWeightSpnr.setValue(updatedFoodWeight);
    }
    
    private void getFeederShadowState() throws IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String shadowState = null;
            
        try {
            shadowState = device.get();
            feederState = objectMapper.readValue(shadowState, SmartCatFeederState.class);
        } catch (AWSIotException ex) {
                System.out.println(System.currentTimeMillis() + ": get failed for " + shadowState);
                feederState = new SmartCatFeederState();
        }
    }
    
    private void setFeederShadowState() throws JsonProcessingException, ParseException{
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        int dateValue   = datePicker.getModel().getYear() * 10000
                        + (datePicker.getModel().getMonth() + 1) * 100
                        + datePicker.getModel().getDay();

        feederState.state.desired.date = dateValue;
        feederState.state.desired.mode = 0;
        feederState.state.desired.catWeight = (double)catWeightSpnr.getValue();
        feederState.state.desired.foodWeight = (double)foodWeightSpnr.getValue();
        
        feederState.state.reported.date = dateValue;
        feederState.state.reported.mode = 0;
        feederState.state.reported.catWeight = (double)catWeightSpnr.getValue();
        feederState.state.reported.foodWeight = (double)foodWeightSpnr.getValue();
        String jsonState = objectMapper.writeValueAsString(feederState);

        try {
            // Send updated document to the shadow
            device.update(jsonState);
        } catch (AWSIotException e) {
            System.out.println(System.currentTimeMillis() + ": update failed for " + jsonState);
        }
    }
    
    private void initIoTMQTTClient(){
        if (awsIotClient == null && SystemConfig.CERTIFICATE_FILE_PATH != null && SystemConfig.PRIVATE_KEY_FILE_PATH != null) {
            KeyStorePasswordPair pair = SystemConfig.getKeyStorePasswordPair(SystemConfig.CERTIFICATE_FILE_PATH, SystemConfig.PRIVATE_KEY_FILE_PATH, null);

            awsIotClient = new AWSIotMqttClient(SystemConfig.CLIENT_END_POINT, SystemConfig.CLIENT_ID, pair.keyStore, pair.keyPassword);
        }
        
        if (awsIotClient == null) {
            throw new IllegalArgumentException("Failed to construct client due to missing certificate or credentials.");
        }
    }
    
    private void connectAWSIoTDevice(String thingName) throws IOException{
        device = new AWSIotDevice(thingName);
        
        try {
            awsIotClient.attach(device);
            awsIotClient.connect();
            getFeederShadowState();
            
            awsIotClient.subscribe(new AWSIotTopic(SystemConfig.CAT_FEEDER_DELTA_TOPIC, AWSIotQos.QOS0){
                    @Override
                    public void onMessage(AWSIotMessage message) {
                        System.out.println(message.getStringPayload());
                        JSONObject reader = new JSONObject(message.getStringPayload());
                        double feedAmount = reader.getJSONObject("state").getDouble("addFoodWeight");
                        foodAmountSpnr.setValue(feedAmount);
                        
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                feedBtn.doClick();
                            }
                        }).start();
                    }
                }
            , false);

            statusLbl.setText("Connected");
            statusLbl.setForeground(Color.GREEN);
        } catch (AWSIotException ex) {
            Logger.getLogger(SmartCatFeederDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void disconnectAWSIoTDevice(){        
        try {
            awsIotClient.detach(device);
            awsIotClient.disconnect();
            
            statusLbl.setText("Disconnected");
            statusLbl.setForeground(Color.RED);
        } catch (AWSIotException ex) {
            Logger.getLogger(SmartCatFeederDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        dataPanel = new javax.swing.JPanel();
        statusPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        statusLbl = new javax.swing.JLabel();
        connectionPanel = new javax.swing.JPanel();
        feedPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SmartCatFeederDevice");
        java.awt.GridBagLayout layout = new java.awt.GridBagLayout();
        layout.rowWeights = new double[] {7.0};
        getContentPane().setLayout(layout);

        dataPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Data"));
        dataPanel.setLayout(new java.awt.GridLayout(4, 2));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 3.0;
        getContentPane().add(dataPanel, gridBagConstraints);

        statusPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jLabel1.setText("Status: ");

        statusLbl.setForeground(new java.awt.Color(255, 0, 0));
        statusLbl.setText("Disconnected");

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusLbl)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(statusLbl)))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(statusPanel, gridBagConstraints);

        connectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Connection"));
        connectionPanel.setToolTipText("");
        connectionPanel.setName(""); // NOI18N
        connectionPanel.setLayout(new java.awt.GridLayout(1, 3));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 2.0;
        getContentPane().add(connectionPanel, gridBagConstraints);

        feedPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Feed"));
        feedPanel.setLayout(new java.awt.GridLayout(1, 3));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(feedPanel, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SmartCatFeederDevice.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SmartCatFeederDevice.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SmartCatFeederDevice.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SmartCatFeederDevice.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SmartCatFeederDevice().setVisible(true);
            }
        });
    }

    private AWSIotMqttClient awsIotClient;
    private AWSIotDevice device;
    private SmartCatFeederState feederState;
    private JTextField thingNameTxtFld;
    private JButton connectBtn;
    private JDatePickerImpl datePicker;
    private JSpinner foodWeightSpnr;
    private JLabel thingNameLbl;
    private JLabel foodAmountLbl;
    private JSpinner foodAmountSpnr;
    private JButton feedBtn;
    private JLabel dateLbl;
    private JLabel catWeightLbl;
    private JLabel foodWeightLbl;
    private JSpinner catWeightSpnr;
    private JButton sendDataBtn;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel connectionPanel;
    private javax.swing.JPanel dataPanel;
    private javax.swing.JPanel feedPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel statusLbl;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables
}
