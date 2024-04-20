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

import java.rmi.RemoteException;

import org.apache.commons.lang.StringUtils;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.extension.Extendable;
import de.willuhn.jameica.gui.extension.Extension;
import de.willuhn.jameica.gui.parts.CheckedContextMenuItem;
import de.willuhn.jameica.gui.parts.ContextMenu;
import de.willuhn.jameica.gui.util.SWTUtil;
import de.willuhn.jameica.hbci.gui.action.CopyClipboard;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

/**
 * A class to provide the context menu entries for Hibiscus splittransaction.
 * @author René Mach
 */
public class ContextMenuKonto implements Extension {
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
    copy.addItem(createContextMenu(Type.STAMMDATEN, i18n.tr("Stammdaten kopieren"), k -> {
      StringBuilder b = new StringBuilder(i18n.tr("Kontoinhaber: "));
        
      b.append(k.getName()).append(System.lineSeparator());
      b.append("IBAN: ").append(StringUtils.deleteWhitespace(k.getIban())).append(System.lineSeparator());
      b.append("BIC: ").append(k.getBic());
        
      return b.toString();
    }));
    
    copy.addItem(createContextMenu(Type.INHABER, i18n.tr("Kontoinhaber"), k -> { return k.getName(); }));
    copy.addItem(createContextMenu(Type.IBAN, i18n.tr("IBAN"), k -> { return StringUtils.deleteWhitespace(k.getIban()); }));
    copy.addItem(createContextMenu(Type.BIC, i18n.tr("BIC"), k -> { return k.getBic(); }));
    copy.addItem(createContextMenu(Type.KONTONUMMER, i18n.tr("Kontonummer"), k -> { return k.getKontonummer(); }));
    copy.addItem(createContextMenu(Type.BLZ, i18n.tr("BLZ"), k -> { return k.getBLZ(); }));
    copy.addItem(createContextMenu(Type.KENNUNG, i18n.tr("Kundenkennung"), k -> { return k.getKundennummer(); }));
    copy.addItem(createContextMenu(Type.NOTIZ, i18n.tr("Notiz"), k -> { return k.getKommentar(); }));
    copy.addItem(new MyContextMenuItem(Type.SALDO, i18n.tr("Saldo"), new CopyClipboard() {
      @Override
      public void handleAction(Object o) throws ApplicationException {
        try {
          if(o instanceof Konto) {
            super.handleAction(String.format("%.2f", ((Konto)o).getSaldo()));
          }
          else if(o instanceof Konto[]) {
            double sum = 0;
            
            for(Konto k : (Konto[])o) {
              sum += k.getSaldo();
            }
            super.handleAction(String.format("%.2f", sum));
          }
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }, true));
    
    ((ContextMenu)extendable).addMenu(copy);
  }
  
  private MyContextMenuItem createContextMenu(int type, String text, KontoHandler handler) {
    MyContextMenuItem menu = new MyContextMenuItem(type, text, new CopyClipboard() {
      @Override
      public void handleAction(Object context) throws ApplicationException {
        try {
          super.handleAction(handler.handleKonto((Konto)context));
        } catch (RemoteException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
      }
    });
    
    return menu;
  }
  
  private static interface KontoHandler {
    public String handleKonto(Konto k) throws RemoteException;
  }
  
  /**
   * Hilfsklasse, um den Menupunkt zu deaktivieren, wenn die Buchung bereits zugeordnet ist.
   */
  private class MyContextMenuItem extends CheckedContextMenuItem
  {
    private boolean arrays;
    private int type;
    
    public MyContextMenuItem(int type, String text, Action a) {
      this(type, text, a, false);
    }
    
    
    /**
     * ct.
     * @param text
     * @param a
     */
    public MyContextMenuItem(int type, String text, Action a, boolean arrays)
    {
      super(text, a);
      this.arrays = arrays;
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
      if (o instanceof Konto) {
        Konto k = (Konto)o;
        
        try {
          switch(type) {
            case Type.STAMMDATEN: result = StringUtils.isNotBlank(k.getName()) || StringUtils.isNotBlank(k.getIban()) || StringUtils.isNotBlank(k.getBic());break;
            case Type.INHABER: result = StringUtils.isNotBlank(k.getName());break;
            case Type.IBAN: result = StringUtils.isNotBlank(k.getIban());break;
            case Type.BIC: result = StringUtils.isNotBlank(k.getBic());break;
            case Type.KONTONUMMER: result = StringUtils.isNotBlank(k.getKontonummer());break;
            case Type.BLZ: result = StringUtils.isNotBlank(k.getBLZ());break;
            case Type.KENNUNG: result = StringUtils.isNotBlank(k.getKundennummer());break;
            case Type.NOTIZ: result = StringUtils.isNotBlank(k.getKommentar());break;
            case Type.SALDO: result = true;break;
          }
        } catch (RemoteException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      else if(arrays && o instanceof Konto[]) {
        result = true;
      }
      
      return result;
    }
    
  }
}
