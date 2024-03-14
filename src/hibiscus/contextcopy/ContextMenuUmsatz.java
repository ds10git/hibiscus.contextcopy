/*
 * Hibiscus splittransaction
 * Copyright (C) 2019 René Mach (dev@tvbrowser.org)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package hibiscus.contextcopy;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.rmi.RemoteException;
import java.text.DateFormat;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.extension.Extendable;
import de.willuhn.jameica.gui.extension.Extension;
import de.willuhn.jameica.gui.parts.CheckedContextMenuItem;
import de.willuhn.jameica.gui.parts.ContextMenu;
import de.willuhn.jameica.gui.parts.ContextMenuItem;
import de.willuhn.jameica.gui.util.SWTUtil;
import de.willuhn.jameica.hbci.rmi.Umsatz;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

/**
 * A class to provide the context menu entries for Hibiscus splittransaction.
 * @author René Mach
 */
public class ContextMenuUmsatz implements Extension {
  private static final DateFormat DATE = DateFormat.getDateInstance(DateFormat.MEDIUM);
  private I18N i18n = Application.getI18n();
  
  @Override
  public void extend(Extendable extendable) {
    if (extendable == null || !(extendable instanceof ContextMenu))
    {
      Logger.warn("invalid extendable, skipping extension");
      return;
    }
    ContextMenu copy = new ContextMenu();
    copy.setText(i18n.tr("Kopieren"));
    copy.setImage(SWTUtil.getImage("edit-copy.png"));
    
    ContextMenu menu = (ContextMenu) extendable;
    menu.addItem(ContextMenuItem.SEPARATOR);
    menu.addMenu(copy);
    
    ContextMenu konto = new ContextMenu();
    konto.setText(i18n.tr("Konto"));
    
    ContextMenu gkonto = new ContextMenu();
    gkonto.setText(i18n.tr("Gegenkonto"));
    
    copy.addMenu(konto);
    copy.addMenu(gkonto);
    copy.addItem(createContextMenu(i18n.tr("Betrag"), u -> { return String.format("%.2f", u.getBetrag()); }));
    copy.addItem(createContextMenu(i18n.tr("Datum"), u -> { return DATE.format(u.getDatum()); }));
    copy.addItem(createContextMenu(i18n.tr("Wertstellung"), u -> { return DATE.format(u.getValuta()); }));
    copy.addItem(createContextMenu(i18n.tr("Neuer Saldo"), u -> { return String.format("%.2f", u.getSaldo()); }));
    copy.addItem(createContextMenu(i18n.tr("Notiz"), u -> { return u.getKommentar(); }));
    
    konto.addItem(createContextMenu(i18n.tr("Stammdaten kopieren"), u -> {
      StringBuilder b = new StringBuilder(i18n.tr("Kontoinhaber: "));
        
      b.append(u.getKonto().getName()).append(System.lineSeparator());
      b.append("IBAN: ").append(u.getKonto().getIban().replaceAll("\\s+", "")).append(System.lineSeparator());
      b.append("BIC: ").append(u.getKonto().getBic());
        
      return b.toString();
    }));
    
    konto.addItem(createContextMenu(i18n.tr("Kontoinhaber"), u -> { return u.getKonto().getName(); }));
    konto.addItem(createContextMenu(i18n.tr("IBAN"), u -> { return u.getKonto().getIban().replaceAll("\\s+", ""); }));
    konto.addItem(createContextMenu(i18n.tr("BIC"), u -> { return u.getKonto().getBic(); }));
    
    gkonto.addItem(createContextMenu(i18n.tr("Stammdaten kopieren"), u -> {
      StringBuilder b = new StringBuilder(i18n.tr("Kontoinhaber: "));
      
      b.append(u.getGegenkontoName()).append(System.lineSeparator());
      b.append("IBAN: ").append(u.getGegenkontoNummer().replaceAll("\\s+", "")).append(System.lineSeparator());
      b.append("BIC: ").append(u.getGegenkontoBLZ());
          
      return b.toString();
    }));
    
    gkonto.addItem(createContextMenu(i18n.tr("Kontoinhaber"), u -> { return u.getGegenkontoName(); }));
    gkonto.addItem(createContextMenu("IBAN", u -> { return u.getGegenkontoNummer().replaceAll("\\s+", ""); }));
    gkonto.addItem(createContextMenu("BIC", u -> { return u.getGegenkontoBLZ(); }));
  }
  
  private MyContextMenuItem createContextMenu(String text, UmsatzHandler handler) {
    MyContextMenuItem menu = new MyContextMenuItem(text, new Action() {
      @Override
      public void handleAction(Object context) throws ApplicationException {
        try {
          Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(handler.handleUmsatz((Umsatz)context)), null);
        } catch (HeadlessException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    });
    
    return menu;
  }
  
  private static interface UmsatzHandler {
    public String handleUmsatz(Umsatz u) throws RemoteException;
  }
  
  /**
   * Hilfsklasse, um den Menupunkt zu deaktivieren, wenn die Buchung bereits zugeordnet ist.
   */
  private class MyContextMenuItem extends CheckedContextMenuItem
  {
    /**
     * ct.
     * @param text
     * @param a
     */
    public MyContextMenuItem(String text, Action a)
    {
      super(text, a);
      
    }

    /**
     * @see de.willuhn.jameica.gui.parts.CheckedContextMenuItem#isEnabledFor(java.lang.Object)
     */
    public boolean isEnabledFor(Object o)
    {
      boolean result = false;
      
      // Wenn wir eine ganze Liste von Buchungen haben, pruefen
      // wir nicht jede einzeln, ob sie schon in SynTAX vorhanden
      // ist. Die werden dann beim Import (weiter unten) einfach ausgesiebt.
      if (o instanceof Umsatz) {
        result = true;
      }
      
      return result;
    }
    
  }
}
