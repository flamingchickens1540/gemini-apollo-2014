package org.team1540.periscope;

import ccre.cluck.CluckGlobals;
import ccre.log.LogLevel;
import ccre.log.Logger;
import ccre.log.NetworkAutologger;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.JOptionPane;

public class Main extends javax.swing.JFrame {

    public Main() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        webcam1 = new org.team1540.periscope.Webcam();
        cVProcessor1 = new org.team1540.periscope.CVProcessor();
        cluckConnector1 = new org.team1540.periscope.CluckConnector();
        tAddress = new javax.swing.JTextField();
        cSelector = new javax.swing.JComboBox();
        imagePane = new org.team1540.periscope.ImagePanel();
        txtCluckRemote = new javax.swing.JTextField();
        btnHistogram = new javax.swing.JToggleButton();
        labInside = new javax.swing.JLabel();
        btnConfigHistogram = new javax.swing.JButton();

        webcam1.setOutput(cVProcessor1);
        webcam1.setWebcamEnabled(false);

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, tAddress, org.jdesktop.beansbinding.ELProperty.create("${text}"), webcam1, org.jdesktop.beansbinding.BeanProperty.create("address"));
        bindingGroup.addBinding(binding);

        cVProcessor1.setTarget(imagePane);

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, btnHistogram, org.jdesktop.beansbinding.ELProperty.create("${selected}"), cVProcessor1, org.jdesktop.beansbinding.BeanProperty.create("histogram"));
        bindingGroup.addBinding(binding);

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, txtCluckRemote, org.jdesktop.beansbinding.ELProperty.create("${text}"), cluckConnector1, org.jdesktop.beansbinding.BeanProperty.create("address"));
        bindingGroup.addBinding(binding);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        tAddress.setColumns(15);
        tAddress.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        tAddress.setText("10.15.40.11");

        cSelector.setModel(cVProcessor1.getPointSettings());

        imagePane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                updatePoint(evt);
            }
        });

        javax.swing.GroupLayout imagePaneLayout = new javax.swing.GroupLayout(imagePane);
        imagePane.setLayout(imagePaneLayout);
        imagePaneLayout.setHorizontalGroup(
            imagePaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        imagePaneLayout.setVerticalGroup(
            imagePaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 479, Short.MAX_VALUE)
        );

        txtCluckRemote.setColumns(15);
        txtCluckRemote.setFont(new java.awt.Font("Monospaced", 0, 11)); // NOI18N
        txtCluckRemote.setText("10.15.40.2:443");
        txtCluckRemote.setToolTipText("");

        btnHistogram.setText("Histogram");

        labInside.setText("Sight");

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, cVProcessor1, org.jdesktop.beansbinding.ELProperty.create("${activeColor}"), labInside, org.jdesktop.beansbinding.BeanProperty.create("foreground"));
        bindingGroup.addBinding(binding);

        btnConfigHistogram.setText("Config");
        btnConfigHistogram.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfigHistogramActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(tAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnHistogram)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(labInside)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnConfigHistogram)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 147, Short.MAX_VALUE)
                .addComponent(txtCluckRemote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(imagePane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtCluckRemote, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnHistogram)
                    .addComponent(labInside)
                    .addComponent(btnConfigHistogram))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(imagePane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        bindingGroup.bind();

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void updatePoint(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_updatePoint
        Point pt = evt.getPoint();
        Dimension size = imagePane.getSize();
        cVProcessor1.putPoint(((float) pt.x) / size.width, ((float) pt.y) / size.height);
    }//GEN-LAST:event_updatePoint

    private void btnConfigHistogramActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfigHistogramActionPerformed
        cVProcessor1.setCurrentConfig(JOptionPane.showInputDialog("Enter histogram config: " + cVProcessor1.getCurrentConfig()));
    }//GEN-LAST:event_btnConfigHistogramActionPerformed

    public static void main(final String args[]) {
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
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                CluckGlobals.ensureInitializedCore();
                NetworkAutologger.register();
                Main m = new Main();
                if (args.length >= 2) {
                    try {
                        m.setSize(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
                    } catch (NumberFormatException ex) {
                        Logger.log(LogLevel.WARNING, "Bad window position!", ex);
                    }
                }
                if (args.length >= 4) {
                    try {
                        m.setLocation(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                    } catch (NumberFormatException ex) {
                        Logger.log(LogLevel.WARNING, "Bad window position!", ex);
                    }
                }
                m.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConfigHistogram;
    private javax.swing.JToggleButton btnHistogram;
    private javax.swing.JComboBox cSelector;
    private org.team1540.periscope.CVProcessor cVProcessor1;
    private org.team1540.periscope.CluckConnector cluckConnector1;
    private org.team1540.periscope.ImagePanel imagePane;
    private javax.swing.JLabel labInside;
    private javax.swing.JTextField tAddress;
    private javax.swing.JTextField txtCluckRemote;
    private org.team1540.periscope.Webcam webcam1;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables
}
