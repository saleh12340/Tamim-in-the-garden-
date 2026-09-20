const STORAGE_KEY = 'enezi_grocery_v2';
const DRAFT_KEY = 'enezi_invoice_draft';

const initialStoreInfo = {
  name: 'بقالة العنزي للمواد الغذائية',
  phone: '776425052',
  subtitle: 'نظام الفواتير والحسابات والمخزون',
  address: 'اليمن',
  printerWidth: '58mm',
  currency: 'ريال'
};

const initialInventory = [
  { id: '1', name: 'أرز الشعلان 10 كجم', barcode: '6281001', purchasePrice: 42000, sellPrice: 45000, stock: 15, minStock: 5, category: 'حبوب' },
  { id: '2', name: 'سمن شوكة وملعقة 1.8 كجم', barcode: '6281002', purchasePrice: 18500, sellPrice: 20000, stock: 22, minStock: 8, category: 'زيوت وسمن' },
  { id: '3', name: 'سكر الأسرة 10 كجم', barcode: '6281003', purchasePrice: 28000, sellPrice: 30000, stock: 12, minStock: 4, category: 'مواد أساسية' },
  { id: '4', name: 'زيت صافيه 1.5 لتر', barcode: '6281004', purchasePrice: 7500, sellPrice: 8500, stock: 30, minStock: 10, category: 'زيوت وسمن' },
  { id: '5', name: 'حليب الممتاز مبخر', barcode: '6281005', purchasePrice: 2200, sellPrice: 2500, stock: 48, minStock: 12, category: 'ألبان' },
  { id: '6', name: 'تونة ريم فاخر', barcode: '6281006', purchasePrice: 3800, sellPrice: 4200, stock: 25, minStock: 6, category: 'معلبات' },
  { id: '7', name: 'شاي الكبوس أصلي 227جم', barcode: '6281007', purchasePrice: 4800, sellPrice: 5300, stock: 20, minStock: 5, category: 'مشروبات' },
  { id: '8', name: 'دقيق السعيد 25 كجم', barcode: '6281008', purchasePrice: 40000, sellPrice: 43000, stock: 8, minStock: 3, category: 'مواد أساسية' }
];

const initialCustomers = [
  { id: 'c1', name: 'محمد صالح العزي', phone: '771234567', balance: 14500, notes: 'عميل دائم' },
  { id: 'c2', name: 'أحمد ناصر الشمري', phone: '772345678', balance: 0, notes: 'سداد نقدي منتظم' },
  { id: 'c3', name: 'عبدالله علي القحطاني', phone: '773456789', balance: 28000, notes: 'حساب شهري' },
  { id: 'c4', name: 'سالم بن مهد', phone: '774567890', balance: 5200, notes: 'حي الروضة' }
];

const initialTransactions = [
  { id: 'tx1', customerId: 'c1', customerName: 'محمد صالح العزي', type: 'debit', amount: 14500, details: 'فاتورة مبيعات رقم 1001', date: '2026-09-18T10:30:00Z', balanceAfter: 14500 },
  { id: 'tx2', customerId: 'c3', customerName: 'عبدالله علي القحطاني', type: 'debit', amount: 38000, details: 'فاتورة مبيعات رقم 1002', date: '2026-09-19T14:15:00Z', balanceAfter: 38000 },
  { id: 'tx3', customerId: 'c3', customerName: 'عبدالله علي القحطاني', type: 'credit', amount: 10000, details: 'سداد دفعة نقدية', date: '2026-09-19T18:00:00Z', balanceAfter: 28000 }
];

const initialInvoices = [
  {
    id: 'inv1001',
    number: 1001,
    customerId: 'c1',
    customerName: 'محمد صالح العزي',
    date: '2026-09-18T10:30:00Z',
    items: [
      { name: 'سمن شوكة وملعقة 1.8 كجم', qty: 1, price: 20000, total: 20000 },
      { name: 'حليب الممتاز مبخر', qty: 2, price: 2500, total: 5000 }
    ],
    total: 25000,
    paid: 10500,
    remaining: 14500,
    paymentType: 'credit',
    notes: 'باقي الحساب آجل'
  },
  {
    id: 'inv1002',
    number: 1002,
    customerId: 'c3',
    customerName: 'عبدالله علي القحطاني',
    date: '2026-09-19T14:15:00Z',
    items: [
      { name: 'أرز الشعلان 10 كجم', qty: 1, price: 45000, total: 45000 }
    ],
    total: 45000,
    paid: 7000,
    remaining: 38000,
    paymentType: 'credit',
    notes: ''
  }
];

const initialPurchases = [
  {
    id: 'p1',
    number: 501,
    supplierName: 'شركة السعيد التجارية',
    date: '2026-09-15T09:00:00Z',
    items: [
      { name: 'دقيق السعيد 25 كجم', qty: 10, cost: 40000, total: 400000 }
    ],
    total: 400000,
    paid: 400000,
    status: 'paid'
  }
];

const initialNotes = [
  {
    id: 'n1',
    title: 'طلبية مورد الزيوت',
    content: 'طلب 20 كرتون زيت صافيه وسمن شوكة وملعقة يوم الأربعاء القادم.',
    date: '2026-09-19T12:00:00Z',
    isPinned: true
  },
  {
    id: 'n2',
    title: 'أرقام سريعة للموردين',
    content: 'مورد الألبان: 770112233\nمورد الدقيق والمواد الغذائية: 775544332',
    date: '2026-09-17T08:00:00Z',
    isPinned: false
  }
];

export function getStoredData() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return {
        storeInfo: initialStoreInfo,
        invoices: initialInvoices,
        customers: initialCustomers,
        transactions: initialTransactions,
        purchases: initialPurchases,
        inventory: initialInventory,
        notes: initialNotes
      };
    }
    const parsed = JSON.parse(raw);
    return {
      storeInfo: { ...initialStoreInfo, ...(parsed.storeInfo || {}) },
      invoices: parsed.invoices || [],
      customers: parsed.customers || [],
      transactions: parsed.transactions || [],
      purchases: parsed.purchases || [],
      inventory: parsed.inventory || initialInventory,
      notes: parsed.notes || []
    };
  } catch (e) {
    console.error('Error loading data', e);
    return {
      storeInfo: initialStoreInfo,
      invoices: initialInvoices,
      customers: initialCustomers,
      transactions: initialTransactions,
      purchases: initialPurchases,
      inventory: initialInventory,
      notes: initialNotes
    };
  }
}

export function saveStoredData(data) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  } catch (e) {
    console.error('Error saving data', e);
  }
}

export function getInvoiceDraft() {
  try {
    const d = localStorage.getItem(DRAFT_KEY);
    return d ? JSON.parse(d) : null;
  } catch (e) {
    return null;
  }
}

export function saveInvoiceDraft(draft) {
  try {
    localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
  } catch (e) {}
}

export function clearInvoiceDraft() {
  try {
    localStorage.removeItem(DRAFT_KEY);
  } catch (e) {}
}

export function formatNumber(n) {
  if (n === null || n === undefined || isNaN(n)) return '0';
  return Number(n).toLocaleString('ar-EG');
}

export function formatDate(dateString) {
  if (!dateString) return '';
  try {
    const d = new Date(dateString);
    return d.toLocaleString('ar-EG', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch (e) {
    return dateString;
  }
}
