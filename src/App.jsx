import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import BottomNav from './components/BottomNav';
import HomeScreen from './components/HomeScreen';
import SalesInvoiceScreen from './components/SalesInvoiceScreen';
import InvoicesHistoryScreen from './components/InvoicesHistoryScreen';
import CustomersScreen from './components/CustomersScreen';
import PurchasesScreen from './components/PurchasesScreen';
import InventoryScreen from './components/InventoryScreen';
import ReportsScreen from './components/ReportsScreen';
import NotesScreen from './components/NotesScreen';
import ScannerScreen from './components/ScannerScreen';
import ThermalReceiptModal from './components/ThermalReceiptModal';
import BackupModal from './components/BackupModal';
import { getStoredData, saveStoredData } from './storage';

export default function App() {
  const [data, setData] = useState(() => getStoredData());
  const [currentScreen, setCurrentScreen] = useState('home');
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [receiptToPreview, setReceiptToPreview] = useState(null);
  const [showBackupModal, setShowBackupModal] = useState(false);
  const [toast, setToast] = useState(null);

  const showToast = (message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast(null);
    }, 3200);
  };

  // Sync to storage on data change
  useEffect(() => {
    saveStoredData(data);
  }, [data]);

  // Invoice Handlers
  const handleSaveInvoice = (newInvoice) => {
    setData((prev) => {
      let updatedCustomers = [...prev.customers];
      let updatedTransactions = [...prev.transactions];
      let customerId = newInvoice.customerId;

      // If customer name was typed and doesn't exist, create customer
      if (!customerId && newInvoice.customerName && newInvoice.customerName !== 'عميل نقدي') {
        const existing = updatedCustomers.find(
          (c) => c.name.trim().toLowerCase() === newInvoice.customerName.trim().toLowerCase()
        );
        if (existing) {
          customerId = existing.id;
        } else {
          customerId = 'c_' + Date.now();
          updatedCustomers.push({
            id: customerId,
            name: newInvoice.customerName,
            phone: newInvoice.customerPhone || '',
            balance: 0,
            notes: ''
          });
        }
      }

      // If customer exists, update balance and add transactions
      if (customerId) {
        const custIdx = updatedCustomers.findIndex((c) => c.id === customerId);
        if (custIdx !== -1) {
          const currentBal = updatedCustomers[custIdx].balance || 0;
          let newBal = currentBal;

          // 1. Debit invoice total (adds to debt)
          if (newInvoice.total > 0) {
            newBal += newInvoice.total;
            updatedTransactions.unshift({
              id: 'tx_' + Date.now() + '_inv',
              customerId: customerId,
              customerName: updatedCustomers[custIdx].name,
              type: 'debit',
              amount: newInvoice.total,
              details: `فاتورة مبيعات رقم #${newInvoice.number}`,
              date: newInvoice.date,
              balanceAfter: newBal
            });
          }

          // 2. Credit paid amount (reduces debt)
          if (newInvoice.paid > 0) {
            newBal -= newInvoice.paid;
            updatedTransactions.unshift({
              id: 'tx_' + Date.now() + '_pay',
              customerId: customerId,
              customerName: updatedCustomers[custIdx].name,
              type: 'credit',
              amount: newInvoice.paid,
              details: `دفعة سداد فاتورة رقم #${newInvoice.number}`,
              date: newInvoice.date,
              balanceAfter: newBal
            });
          }

          updatedCustomers[custIdx] = {
            ...updatedCustomers[custIdx],
            balance: newBal
          };
        }
      }

      // Decrement inventory stock
      let updatedInventory = [...prev.inventory];
      if (newInvoice.items) {
        newInvoice.items.forEach((item) => {
          const invIdx = updatedInventory.findIndex(
            (p) => p.name.trim().toLowerCase() === item.name.trim().toLowerCase()
          );
          if (invIdx !== -1) {
            updatedInventory[invIdx] = {
              ...updatedInventory[invIdx],
              stock: Math.max(0, (updatedInventory[invIdx].stock || 0) - (item.qty || 1))
            };
          }
        });
      }

      return {
        ...prev,
        invoices: [newInvoice, ...prev.invoices],
        customers: updatedCustomers,
        transactions: updatedTransactions,
        inventory: updatedInventory
      };
    });
  };

  const handleDeleteInvoice = (invoiceId) => {
    setData((prev) => {
      const invoice = prev.invoices.find((i) => i.id === invoiceId);
      if (!invoice) return prev;

      // Revert the invoice's stock impact before deleting it.
      let updatedInventory = [...prev.inventory];
      (invoice.items || []).forEach((soldItem) => {
        const idx = updatedInventory.findIndex(
          (p) => p.name?.trim().toLowerCase() === soldItem.name?.trim().toLowerCase()
        );
        if (idx !== -1) {
          updatedInventory[idx] = {
            ...updatedInventory[idx],
            stock: (updatedInventory[idx].stock || 0) + (soldItem.qty || 0)
          };
        }
      });

      // Reverse only the two automatic account entries belonging to this invoice.
      const invoiceMarker = String(invoice.number);
      const autoTx = prev.transactions.filter((tx) => {
        const details = String(tx.details || '');
        return details === `فاتورة مبيعات رقم #${invoiceMarker}` ||
          details === `دفعة سداد فاتورة رقم #${invoiceMarker}`;
      });

      let updatedCustomers = [...prev.customers];
      autoTx.forEach((tx) => {
        const idx = updatedCustomers.findIndex((c) => c.id === tx.customerId);
        if (idx === -1) return;
        const delta = tx.type === 'debit' ? -Number(tx.amount || 0) : Number(tx.amount || 0);
        updatedCustomers[idx] = {
          ...updatedCustomers[idx],
          balance: (updatedCustomers[idx].balance || 0) + delta
        };
      });

      return {
        ...prev,
        invoices: prev.invoices.filter((i) => i.id !== invoiceId),
        inventory: updatedInventory,
        customers: updatedCustomers,
        transactions: prev.transactions.filter((tx) => !autoTx.some((x) => x.id === tx.id))
      };
    });
  };

  // Customer Handlers
  const handleAddCustomer = (cust) => {
    const newCust = {
      id: 'c_' + Date.now(),
      ...cust
    };
    setData((prev) => ({
      ...prev,
      customers: [newCust, ...prev.customers]
    }));
  };

  const handleUpdateCustomer = (cust) => {
    setData((prev) => ({
      ...prev,
      customers: prev.customers.map((c) => (c.id === cust.id ? cust : c))
    }));
    if (selectedCustomer && selectedCustomer.id === cust.id) {
      setSelectedCustomer(cust);
    }
  };

  const handleDeleteCustomer = (customerId) => {
    setData((prev) => ({
      ...prev,
      customers: prev.customers.filter((c) => c.id !== customerId),
      transactions: prev.transactions.filter((tx) => tx.customerId !== customerId)
    }));
    if (selectedCustomer && selectedCustomer.id === customerId) {
      setSelectedCustomer(null);
    }
  };

  const handleAddTransaction = (tx) => {
    setData((prev) => {
      const custIdx = prev.customers.findIndex((c) => c.id === tx.customerId);
      if (custIdx === -1) return prev;

      const cust = prev.customers[custIdx];
      const delta = tx.type === 'debit' ? tx.amount : -tx.amount;
      const newBal = (cust.balance || 0) + delta;

      const updatedCust = {
        ...cust,
        balance: newBal
      };

      const newTx = {
        id: 'tx_' + Date.now(),
        ...tx,
        balanceAfter: newBal
      };

      const newCustomers = [...prev.customers];
      newCustomers[custIdx] = updatedCust;

      if (selectedCustomer && selectedCustomer.id === tx.customerId) {
        setSelectedCustomer(updatedCust);
      }

      return {
        ...prev,
        customers: newCustomers,
        transactions: [newTx, ...prev.transactions]
      };
    });
  };

  const handleDeleteTransaction = (txId) => {
    setData((prev) => {
      const tx = prev.transactions.find((t) => t.id === txId);
      if (!tx) return prev;

      const custIdx = prev.customers.findIndex((c) => c.id === tx.customerId);
      let newCustomers = [...prev.customers];
      if (custIdx !== -1) {
        const cust = prev.customers[custIdx];
        const reverseDelta = tx.type === 'debit' ? -tx.amount : tx.amount;
        const newBal = (cust.balance || 0) + reverseDelta;
        newCustomers[custIdx] = { ...cust, balance: newBal };
        if (selectedCustomer && selectedCustomer.id === tx.customerId) {
          setSelectedCustomer(newCustomers[custIdx]);
        }
      }

      return {
        ...prev,
        customers: newCustomers,
        transactions: prev.transactions.filter((t) => t.id !== txId)
      };
    });
  };

  // Purchase Handlers
  const handleAddPurchase = (purchase) => {
    setData((prev) => {
      let updatedInventory = [...prev.inventory];
      if (purchase.items) {
        purchase.items.forEach((pItem) => {
          const invIdx = updatedInventory.findIndex(
            (i) => i.name.trim().toLowerCase() === pItem.name.trim().toLowerCase()
          );
          if (invIdx !== -1) {
            updatedInventory[invIdx] = {
              ...updatedInventory[invIdx],
              stock: (updatedInventory[invIdx].stock || 0) + (pItem.qty || 0),
              purchasePrice: pItem.cost || updatedInventory[invIdx].purchasePrice
            };
          } else {
            updatedInventory.push({
              id: 'item_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
              name: pItem.name,
              barcode: '',
              purchasePrice: pItem.cost,
              sellPrice: Math.round(pItem.cost * 1.15),
              stock: pItem.qty,
              minStock: 5,
              category: 'مواد أساسية'
            });
          }
        });
      }

      return {
        ...prev,
        purchases: [purchase, ...prev.purchases],
        inventory: updatedInventory
      };
    });
  };

  // Inventory Handlers
  const handleAddProduct = (item) => {
    setData((prev) => ({
      ...prev,
      inventory: [item, ...prev.inventory]
    }));
  };

  const handleUpdateProduct = (item) => {
    setData((prev) => ({
      ...prev,
      inventory: prev.inventory.map((i) => (i.id === item.id ? item : i))
    }));
  };

  const handleDeleteProduct = (itemId) => {
    setData((prev) => ({
      ...prev,
      inventory: prev.inventory.filter((i) => i.id !== itemId)
    }));
  };

  // Notes Handlers
  const handleAddNote = (note) => {
    setData((prev) => ({
      ...prev,
      notes: [note, ...prev.notes]
    }));
  };

  const handleDeleteNote = (noteId) => {
    setData((prev) => ({
      ...prev,
      notes: prev.notes.filter((n) => n.id !== noteId)
    }));
  };

  const handleTogglePinNote = (noteId) => {
    setData((prev) => ({
      ...prev,
      notes: prev.notes.map((n) => (n.id === noteId ? { ...n, isPinned: !n.isPinned } : n))
    }));
  };

  // Scanned Invoices (invoices_images) Handlers
  const handleSaveScannedInvoice = (scannedDoc) => {
    setData((prev) => ({
      ...prev,
      scannedInvoices: [scannedDoc, ...(prev.scannedInvoices || [])]
    }));
  };

  const handleDeleteScannedInvoice = (docId) => {
    setData((prev) => ({
      ...prev,
      scannedInvoices: (prev.scannedInvoices || []).filter((d) => d.id !== docId)
    }));
  };

  // Store Settings & Backup
  const handleUpdateStoreInfo = (info) => {
    setData((prev) => ({
      ...prev,
      storeInfo: { ...prev.storeInfo, ...info }
    }));
  };

  const handleRestoreData = (newData) => {
    setData(newData);
  };

  return (
    <div className="app-container">
      {/* Dynamic Floating Toast Notification */}
      {toast && (
        <div className={`toast-notification ${toast.type}`}>
          <span>{toast.message}</span>
        </div>
      )}

      <Header
        storeInfo={data.storeInfo || { name: 'بقالة العنزي', phone: '776425052' }}
        onOpenBackup={() => setShowBackupModal(true)}
        onNewInvoice={() => {
          setSelectedCustomer(null);
          setCurrentScreen('invoice');
        }}
        onGoHome={() => {
          setSelectedCustomer(null);
          setCurrentScreen('home');
        }}
      />

      <BottomNav
        currentScreen={currentScreen}
        data={data}
        setScreen={(screen) => {
          setSelectedCustomer(null);
          setCurrentScreen(screen);
        }}
      />

      <main className="main-content">
        {currentScreen === 'home' && (
          <HomeScreen
            data={data}
            setScreen={setCurrentScreen}
            onSelectCustomer={(c) => {
              setSelectedCustomer(c);
              setCurrentScreen('customers');
            }}
            onOpenReceipt={(inv) => setReceiptToPreview(inv)}
            showToast={showToast}
          />
        )}

        {currentScreen === 'scanner' && (
          <ScannerScreen
            data={data}
            onSaveScannedInvoice={handleSaveScannedInvoice}
            onDeleteScannedInvoice={handleDeleteScannedInvoice}
            showToast={showToast}
          />
        )}

        {currentScreen === 'invoice' && (
          <SalesInvoiceScreen
            data={data}
            onSaveInvoice={handleSaveInvoice}
            onOpenReceipt={(inv) => setReceiptToPreview(inv)}
            showToast={showToast}
          />
        )}

        {currentScreen === 'invoices_history' && (
          <InvoicesHistoryScreen
            data={data}
            onDeleteInvoice={handleDeleteInvoice}
            onOpenReceipt={(inv) => setReceiptToPreview(inv)}
            showToast={showToast}
          />
        )}

        {currentScreen === 'customers' && (
          <CustomersScreen
            data={data}
            selectedCustomer={selectedCustomer}
            onSelectCustomer={setSelectedCustomer}
            onAddCustomer={handleAddCustomer}
            onUpdateCustomer={handleUpdateCustomer}
            onDeleteCustomer={handleDeleteCustomer}
            onAddTransaction={handleAddTransaction}
            onDeleteTransaction={handleDeleteTransaction}
            storeInfo={data.storeInfo}
            showToast={showToast}
          />
        )}

        {currentScreen === 'purchases' && (
          <PurchasesScreen
            data={data}
            onAddPurchase={handleAddPurchase}
            inventory={data.inventory}
            showToast={showToast}
          />
        )}

        {currentScreen === 'inventory' && (
          <InventoryScreen
            data={data}
            onAddProduct={handleAddProduct}
            onUpdateProduct={handleUpdateProduct}
            onDeleteProduct={handleDeleteProduct}
            showToast={showToast}
          />
        )}

        {currentScreen === 'reports' && (
          <ReportsScreen
            data={data}
            storeInfo={data.storeInfo}
            showToast={showToast}
          />
        )}

        {currentScreen === 'notes' && (
          <NotesScreen
            data={data}
            onAddNote={handleAddNote}
            onDeleteNote={handleDeleteNote}
            onTogglePin={handleTogglePinNote}
            showToast={showToast}
          />
        )}
      </main>

      {/* Modals */}
      {receiptToPreview && (
        <ThermalReceiptModal
          receipt={receiptToPreview}
          storeInfo={data.storeInfo}
          onClose={() => setReceiptToPreview(null)}
          showToast={showToast}
        />
      )}

      {showBackupModal && (
        <BackupModal
          data={data}
          onRestoreData={handleRestoreData}
          onUpdateStoreInfo={handleUpdateStoreInfo}
          onClose={() => setShowBackupModal(false)}
          showToast={showToast}
        />
      )}
    </div>
  );
}
