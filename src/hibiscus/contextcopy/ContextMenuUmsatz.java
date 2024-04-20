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
import java.rmi.RemoteException;
import java.text.DateFormat;

import org.apache.commons.lang.StringUtils;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.extension.Extendable;
import de.willuhn.jameica.gui.extension.Extension;
import de.willuhn.jameica.gui.parts.CheckedContextMenuItem;
import de.willuhn.jameica.gui.parts.ContextMenu;
import de.willuhn.jameica.gui.parts.ContextMenuItem;
import de.willuhn.jameica.gui.util.SWTUtil;
import de.willuhn.jameica.hbci.gui.action.CopyClipboard;
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
    copy.setText(i18n.tr("Kopieren in Zwischenablage"));
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
    copy.addItem(new MyContextMenuItem(Type.BETRAG, i18n.tr("Betrag"), new CopyClipboard() {
      @Override
      public void handleAction(Object o) throws ApplicationException {
        try {
          double sum = 0;
          
          if(o instanceof Umsatz) {
            sum = ((Umsatz)o).getBetrag();
          }
          else if(o instanceof Umsatz[]) {
            for(Umsatz u : ((Umsatz[])o)) {
              sum += u.getBetrag();
            }
          }
          
          super.handleAction(String.format("%.2f", sum));
        } catch (HeadlessException | RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      
    }, true));
    copy.addItem(createContextMenu(Type.DATUM, i18n.tr("Datum"), u -> { return DATE.format(u.getDatum()); }));
    copy.addItem(createContextMenu(Type.WERTSTELLUNG, i18n.tr("Wertstellung"), u -> { return DATE.format(u.getValuta()); }));
    copy.addItem(createContextMenu(Type.SALDO, i18n.tr("Neuer Saldo"), u -> { return String.format("%.2f", u.getSaldo()); }));
    copy.addItem(createContextMenu(Type.NOTIZ, i18n.tr("Notiz"), u -> { return u.getKommentar(); }));
    copy.addItem(createContextMenu(Type.ZWECK, i18n.tr("Verwendungszweck"), u -> {
      StringBuilder s = new StringBuilder(u.getZweck());
      
      if(StringUtils.isNotBlank(u.getZweck2())) {
        s.append(" ").append(u.getZweck2());
      }
      
      if(u.getWeitereVerwendungszwecke() != null) {
        for(String z : u.getWeitereVerwendungszwecke()) {
          if(z != null && !z.isBlank()) {
            s.append(" ").append(z);
          }
        }
      }
      
      return s.toString(); 
    }));
    
    konto.addItem(createContextMenu(Type.STAMMDATEN, i18n.tr("Stammdaten kopieren"), u -> {
      StringBuilder b = new StringBuilder(i18n.tr("Kontoinhaber: "));
        
      b.append(u.getKonto().getName()).append(System.lineSeparator());
      b.append("IBAN: ").append(StringUtils.deleteWhitespace(u.getKonto().getIban())).append(System.lineSeparator());
      b.append("BIC: ").append(u.getKonto().getBic());
        
      return b.toString();
    }));
    
    konto.addItem(createContextMenu(Type.INHABER, i18n.tr("Kontoinhaber"), u -> { return u.getKonto().getName(); }));
    konto.addItem(createContextMenu(Type.IBAN, i18n.tr("IBAN"), u -> { return StringUtils.deleteWhitespace(u.getKonto().getIban()); }));
    konto.addItem(createContextMenu(Type.BIC, i18n.tr("BIC"), u -> { return u.getKonto().getBic(); }));
    
    gkonto.addItem(createContextMenu(Type.GEGENKONTO + Type.STAMMDATEN, i18n.tr("Stammdaten kopieren"), u -> {
      StringBuilder b = new StringBuilder(i18n.tr("Kontoinhaber: "));
      
      b.append(u.getGegenkontoName()).append(System.lineSeparator());
      b.append("IBAN: ").append(StringUtils.deleteWhitespace(u.getGegenkontoNummer())).append(System.lineSeparator());
      b.append("BIC: ").append(u.getGegenkontoBLZ());
      
      return b.toString();
    }));
    
    gkonto.addItem(createContextMenu(Type.GEGENKONTO + Type.INHABER, i18n.tr("Kontoinhaber"), u -> { return u.getGegenkontoName(); }));
    gkonto.addItem(createContextMenu(Type.GEGENKONTO + Type.IBAN, "IBAN", u -> { return StringUtils.deleteWhitespace(u.getGegenkontoNummer()); }));
    gkonto.addItem(createContextMenu(Type.GEGENKONTO + Type.BIC, "BIC", u -> { return u.getGegenkontoBLZ(); }));
  }
  
  private MyContextMenuItem createContextMenu(int type, String text, UmsatzHandler handler) {
    MyContextMenuItem menu = new MyContextMenuItem(type, text, new CopyClipboard() {
      @Override
      public void handleAction(Object context) throws ApplicationException {
        try {
          super.handleAction(handler.handleUmsatz((Umsatz)context));
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
    private boolean array;
    private int type;
    
    public MyContextMenuItem(int type, String text, Action a)
    {
      this(type, text, a, false);
    }
    
    /**
     * ct.
     * @param text
     * @param a
     */
    public MyContextMenuItem(int type, String text, Action a, boolean array)
    {
      super(text, a);
      this.array = array;
      this.type = type;
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
        Umsatz u = (Umsatz)o;
        
        try {
          switch(type) {
            case Type.STAMMDATEN: result = StringUtils.isNotBlank(u.getKonto().getName()) || StringUtils.isNotBlank(u.getKonto().getIban()) || StringUtils.isNotBlank(u.getKonto().getBic());break;
            case Type.INHABER: result = StringUtils.isNotBlank(u.getKonto().getName());break;
            case Type.IBAN: result = StringUtils.isNotBlank(u.getKonto().getIban());break;
            case Type.BIC: result = StringUtils.isNotBlank(u.getKonto().getBic());break;
            case Type.SALDO:
            case Type.BETRAG: result = true;break;
            case Type.DATUM: result = u.getDatum() != null;break;
            case Type.WERTSTELLUNG: result = u.getValuta() != null;break;
            case Type.NOTIZ: result = StringUtils.isNotBlank(u.getKommentar());break;
            case Type.ZWECK: result = StringUtils.isNotBlank(u.getZweck());break;
            case Type.GEGENKONTO + Type.STAMMDATEN: result = StringUtils.isNotBlank(u.getGegenkontoName()) || StringUtils.isNotBlank(u.getGegenkontoNummer()) || StringUtils.isNotBlank(u.getGegenkontoBLZ());break;
            case Type.GEGENKONTO + Type.INHABER: result = StringUtils.isNotBlank(u.getGegenkontoName());break;
            case Type.GEGENKONTO + Type.IBAN: result = StringUtils.isNotBlank(u.getGegenkontoNummer());break;
            case Type.GEGENKONTO + Type.BIC: result = StringUtils.isNotBlank(u.getGegenkontoBLZ());break;            
          }
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      else if (array && o instanceof Umsatz[]) {
        result = true;
      }
      
      return result;
    }
    
  }
}
