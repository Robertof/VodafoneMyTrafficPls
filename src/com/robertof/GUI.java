/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.robertof;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Roberto
 */
public class GUI extends javax.swing.JFrame {
    private String fraseDaCioccolatino = " Sicuro di essere connesso da una IK vodafone? Prova su: http://contatori.vodafone.it/homepage";
    protected String _tGroup, dFrom = "", dTo = "", tMax = "", tUt = "", tPRes = "", tariffName = "", traffIncl = "", tarOltrSog = "";
    private String[] months = new String[] { "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno", "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre" };
    private String[] measures = new String[] { "KB", "MB", "GB", "TB" };
    protected int timerFreq = 0;
    protected Thread existingTimer;
    private boolean stopTimer;
    private String threadDead;
    private float percStartPUsed = 0;
    private int totalTrafficint;
    private boolean updateCompleted = false;
    public GUI() {
        initComponents();
        updateData(true);
    }

    private synchronized void updateData()
    {
        updateData(false);
    }
    
    private synchronized void updateData(final boolean firstRun)
    {
        Thread t1 = new Thread (new Runnable() {
            public void run()
            {
                updateCompleted = false;
                statusLabel.setText("Aggiornamento dei dati in corso...");
                aggButton.setEnabled(false);
                dettTariffButton.setEnabled(false);
                jButton1.setEnabled(false); // stats
                String vodaPage = Main.doHTTPGetRequest("http://www.contatori.vodafone.it/homepage");
                Pattern regex1 = Pattern.compile("Stai utilizzando il numero.+?<b>([^>]+)</b>");
                Matcher mr1    = regex1.matcher(vodaPage);
                if (!mr1.find())
                {
                    Main.throwError("Impossibile recuperare il numero di telefono della SIM.");
                    System.exit(1);
                }
                String telNumber = mr1.group(1);
                Pattern regex2 = Pattern.compile("<td class=\"tdOddwhite verticalCenteredCell left\">([^<]+)</td>");
                Matcher mr2    = regex2.matcher(vodaPage);
                boolean goodThings = false, stop = false;
                int count = 0;
                while (mr2.find())
                {
                    goodThings = true;
                    _tGroup = mr2.group(1);
                    if (stop) break;
                    switch (++count)
                    {
                        case 1:
                            // date from
                            // check if it contains a month
                            if (Main.stringContains(months, _tGroup) == null)
                            {
                                Main.throwError ("Impossibile ricavare il periodo d'inizio della tariffa." + fraseDaCioccolatino);
                                System.exit(1);
                            }
                            dFrom = _tGroup;
                         break;
                         case 2:
                             // date to
                             if (Main.stringContains(months, _tGroup) == null)
                             {
                                 Main.throwError("Impossibile ricavare il periodo di fine della tariffa." + fraseDaCioccolatino);
                                 System.exit(1);
                             }
                             dTo = _tGroup;
                         break;
                         case 3:
                             // total traffic (in GB/MB/TB)
                             if (Main.stringContains(measures, _tGroup) == null)
                             {
                                 Main.throwError("Impossibile ricavare il traffico totale. " + fraseDaCioccolatino);
                                 System.exit(1);
                             }
                             tMax = _tGroup;
                         break;
                         case 4:
                             // percentage of utilized traffic
                             if (!_tGroup.contains("%"))
                             {
                                 Main.throwError("Impossibile ricavare la percentuale del traffico utilizzato." + fraseDaCioccolatino);
                                 System.exit(1);
                             }
                             tUt = _tGroup;
                         break;
                         case 5:
                             // percentage of remaining traffic
                             if (!_tGroup.contains("%"))
                             {
                                 Main.throwError("Impossibile ricavare la percentuale del traffico rimanente." + fraseDaCioccolatino);
                                 System.exit(1);
                             }
                             tPRes = _tGroup;
                             stop = true;
                         break;
                    }
                }
                if (!goodThings || count != 5)
                {
                    Main.throwError("Impossibile ricavare i dati." + fraseDaCioccolatino);
                    System.exit(1);
                }
                Pattern regex4 = Pattern.compile("<span class=\"ticker\">([^<]+)</span>");
                Matcher mr4    = regex4.matcher(vodaPage);
                if (!mr4.find())
                {
                    Main.throwError("Impossibile ricavare il nome della tariffa." + fraseDaCioccolatino);
                    System.exit(1);
                }
                tariffName = mr4.group(1);
                if (!mr4.find())
                {
                    Main.throwError("Impossibile ricavare il traffico incluso dell'offerta." + fraseDaCioccolatino);
                    System.exit(1);
                }
                traffIncl = mr4.group(1);
                if (!mr4.find())
                {
                    Main.throwError("Impossibile ricavare la tariffa oltre soglia." + fraseDaCioccolatino);
                    System.exit(1);
                }
                tarOltrSog = mr4.group(1);
                numberLabel.setText(telNumber);
                percTraffico.setText(tUt);
                tPeriodLabel.setText("dal " + dFrom + " al " + dTo);
                String _tu2 = tUt.replace("%", "");
                float tUtZ = Float.parseFloat(_tu2);
                if (firstRun)
                    percStartPUsed = tUtZ;
                jProgressBar1.setValue((int)tUtZ);
                Pattern regex3 = Pattern.compile("(\\d+)");
                Matcher m3     = regex3.matcher(tMax);
                if (!m3.find())
                {
                    Main.throwError ("Impossibile ricavare il traffico totale.");
                    System.exit(1);
                }
                Integer s = Integer.parseInt(m3.group(1));
                String measureToUse = Main.stringContains(measures, tMax);
                totalTrafficint = s;
                double res = (tUtZ * s) / 100;
                DecimalFormat formatter = new DecimalFormat("0.00");
                usedTraffic.setText(formatter.format(res) + " " + measureToUse.toUpperCase() + "/" + tMax);
                statusLabel.setText("Pronto");
                aggButton.setEnabled(true);
                dettTariffButton.setEnabled(true);
                jButton1.setEnabled(true);
                updateCompleted = true;
            }
        });
        t1.start();
    }
    public void unregisterTimer()
    {
        if (existingTimer != null && existingTimer.isAlive())
        {
            stopTimer = true;
            existingTimer.interrupt();
            existingTimer = null;
        }
    }
    // delay is in seconds.
    public void registerTimer (int delay)
    {
        if (existingTimer != null && existingTimer.isAlive())
        {
            stopTimer = true;
            existingTimer.interrupt();
            while (threadDead==null||!threadDead.equals("d"+timerFreq)){};
            threadDead    = null;
            existingTimer = null;
        }
        stopTimer = false;
        if (delay < 0 || delay == 0)
            return;
        timerFreq = delay;
        // register new timer
        existingTimer = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println ("Thread d" + timerFreq + ": started");
                while (true)
                {
                    if (stopTimer)
                    {
                        threadDead = "d" + timerFreq;
                        stopTimer = false;
                        System.out.println ("Timer thread d" + timerFreq + " has been stopped.");
                        return;
                    }
                    try {
                        Thread.sleep(timerFreq * 1000);
                    } catch (InterruptedException e) {}
                    System.out.println ("Timer thread d" + timerFreq + ": updating");
                    if (aggButton.isEnabled() == true) // only update if it isn't updating
                        updateData();
                    else
                        statusLabel.setText("Attenzione: aggiornamento dei dati già in corso - il timer eviterà di farlo.");
                }
            }
        });
        existingTimer.start();
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        TrafficData = new javax.swing.JPanel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jLabel1 = new javax.swing.JLabel();
        usedTraffic = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        percTraffico = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        numberLabel = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        tPeriodLabel = new javax.swing.JLabel();
        controls = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        aboutButton = new javax.swing.JButton();
        autoAggButton = new javax.swing.JButton();
        aggButton = new javax.swing.JButton();
        dettTariffButton = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Vodafone Traffic Retriever v1.0");
        setPreferredSize(new java.awt.Dimension(540, 215));
        setResizable(false);

        jProgressBar1.setStringPainted(true);

        jLabel1.setText("Traffico utilizzato: ");

        usedTraffic.setText("-");

        jLabel2.setText("Percentuale traffico utilizzato:");

        percTraffico.setText("-");

        jLabel5.setText("Numero:");
        jLabel5.setPreferredSize(new java.awt.Dimension(41, 9));

        numberLabel.setText("-");

        jLabel7.setText("Periodo di validità offerta:");

        tPeriodLabel.setText("-");

        javax.swing.GroupLayout TrafficDataLayout = new javax.swing.GroupLayout(TrafficData);
        TrafficData.setLayout(TrafficDataLayout);
        TrafficDataLayout.setHorizontalGroup(
            TrafficDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(TrafficDataLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(TrafficDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jProgressBar1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(TrafficDataLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(usedTraffic))
                    .addGroup(TrafficDataLayout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(percTraffico))
                    .addGroup(TrafficDataLayout.createSequentialGroup()
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(numberLabel))
                    .addGroup(TrafficDataLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(tPeriodLabel)))
                .addContainerGap())
        );
        TrafficDataLayout.setVerticalGroup(
            TrafficDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, TrafficDataLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(TrafficDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(usedTraffic, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(2, 2, 2)
                .addGroup(TrafficDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(percTraffico))
                .addGroup(TrafficDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(TrafficDataLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(TrafficDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(tPeriodLabel)
                            .addComponent(jLabel7)))
                    .addGroup(TrafficDataLayout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addGroup(TrafficDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(numberLabel))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(569, 569, 569))
        );

        statusLabel.setText("Pronto");

        jLabel3.setText("Stato:");

        aboutButton.setText("Informazioni su..");
        aboutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });

        autoAggButton.setText("Imposta autoaggiornamento..");
        autoAggButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoAggButtonActionPerformed(evt);
            }
        });

        aggButton.setText("Aggiorna");
        aggButton.setEnabled(false);
        aggButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aggButtonActionPerformed(evt);
            }
        });

        dettTariffButton.setText("Dettagli tariffa...");
        dettTariffButton.setEnabled(false);
        dettTariffButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dettTariffButtonActionPerformed(evt);
            }
        });

        jButton1.setText("Statistiche (dall'apertura del programma)");
        jButton1.setEnabled(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("Chiudi");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout controlsLayout = new javax.swing.GroupLayout(controls);
        controls.setLayout(controlsLayout);
        controlsLayout.setHorizontalGroup(
            controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlsLayout.createSequentialGroup()
                .addGap(2, 2, 2)
                .addGroup(controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(controlsLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(statusLabel))
                    .addGroup(controlsLayout.createSequentialGroup()
                        .addGroup(controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jButton1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(controlsLayout.createSequentialGroup()
                                .addComponent(aggButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(autoAggButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(dettTariffButton)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(aboutButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        controlsLayout.setVerticalGroup(
            controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlsLayout.createSequentialGroup()
                .addGroup(controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(autoAggButton)
                    .addComponent(aggButton)
                    .addComponent(aboutButton)
                    .addComponent(dettTariffButton))
                .addGap(1, 1, 1)
                .addGroup(controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(controlsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusLabel)
                    .addComponent(jLabel3))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(TrafficData, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(controls, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(TrafficData, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(controls, javax.swing.GroupLayout.DEFAULT_SIZE, 69, Short.MAX_VALUE)
                .addContainerGap())
        );

        getAccessibleContext().setAccessibleName("Vodafone Traffic Retriever v1.1");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void aggButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aggButtonActionPerformed
        updateData();
    }//GEN-LAST:event_aggButtonActionPerformed

    private void autoAggButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoAggButtonActionPerformed
        new AutoUpdateGUI(this).setVisible(true);
    }//GEN-LAST:event_autoAggButtonActionPerformed

    private void dettTariffButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dettTariffButtonActionPerformed
        new InfoTariffaGUI(this).setVisible(true);
    }//GEN-LAST:event_dettTariffButtonActionPerformed

    private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutButtonActionPerformed
        javax.swing.JOptionPane.showMessageDialog(null, "Programma creato da: Robertof\nCon: NetBeans 7.1 (+ Java)\nGrazie Stackoverflow. <3", "Informazioni su..", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_aboutButtonActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        Thread p1 = new Thread (new Runnable() {
            public void run()
            {
                updateData();
                while (!updateCompleted);
                statCallback();
            }
        });
        p1.start();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void statCallback()
    {
        // statistics update is finished
        double gbUsedOnStart = (percStartPUsed * totalTrafficint) / 100;
        String tu = tUt.replace("%", "");
        float  pU = Float.parseFloat(tu);
        double gbUsedNow     = (pU             * totalTrafficint) / 100;
        double gbUsed        = gbUsedNow - gbUsedOnStart;
        String measureToUse  = Main.stringContains(measures, tMax);
        DecimalFormat df     = new DecimalFormat ("0.00");
        String finalVal      = df.format(gbUsed) + " " + measureToUse.toUpperCase();
        if (gbUsed < 1 && measureToUse.equals("gb"))
        {
            // calculate in MB
            double MBUsed  = gbUsed * 1024; // 1024 MB = 1 GiB
            boolean iskb   = (MBUsed < 1);
            if (MBUsed < 1)
                MBUsed = MBUsed * 1024;
            finalVal      += " (" + df.format(MBUsed) + " " + ((iskb) ? "KB" : "MB") + ")";
        }
        else if (gbUsed < 1 && measureToUse.equals("mb"))
        {
            double KBUsed = gbUsed * 1024;
            finalVal      += " (" + df.format(KBUsed) + " KB)";
        }
        // END of used GB - let's print a JOptionPane
        String jOptionPaneMessageDialogText = "-- Statistiche di uso dell'Internet Key --\n";
        jOptionPaneMessageDialogText       += " > NOTA: Le statistiche sono calcolate dall'avvio del programma.\n";
        jOptionPaneMessageDialogText       += " > Se piu' utenti stanno usando l'Internet Key anche il loro consumo di banda sara' incluso nelle statistiche.\n";
        jOptionPaneMessageDialogText       += "== INIZIO STATISTICHE ==\n";
        jOptionPaneMessageDialogText       += "  > Dati usati dall'inizio del programma: " + finalVal + "\n";
        jOptionPaneMessageDialogText       += "  > Aumento in percentuale: " + df.format(pU - percStartPUsed) + "% (perc. iniziale: " + df.format(percStartPUsed) + "%, perc. attuale: " + df.format(pU) + "%)\n";
        jOptionPaneMessageDialogText       += "== FINE STATISTICHE   ==\n";
        javax.swing.JOptionPane.showMessageDialog(null, jOptionPaneMessageDialogText, "Statistiche utilizzo Internet Key", javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel TrafficData;
    private javax.swing.JButton aboutButton;
    private javax.swing.JButton aggButton;
    private javax.swing.JButton autoAggButton;
    private javax.swing.JPanel controls;
    private javax.swing.JButton dettTariffButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JProgressBar jProgressBar1;
    private javax.swing.JLabel numberLabel;
    private javax.swing.JLabel percTraffico;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JLabel tPeriodLabel;
    private javax.swing.JLabel usedTraffic;
    // End of variables declaration//GEN-END:variables
}
