/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hcw22.smartcatfeeder;

import com.amazonaws.services.iot.client.AWSIotDevice;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcw22.smartcatfeeder.SystemConfig.KeyStorePasswordPair;
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

/**
 *
 * @author Tom Wong
 */
public class SmartCatFeederDevice extends javax.swing.JFrame {

    public class DateLabelFormatter extends AbstractFormatter {

        private String datePattern = "yyyy-MM-dd";
        private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);

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
        dateLbl = new JLabel();
        catWeightLbl = new JLabel();
        weightSpnr = new JSpinner();
        foodWeightLbl = new JLabel();
        foodWeightSpnr = new JSpinner();
        
        thingNameLbl.setText("Device/Thing Name: ");
        thingNameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        connectionPanel.add(thingNameLbl);
        
        thingNameTxtFld.setText(SystemConfig.DEFAULT_THING_NAME);
        connectionPanel.add(thingNameTxtFld);
        
        
        connectionPanel.add(new JLabel());
        
        connectBtn.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                if(connectBtn.getText() == "Connect"){
                    connectAWSIoTDevice(thingNameTxtFld.getText());
                    connectBtn.setText("Disconnect");
                }
                else{
                    disconnectAWSIoTDevice();
                    connectBtn.setText("Connect");
                }
            }
        });
        
        connectionPanel.add(connectBtn);
        
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
        datePicker.getJFormattedTextField().getText();
        
        dataPanel.add(datePicker);

        catWeightLbl.setText("Cat Weight: ");
        catWeightLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        dataPanel.add(catWeightLbl);
        
        weightSpnr.setModel(new SpinnerNumberModel(3.0, 0.0, 100.0, 0.1));
        dataPanel.add(weightSpnr);

        foodWeightLbl.setText("Food Weight: ");
        foodWeightLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        
        dataPanel.add(foodWeightLbl);
        
        foodWeightSpnr.setModel(new SpinnerNumberModel(3.0, 0.0, 100.0, 0.1));
        dataPanel.add(foodWeightSpnr);

        pack();
        
        initIoTMQTTClient();
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
        }
    }
    
    private void setFeederShadowState() throws JsonProcessingException{
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
    
    private void connectAWSIoTDevice(String thingName){
        device = new AWSIotDevice(thingName);
        
        try {
            awsIotClient.attach(device);
            awsIotClient.connect();
            device.delete();
            statusLbl.setText("Connected");
        } catch (AWSIotException ex) {
            Logger.getLogger(SmartCatFeederDevice.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void disconnectAWSIoTDevice(){        
        try {
            device.delete();
            awsIotClient.detach(device);
            awsIotClient.disconnect();
            
            statusLbl.setText("Disconnected");
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

        dataPanel = new javax.swing.JPanel();
        statusPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        statusLbl = new javax.swing.JLabel();
        connectionPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SmartCatFeederDevice");

        dataPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Data"));
        dataPanel.setLayout(new java.awt.GridLayout(3, 2));
        getContentPane().add(dataPanel, java.awt.BorderLayout.CENTER);

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
                .addContainerGap(202, Short.MAX_VALUE))
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
                .addGap(0, 11, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(statusLbl)))
        );

        getContentPane().add(statusPanel, java.awt.BorderLayout.PAGE_END);

        connectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Connection"));
        connectionPanel.setToolTipText("");
        connectionPanel.setName(""); // NOI18N
        connectionPanel.setLayout(new java.awt.GridLayout(2, 2));
        getContentPane().add(connectionPanel, java.awt.BorderLayout.PAGE_START);

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
    private JDatePickerImpl datePicker;
    private JSpinner foodWeightSpnr;
    private JLabel thingNameLbl;
    private JLabel dateLbl;
    private JLabel catWeightLbl;
    private JLabel foodWeightLbl;
    private JSpinner weightSpnr;
    private JTextField thingNameTxtFld;
    private JButton connectBtn;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel connectionPanel;
    private javax.swing.JPanel dataPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel statusLbl;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables
}
