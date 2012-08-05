package com.robertof;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Robertof
 */
public class Main {
    public static void main(String args[]) {
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (javax.swing.JOptionPane.showConfirmDialog(null, "Questo programma funziona solo con Internet Key Vodafone. Sei sicuro di voler continuare?", "Attenzione", javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE) == javax.swing.JOptionPane.CANCEL_OPTION)
                    System.exit(0);
                GUI maingui = new GUI();
                maingui.setVisible(true);
            }
        });
    }
    public static String doHTTPGetRequest (String urls)
    {
        BufferedReader in = null;
        try {
            URL url = new URL(urls);
            in = new BufferedReader (new InputStreamReader (url.openStream()));
            String line, everything = "";
            while ((line = in.readLine()) != null)
            {
                everything += line; // strip newlines
            }
            return everything;
        }
        catch (MalformedURLException ex) {
            throwError ("Cannot handle URL: " + urls);
        }
        catch (IOException ex2)
        {
            throwError ("Got exception: " + ex2.getMessage());
        }
        finally
        {
            if (in != null)
            {
                try {
                    in.close();
                } catch (IOException ex) {}
            }
        }
        return null;
    }
    public static void throwError (String error)
    {
        javax.swing.JOptionPane.showMessageDialog(null, error, "Errore", javax.swing.JOptionPane.ERROR_MESSAGE);
    }
    public static String stringContains (String[] haystack, String needle)
    {
        for (String val : haystack)
        {
            if (needle.toLowerCase().contains(val.toLowerCase()))
                return val.toLowerCase();
        }
        return null;
    }
}
